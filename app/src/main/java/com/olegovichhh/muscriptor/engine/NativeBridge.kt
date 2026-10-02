package com.olegovichhh.muscriptor.engine
object NativeBridge {
    init { System.loadLibrary("muscriptor_android") }
    external fun engineVersion(): String
    external fun transcribePcm16k(modelPath: String, pcm: FloatArray): String
}
