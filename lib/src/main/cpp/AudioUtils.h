//
// Created by Titdom on 2022/11/8.
//
#include <stdint.h>

#ifndef AUDIO_MIXER_AUDIOUTILS_H
#define AUDIO_MIXER_AUDIOUTILS_H

typedef void (*AudioUtils_Converter)(const uint8_t *src, uint8_t *dst);

void AudioUtils_bit8ToBit8(const uint8_t *src, uint8_t *dst);

void AudioUtils_bit8ToBit16(const uint8_t *src, uint8_t *dst);

void AudioUtils_bit16ToBit8(const uint8_t *src, uint8_t *dst);

void AudioUtils_bit16ToBit16(const uint8_t *src, uint8_t *dst);

AudioUtils_Converter
AudioUtils_getConverter(const int32_t src_unit_size, const int32_t dst_unit_size);

#endif //AUDIO_MIXER_AUDIOUTILS_H
