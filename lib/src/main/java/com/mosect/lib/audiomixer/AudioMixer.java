package com.mosect.lib.audiomixer;

public class AudioMixer {

    static {
        System.loadLibrary("mosect_audio_mixer");
    }

    private final AudioBuffer outputBuffer;

    public AudioMixer(int sampleRate, int channelCount, int timeLength, PcmType pcmType) {
        outputBuffer = new AudioBuffer(sampleRate, channelCount, timeLength, pcmType);
    }

    public AudioBuffer getOutputBuffer() {
        return outputBuffer;
    }

    public AudioBuffer createInput(int sampleRate, int channelCount, PcmType pcmType) {
        return new AudioBuffer(sampleRate, channelCount, outputBuffer.getTimeLength(), pcmType);
    }

    public void mix(AudioBuffer[] inputs) {
        int status = mixBuffer(inputs, outputBuffer);
        if (status != 0) {
            throw new IllegalStateException("Invalid mix status: " + status);
        }
    }

    private native int mixBuffer(AudioBuffer[] inputs, AudioBuffer output);
}
