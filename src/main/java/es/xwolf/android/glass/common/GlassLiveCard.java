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

import com.google.android.glass.timeline.DirectRenderingCallback;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.view.SurfaceHolder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;

public abstract class GlassLiveCard extends Service {// Version 1

    private class GlassLiveCardRenderer implements DirectRenderingCallback {

        private SurfaceHolder mHolder;
        private boolean mRenderingPaused;
        private String mTag;

        /**
         * Uses the provided {@code width} and {@code height} to measure and layout the
         * inflated
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // Measure and layout the view with the canvas dimensions.
            int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

            mView.measure(measuredWidth, measuredHeight);
            mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
        }

        /**
         * Keeps the created {@link SurfaceHolder} and updates this class' rendering
         * state.
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // The creation of a new Surface implicitly resumes the rendering.
            mRenderingPaused = false;
            mHolder = holder;
            updateRenderingState();
        }

        /**
         * Removes the {@link SurfaceHolder} used for drawing and stops rendering.
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mHolder = null;
            updateRenderingState();
        }

        /**
         * Updates this class' rendering state according to the provided {@code paused}
         * flag.
         */
        @Override
        public void renderingPaused(SurfaceHolder holder, boolean paused) {
            mRenderingPaused = paused;
            updateRenderingState();
        }

        /**
         * Starts or stops rendering according to the {@link LiveCard}'s state.
         */
        private void updateRenderingState() {
            if (mHolder != null && !mRenderingPaused) {
                start();
            } else {
                stop();
            }
        }

        /**
         * Draws the view in the SurfaceHolder's canvas.
         */
        void draw() {
            Canvas canvas;
            try {
                canvas = mHolder.lockCanvas();
            } catch (Exception e) {
                Log.e(getCardTag() + "-renderer", "Unable to lock canvas: " + e);
                return;
            }
            if (canvas != null) {
                mView.draw(canvas);
                mHolder.unlockCanvasAndPost(canvas);
            }
        }

        private void start() {
            mInBackground = false;
            if (!mRunInBackground)
                startInterval();
            else if (mIntervalRunnable != null)
                startInterval();
        }

        private void stop() {
            mInBackground = true;
            if (!mRunInBackground)
                stopInterval();
        }

    }

    private class GlassLiveCardView extends FrameLayout {

        public GlassLiveCardView(Context context, AttributeSet attrs, int style) {
            super(context, attrs, style);
        }
    }

    protected abstract String getCardTag();

    private final Handler mHandler = new Handler();
    private LiveCard mLiveCard;
    private GlassLiveCardRenderer mRenderer;
    private GlassLiveCardView mView;
    private Runnable mIntervalRunnable;
    private boolean mRunning;
    private boolean mRunInBackground;
    private boolean mInBackground;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, getCardTag());

            // Keep track of the callback to remove it before unpublishing.
            mView = new GlassLiveCardView(this, null, 0);
            initLayout();
            mRenderer = new GlassLiveCardRenderer();
            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mRenderer);

            Intent menuIntent = getMenu();
            if (menuIntent != null)
                mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            mLiveCard.attach(this);
            init();
            if (isSticky())
                mLiveCard.publish((intent == null) ? PublishMode.SILENT : PublishMode.REVEAL);
            else
                mLiveCard.publish(PublishMode.REVEAL);
        } else {
            mLiveCard.navigate();
        }

        // Return START_NOT_STICKY to prevent the system from restarting the service if
        // it is killed
        // (e.g., due to an error). It doesn't make sense to restart automatically
        // because the
        // stopwatch state will have been lost.
        return isSticky() ? START_STICKY : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        stopInterval();
        super.onDestroy();
    }

    /*
     * Return true if the card is a sticky one
     */
    protected boolean isSticky() {
        return false;
    }

    /*
     * Return the menu Intent if any, if no menu the return null
     */
    protected Intent getMenu() {
        return null;
    }

    /*
     * Init view
     */
    protected abstract void init();

    /*
     * Initializes the layout for the view
     */
    protected abstract void initLayout();

    /*
     * Updates the card based on interval or other updater events.
     */
    protected abstract boolean updateCard(boolean inBackground);

    /*
     * Starts or resets an updater based on intervals.
     */
    protected void setIntervalUpdate(long intervalMS, long backgroundIntervalMS) {
        mRunInBackground = backgroundIntervalMS > 0;
        if (intervalMS <= 0) {
            if (mIntervalRunnable != null) {
                stopInterval();
                mIntervalRunnable = null;
            }
        } else {
            mIntervalRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mRunning) {
                        doUpdate();
                        postDelayed(mIntervalRunnable, mInBackground ? backgroundIntervalMS : intervalMS);
                    }
                }
            };
        }
    }

    // @Override
    public boolean postDelayed(Runnable action, long delayMillis) {
        return mHandler.postDelayed(action, delayMillis);
    }

    // @Override
    public boolean removeCallbacks(Runnable action) {
        mHandler.removeCallbacks(action);
        return true;
    }

    /**
     * Starts the interval.
     */
    private void startInterval() {
        if (!mRunning) {
            mRunning = true;
            postDelayed(mIntervalRunnable, 1);
        }
    }

    /**
     * Stops the interval.
     */
    private void stopInterval() {
        if (mRunning) {
            mRunning = false;
            removeCallbacks(mIntervalRunnable);
        }
    }

    private void doUpdate() {
        if (updateCard(mInBackground))
            mRenderer.draw();
    }

    protected final View findViewById(int id) {
        return mView.findViewById(id);
    }

    protected final GlassLiveCardView getView() {
        return mView;
    }
}
