package com.mosect.lib.audiomixer;

import java.util.List;

public class AudioMixer {

    static {
        System.loadLibrary("mosect_audio_mixer");
    }

    private final static int MAX_MIX_COUNT = 100;

    private AudioBuffer outputBuffer;
    private boolean released = false;

    public AudioMixer(int sampleRate, int channelCount, int timeLength, PcmType pcmType) {
        outputBuffer = new AudioBuffer(sampleRate, channelCount, timeLength, pcmType);
    }

    public AudioBuffer getOutputBuffer() {
        return outputBuffer;
    }

    public AudioBuffer createInput(int sampleRate, int channelCount, PcmType pcmType) {
        return new AudioBuffer(sampleRate, channelCount, outputBuffer.getTimeLength(), pcmType);
    }

    public void mix(List<AudioBuffer> inputs) {
        if (inputs.isEmpty()) throw new IllegalArgumentException("Empty inputs");
        if (inputs.size() > MAX_MIX_COUNT) {
            throw new IllegalArgumentException("Too many inputs, max is " + MAX_MIX_COUNT);
        }

        long[] array = new long[inputs.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = inputs.get(i).getObjId();
        }
        int status = mixBuffer(array, outputBuffer.getObjId());
        if (status != 0) {
            throw new IllegalStateException("Invalid mix status: " + status);
        }
    }

    public void release() {
        if (!released) {
            outputBuffer.release();
            outputBuffer = null;
            released = true;
        }
    }

    private static native int mixBuffer(long[] inputs, long output);
}
