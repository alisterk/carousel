package com.apps.accent.carousel.layout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.apps.accent.carousel.R;


public class CarouselLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = CarouselLayoutManager.class.getName();

    public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    public static final int VERTICAL = OrientationHelper.VERTICAL;

    private static final int FLING_NONE = 0;

    private static final int FLING_FORWARD = 1;

    private static final int FLING_BACKWARD = 2;

    private int orientation = HORIZONTAL;

    private SparseArray<View> viewCache = new SparseArray<>();

    private int anchorPos = Integer.MAX_VALUE / 2;

    private int ITEMS_OFFSET = 100;

    private final float ITEMS_SCALE = 0.9f;

    private final float ITEM_WIDTH_SCALE = 0.7f;

    private float rotationDegree = 8.0f;
    private boolean useRotationFeature = false;

    private int childW;

    private int widthSpec;
    private int heightSpec;

    private int backwardOffsetAmount;

    private int signDirection = 1;

    private int scrollState = RecyclerView.SCROLL_STATE_IDLE;

    private int flingType = FLING_NONE;

    private float wrongTail;

    private float wrongDx;

    // for testing
    private int totalShiftValue;

    protected CarouselLayoutManager(boolean rotateFeature, float rotateVal, Context context) {
        useRotationFeature = rotateFeature;
        rotationDegree = rotateVal;
        ITEMS_OFFSET = (int) convertDpToPixel(context.getResources().getDimension(R.dimen.card_item_offset), context);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        if (position >= getItemCount()) {
            Log.e(TAG, "Cannot scroll to " + position + ", item count is " + getItemCount());
            return;
        }

        LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return CarouselLayoutManager.this.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }
        };
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    private PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        final int firstChildPos = getPosition(getChildAt(0));
        final int direction = targetPosition < firstChildPos ? -1 : 1;
        if (orientation == HORIZONTAL) {
            return new PointF(direction, 0);
        } else {
            return new PointF(0, direction);
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return orientation == HORIZONTAL;
    }

    @Override
    public boolean canScrollVertically() {
        return orientation == VERTICAL;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int delta = dx;
        if (state.getItemCount() == 0 || getChildCount() == 0) {
            return 0;
        }

        dx += wrongDx;
        wrongDx = 0;
        int rWHalf = getWidth() / 2;
//        totalShiftValue += dx;

        View topChild = findViewByPosition(anchorPos);
        View rightChild = findViewByPosition(anchorPos + 1);
        View leftChild = findViewByPosition(anchorPos - 1);

        int topCOffset;

        int neighborOffset;

        int halfDist = ITEMS_OFFSET / 2;

        float topItemScale = 1.0f;
        float topItemRotation;
        int topChildCenter = topChildCenter(topChild);
        int boundary = rWHalf - ITEMS_OFFSET;

        if (scrollState == RecyclerView.SCROLL_STATE_DRAGGING || scrollState == RecyclerView.SCROLL_STATE_SETTLING) {

            // top child translates with *(rWHalf + backwardOffsetAmount)

            if (dx > 0) {

                if ((signDirection == -1 && topChildCenter <= getWidth())) {
                    signDirection = -1; // opposite from dx // should scale down // the anchor position should be changed

                    if (topChildCenter <= boundary) {
                        if (topChildCenter + dx > boundary) {
                            dx = boundary - topChildCenter; // we don't need no further wrongDx value
                            if (dx == 0) {
                                return 1000; // for instant kill
                            }
                        }

                        // left wing
                        topItemScale = 1 - (topChildCenter) / (float) (backwardOffsetAmount) * (1 - ITEMS_SCALE);

                        topItemRotation = - ( (boundary - topChildCenter) / (float) boundary) * rotationDegree;
                    } else { // when top child is on the right most position

                        if (topChildCenter + dx > getWidth()) {
                            wrongDx = dx - (getWidth() - topChildCenter);
                            dx = getWidth() - topChildCenter;
                        }

                        // right wing
                        topItemScale = 1 - (getWidth() - topChildCenter) / (float) (backwardOffsetAmount) * (1 - ITEMS_SCALE);

                        topItemRotation = ( (topChildCenter - (getWidth() - boundary)) / (float) boundary) * rotationDegree;
                    }
                } else {
                    if (topChildCenter - dx < 0) {
                        wrongDx = dx - topChildCenter;
                        dx = topChildCenter;
                    } else {
                        signDirection = 1;
                    }
                    topItemRotation = ( (topChildCenter - rWHalf) / (float) rWHalf) * rotationDegree;
                }
            } else {

                if (signDirection == -1 && topChildCenter >= 0) {
                    signDirection = -1; // opposite from dx // should scale up

                    if (topChildCenter >= getWidth() - boundary) {
                        if (topChildCenter + dx < getWidth() - boundary) {
                            dx = - (topChildCenter - (getWidth() - boundary));
                            if (dx == 0) {
                                return -1000;
                            }
                        }

                        // right wing
                        topItemScale = 1 - (getWidth() - topChildCenter) / (float) (backwardOffsetAmount) * (1 - ITEMS_SCALE);

                        topItemRotation =  ( (topChildCenter - (getWidth() - boundary)) / (float) boundary) * rotationDegree;
                    } else {
                        if (topChildCenter - Math.abs(dx) < 0) {
                            wrongDx = dx + topChildCenter;
                            dx = -topChildCenter;
                        }

                        // left wing
                        topItemScale = 1 - topChildCenter / (float) (backwardOffsetAmount) * (1 - ITEMS_SCALE);

                        topItemRotation = - ( (boundary - topChildCenter) / (float) boundary) * rotationDegree;
                    }
                } else {
                    if (topChildCenter + Math.abs(dx) > getWidth()) {
                        wrongDx = dx + (getWidth() - topChildCenter);
                        dx = - (getWidth() - topChildCenter);
                    } else {
                        signDirection = 1;
                    }
                    topItemRotation = ( (topChildCenter - rWHalf) / (float) rWHalf) * rotationDegree;
                }
            }

            topCOffset = signDirection * dx;

            topChild.offsetLeftAndRight(-topCOffset);


            if (flingType == FLING_NONE) {
                Log.d("dx", "dx = " + signDirection * dx);
            }

            topChild.setScaleY(topItemScale);
            topChild.setScaleX(topItemScale);

            if (useRotationFeature) {

                topChild.setPivotX(0.0f);
                topChild.setPivotY(topChild.getHeight());
                topChild.setRotation(topItemRotation);

                topChild.setPivotX(topChild.getWidth() / 2);
                topChild.setPivotY(topChild.getHeight() / 2);

            }
            Log.d("topone", String.valueOf(topItemScale));

            // left and right translates half of items_offset
            if (leftChild != null) {
                if (topChildCenter >= rWHalf) {
                    float innerOffset;
                    if (signDirection == 1) {
                        innerOffset = (-topCOffset / (float) (rWHalf)) * halfDist + wrongTail;
                    } else {
                        innerOffset = (topCOffset / (float) (backwardOffsetAmount)) * halfDist + wrongTail;
                    }

                    neighborOffset = Math.round(innerOffset);

                    wrongTail = innerOffset - neighborOffset;

                    leftChild.offsetLeftAndRight(neighborOffset);
                    float innerScale = leftChild.getScaleX() + (neighborOffset / (float) ITEMS_OFFSET) * (1 - ITEMS_SCALE);
                    leftChild.setScaleX(innerScale);
                    leftChild.setScaleY(innerScale);
                }
            }
            if (rightChild != null) {
                if (topChildCenter <= rWHalf) {
                    float innerOffset;
                    if (signDirection == 1) {
                        innerOffset = (topCOffset / (float) (rWHalf)) * halfDist + wrongTail;
                    } else {
//                        Log.d("tag2", String.valueOf(rightChild.getRight() - ITEMS_OFFSET/2 - getRightAxisY()));
                        innerOffset = (-topCOffset / (float) backwardOffsetAmount) * halfDist + wrongTail;
                    }

                    neighborOffset = Math.round(innerOffset);

                    wrongTail = innerOffset - neighborOffset;

                    rightChild.offsetLeftAndRight(-neighborOffset);
                    float innerScale = rightChild.getScaleX() + (neighborOffset / (float) ITEMS_OFFSET) * (1 - ITEMS_SCALE);
                    rightChild.setScaleX(innerScale);
                    rightChild.setScaleY(innerScale);
                }
            }
            if (wrongDx != 0) {
                signDirection = -signDirection; // revert directions
            }
            fillAll(recycler);

        }

        return delta;  // animation gets aborted in case of over_scroll_x if we return changed dx
    }

    private void rightWingSettle(int viewCenter, int boundary) {

    }

    private void leftWingSettle(int viewCenter, int boundary) {

    }

    public int getScrollAmount(int velocity, boolean fling) {
        scrollState = RecyclerView.SCROLL_STATE_DRAGGING;

        View topChild = findViewByPosition(anchorPos);
        int offset;
        int relative = topChildDist(topChild);
        if (relative * velocity > 0) { // same var sign check
            if (signDirection == -1) {
                if (fling) {
                    offset = (int) (Math.signum(relative) * (getWidth() / 2 - Math.abs(relative) + getWidth() + backwardOffsetAmount));
                    flingType = velocity > 0 ? FLING_FORWARD : FLING_BACKWARD;
                } else {
                    offset = (int) (Math.signum(relative) * (getWidth() / 2 - Math.abs(relative) + getWidth() / 2)); // back to the old position set
                    flingType = FLING_NONE;
                }
                Log.d("dx", "backward space = " + String.valueOf(offset + getWidth() / 2) + " all = " + String.valueOf(offset));
                return offset;
            }
        }
        if (velocity > 0) {
            if (fling || signDirection == -1) {
                flingType = FLING_FORWARD;
                offset = topChild.getRight() - childW / 2 + backwardOffsetAmount;
            } else {
                flingType = FLING_NONE;
                offset = -(getRightAxisY() - topChild.getRight());
            }
        } else {
            if (fling || signDirection == -1) {
                flingType = FLING_BACKWARD;
                offset = -(getWidth() - childW / 2 - topChild.getLeft() + backwardOffsetAmount);
            } else {
                flingType = FLING_NONE;
                offset = (topChild.getLeft() - getLeftAxisY());
            }
        }

        return offset;
    }

    public int getTravelDistance() {
        View topChild = findViewByPosition(anchorPos);
        return getWidth() - childW / 2 - topChild.getLeft() + backwardOffsetAmount;
    }

    private int topChildCenter(View topChild) {
        return topChild.getLeft() + childW / 2;
    }


    private int topChildDist(View topChild) {
        return topChildCenter(topChild) - getWidth() / 2;
    }

    @Override
    public void onScrollStateChanged(int state) {
        scrollState = state;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        detachAndScrapAttachedViews(recycler);
        signDirection = 1;
        wrongTail = 0.0f;
        flingType = FLING_NONE;

        totalShiftValue = 0;

        childW = (int) ((getWidth() - getPaddingLeft() - getPaddingRight()) * ITEM_WIDTH_SCALE);

        backwardOffsetAmount = getLeftAxisY() - ITEMS_OFFSET + childW / 2;

        widthSpec = View.MeasureSpec.makeMeasureSpec(childW, View.MeasureSpec.EXACTLY);
        heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.AT_MOST);

        fillAll(recycler);

    }

    public void incrementPos() {
        if (flingType == FLING_FORWARD) {
            anchorPos++;
        } else if (flingType == FLING_BACKWARD) {
            anchorPos--;
        }
    }

    private void fillAll(RecyclerView.Recycler recycler) {
        viewCache.clear();
        for (int i = 0, cnt = getChildCount(); i < cnt; i++) {
            View view = getChildAt(i);
            int pos = getPosition(view);
            viewCache.put(pos, view);
        }

        for (int i = 0; i < viewCache.size(); i++) {
            detachView(viewCache.valueAt(i));
        }

        fillCenter(recycler);

        fillLeft(recycler, anchorPos);

        fillRight(recycler, anchorPos);

        for (int i=0; i < viewCache.size(); i++) {
            recycler.recycleView(viewCache.valueAt(i));
        }

    }

    private void fillCenter(RecyclerView.Recycler recycler) {
        int pos = anchorPos;
        int rW = getWidth();

        View view = viewCache.get(pos);
        if (view == null) {
            view = recycler.getViewForPosition(pos);
            addView(view, 2);

            measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
//            measureChildWithMargins(view, 0, 0);
            int decoratedMeasuredHeight = getDecoratedMeasuredHeight(view);
            int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);

            layoutDecorated(view, (rW - decoratedMeasuredWidth) / 2, getPaddingTop(),
                    (rW - decoratedMeasuredWidth) / 2 + childW, decoratedMeasuredHeight - getPaddingBottom());
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
            view.setRotation(0.0f);
        } else {
            attachView(view, 2);
            viewCache.remove(pos);
        }
    }

    private void fillLeft(RecyclerView.Recycler recycler, int anchorPos) {
        int pos = anchorPos - 1;
        int rW = getWidth();
        View topChild = getChildAt(getChildCount() - 1);
        for (int i = 0; i < 2 ; i++, pos--) {
            View view = viewCache.get(pos);
            if (topChild == null || topChild.getLeft() > getLeftAxisY() - ITEMS_OFFSET) {
                if (view == null) {
                    view = recycler.getViewForPosition(pos);
                    addView(view, 0);
                    measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
//                    measureChildWithMargins(view, 0, 0);
                    int decoratedMeasuredHeight = getDecoratedMeasuredHeight(view);
                    int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);

                    layoutDecorated(view, (rW - decoratedMeasuredWidth) / 2 - ITEMS_OFFSET, getPaddingTop(),
                            (rW - decoratedMeasuredWidth) / 2 + childW - ITEMS_OFFSET, decoratedMeasuredHeight - getPaddingBottom());
                    view.setScaleX(ITEMS_SCALE);
                    view.setScaleY(ITEMS_SCALE);
                    view.setRotation(0.0f);
                } else {
                    attachView(view, 0);
                    viewCache.remove(pos);
                }
            }
        }
    }

    private void fillRight(RecyclerView.Recycler recycler, int anchorPos) {
        int pos = anchorPos + 1;
        int rW = getWidth();
        View topChild = getChildAt(getChildCount() - 1);
        for (int i = 0; i < 2; i++, pos++) {
            View view = viewCache.get(pos);
            if (topChild == null || topChild.getRight() < getRightAxisY() + ITEMS_OFFSET) {
                if (view == null) {
                    view = recycler.getViewForPosition(pos);
                    addView(view, 0);
                    measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
//                    measureChildWithMargins(view, 0, 0);
                    int decoratedMeasuredHeight = getDecoratedMeasuredHeight(view);
                    int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);

                    layoutDecorated(view, (rW - decoratedMeasuredWidth) / 2 + ITEMS_OFFSET, getPaddingTop(),
                            (rW - decoratedMeasuredWidth) / 2 + childW + ITEMS_OFFSET, decoratedMeasuredHeight - getPaddingBottom());
                    view.setScaleX(ITEMS_SCALE);
                    view.setScaleY(ITEMS_SCALE);
                    view.setRotation(0.0f);
                } else {
                    attachView(view, 0);
                    viewCache.remove(pos);
                }
            }
        }

    }

    private int getLeftAxisY() {
        return (getWidth() - childW) / 2;
    }

    private int getRightAxisY() {
        return getLeftAxisY() + childW;
    }


    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec) {
        Rect decorRect = new Rect();
        calculateItemDecorationsForChild(child, decorRect);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        widthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + decorRect.left,
                lp.rightMargin + decorRect.right);
        heightSpec = updateSpecWithExtra(heightSpec, lp.topMargin + decorRect.top + getPaddingTop(),
                lp.bottomMargin + decorRect.bottom + getPaddingBottom());
        child.measure(widthSpec, heightSpec);
    }

    private int updateSpecWithExtra(int spec, int startInset, int endInset) {
        if (startInset == 0 && endInset == 0) {
            return spec;
        }
        final int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            return View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.getSize(spec) - startInset - endInset, mode);
        }
        return spec;
    }


    @Override
    public View findViewByPosition(int position) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        final int firstChild = getPosition(getChildAt(0));
        final int viewPosition = position - firstChild;
        if (viewPosition >= 0 && viewPosition < childCount) {
            final View child = getChildAt(viewPosition);
            if (getPosition(child) == position) {
                return child; // in pre-layout, this may not match
            }
        }
        // fallback to traversal. This might be necessary in pre-layout.
        return super.findViewByPosition(position);
    }

    private float convertDpToPixel(float dp, Context context) {
        if (context == null) return -1;

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }
}
