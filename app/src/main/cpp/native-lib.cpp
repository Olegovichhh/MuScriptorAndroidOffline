#include <jni.h>
#include <string>
#include <vector>
#include "muscriptor/muscriptor.hpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_olegovichhh_muscriptor_engine_NativeBridge_engineVersion(JNIEnv* env, jobject) {
    return env->NewStringUTF("muscriptor.cpp/ggml JNI CPU");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_olegovichhh_muscriptor_engine_NativeBridge_transcribePcm16k(JNIEnv* env, jobject, jstring modelPath, jfloatArray pcm) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    const jsize count = env->GetArrayLength(pcm);
    std::vector<float> samples(static_cast<size_t>(count));
    env->GetFloatArrayRegion(pcm, 0, count, samples.data());
    auto transcriber = msl::Transcriber::load(path);
    env->ReleaseStringUTFChars(modelPath, path);
    if (!transcriber) {
        const std::string msg = msl::describe(transcriber.error());
        return env->NewStringUTF(msg.c_str());
    }
    auto result = transcriber->transcribe(samples);
    if (!result) {
        const std::string msg = msl::describe(result.error());
        return env->NewStringUTF(msg.c_str());
    }
    const std::string ok = "OK notes=" + std::to_string(result->size());
    return env->NewStringUTF(ok.c_str());
}
