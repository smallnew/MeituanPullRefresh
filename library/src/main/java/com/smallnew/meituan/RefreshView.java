package com.smallnew.meituan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import java.util.Random;

/**
 * Created by Apisov on 02/03/2015.
 * edit by smallnew on 12/26/2015
 * https://dribbble.com/shots/1623131-Pull-to-Refresh
 */
public class RefreshView extends Drawable implements Drawable.Callback, Animatable {

    private static final float SCALE_END_PERCENT = 0.5f;
    private static final int ANIMATION_DURATION = 400;

    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();

    // Multiply with this animation interpolator time
    private static final int LOADING_ANIMATION_COEFFICIENT = 80;
    private static final int SLOW_DOWN_ANIMATION_COEFFICIENT = 6;

    private Context mContext;
    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Matrix mAdditionalMatrix;
    private Animation mAnimation;

    private int mTop;
    private int mScreenWidth;
    private boolean mInverseDirection;

    private int mMeituanPullWidthCenter;
    private int mMeituanPullHeightCenter;
    private int mMeituanPullEndHeightCenter;

    private float mPercent = 0.0f;

    private Bitmap mMeituanPull;
    private Bitmap mMeituanPullEnd1;
    private Bitmap mMeituanPullEnd2;
    private Bitmap mMeituanPullEnd3;
    private Bitmap mMeituanPullEnd4;
    private Bitmap mMeituanPullEnd5;

    private Bitmap mMeituanReflesh1;
    private Bitmap mMeituanReflesh2;
    private Bitmap mMeituanReflesh3;
    private Bitmap mMeituanReflesh4;
    private Bitmap mMeituanReflesh5;
    private Bitmap mMeituanReflesh6;

    private boolean isRefreshing = false;
    private float mLoadingAnimationTime;
    private float mLastAnimationTime;


    private Random mRandom;
    private boolean mEndOfRefreshing;

    private enum AnimationPart {
        FIRST,
        SECOND,
        THIRD,
        FOURTH,
        FIVE,
        SIX,
        SEVEN,
        EIGHT
    }

    public RefreshView(Context context, PullToRefreshView parent) {
        mContext = context;
        mParent = parent;
        mMatrix = new Matrix();
        mAdditionalMatrix = new Matrix();

        initiateDimens();
        createBitmaps();
        setupAnimations();
    }

    private void initiateDimens() {
        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mTop = -mParent.getTotalDragDistance();
    }

    private void createBitmaps() {
        mMeituanPull = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pull_image);
        mMeituanPullEnd1 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pull_end_image_frame_01);
        mMeituanPullEnd2 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pull_end_image_frame_02);
        mMeituanPullEnd3 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pull_end_image_frame_03);
        mMeituanPullEnd4 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pull_end_image_frame_04);
        mMeituanPullEnd5 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pull_end_image_frame_05);

        mMeituanReflesh1 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.refreshing_image_frame_01);
        mMeituanReflesh2 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.refreshing_image_frame_02);
        mMeituanReflesh3 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.refreshing_image_frame_03);
        mMeituanReflesh4 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.refreshing_image_frame_05);
        mMeituanReflesh5 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.refreshing_image_frame_06);
        mMeituanReflesh6 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.refreshing_image_frame_07);

        mMeituanPullEndHeightCenter = mMeituanPullEnd1.getHeight() / 2;
        mMeituanPullWidthCenter = mMeituanPull.getWidth() / 2;
        mMeituanPullHeightCenter = mMeituanPull.getHeight() / 2;

    }

    public void offsetTopAndBottom(int offset) {
        mTop += offset;
        invalidateSelf();
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleDrawable(this, what, when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleDrawable(this, what);
        }
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    /**
     * Our animation depend on type of current work of refreshing.
     * We should to do different things when it's end of refreshing
     *
     * @param endOfRefreshing - we will check current state of refresh with this
     */
    public void setEndOfRefreshing(boolean endOfRefreshing) {
        mEndOfRefreshing = endOfRefreshing;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final int saveCount = canvas.save();
        // DRAW BACKGROUND.
        canvas.drawColor(mContext.getResources().getColor(R.color.refresh_background));
        drawMeituan(canvas);
        canvas.restoreToCount(saveCount);
    }

    private void drawMeituan(Canvas canvas) {
        Matrix matrixTuan = mMatrix;
        matrixTuan.reset();
        float dragPercent = Math.min(1f, Math.abs(mPercent));
        float scale;

        if (mPercent < SCALE_END_PERCENT && !isRefreshing) {
            float scalePercentDelta = dragPercent;
            if (scalePercentDelta > 0) {
                scale = 0.1f + (2.0f - 0.1f) * scalePercentDelta;
            } else {
                scale = 0.1f;
            }
            // Current  position of MeituanPull
            float offsetX = (mScreenWidth / 2) - mMeituanPullWidthCenter;
            float offsetY = mParent.getTotalDragDistance() * dragPercent - mMeituanPullHeightCenter * 2f;;
            offsetY = 0;

            matrixTuan.postScale(scale, scale, mMeituanPullWidthCenter, mMeituanPullHeightCenter / 8);
            matrixTuan.postTranslate(offsetX, offsetY);
            canvas.drawBitmap(mMeituanPull, matrixTuan, null);
        } else if (mPercent >= SCALE_END_PERCENT && !isRefreshing) {
            float offsetY = mParent.getTotalDragDistance() * dragPercent - mMeituanPullEndHeightCenter * 2;
            float offsetX = (mScreenWidth / 2) - mMeituanPullWidthCenter;
            Bitmap currentTuan = mMeituanPullEnd1;
            if (mPercent >= 0.5f && mPercent < 0.55) {
                currentTuan = mMeituanPullEnd1;
            } else if (mPercent >= 0.55f && mPercent < 0.6) {
                currentTuan = mMeituanPullEnd2;
            } else if (mPercent >= 0.6f && mPercent < 0.65) {
                currentTuan = mMeituanPullEnd3;
            } else if (mPercent >= 0.65f && mPercent < 0.7) {
                currentTuan = mMeituanPullEnd4;
            } else if (mPercent >= 0.7f) {
                currentTuan = mMeituanPullEnd5;
            }
            matrixTuan.postTranslate(offsetX, offsetY);
            canvas.drawBitmap(currentTuan, matrixTuan, null);
        } else if (isRefreshing) {
            float offsetY = mParent.getTotalDragDistance() * dragPercent - mMeituanPullEndHeightCenter * 2;
            float offsetX = (mScreenWidth / 2) - mMeituanPullWidthCenter;
            Bitmap currentTuan = mMeituanReflesh1;
            if (checkCurrentAnimationPart(AnimationPart.FIRST)) {
                currentTuan = mMeituanReflesh1;
            } else if (checkCurrentAnimationPart(AnimationPart.SECOND)) {
                currentTuan = mMeituanReflesh2;
            } else if (checkCurrentAnimationPart(AnimationPart.THIRD)) {
                currentTuan = mMeituanReflesh3;
            } else if (checkCurrentAnimationPart(AnimationPart.FOURTH)) {
                currentTuan = mMeituanReflesh2;
            } else if (checkCurrentAnimationPart(AnimationPart.FIVE)) {
                currentTuan = mMeituanReflesh4;
            } else if (checkCurrentAnimationPart(AnimationPart.SIX)) {
                currentTuan = mMeituanReflesh5;
            }else if (checkCurrentAnimationPart(AnimationPart.SEVEN)) {
                currentTuan = mMeituanReflesh6;
            }else if (checkCurrentAnimationPart(AnimationPart.EIGHT)) {
                currentTuan = mMeituanReflesh5;
            }
            matrixTuan.postTranslate(offsetX, offsetY);
            canvas.drawBitmap(currentTuan, matrixTuan, null);
        }

    }

    /**
     * On drawing we should check current part of animation
     *
     * @param part - needed part of animation
     * @return - return true if current part
     */
    private boolean checkCurrentAnimationPart(AnimationPart part) {
        switch (part) {
            case FIRST:
            case SECOND:
            case THIRD:
            case FOURTH:
            case FIVE:
            case SIX:
            case SEVEN:{
                return mLoadingAnimationTime < getAnimationTimePart(part);
            }
            case EIGHT: {
                return mLoadingAnimationTime > getAnimationTimePart(AnimationPart.SEVEN);
            }
            default:
                return false;
        }
    }

    /**
     * Get part of animation duration
     *
     * @param part - needed part of time
     * @return - interval of time
     */
    private int getAnimationTimePart(AnimationPart part) {
        switch (part) {
            case FIRST: {
                return LOADING_ANIMATION_COEFFICIENT * 1 / 8;
            }
            case SECOND: {
                return LOADING_ANIMATION_COEFFICIENT * 2 / 8;
            }
            case THIRD: {
                return LOADING_ANIMATION_COEFFICIENT * 3 / 8;
            }
            case FOURTH: {
                return LOADING_ANIMATION_COEFFICIENT * 4 / 8;
            }
            case FIVE: {
                return LOADING_ANIMATION_COEFFICIENT * 5 / 8;
            }
            case SIX: {
                return LOADING_ANIMATION_COEFFICIENT * 6 /8;
            }
            case SEVEN: {
                return LOADING_ANIMATION_COEFFICIENT * 7 / 8;
            }

            default:
                return 0;//FIRST
        }
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public void resetOriginals() {
        setPercent(0);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void start() {
        mAnimation.reset();
        isRefreshing = true;
        mParent.startAnimation(mAnimation);
        mLastAnimationTime = 0;
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        isRefreshing = false;
        mEndOfRefreshing = false;
        resetOriginals();
    }

    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, @NonNull Transformation t) {
                setLoadingAnimationTime(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);
//        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(LINEAR_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }

    private void setLoadingAnimationTime(float loadingAnimationTime) {
        /**SLOW DOWN ANIMATION IN {@link #SLOW_DOWN_ANIMATION_COEFFICIENT} time */
        mLoadingAnimationTime = LOADING_ANIMATION_COEFFICIENT * (loadingAnimationTime);   // SLOW_DOWN_ANIMATION_COEFFICIENT
        Log.e("setLoadingAnimationTime", "loadingAnimationTime = " + loadingAnimationTime + "  mLoadingAnimationTime=" + mLoadingAnimationTime);
        invalidateSelf();
    }

}
