/*
 * Copyright 2023 XWolf Override
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package es.xwolf.android.glass.common;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class Audio {
    private static final int SOUND_PRIORITY = 1;
    private static final int DEFAULT_MAX_STREAMS = 1;

    private final SoundPool mSoundPool;

    public Audio(){
        mSoundPool = new SoundPool(DEFAULT_MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
    }

    public Audio(int maxStreams){
        mSoundPool = new SoundPool(maxStreams, AudioManager.STREAM_MUSIC, 0);
    }

    public int load(Context ctx, int resource){
        return mSoundPool.load(ctx, resource, SOUND_PRIORITY);
    }


    /**
     * Plays the provided {@code soundId}.
     */
    public void playSound(int soundId) {
        mSoundPool.play(soundId,
                        1 /* leftVolume */,
                        1 /* rightVolume */,
                        SOUND_PRIORITY,
                        0 /* loop */,
                        1 /* rate */);
    }
}
