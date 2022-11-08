cmake_minimum_required(VERSION 3.4.1)
add_library (
        mosect_audio_mixer
        SHARED
        src/main/cpp/AudioMixer.c
        src/main/cpp/AudioBuffer.c
        src/main/cpp/AudioUtils.c
)
