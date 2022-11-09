//
// Created by Titdom on 2022/11/7.
//
#include <jni.h>
#include <stdint.h>
#include "AudioBuffer.h"

#define AudioMixer_MAX_MIX_COUNT 100
#define AudioMixer_SETPSIZE 32

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_com_mosect_lib_audiomixer_AudioMixer_mixBuffer(JNIEnv *env, jclass clazz, jlongArray inputs,
                                                    jlong output) {
    jboolean help;
    jlong input_count = (*env)->GetArrayLength(env, inputs);
    if (input_count > AudioMixer_MAX_MIX_COUNT) return 1;

    jlong *input_elements = (*env)->GetLongArrayElements(env, inputs, &help);
    NativeBufferPtr output_ptr = (NativeBufferPtr) output;
    NativeBufferPtr input_ptr_list[AudioMixer_MAX_MIX_COUNT];
    jfloat scale_list[AudioMixer_MAX_MIX_COUNT];
    for (int i = 0; i < input_count; ++i) {
        NativeBufferPtr ptr = (NativeBufferPtr) input_elements[i];
        input_ptr_list[i] = ptr;
        scale_list[i] = (jfloat) ptr->sample_count / (jfloat) output_ptr->sample_count;
    }
    (*env)->ReleaseLongArrayElements(env, inputs, input_elements, JNI_ABORT);

    // 采用归一化混合声音
    for (int channel = 0; channel < output_ptr->channel_count; ++channel) {
        jfloat F = 1.0f;
        for (int i = 0; i < output_ptr->sample_count; ++i) {
            jint value = 0;
            for (int input_index = 0; input_index < input_count; ++input_index) {
                jint src_index = (jint) (scale_list[input_index] * (jfloat) i);
                NativeBufferPtr input_ptr = input_ptr_list[input_index];
                if (channel < input_ptr->channel_count) {
                    // 采样
                    jint inputValue = AudioBuffer_readValue(input_ptr, channel, src_index);
                    // 叠加
                    value += inputValue;
                }
            }
            // 归一化处理
            value = (jint) ((jfloat) value * F);
            if (value > INT16_MAX) {
                F = (jfloat) INT16_MAX / (jfloat) value;
                value = INT16_MAX;
            } else if (value < INT16_MIN) {
                F = (jfloat) INT16_MIN / (jfloat) value;
                value = INT16_MIN;
            }
            if (F < 1) {
                F += (1.0f - F) / (jfloat) AudioMixer_SETPSIZE;
            }

            // 保存到输出
            AudioBuffer_writeValue(output_ptr, channel, i, value);
        }
    }
    return 0;
}

#ifdef __cplusplus
}
#endif

