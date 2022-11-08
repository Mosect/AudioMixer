package com.mosect.app.audiomixer;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mosect.lib.audiomixer.AudioBuffer;
import com.mosect.lib.audiomixer.PcmType;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Act/Main";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AudioBuffer audioBuffer = new AudioBuffer(44100, 2, 50000, PcmType.BIT16);
        ByteBuffer buffer = audioBuffer.getBuffer();
        buffer.position(0);
        for (int i = 0; i < 500; i++) {
            buffer.putShort((short) i);
        }
        AudioBuffer audioBuffer2 = new AudioBuffer(39800, 2, 50000, PcmType.BIT8);
        audioBuffer2.write(audioBuffer, 0, 1);
        ByteBuffer buffer2 = audioBuffer2.getBuffer();
        buffer2.position(0);
        for (int i = 0; i < 100; i++) {
            Log.d(TAG, "audioBuffer2: " + buffer2.getShort());
        }
    }
}
