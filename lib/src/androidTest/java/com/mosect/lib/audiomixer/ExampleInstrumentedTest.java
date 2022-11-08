package com.mosect.lib.audiomixer;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final String TAG = "AudioTest";

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.mosect.lib.audiomixer.test", appContext.getPackageName());
    }

    public void testAudioBuffer() {
        AudioBuffer audioBuffer = new AudioBuffer(44100, 2, 50000, PcmType.BIT16);
        audioBuffer.clear();
        ByteBuffer buffer = audioBuffer.getBuffer();
        buffer.position(0);
        for (int i = 0; i < 500; i++) {
            int value = buffer.getShort();
            Log.d(TAG, "testAudioBuffer: " + value);
        }
    }
}