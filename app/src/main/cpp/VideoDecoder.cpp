//
// Created by Stapler on 2021/10/15.
//

#include "VideoDecoder.h"

VideoDecoder::VideoDecoder(JNIEnv *jniEnv, jstring path) {
    Init(jniEnv,path);
}


void VideoDecoder::Encode(const uint8_t** yuv_data, int width, int height) {
    LOGI(TAG,"Encode data .....");
    sPicture->uiTimeStamp = timestamp_++;
    int y_size = sPicture->iPicWidth * sPicture->iPicHeight;
    memcpy(sPicture->pData[0] + 0,				yuv_data[0], y_size);
    memcpy(sPicture->pData[0] + y_size,			yuv_data[1], y_size / 4);
    memcpy(sPicture->pData[0] + y_size * 5 / 4, yuv_data[2], y_size / 4);
    SFrameBSInfo encoded_frame_info;
    int err = ppEncoder->EncodeFrame(sPicture, &encoded_frame_info);
    if (err) {
        LOGE(TAG, "Encode err err=%d", err);
        return;
    }
    if (encoded_frame_info.eFrameType != videoFrameTypeSkip) {
        for (int iLayer=0; iLayer < encoded_frame_info.iLayerNum; iLayer++)
        {
            SLayerBSInfo* pLayerBsInfo = &encoded_frame_info.sLayerInfo[iLayer];

            int iLayerSize = 0;
            int iNalIdx = pLayerBsInfo->iNalCount - 1;
            do {
                iLayerSize += pLayerBsInfo->pNalLengthInByte[iNalIdx];
                --iNalIdx;
            } while (iNalIdx >= 0);

            unsigned char *outBuf = pLayerBsInfo->pBsBuf;
            //TODO 写入文件
            FILE  *file = NULL;
            file = fopen(m_path,"a");
            if(file == NULL){        //容错
                LOGD("文件创建失败%s","222");
            }
            fputs(reinterpret_cast<const char *>(outBuf), file);
            fclose(file);
//            outFi.write((char *)outBuf, iLayerSize);
        }
    }
}


void VideoDecoder::Init(JNIEnv *jniEnv, jstring path) {
    LOGI(TAG,"VideoDecoder Init start")
    jobject m_path_ref = jniEnv->NewGlobalRef(path);
    m_path = jniEnv->GetStringUTFChars(path, NULL);
    jniEnv->GetJavaVM(&m_jvm_for_thread);
    int result = WelsCreateSVCEncoder(&ppEncoder);
    if (result != cmResultSuccess) {
        LOGE(TAG,"WelsCreateSVCEncoder fail ! result=%d",result)
        return;
    }
    //获取默认参数
    ppEncoder->GetDefaultParams(&paramExt);
    ECOMPLEXITY_MODE complexityMode = HIGH_COMPLEXITY;
    RC_MODES rc_mode = RC_BITRATE_MODE;
    bool bEnableAdaptiveQuant = false;
    //TODO 这里暂时写死,需要外面对应预览分辨率也设置成一样
    paramExt.iPicWidth = 480;
    paramExt.iPicHeight = 960;
    paramExt.iTargetBitrate = 150000;
    paramExt.iMaxBitrate = 250000;
    paramExt.bEnableAdaptiveQuant = bEnableAdaptiveQuant;
    paramExt.iRCMode = rc_mode;
    paramExt.fMaxFrameRate = 25;
    paramExt.iComplexityMode = complexityMode;
    paramExt.iNumRefFrame = -1;
    paramExt.bEnableFrameSkip = false;
//    paramExt.eSpsPpsIdStrategy  ;
    paramExt.iEntropyCodingModeFlag = 0;
    paramExt.bEnableSSEI = true;
    paramExt.bEnableSceneChangeDetect  = true;
    ppEncoder->InitializeExt(&paramExt);
// 初始化图片对象
    memset(sPicture, 0, sizeof(SSourcePicture));
    sPicture->iPicWidth = 480;
    sPicture->iPicHeight = 960;
    sPicture->iColorFormat = videoFormatI420;
    sPicture->iStride[0] = sPicture->iPicWidth;
    sPicture->iStride[1] = sPicture->iStride[2] = sPicture->iPicWidth >> 1;
    //yuvData = CFDataCreateMutable(kCFAllocatorDefault, inputSize.width * inputSize.height * 3 >> 1);
    picture_buffer_ = new uint8_t[sPicture->iPicWidth * sPicture->iPicHeight * 3 >> 1];
    sPicture->pData[0] = (unsigned char*)picture_buffer_;//CFDataGetMutableBytePtr(yuvData);
    sPicture->pData[1] = sPicture->pData[0] + sPicture->iPicWidth * sPicture->iPicHeight;
    sPicture->pData[2] = sPicture->pData[1] + (sPicture->iPicWidth * sPicture->iPicHeight >> 2);
    LOGI(TAG,"VideoDecoder Init end")
}


VideoDecoder::~VideoDecoder() {
    //TODO
    if (ppEncoder) {
        ppEncoder->Uninitialize();
        WelsDestroySVCEncoder (ppEncoder);
    }
}

