package com.stapler.openh264demo

/**
 * Created by Stapler on 2021/10/13.
 */

class H264Encoder {
    init {
        System.loadLibrary("native-lib")
    }

    external fun createEncoder(width: Int, height: Int, outputPath: String): Long
    external fun destroyEncoder()
    external fun encode(pEncoder: Long, data: ByteArray?, width: Int, height: Int): Long

}
