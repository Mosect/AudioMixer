package com.mosect.lib.audiomixer;

import java.nio.ByteBuffer;

public class AudioBuffer {

    private final static int SECOND_LENGTH = 1000000;

    private final int sampleRate;
    private final int channelCount;
    private final int timeLength;
    private final int sampleCount;
    private final int bufferLength;
    private final ByteBuffer buffer;

    AudioBuffer(int sampleRate, int channelCount, int timeLength, PcmType pcmType) {
        if (sampleRate <= 0 || sampleRate >= 999999)
            throw new IllegalArgumentException("Unsupported sampleRate: " + sampleRate);
        if (channelCount <= 0 || channelCount > 10)
            throw new IllegalArgumentException("Unsupported channelCount: " + channelCount);
        if (timeLength <= 0 || timeLength >= 10 * SECOND_LENGTH)
            throw new IllegalArgumentException("Unsupported timeLength: " + timeLength);
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.timeLength = timeLength;
        // 计算采样次数
        sampleCount = (int) ((float) SECOND_LENGTH / sampleRate * timeLength);
        // 计算buffer长度
        int byteCount;
        switch (pcmType) {
            case BIT8:
                byteCount = 1;
                break;
            case BIT16:
                byteCount = 2;
                break;
            default:
                throw new IllegalArgumentException("Unsupported pcmType: " + pcmType);
        }
        bufferLength = sampleCount * channelCount * byteCount;
        buffer = ByteBuffer.allocateDirect(bufferLength);
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getTimeLength() {
        return timeLength;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public int getBufferLength() {
        return bufferLength;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void clear() {
        clearBuffer(buffer);
    }

    private static native void clearBuffer(ByteBuffer buffer);
}
