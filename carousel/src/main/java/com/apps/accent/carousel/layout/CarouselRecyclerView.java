package com.apps.accent.carousel.layout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.hardware.SensorManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.apps.accent.carousel.R;


public class CarouselRecyclerView extends RecyclerView {

    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mScrollPointerId = -1;
    private boolean isSwiping = false;

    private float mFlingFriction = ViewConfiguration.getScrollFriction();
    private float mPhysicalCoeff;

    private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));

    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)

    Callback callback;
    int mMinFlingVelocity;
    int mMaxFlingVelocity;

    private CarouselLayoutManager layoutManager;

    public CarouselRecyclerView(Context context) {
        this(context, null);
    }

    public CarouselRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarouselRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        callback = new Callback();
        addOnScrollListener(scrollListener);



        final ViewConfiguration vc = ViewConfiguration.get(context);
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        this.setChildDrawingOrderCallback(callback);

        float mPpi = context.getResources().getDisplayMetrics().density * 160.0f;

        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * mPpi
                * 0.84f; // look and feel tuning


        if (attrs != null) {
            int defStyleRes = 0;
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CarouselRecyclerView,
                    defStyle, defStyleRes);
            boolean rotate = a.getBoolean(R.styleable.CarouselRecyclerView_item_rotation, false);
            float rotateVal = a.getFloat(R.styleable.CarouselRecyclerView_rotation_value, 10.0f);
            a.recycle();
            layoutManager = new CarouselLayoutManager(rotate, rotateVal, getContext());
            setLayoutManager(layoutManager);
        }
    }

    private int findFrontViewIndex(RecyclerView recyclerView, int count) {
        int anchor = 0;
        for (int i = 1; i < count; i++) {
            if (recyclerView.getChildAt(anchor).getScaleX() < recyclerView.getChildAt(i).getScaleX()) {
                anchor = i;
            }
        }
        return anchor;
    }

    @Override
    public void draw(Canvas c) {
        callback.nextChildIndexToRender = 0;
        super.draw(c);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        if (velocityX > mMaxFlingVelocity) {
            Log.d("overflow", "for " + velocityX);
        }
        CarouselLayoutManager manager = (CarouselLayoutManager)getLayoutManager();

        boolean fling = true;
        final boolean canScrollHorizontal = getLayoutManager().canScrollHorizontally();
        if (!canScrollHorizontal || Math.abs(velocityX) < mMinFlingVelocity) {
            fling = false;
        }

        int scrollAmount = manager.getScrollAmount(velocityX, fling);
        if (fling) {
            int neededVelocity = getSplineFlingVelocity(Math.abs(scrollAmount));
            neededVelocity = Math.max(neededVelocity, Math.abs(velocityX));
            super.fling((int) (Math.signum(velocityX) * neededVelocity), velocityY);
        } else {
            smoothScrollBy(scrollAmount, 0);
        }

        return true;
    }

    private int getSplineFlingVelocity(int distance) {
        final double decelMinusOne = DECELERATION_RATE - 1.0;

        return (int) (Math.exp((decelMinusOne * Math.log(distance / (mFlingFriction * mPhysicalCoeff))) / DECELERATION_RATE)
                * (mFlingFriction * mPhysicalCoeff) / INFLEXION);
    }

    private double getSplineDeceleration(int velocity) {
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }

    private double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
    }

    private class Callback implements ChildDrawingOrderCallback {
        public int nextChildIndexToRender = 0;

        @Override
        public int onGetChildDrawingOrder(int childCount, int i) {
            if (i == childCount - 1) {
                nextChildIndexToRender = 0;
                return findFrontViewIndex(CarouselRecyclerView.this, childCount);
            } else {
                if (nextChildIndexToRender == findFrontViewIndex(CarouselRecyclerView.this, childCount)) {
                    nextChildIndexToRender++;
                }
                return nextChildIndexToRender++;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        boolean canScrollVertically = layoutManager.canScrollVertically();

        final int action = MotionEventCompat.getActionMasked(e);
        final int actionIndex = MotionEventCompat.getActionIndex(e);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrollPointerId = MotionEventCompat.getPointerId(e, 0);
                mInitialTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = (int) (e.getY() + 0.5f);

                break;

            case MotionEventCompat.ACTION_POINTER_DOWN:
                mInitialTouchX  = (int) (MotionEventCompat.getX(e, actionIndex) + 0.5f);
                mInitialTouchY  = (int) (MotionEventCompat.getY(e, actionIndex) + 0.5f);
                break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(e, mScrollPointerId);
                final int x = (int) (MotionEventCompat.getX(e, index) + 0.5f);
                final int y = (int) (MotionEventCompat.getY(e, index) + 0.5f);
                final int dx = x - mInitialTouchX;
                final int dy = y - mInitialTouchY;
                if (dy > dx) {
                    isSwiping = true;
                    return true;
                }
            }
            break;

            case MotionEventCompat.ACTION_POINTER_UP: {
//                onPointerUp(e);
            } break;

            case MotionEvent.ACTION_UP: {
                stopNestedScroll();
            } break;

            case MotionEvent.ACTION_CANCEL: {
//                cancelTouch();
            }
        }
        return super.onInterceptTouchEvent(e);
    }


    private OnScrollListener scrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState)  {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                ((CarouselLayoutManager)recyclerView.getLayoutManager()).incrementPos();
                requestLayout();
            }
        }
    };
}
