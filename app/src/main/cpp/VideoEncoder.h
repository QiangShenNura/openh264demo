//
// Created by Stapler on 2021/10/15.
//

#ifndef OPENH264DEMO_VIDEOENCODER_H
#define OPENH264DEMO_VIDEOENCODER_H

#include <jni.h>
#include <string>
#include <thread>
#include <iostream>
#include <fstream>
#include "utils/logger.h"
#include "openh264/include/codec_api.h"
#include "libyuv/include/libyuv/rotate.h"

class VideoEncoder {
private:
    const char *TAG = "VideoDecoder";
    const char *m_path = NULL;
    JavaVM *m_jvm_for_thread = NULL;
    ISVCEncoder *ppEncoder = NULL;
    SEncParamExt paramExt;
    SSourcePicture sPicture = {0} ;
    SFrameBSInfo encoded_frame_info = {0};
    uint8_t* picture_buffer_;
    uint64_t encoded_frame_count_;
    uint64_t timestamp_;
    FILE *m_file = NULL;
    std::ofstream out264;

    uint8_t* src_y = NULL;
    uint8_t* src_u = NULL;
    uint8_t* src_v = NULL;
    uint8_t* dst_data = NULL;
    uint8_t* dst_y = NULL;
    uint8_t* dst_u = NULL;
    uint8_t* dst_v = NULL;
//    SEncParamExt encParam = NULL;
public:
    VideoEncoder(JNIEnv *jniEnv, jstring path);
    virtual ~VideoEncoder();

    void Init(JNIEnv *env, jstring path);

    void Encode(uint8_t* yuv_data, int width, int height);

};

#endif //OPENH264DEMO_VIDEOENCODER_H
