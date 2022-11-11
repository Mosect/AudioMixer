package com.mosect.app.audiomixer;

import android.app.Application;

public class MainApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        AudioMixer mixer = new AudioMixer(48000, 2, 10000, PcmType.BIT16);
//        AudioTrack track = mixer.requestTrack(44100, 2, PcmType.BIT16);
//        byte[] data = new byte[512];
//        Random random = new Random();
//        random.nextBytes(data);
//        track.write(data, 0, data.length);
//        track.flush();
//        ByteBuffer dst = mixer.tick();
//        byte[] data2 = new byte[512];
//        dst.get(data2);
//        boolean eq = Arrays.equals(data, data2);
//        System.out.println("EQ: " + eq);
    }
}
