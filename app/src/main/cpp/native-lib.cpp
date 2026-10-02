#include <jni.h>
#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>
#include "muscriptor/muscriptor.hpp"

static void put32(std::vector<uint8_t>& b, uint32_t v){ b.push_back(v>>24); b.push_back(v>>16); b.push_back(v>>8); b.push_back(v); }
static void put16(std::vector<uint8_t>& b, uint16_t v){ b.push_back(v>>8); b.push_back(v); }
static void varlen(std::vector<uint8_t>& b, uint32_t v){
    uint8_t tmp[5]; int n=0; tmp[n++]=v&0x7f; while((v>>=7)) tmp[n++]=0x80|(v&0x7f); while(n) b.push_back(tmp[--n]);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_olegovichhh_muscriptor_engine_NativeBridge_engineVersion(JNIEnv* env, jobject) {
    return env->NewStringUTF("muscriptor.cpp/ggml JNI CPU");
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_olegovichhh_muscriptor_engine_NativeBridge_transcribePcm16k(JNIEnv* env, jobject, jstring modelPath, jfloatArray pcm) {
    const char* path=env->GetStringUTFChars(modelPath,nullptr);
    jsize count=env->GetArrayLength(pcm);
    std::vector<float> samples((size_t)count);
    env->GetFloatArrayRegion(pcm,0,count,samples.data());
    auto transcriber=msl::Transcriber::load(path);
    env->ReleaseStringUTFChars(modelPath,path);
    if(!transcriber) return nullptr;
    auto result=transcriber->transcribe(samples);
    if(!result) return nullptr;

    struct E { uint32_t tick; bool on; int pitch; };
    std::vector<E> ev;
    constexpr double ticksPerSecond=960.0; // 120 BPM, PPQ 480
    for(const auto& n:*result){
        ev.push_back({(uint32_t)(n.onset*ticksPerSecond),true,n.pitch});
        ev.push_back({(uint32_t)(n.offset*ticksPerSecond),false,n.pitch});
    }
    std::sort(ev.begin(),ev.end(),[](const E&a,const E&b){ return a.tick==b.tick ? a.on<b.on : a.tick<b.tick; });
    std::vector<uint8_t> tr;
    uint32_t last=0;
    for(const auto&e:ev){
        varlen(tr,e.tick-last); last=e.tick;
        tr.push_back(e.on?0x90:0x80); tr.push_back((uint8_t)std::clamp(e.pitch,0,127)); tr.push_back(e.on?96:0);
    }
    tr.insert(tr.end(),{0x00,0xff,0x2f,0x00});
    std::vector<uint8_t> midi={'M','T','h','d'}; put32(midi,6); put16(midi,0); put16(midi,1); put16(midi,480);
    midi.insert(midi.end(),{'M','T','r','k'}); put32(midi,(uint32_t)tr.size()); midi.insert(midi.end(),tr.begin(),tr.end());
    jbyteArray out=env->NewByteArray((jsize)midi.size());
    env->SetByteArrayRegion(out,0,(jsize)midi.size(),reinterpret_cast<const jbyte*>(midi.data()));
    return out;
}
