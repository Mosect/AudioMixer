package com.mosect.app.audiomixer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioPlayer {

    private static final String TAG = "AudioPlayer";

    private final Context context;
    private final String assetsName;
    private int state = 0;
    private OnPcmPlayListener onPcmPlayListener;

    public AudioPlayer(Context context, String assetsName) {
        this.context = context;
        this.assetsName = assetsName;
    }

    public void start() {
        if (state == 0) {
            state = 1;
            new Thread(this::loop).start();
        }
    }

    public void release() {
        if (state != 2) {
            state = 2;
        }
    }

    private void loop() {
        MediaCodec codec = null;
        try (AssetFileDescriptor afd = context.getAssets().openFd(assetsName)) {
            MediaExtractor extractor = new MediaExtractor();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                extractor.setDataSource(afd);
            } else {
                extractor.setDataSource(afd.getFileDescriptor());
            }
            int trackCount = extractor.getTrackCount();
            int trackIndex = -1;
            MediaFormat mediaFormat = null;
            String formatMime = null;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    mediaFormat = format;
                    trackIndex = i;
                    formatMime = mime;
                    break;
                }
            }
            if (trackIndex < 0) {
                throw new IOException("Audio track not found");
            }
            int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            Log.d(TAG, String.format("Audio { format=%s, sampleRate=%s, channelCount=%s }", formatMime, sampleRate, channelCount));

            extractor.selectTrack(trackIndex);
            codec = MediaCodec.createDecoderByType(formatMime);
            codec.configure(mediaFormat, null, null, 0);

            while (state == 1) {
                codec.start();
                ByteBuffer[] inputBuffers = codec.getInputBuffers();
                ByteBuffer[] outputBuffers = codec.getOutputBuffers();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                long lastFrameTime = -1;
                long lastSystemTime = -1;

                while (state == 1) {
                    int index = codec.dequeueInputBuffer(25000);
                    if (index >= 0) {
                        ByteBuffer buffer;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            buffer = codec.getInputBuffer(index);
                        } else {
                            buffer = inputBuffers[index];
                        }
                        buffer.clear();
                        int size = extractor.readSampleData(buffer, 0);
                        if (size >= 0) {
                            codec.queueInputBuffer(index, 0, size, extractor.getSampleTime(), 0);
                            extractor.advance();
                        } else {
                            codec.queueInputBuffer(index, 0, 0, 0, 0);
                            // 重复播放
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                            codec.stop();
                            break;
                        }
                    }

                    index = codec.dequeueOutputBuffer(bufferInfo, 25000);
                    if (index >= 0) {
                        ByteBuffer buffer;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            buffer = codec.getOutputBuffer(index);
                        } else {
                            buffer = outputBuffers[index];
                        }
                        boolean config = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
                        if (!config && bufferInfo.size > 0) {
                            long frameTime = bufferInfo.presentationTimeUs * 1000;
                            buffer.position(bufferInfo.offset).limit(bufferInfo.offset + bufferInfo.size);

                            if (lastFrameTime >= 0) {
                                long framePartTime = frameTime - lastFrameTime;
                                long systemPartTime = System.nanoTime() - lastSystemTime;
                                if (framePartTime > systemPartTime && systemPartTime >= 0) {
                                    long pauseTime = framePartTime - systemPartTime;
                                    long mills = pauseTime / 1000000;
                                    int nanos = (int) (pauseTime % 1000000);
                                    try {
                                        //noinspection BusyWait
                                        Thread.sleep(mills, nanos);
                                    } catch (InterruptedException ignored) {
                                    }
                                }
                            }
                            lastSystemTime = System.nanoTime();
                            lastFrameTime = frameTime;
                            OnPcmPlayListener listener = onPcmPlayListener;
                            if (null != listener) {
                                listener.onPcmPlay(buffer, bufferInfo.presentationTimeUs, bufferInfo.size);
                            }
                        }
                        codec.releaseOutputBuffer(index, false);
                    } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        outputBuffers = codec.getOutputBuffers();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loop: ", e);
        } finally {
            if (null != codec) codec.release();
        }
    }

    public void setOnPcmPlayListener(OnPcmPlayListener onPcmPlayListener) {
        this.onPcmPlayListener = onPcmPlayListener;
    }

    public interface OnPcmPlayListener {

        void onPcmPlay(ByteBuffer data, long frameTime, int size);
    }
}
