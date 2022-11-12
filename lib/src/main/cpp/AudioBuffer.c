//
// Created by Titdom on 2022/11/7.
//
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "AudioBuffer.h"
#include "AudioUtils.h"

#ifdef __cplusplus
extern "C" {
#endif

uint8_t *AudioBuffer_getSampleAddress(NativeBufferPtr ptr, jint channel, jint index) {
    return ptr->channel_buffer_list[channel] + ptr->channel_stride * index;
}

jint AudioBuffer_readValue(NativeBufferPtr ptr, jint channel, jint index) {
    short value;
    uint8_t *address = AudioBuffer_getSampleAddress(ptr, channel, index);
    ptr->read_converter(address, (uint8_t *) &value);
    return value;
}

void AudioBuffer_writeValue(NativeBufferPtr ptr, jint channel, jint index, jint value) {
    jshort safeValue = (jshort) value;
    uint8_t *address = AudioBuffer_getSampleAddress(ptr, channel, index);
    ptr->write_converter((uint8_t *) &safeValue, address);
}

JNIEXPORT jint JNICALL
Java_com_mosect_lib_audiomixer_AudioBuffer_createBuffer(JNIEnv *env, jclass clazz, jint sample_rate,
                                                        jint channel_count, jint time_length,
                                                        jint pcm_type, jlongArray out) {
    if (sample_rate <= 0 || sample_rate > 1000000) {
        return com_mosect_lib_audiomixer_AudioBuffer_ERROR_INVALID_SAMPLE_RATE;
    }
    if (channel_count <= 0 || channel_count > AudioBuffer_MAX_CHANNEL_COUNT) {
        return com_mosect_lib_audiomixer_AudioBuffer_ERROR_INVALID_CHANNEL_COUNT;
    }
    if (time_length < 500 || time_length > 10000000) {
        return com_mosect_lib_audiomixer_AudioBuffer_ERROR_INVALID_TIME_LENGTH;
    }
    jboolean help;
    jint unit_size;
    switch (pcm_type) {
        case 1:
            unit_size = 1;
            break;
        case 2:
            unit_size = 2;
            break;
        default:
            return com_mosect_lib_audiomixer_AudioBuffer_ERROR_INVALID_PCM_TYPE;
    }
    // 计算采样次数
    jint sample_count = (jint) ((jfloat) sample_rate / 1000000.0F * (jfloat) time_length);
    jint buffer_length = sample_count * channel_count * unit_size;
    uint8_t *address = malloc(buffer_length);
    if (NULL == address) {
        return com_mosect_lib_audiomixer_AudioBuffer_ERROR_ALLOC_FAILED;
    }
    // 创建buffer
    jobject buffer = (*env)->NewDirectByteBuffer(env, address, buffer_length);
    if (NULL == buffer) {
        free(address);
        return com_mosect_lib_audiomixer_AudioBuffer_ERROR_ALLOC_FAILED;
    }
    // 创建原生Buffer对象
    NativeBufferPtr buffer_ptr = malloc(sizeof(NativeBuffer));
    buffer_ptr->sample_rate = sample_rate;
    buffer_ptr->channel_count = channel_count;
    buffer_ptr->time_length = time_length;
    buffer_ptr->pcm_type = pcm_type;
    buffer_ptr->unit_size = unit_size;
    buffer_ptr->sample_count = sample_count;
    buffer_ptr->buffer = (*env)->NewGlobalRef(env, buffer);
    buffer_ptr->buffer_size = buffer_length;
    buffer_ptr->channel_stride = unit_size * channel_count;
    for (int i = 0; i < channel_count; ++i) {
        buffer_ptr->channel_buffer_list[i] = address + unit_size * i;
    }
    buffer_ptr->read_converter = AudioUtils_getConverter(unit_size, 2);
    buffer_ptr->write_converter = AudioUtils_getConverter(2, unit_size);

    jlong *out_address = (*env)->GetLongArrayElements(env, out, &help);
    *out_address = (jlong) buffer_ptr;
    (*env)->ReleaseLongArrayElements(env, out, out_address, 0);
    return 0;
}

JNIEXPORT void JNICALL
Java_com_mosect_lib_audiomixer_AudioBuffer_clearBuffer(JNIEnv *env, jclass clazz, jlong obj_id) {
    NativeBufferPtr buffer_ptr = (NativeBufferPtr) obj_id;
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer_ptr->buffer);
    void *address = (*env)->GetDirectBufferAddress(env, buffer_ptr->buffer);
    memset(address, 0, capacity);
}

JNIEXPORT jobject JNICALL
Java_com_mosect_lib_audiomixer_AudioBuffer_getNativeBuffer(JNIEnv *env, jclass clazz,
                                                           jlong obj_id) {
    NativeBufferPtr buffer_ptr = (NativeBufferPtr) obj_id;
    return buffer_ptr->buffer;
}

JNIEXPORT void JNICALL
Java_com_mosect_lib_audiomixer_AudioBuffer_releaseBuffer(JNIEnv *env, jclass clazz, jlong obj_id) {
    NativeBufferPtr buffer_ptr = (NativeBufferPtr) obj_id;
    // 释放内存地址
    void *address = (*env)->GetDirectBufferAddress(env, buffer_ptr->buffer);
    free(address);
    // 删除全局引用
    (*env)->DeleteGlobalRef(env, buffer_ptr->buffer);
    // 释放buffer本身
    free(buffer_ptr);
}

JNIEXPORT void JNICALL
Java_com_mosect_lib_audiomixer_AudioBuffer_writeBuffer(JNIEnv *env, jclass clazz, jlong src_id,
                                                       jint src_channel, jlong dst_id,
                                                       jint dst_channel) {
    NativeBufferPtr src_ptr = (NativeBufferPtr) src_id;
    NativeBufferPtr dst_ptr = (NativeBufferPtr) dst_id;
    jfloat scale = (jfloat) src_ptr->sample_count / (jfloat) dst_ptr->sample_count;
    AudioUtils_Converter converter = AudioUtils_getConverter(src_ptr->unit_size,
                                                             dst_ptr->unit_size);
    for (int i = 0; i < dst_ptr->sample_count; ++i) {
        jint src_index = (jint) ((jfloat) i * scale);
        uint8_t *src_address = AudioBuffer_getSampleAddress(src_ptr, src_channel, src_index);
        uint8_t *dst_address = AudioBuffer_getSampleAddress(dst_ptr, dst_channel, i);
        converter(src_address, dst_address);
    }
}

JNIEXPORT jint JNICALL
Java_com_mosect_lib_audiomixer_AudioBuffer_getNativeBufferSize(JNIEnv *env, jclass clazz,
                                                               jlong obj_id) {
    NativeBufferPtr buffer_ptr = (NativeBufferPtr) obj_id;
    return buffer_ptr->buffer_size;
}

#ifdef __cplusplus
}
#endif

