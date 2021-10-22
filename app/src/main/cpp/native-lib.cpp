#include <jni.h>
#include <string>
#include "VideoEncoder.h"
extern "C" JNIEXPORT jstring JNICALL
Java_com_stapler_openh264demo_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_stapler_openh264demo_H264Encoder_createEncoder(JNIEnv *env, jobject thiz, jint width,
                                                        jint height, jstring output_path) {
    VideoEncoder *decoder = new VideoEncoder(env, output_path);
    return (jlong)decoder;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_stapler_openh264demo_H264Encoder_encode(JNIEnv *env, jobject thiz, jlong p_encoder,
                                                 jbyteArray data, jint width, jint height) {
    if (0 != p_encoder) {
        VideoEncoder *decoder = (VideoEncoder *)p_encoder;
        jbyte *bytes = env->GetByteArrayElements(data, 0);
        int arrayLength = env->GetArrayLength(data);
        char *chars = new char[arrayLength + 1];
        memset(chars, 0x0, arrayLength + 1);
        memcpy(chars, bytes, arrayLength);
        chars[arrayLength] = 0;
        env->ReleaseByteArrayElements(data, bytes, 0);
        decoder->Encode(reinterpret_cast<uint8_t *>(chars), width, height);
    }
    return 0;
}
