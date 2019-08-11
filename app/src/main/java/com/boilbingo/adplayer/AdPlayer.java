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
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class AdPlayer extends View implements View.OnClickListener {

    private final int DEFAULT_SWITCH_TIME = 3 * 1000;

    private final int DEFAULT_TITLE_BG_HEIGHT = 100;

    // Store the current index of shown picture
    private int mCurIndex;

    private int mPreIndex = -1;

    // Array to store AD pictures
    private List<Bitmap> mAdPictures;

    private int mIndexIconRadius = 10;

    // The space between two center of index icon
    private int mIndexIconSpace = 35;

    private int mIndexIconMargin = 50;

    private int mIndexIconPos;

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

    private int mTitleTextSize = 40;

    private int mTitleBgColor = 0x80000000;

    private int mTitleBgHeight = DEFAULT_TITLE_BG_HEIGHT;



    // The time switch to next picture (ms).
    private int mSwitchTime = DEFAULT_SWITCH_TIME;

    private Handler mPlayHandler = new Handler();

    private Runnable mPlayRunnable = new Runnable() {
        @Override
        public void run() {
            // Refresh view while updating index success
            if (updateIndex(true)) {
                // Start to switch
                mSwitching = true;
                mSwitchedWidth = 0;

                // Refresh view.
                invalidate();

                // Post next switch task
                //postNextSwitchTask();
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
            // Store previous index
            mPreIndex = mCurIndex;
            // Update current index
            if (forward) {
                mCurIndex = ++mCurIndex % mAdPictures.size();
            } else {
                mCurIndex = (--mCurIndex < 0) ? mAdPictures.size() - 1 : mCurIndex;
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
            mCurIndex = 0;

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
    private Bitmap getCurrentScaledPicture() {
        Bitmap oldBm = mAdPictures.get(mCurIndex);
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
            mAdPictures.set(mCurIndex, newBm);

            oldBm.recycle();
        }
        return mAdPictures.get(mCurIndex);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // super.onDraw(canvas);

        if (mAdPictures != null && mAdPictures.size() > 0) {
            // Draw picture
            //


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

    private boolean mSwitching = false;
    private boolean mForward = true;
    private int mSwitchedWidth = 0;
    private int mTotalSwitchingTime = 600;
    private int mDelayTimeWhildSwitching = 20;

    private Runnable swtichingPictureRun = new Runnable() {
        @Override
        public void run() {
            if (mSwitching) {
                // Clear same kind callbacks at queue.
                // Because onDraw maybe called several times.
                mPlayHandler.removeCallbacks(this);

                // Calculate alpha to ensure finish switch in given time
                int width = getMeasuredWidth();
                int alpha = width * mDelayTimeWhildSwitching / mTotalSwitchingTime;
                if (mForward) {
                    mSwitchedWidth -= alpha;
                } else {
                    mSwitchedWidth += alpha;
                }
                if (Math.abs(mSwitchedWidth) > width) {
                    // Finish switching task
                    mSwitchedWidth = width;
                    mSwitching = false;
                    // Post the next switch task
                    postNextSwitchTask();
                }
                mPlayHandler.postDelayed(swtichingPictureRun, mDelayTimeWhildSwitching);
                // Refresh view
                invalidate();
            }
        }
    };

    private void drawDisplayBitmap(Canvas canvas) {
        if (mSwitching) {
            if (mForward) {
                // switch to next picture
                // draw left
                canvas.drawBitmap(mAdPictures.get(mPreIndex), mSwitchedWidth, 0, null);
                //draw right
                canvas.drawBitmap(getCurrentScaledPicture(), getMeasuredWidth() + mSwitchedWidth, 0, null);
            } else {
                // switch to previous picture
                // draw left
                canvas.drawBitmap(mAdPictures.get(mPreIndex), -getMeasuredWidth() + mSwitchedWidth, 0, null);
                //draw right
                canvas.drawBitmap(getCurrentScaledPicture(), mSwitchedWidth, 0, null);
            }

            mPlayHandler.postDelayed(swtichingPictureRun, mSwitchedWidth == 0 ? 0 : mDelayTimeWhildSwitching);
        } else {
            canvas.drawBitmap(getCurrentScaledPicture(), 0, 0, null);
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
}
