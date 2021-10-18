//
// Created by Stapler on 2021/10/15.
//

#ifndef OPENH264DEMO_VIDEODECODER_H
#define OPENH264DEMO_VIDEODECODER_H

#include <jni.h>
#include <string>
#include <thread>
#include "utils/logger.h"
#include "openh264/include/codec_api.h"

class VideoDecoder {
private:
    const char *TAG = "VideoDecoder";
    const char *m_path = NULL;
    JavaVM *m_jvm_for_thread = NULL;
    ISVCEncoder *ppEncoder = NULL;
    SEncParamExt paramExt;
    SSourcePicture *sPicture;
    uint8_t* picture_buffer_;
    uint64_t encoded_frame_count_;
    uint64_t timestamp_;
//    SEncParamExt encParam = NULL;
public:
    VideoDecoder(JNIEnv *jniEnv,jstring path);
    virtual ~VideoDecoder();

    void Init(JNIEnv *env, jstring path);

    //静态方法,用于编码线程回调
    void Encode(const uint8_t** yuv_data, int width, int height);

};

#endif //OPENH264DEMO_VIDEODECODER_H
