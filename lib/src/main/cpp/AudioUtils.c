//
// Created by Titdom on 2022/11/8.
//
#include <jni.h>
#include <stdint.h>
#include "AudioUtils.h"

void AudioUtils_bit8ToBit8(const uint8_t *src, uint8_t *dst) {
    *dst = *src;
}

void AudioUtils_bit8ToBit16(const uint8_t *src, uint8_t *dst) {
    jfloat value = *src;
    uint16_t fv = (uint16_t) (value / 0xFF * 0xFFFF);
    *((uint16_t *) dst) = fv;
}

void AudioUtils_bit16ToBit8(const uint8_t *src, uint8_t *dst) {
    jfloat value = *((const uint16_t *) src);
    uint8_t fv = (uint8_t) (value / 0xFFFF * 0xFF);
    *dst = fv;
}

void AudioUtils_bit16ToBit16(const uint8_t *src, uint8_t *dst) {
    *((uint16_t *) dst) = *((const uint16_t *) src);
}

AudioUtils_Converter
AudioUtils_getConverter(const int32_t src_unit_size, const int32_t dst_unit_size) {
    if (src_unit_size == 1) {
        if (dst_unit_size == 1) return AudioUtils_bit8ToBit8;
        if (dst_unit_size == 2) return AudioUtils_bit8ToBit16;
    } else if (src_unit_size == 2) {
        if (dst_unit_size == 1) return AudioUtils_bit16ToBit8;
        if (dst_unit_size == 2) return AudioUtils_bit16ToBit16;
    }
    return NULL;
}
