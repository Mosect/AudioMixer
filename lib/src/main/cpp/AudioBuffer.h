//
// Created by Titdom on 2022/11/8.
//

#include <jni.h>
#include "AudioUtils.h"

#ifndef AUDIO_MIXER_AUDIOBUFFER_H
#define AUDIO_MIXER_AUDIOBUFFER_H

#define com_mosect_lib_audiomixer_AudioBuffer_ERROR_INVALID_SAMPLE_RATE 1
#define com_mosect_lib_audiomixer_AudioBuffer_ERROR_INVALID_CHANNEL_COUNT 2
#define com_mosect_lib_audiomixer_AudioBuffer_ERROR_INVALID_TIME_LENGTH 3
#define com_mosect_lib_audiomixer_AudioBuffer_ERROR_INVALID_PCM_TYPE 4
#define com_mosect_lib_audiomixer_AudioBuffer_ERROR_ALLOC_FAILED 5

#define AudioBuffer_MAX_CHANNEL_COUNT 10

typedef struct {
    jint sample_rate;
    jint channel_count;
    jint time_length;
    jint pcm_type;
    jint unit_size;
    jint sample_count; // 采样次数
    jint channel_stride; // 通道数据步长，即采样间隔
    jobject buffer;
    uint8_t *channel_buffer_list[AudioBuffer_MAX_CHANNEL_COUNT]; // 通道buffer列表
    AudioUtils_Converter read_converter; // 读取转换器
    AudioUtils_Converter write_converter;
} NativeBuffer;
typedef NativeBuffer *NativeBufferPtr;

/**
 * 获取采样地址
 * @param ptr buffer对象
 * @param channel 通道
 * @param index 采样下标
 * @return 采样地址
 */
uint8_t *AudioBuffer_getSampleAddress(NativeBufferPtr ptr, jint channel, jint index);

/**
 * 读取采样值
 * @param ptr buffer对象
 * @param channel 通道
 * @param index 采样下标
 * @return 采样值
 */
jint AudioBuffer_readValue(NativeBufferPtr ptr, jint channel, jint index);

/**
 * 写入采样值
 * @param ptr buffer对象
 * @param channel 通道
 * @param index 采样下标
 * @param value 采样值
 */
void AudioBuffer_writeValue(NativeBufferPtr ptr, jint channel, jint index, jint value);

#endif //AUDIO_MIXER_AUDIOBUFFER_H
