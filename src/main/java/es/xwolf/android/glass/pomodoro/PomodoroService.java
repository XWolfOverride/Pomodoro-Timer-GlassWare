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

package es.xwolf.android.glass.pomodoro;

import es.xwolf.android.glass.common.Audio;
import es.xwolf.android.glass.common.GlassLiveCard;
import es.xwolf.android.glass.common.XPowerManager;

import com.google.android.glass.timeline.DirectRenderingCallback;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

/**
 * Service owning the LiveCard living in the timeline.
 */
public final class PomodoroService extends GlassLiveCard {

    static final long DELAY_MILLIS = 1000 / 5; // 5 fps
    static final long DELAY_MILLIS_BACKGROUND = 1000 / 1; // 1 per second

    private XPowerManager powman;
    private Audio audio = new Audio();

    private TextView mMinutesView;
    private TextView mSecondsView;
    private TextView mDescriptionView;

    private long mNextChangeTimestamp;
    private boolean onPause;
    private int pauseCount = 0;

    private String rTextActivity;
    private String rTextPause;
    private int rColorActivity;
    private int rColorPause;
    private int rSoundActivity;
    private int rSoundPause;

    @Override
    protected String getCardTag() {
        return "pomodoro";
    }

    @Override
    protected Intent getMenu() {
        Intent menuIntent = new Intent(this, MenuActivity.class);
        menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return menuIntent;
    }

    @Override
    protected void init() {
        rTextActivity = getResources().getString(R.string.in_activity);
        rTextPause = getResources().getString(R.string.in_pause);
        rColorActivity = getResources().getColor(R.color.in_activity);
        rColorPause = getResources().getColor(R.color.in_pause);
        rSoundActivity = audio.load(this, R.raw.start_activity);
        rSoundPause = audio.load(this, R.raw.start_pause);
        initPomodoro(false, false, false);
        setIntervalUpdate(DELAY_MILLIS, DELAY_MILLIS_BACKGROUND);
    }

    @Override
    protected void initLayout() {
        LayoutInflater.from(this).inflate(R.layout.card_pomodoro, getView());

        mMinutesView = (TextView) findViewById(R.id.minute);
        mSecondsView = (TextView) findViewById(R.id.second);
        mDescriptionView = (TextView) findViewById(R.id.description);
    }

    private void initPomodoro(boolean asPause, boolean sound, boolean wake) {
        if (asPause)
            pauseCount++;
        setTime(asPause ? (pauseCount % 3 == 0 ? 5 : 15) : 25, asPause);
        if (sound)
            audio.playSound(asPause ? rSoundPause : rSoundActivity);
        if (wake) {
            Awake();
            Toast toast = Toast.makeText(this, "Pomodoro: " + (onPause ? rTextPause : rTextActivity),
                    Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /**
     * Returns {@link SystemClock.elapsedRealtime}, overridable for testing.
     */
    protected long getElapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    public void setTime(int minutes, boolean onPause) {
        mNextChangeTimestamp = TimeUnit.MINUTES.toMillis(minutes) + getElapsedRealtime();
        // mNextChangeTimestamp = TimeUnit.SECONDS.toMillis(10) + getElapsedRealtime();
        this.onPause = onPause;
    }

    @Override
    protected boolean updateCard(boolean inBackground) {
        long now = getElapsedRealtime();
        if (mNextChangeTimestamp < now) {
            initPomodoro(!onPause, true, inBackground);
            now++;
        }
        if (!inBackground) {
            long millisLeft = mNextChangeTimestamp - now;
            mMinutesView.setText(String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(millisLeft)));
            millisLeft %= TimeUnit.MINUTES.toMillis(1);
            mSecondsView.setText(String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(millisLeft)));
            mDescriptionView.setText(onPause ? rTextPause : rTextActivity);
            mDescriptionView.setTextColor(onPause ? rColorPause : rColorActivity);
        }
        return true;
    }

    private void Awake() {
        if (powman == null)
            powman = new XPowerManager(this);
        powman.wakeScreen();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                powman.wakeUnlock();
            }
        }, 3000);// 3secs
    }
}
