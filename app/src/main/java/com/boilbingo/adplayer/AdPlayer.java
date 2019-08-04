package com.boilbingo.adplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class AdPlayer extends View implements View.OnClickListener {

    private final int DEFAULT_SWITCH_TIME = 3 * 1000;

    // Store the current index of shown picture
    private int mIndex;

    // Array to store AD pictures
    private List<Bitmap> mAdPictures;

    private int mIndexIconRadius = 10;

    // The space between two center of index icon
    private int mIndexIconSpace = 35;

    private int mIndexIconMargin = 50;

    private class Point {
        int x;
        int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private Paint mIndexIconPaint;


    // The time switch to next picture (ms).
    private int mSwitchTime = DEFAULT_SWITCH_TIME;

    private Handler mPlayHandler = new Handler();

    private Runnable mPlayRunnable = new Runnable() {
        @Override
        public void run() {
            // Refresh view while updating index success
            if (updateIndex(true)) {
                // Refresh view.
                invalidate();

                // Post next switch task
                postNextSwitchTask();
            }
        }
    };

    public interface ItemClickListener {
        void onItemClick(int index);
    }

    private ItemClickListener mItemClickListener;

    /**
     * Constructor
     * @param context
     */
    public AdPlayer(Context context) {
        super(context);
    }

    public AdPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);

        mIndexIconPaint = new Paint();
        setOnClickListener(this);
    }

    public void setAdPictures(List<Bitmap> adPictures) {
        mAdPictures = adPictures;
    }

    public void setItemClickListener(ItemClickListener listener) {
        mItemClickListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (mItemClickListener != null) {
            mItemClickListener.onItemClick(mIndex);
        }
    }

    /**
     * Update the current index of shown picture
     * @param forward
     * @return true for success and false for failure.
     */
    private boolean updateIndex(boolean forward) {
        boolean result = false;

        if (mAdPictures != null && mAdPictures.size() > 1) {
            if (forward) {
                mIndex = ++mIndex % mAdPictures.size();
            } else {
                mIndex = (--mIndex < 0) ? mAdPictures.size() - 1 : mIndex;
            }
            result = true;
        }

        return result;
    }

    /**
     * Post next switch task
     */
    private void postNextSwitchTask() {
        mPlayHandler.postDelayed(mPlayRunnable, mSwitchTime);
    }


    /**
     * Start to play AD.
     * Should stop() while exit.
     */
    public void start() {
        if (mAdPictures != null && mAdPictures.size() > 0) {
            // Initial index to 0
            mIndex = 0;

            // Refresh player to show first picture
            invalidate();

            // Post task to switch picture at next time point
            postNextSwitchTask();
        }
    }

    /**
     * Stop play AD.
     */
    public void stop() {
        // Remove switch task
        mPlayHandler.removeCallbacks(mPlayRunnable);
    }

    /**
     * Calculate the position of first index icon
     * @return pos
     */
    private Point calculateFirstIndexIconPos() {
        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();
        int halfCenterLen = mIndexIconSpace * (mAdPictures.size() - 1) / 2;

        return new Point(viewWidth / 2 - halfCenterLen, viewHeight - mIndexIconMargin);
    }

    /**
     * Return scaled picture to show
     * @return bitmap
     */
    private Bitmap getCurrentScaledPicture() {
        Bitmap oldBm = mAdPictures.get(mIndex);
        int bmWidth = oldBm.getWidth();
        int bmHeight = oldBm.getHeight();
        float scaleWidth = ((float)getMeasuredWidth()) / bmWidth;
        float scaleHeight = ((float)getMeasuredHeight()) / bmHeight;

        boolean needScale = (scaleWidth != 1.0f || scaleHeight != 1.0f);
        if (needScale) {
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            Bitmap newBm = Bitmap.createBitmap(oldBm,
                    0, 0, bmWidth, bmHeight, matrix, true);
            mAdPictures.set(mIndex, newBm);

            oldBm.recycle();
        }
        return mAdPictures.get(mIndex);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // super.onDraw(canvas);

        if (mAdPictures != null && mAdPictures.size() > 0) {
            // Draw picture
            canvas.drawBitmap(getCurrentScaledPicture(), 0, 0, null);

            // Draw index icon
            Point firstIndexPos = calculateFirstIndexIconPos();
            for (int i = 0; i < mAdPictures.size(); i++) {
                mIndexIconPaint.setColor(Color.DKGRAY);
                if (i == mIndex) {
                    mIndexIconPaint.setColor(Color.RED);
                }

                canvas.drawCircle(firstIndexPos.x + mIndexIconSpace * i,
                        firstIndexPos.y, mIndexIconRadius, mIndexIconPaint);
            }
        }
    }
}
