package com.mosect.lib.audiomixer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 表示一段音频buffer
 */
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
    private final int bufferSize;

    /**
     * 创建音频buffer
     *
     * @param sampleRate   采样率
     * @param channelCount 声道数量
     * @param timeLength   时间长度，单位：微秒
     * @param pcmType      pcm格式类型
     */
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
        this.bufferSize = getNativeBufferSize(objId);
        this.buffer.order(ByteOrder.nativeOrder());
    }

    /**
     * 获取采样率
     *
     * @return 采样率
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * 获取声道数量
     *
     * @return 声道数量
     */
    public int getChannelCount() {
        return channelCount;
    }

    /**
     * 获取时间长度
     *
     * @return 时间长度，单位：微秒
     */
    public int getTimeLength() {
        return timeLength;
    }

    /**
     * 获取pcm格式类型
     *
     * @return pcm格式类型
     */
    public PcmType getPcmType() {
        return pcmType;
    }

    /**
     * 清空buffer数据
     */
    public void clear() {
        checkReleased();
        clearBuffer(objId);
    }

    /**
     * 释放buffer
     */
    public void release() {
        if (!released) {
            buffer = null;
            releaseBuffer(objId);
            released = true;
        }
    }

    /**
     * 向某个声道写入数据
     *
     * @param src        数据源buffer
     * @param srcChannel 源声道编号
     * @param dstChannel 目标声道编号
     */
    public void write(AudioBuffer src, int srcChannel, int dstChannel) {
        checkReleased();
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

    /**
     * 获取buffer对象
     *
     * @return buffer对象
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * 获取buffer的大小
     *
     * @return buffer大小
     */
    public int getBufferSize() {
        return bufferSize;
    }

    private void checkReleased() {
        if (released) {
            throw new IllegalStateException("Released");
        }
    }

    long getObjId() {
        checkReleased();
        return objId;
    }

    private static native int createBuffer(int sampleRate, int channelCount, int timeLength, int pcmType, long[] out);

    private static native void clearBuffer(long objId);

    private static native ByteBuffer getNativeBuffer(long objId);

    private static native int getNativeBufferSize(long objId);

    private static native void releaseBuffer(long objId);

    private static native void writeBuffer(long srcId, int srcChannel, long dstId, int dstChannel);

}
