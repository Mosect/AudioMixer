package com.mosect.lib.audiomixer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioBuffer {

    static {
        System.loadLibrary("mosect_audio_mixer");
    }


    private final static int ERROR_INVALID_SAMPLE_RATE = 1;
    private final static int ERROR_INVALID_CHANNEL_COUNT = 2;
    private final static int ERROR_INVALID_TIME_LENGTH = 3;
    private final static int ERROR_INVALID_PCM_TYPE = 4;
    private final static int ERROR_ALLOC_FAILED = 5;

    private final int sampleRate;
    private final int channelCount;
    private final int timeLength;
    private final PcmType pcmType;
    private final long objId;
    private boolean released = false;
    private ByteBuffer buffer;

    public AudioBuffer(int sampleRate, int channelCount, int timeLength, PcmType pcmType) {
        long[] ids = new long[1];
        int status = createBuffer(sampleRate, channelCount, timeLength, pcmType.getCode(), ids);
        if (status != 0) {
            String msg;
            switch (status) {
                case ERROR_INVALID_SAMPLE_RATE:
                    msg = "Invalid sampleRate: " + sampleRate;
                    break;
                case ERROR_INVALID_CHANNEL_COUNT:
                    msg = "Invalid channelCount: " + channelCount;
                    break;
                case ERROR_INVALID_TIME_LENGTH:
                    msg = "Invalid timeLength: " + timeLength;
                    break;
                case ERROR_INVALID_PCM_TYPE:
                    msg = "Invalid pcmType: " + pcmType;
                    break;
                case ERROR_ALLOC_FAILED:
                    msg = "Alloc failed";
                    break;
                default:
                    msg = "Unknown error: " + status;
                    break;
            }
            throw new IllegalArgumentException(msg);
        }
        this.objId = ids[0];
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.timeLength = timeLength;
        this.pcmType = pcmType;
        this.buffer = getNativeBuffer(objId);
        this.buffer.order(ByteOrder.nativeOrder());
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

    public PcmType getPcmType() {
        return pcmType;
    }

    public void clear() {
        if (released) {
            throw new IllegalStateException("Released");
        }
        clearBuffer(objId);
    }

    public void release() {
        if (!released) {
            buffer = null;
            releaseBuffer(objId);
            released = true;
        }
    }

    public void write(AudioBuffer src, int srcChannel, int dstChannel) {
        if (srcChannel < 0 || srcChannel >= src.getChannelCount()) {
            throw new IllegalArgumentException("Invalid srcChannel: " + srcChannel);
        }
        if (dstChannel < 0 || dstChannel >= getChannelCount()) {
            throw new IllegalArgumentException("Invalid dstChannel: " + dstChannel);
        }
        long srcId = src.getObjId();
        long dstId = getObjId();
        writeBuffer(srcId, srcChannel, dstId, dstChannel);
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    long getObjId() {
        if (released) {
            throw new IllegalStateException("Released");
        }
        return objId;
    }

    private static native int createBuffer(int sampleRate, int channelCount, int timeLength, int pcmType, long[] out);

    private static native void clearBuffer(long objId);

    private static native ByteBuffer getNativeBuffer(long objId);

    private static native void releaseBuffer(long objId);

    private static native void writeBuffer(long srcId, int srcChannel, long dstId, int dstChannel);

}
