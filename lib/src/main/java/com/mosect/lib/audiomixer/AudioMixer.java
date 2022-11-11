package com.mosect.lib.audiomixer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AudioMixer {

    static {
        System.loadLibrary("mosect_audio_mixer");
    }

    private final static int MAX_MIX_COUNT = 100;

    private AudioBuffer outputBuffer;
    private boolean released = false;
    private final List<Track> tracks = new ArrayList<>();
    private final byte[] lock = new byte[0];
    private final long[] inputs = new long[MAX_MIX_COUNT];
    private boolean locked = false;

    public AudioMixer(int sampleRate, int channelCount, int timeLength, PcmType pcmType) {
        outputBuffer = new AudioBuffer(sampleRate, channelCount, timeLength, pcmType);
    }

    public AudioTrack requestTrack(int sampleRate, int channelCount, PcmType pcmType) {
        synchronized (lock) {
            if (released) return null;
            AudioBuffer buffer = new AudioBuffer(sampleRate, channelCount, outputBuffer.getTimeLength(), pcmType);
            Track track = new Track(buffer);
            tracks.add(track);
            return track;
        }
    }

    public void release() {
        List<Track> tracks = null;
        synchronized (lock) {
            if (!released) {
                outputBuffer.release();
                outputBuffer = null;
                tracks = new ArrayList<>(this.tracks);
                this.tracks.clear();
                released = true;
                lock.notifyAll();
            }
        }
        if (null != tracks) {
            for (Track track : tracks) {
                track.release();
            }
        }
    }

    private void flushTrack() {
        synchronized (lock) {
            if (isFlushOk()) {
                // 所有的轨道已经锁定
                lock.notifyAll();
            }
        }
    }

    private void removeTrack(Track track) {
        synchronized (lock) {
            tracks.remove(track);
        }
    }

    public ByteBuffer tick() {
        synchronized (lock) {
            if (!released && !tracks.isEmpty() && isFlushOk()) {
                mix();
                outputBuffer.getBuffer().position(0);
                locked = true;
                return outputBuffer.getBuffer();
            }
            return null;
        }
    }

    public ByteBuffer tickAndWait() {
        synchronized (lock) {
            if (tracks.isEmpty()) return null;
            while (!released) {
                if (isFlushOk()) {
                    break;
                }
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                }
            }
            if (!released) {
                mix();
                outputBuffer.getBuffer().position(0);
                locked = true;
                return outputBuffer.getBuffer();
            }
            return null;
        }
    }

    public void unlock() {
        List<Track> tracks = null;
        synchronized (lock) {
            if (!released && locked) {
                tracks = new ArrayList<>(this.tracks);
                locked = false;
            }
        }
        if (null != tracks) {
            for (Track track : tracks) {
                track.reset();
            }
        }
    }

    private boolean isFlushOk() {
        for (Track t : tracks) {
            if (!t.isLocked()) {
                return false;
            }
        }
        return true;
    }

    private void mix() {
        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            long id = track.getBuffer().getObjId();
            inputs[i] = id;
        }
        int status = mixBuffer(inputs, tracks.size(), outputBuffer.getObjId());
        if (status != 0) throw new IllegalStateException("Invalid mix status: " + status);
    }

    private static native int mixBuffer(long[] inputs, int inputCount, long output);

    private class Track implements AudioTrack {

        private AudioBuffer buffer;
        private boolean locked = false;
        private int writeLen = 0;
        private boolean deleted = false;
        private final byte[] lock = new byte[0];

        public Track(AudioBuffer buffer) {
            this.buffer = buffer;
            buffer.clear();
        }

        @Override
        public int write(ByteBuffer data) {
            synchronized (lock) {
                if (deleted) return WRITE_RESULT_DELETED;
                if (locked) return WRITE_RESULT_LOCKED;
                ByteBuffer buffer = this.buffer.getBuffer();
                int maxLen = this.buffer.getBufferSize() - writeLen;
                int size = data.remaining();
                if (size <= 0) return 0;
                int safeSize = Math.min(maxLen, size);
                if (safeSize > 0) {
                    data.limit(data.position() + safeSize);
                    buffer.put(data);
                    writeLen += safeSize;
                    return safeSize;
                }
                return WRITE_RESULT_FULL;
            }
        }

        @Override
        public int write(byte[] data, int offset, int size) {
            if (offset < 0 || size < 0 || offset + size > data.length) {
                throw new ArrayIndexOutOfBoundsException(String.format("{ data.length=%s, offset=%s, size=%s }", data.length, offset, size));
            }
            synchronized (lock) {
                if (deleted) return WRITE_RESULT_DELETED;
                if (locked) return WRITE_RESULT_LOCKED;
                ByteBuffer buffer = this.buffer.getBuffer();
                int maxLen = this.buffer.getBufferSize() - writeLen;
                int safeSize = Math.min(maxLen, size);
                if (safeSize == 0) return 0;
                if (safeSize > 0) {
                    buffer.put(data, offset, safeSize);
                    writeLen += safeSize;
                    return safeSize;
                }
                return WRITE_RESULT_FULL;
            }
        }

        @Override
        public boolean flush() {
            boolean flushOk = false;
            synchronized (lock) {
                if (!deleted && !locked) {
                    locked = true;
                    buffer.getBuffer().position(0);
                    flushOk = true;
                }
            }
            if (flushOk) {
                flushTrack();
            }
            return flushOk;
        }

        @Override
        public void waitUnlock() {
            synchronized (lock) {
                if (!deleted && locked) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        @Override
        public void delete() {
            if (release()) {
                removeTrack(this);
            }
        }

        public boolean release() {
            synchronized (lock) {
                if (!deleted) {
                    deleted = true;
                    buffer.release();
                    buffer = null;
                    lock.notifyAll();
                    return true;
                }
            }
            return false;
        }

        public void reset() {
            synchronized (lock) {
                if (!deleted && locked) {
                    locked = false;
                    buffer.clear();
                    writeLen = 0;
                    buffer.getBuffer().position(0);
                    lock.notifyAll();
                }
            }
        }

        public boolean isLocked() {
            return locked;
        }

        public AudioBuffer getBuffer() {
            return buffer;
        }
    }
}
