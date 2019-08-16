package com.boilbingo.adplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class AdPlayer extends View implements View.OnClickListener {

    private final int DEFAULT_SWITCH_TIME = 3 * 1000;

    private final int DEFAULT_TITLE_BG_HEIGHT = 100;

    private final int DEFAULT_SMOOTH_SWITCHING_HEIGHT = 300;

    private final int DEFAULT_INDEX_ICON_RADIUS = 10;

    private final int DEFAULT_INDEX_ICON_SPACE = 35;

    private final int DEFAULT_INDEX_ICON_MARGIN = 50;

    private final int DEFAULT_TITLE_TEXT_SIZE = 40;

    private final int DEFAULT_TITLE_BG_COLOR = 0x80000000;

    private final int DEFAULT_DELAY_TIME_WHILE_SWITCHING = 10;



    // Store the current index of shown picture
    private int mCurIndex;

    // previous index of current index
    private int mPreIndex;

    // Array to store AD pictures
    private List<Bitmap> mAdPictures;

    // radius of index icon
    private int mIndexIconRadius = DEFAULT_INDEX_ICON_RADIUS;

    // The space between two center of index icon
    private int mIndexIconSpace = DEFAULT_INDEX_ICON_SPACE;

    private int mIndexIconMargin = DEFAULT_INDEX_ICON_MARGIN;

    private class Point {
        int x;
        int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private Paint mIndexIconPaint;

    private Paint mTitlePaint;

    private Paint mTitleBgPaint;

    private List<String> mTitles;

    private int mTitleTextColor = Color.WHITE;

    private int mTitleTextSize = DEFAULT_TITLE_TEXT_SIZE;

    private int mTitleBgColor = DEFAULT_TITLE_BG_COLOR;

    private int mTitleBgHeight = DEFAULT_TITLE_BG_HEIGHT;

    // left value of left picture displayed
    private int mLeftOfLeftPicture = 0;

    // The time switch to next picture (ms).
    private int mSwitchTime = DEFAULT_SWITCH_TIME;

    // Total time while smooth switching
    private int mTotalSwitchingTime = DEFAULT_SMOOTH_SWITCHING_HEIGHT;

    // Refresh frequency while smooth switching
    private int mDelayTimeWhileSwitching = DEFAULT_DELAY_TIME_WHILE_SWITCHING;

    // Track velocity after finger leave screen
    private VelocityTracker mVelocityTracker;

    // x coordinate of touch down
    private float mStartX;

    // displacement from mStartX
    private int mDisplacement;

    // Index of left displayed pictures, it is current index if just one picture.
    private int mLeftIndex;

    // Index of right displayed pictures, no use while just one picture.
    private int mRightIndex;

    // Rest length for switching
    private int mRestWidthBeforeSiwtching = 0;

    // To +/- per mDelayTimeWhileSwitching
    private int mAlpha = 0;

    // Check if left picture toward left
    private boolean mTowardLeft = true;

    private Handler mHandler = new Handler();

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

        mTitlePaint = new Paint();
        mTitlePaint.setColor(mTitleTextColor);
        mTitlePaint.setTextSize(mTitleTextSize);

        mTitleBgPaint = new Paint();
        mTitleBgPaint.setColor(mTitleBgColor);

        setOnClickListener(this);
    }

    public void setAdPictures(List<Bitmap> adPictures) {
        mAdPictures = adPictures;
    }

    public void addAdWithTitle(String title, Bitmap adPicture) {
        if (mTitles == null) mTitles = new ArrayList<>();
        if (mAdPictures == null) mAdPictures = new ArrayList<>();
        mTitles.add(title);
        mAdPictures.add(adPicture);
    }

    public void setItemClickListener(ItemClickListener listener) {
        mItemClickListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (mItemClickListener != null) {
            mItemClickListener.onItemClick(mCurIndex);
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
                // Store previous index
                mPreIndex = mCurIndex;
                // Update current index
                mCurIndex = ++mCurIndex % mAdPictures.size();
            } else {
                // Update current index
                mCurIndex = mPreIndex;
                // Store previous index
                mPreIndex = (--mPreIndex < 0) ? mAdPictures.size() - 1 : mPreIndex;;
            }
            result = true;
        }

        return result;
    }

    private Runnable mPlayRunnable = new Runnable() {
        @Override
        public void run() {
            // Refresh view while updating index success
            updateIndex(true);
            mLeftIndex = mPreIndex;

            // Start to switch
            // Initial
            mLeftOfLeftPicture = 0;

            // startSmoothSwitching
            startSmoothSwitching();
        }
    };

    /**
     * Post next switch task
     */
    private void postNextSwitchTask() {
        mHandler.postDelayed(mPlayRunnable, mSwitchTime);
    }


    /**
     * Start to play AD.
     * Should stop() while exit.
     */
    public void start() {
        if (mAdPictures != null && mAdPictures.size() > 0) {
            // Initial index to 0
            mLeftIndex = mCurIndex = 0;
            mPreIndex = mAdPictures.size() - 1;

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
        mHandler.removeCallbacks(mPlayRunnable);
    }

    /**
     * Calculate the position of first index icon
     * @return pos
     */
    private Point calculateFirstIndexIconPos() {
        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();
        int halfCenterLen = mIndexIconSpace * (mAdPictures.size() - 1) / 2;

        // Locate to right if have title
        if (mTitles != null && mTitles.size() > 0) {
            return new Point(viewWidth - halfCenterLen - mIndexIconMargin * 2,
                    viewHeight - mIndexIconMargin);
        }

        // Locate to center
        return new Point(viewWidth / 2 - halfCenterLen, viewHeight - mIndexIconMargin);
    }

    /**
     * Return scaled picture to show
     * @return bitmap
     */
    private Bitmap getScaledPicture(int index) {
        Bitmap oldBm = mAdPictures.get(index);
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
            mAdPictures.set(index, newBm);

            oldBm.recycle();
        }
        return mAdPictures.get(index);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // super.onDraw(canvas);

        if (mAdPictures != null && mAdPictures.size() > 0) {
            // Draw picture
            drawDisplayBitmap(canvas);

            // Draw title
            if (mTitles != null) {
                // Draw tile background
                canvas.drawRect(0,
                        getMeasuredHeight() - mTitleBgHeight,
                        getMeasuredWidth(),
                        getMeasuredHeight(), mTitleBgPaint);

                String title = mTitles.get(mCurIndex);
                if(!TextUtils.isEmpty(title)) {
                    // Get text bounds
                    Rect textBounds = new Rect();
                    mTitlePaint.getTextBounds(title, 0, title.length(), textBounds);

                    // Get the drawable rect to write title
                    Rect drawableRect = getDrawableTextRect(textBounds);

                    String writableText = getWritableText(title, textBounds, drawableRect);
                    if (!TextUtils.isEmpty(writableText)) {
                        title = writableText;
                        mTitles.set(mCurIndex, title);
                    }

                    canvas.drawText(title,
                            drawableRect.left, drawableRect.bottom, mTitlePaint);
                }
            }

            // Draw index icon
            Point firstIndexPos = calculateFirstIndexIconPos();
            for (int i = 0; i < mAdPictures.size(); i++) {
                mIndexIconPaint.setColor(Color.DKGRAY);
                if (i == mCurIndex) {
                    mIndexIconPaint.setColor(Color.RED);
                }

                canvas.drawCircle(firstIndexPos.x + mIndexIconSpace * i,
                        firstIndexPos.y, mIndexIconRadius, mIndexIconPaint);
            }
        }
    }

    private void startSmoothSwitching() {
        // Initial before smooth switch
        int totalWidth = getMeasuredWidth();
        // Compute the rest length to switch
        if (mTowardLeft) {
            mRestWidthBeforeSiwtching = totalWidth - Math.abs(mLeftOfLeftPicture);
        } else {
            mRestWidthBeforeSiwtching = Math.abs(mLeftOfLeftPicture);
        }
        // Compute the total time for rest length to switch
        mTotalSwitchingTime = DEFAULT_SMOOTH_SWITCHING_HEIGHT * mRestWidthBeforeSiwtching / totalWidth;

        if (mTotalSwitchingTime >= mDelayTimeWhileSwitching) {
            // Calculate alpha to ensure finish switch in given total time
            mAlpha = mRestWidthBeforeSiwtching * mDelayTimeWhileSwitching / mTotalSwitchingTime;
        } else {
            // Smaller than refresh rate, not need to switch
            mAlpha = 0;
            mLeftOfLeftPicture = 0;
        }

        mHandler.postDelayed(smoothSwitchingRunnable, mDelayTimeWhileSwitching);
    }

    private Runnable smoothSwitchingRunnable = new Runnable() {
        @Override
        public void run() {
            // change by direction
            if (mTowardLeft) {
                mLeftOfLeftPicture -= mAlpha;
            } else {
                mLeftOfLeftPicture += mAlpha;
            }

            if (-mLeftOfLeftPicture >= getMeasuredWidth() || mLeftOfLeftPicture >= 0) {
                // Finish switching task
                // reset offset
                mLeftOfLeftPicture = 0;
                mTowardLeft = true;

                mLeftIndex = mCurIndex;


                // Post the next switch task
                postNextSwitchTask();
            } else {
                // post next recycle
                mHandler.postDelayed(this, mDelayTimeWhileSwitching);
            }

            // Refresh view
            invalidate();
        }
    };

    /**
     * Draw the pictures to bitmap
     * @param canvas: from onDraw
     */
    private void drawDisplayBitmap(Canvas canvas) {

        if (mLeftOfLeftPicture != 0) {
            // In progress of smooth switching
            // draw left
            canvas.drawBitmap(getScaledPicture(mLeftIndex), mLeftOfLeftPicture, 0, null);
            //draw right
            mRightIndex = (mLeftIndex + 1) % mAdPictures.size();
            canvas.drawBitmap(getScaledPicture(mRightIndex),
                    getMeasuredWidth() + mLeftOfLeftPicture, 0, null);
        } else {
            // Draw current picture
            canvas.drawBitmap(getScaledPicture(mLeftIndex), 0, 0, null);
        }
    }

    /**
     * Calculate the rect that can used to write title
     * @param textBounds: the real rect of title
     * @return Rect object
     */
    private Rect getDrawableTextRect(Rect textBounds) {
        int left = mIndexIconMargin;
        // The space of rect's bottom to title's bottom.
        int bottomMargin = (mTitleBgHeight - textBounds.height()) / 2;
        int top = getMeasuredHeight() - mTitleBgHeight + bottomMargin;
        int right = left + getMeasuredWidth() - mIndexIconMargin * 4 -
                mIndexIconSpace * (mAdPictures.size() - 1) + 2 * mIndexIconRadius;
        int bottom = getMeasuredHeight() - bottomMargin;
        return new Rect(left, top, right, bottom);
    }

    /**
     * Cut title if title's length is larger than drawable rect's width
     * @param text: title
     * @param textBounds: the real rect of title
     * @param drawableRect: the rect that can used to write title
     * @return A string that can be shown
     */
    private String getWritableText(String text, Rect textBounds, Rect drawableRect) {
        String result = null;
        boolean needCut = false;
        // Cut title until can be written
        // TODO: Optimize if the title length is very long
        while (textBounds.width() > drawableRect.width()) {
            needCut = true;
            text = text.subSequence(0, text.length() - 1 - 3) + "...";
            mTitlePaint.getTextBounds(text, 0, text.length(), textBounds);
        }

        if (needCut) result = text;

        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mLeftOfLeftPicture != 0) {
                    // it is switching, don't handle the set of events
                    return false;
                }
                // Cancel task of auto switch to next picture
                stop();
                // Initial velocity tracker and displacement
                mVelocityTracker = VelocityTracker.obtain();
                mDisplacement = 0;
                // record start x coordinate
                mStartX = event.getX();

                break;

            case MotionEvent.ACTION_MOVE:
                // Add event to tracker for further computation
                mVelocityTracker.addMovement(event);

                // use displacement to check toward left or right to drag direction
                mDisplacement = (int)(event.getX() - mStartX);
                if (mDisplacement > 0) {
                    // Toward right, need to change mLeftIndex to mPreIndex
                    mLeftIndex = mPreIndex;
                    // Update the left of left picture
                    mLeftOfLeftPicture = mDisplacement - getMeasuredWidth();
                } else {
                    // Toward left, mLeftIndex is mCurIndex
                    mLeftIndex = mCurIndex;
                    // Left of left picture equals displacement
                    mLeftOfLeftPicture = mDisplacement;
                }

                invalidate();

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);

                // check if needs to switch
                if (Math.abs(mDisplacement) > getMeasuredWidth() / 2
                        || Math.abs(mVelocityTracker.getXVelocity()) > 200f) {
                    // Switch to left or right, it is the direction of displacement
                    mTowardLeft = mDisplacement < 0;
                    // Update index to previous if toward right, or update to next
                    updateIndex(mTowardLeft);

                    startSmoothSwitching();
                } else {
                    if (Math.abs(mDisplacement) > 0) {
                        // rollback so its direction is opposite to displacement
                        mTowardLeft = !(mDisplacement < 0);
                        startSmoothSwitching();
                    }
                }

                // recycle
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                break;
        }
        return true;
    }

}
