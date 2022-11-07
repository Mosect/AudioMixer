//
// Created by Titdom on 2022/11/7.
//
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_mosect_lib_audiomixer_AudioMixer
 * Method:    mixBuffer
 * Signature: ([Lcom/mosect/lib/audiomixer/AudioBuffer;Lcom/mosect/lib/audiomixer/AudioBuffer;)I
 */
JNIEXPORT jint JNICALL Java_com_mosect_lib_audiomixer_AudioMixer_mixBuffer
        (JNIEnv *env, jobject thisObj, jobjectArray inputs, jobject output) {
    return 0;
}

#ifdef __cplusplus
}
#endif