package com.fanwe.library.pulltorefresh;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by Administrator on 2017/6/26.
 */

public class SDPullToRefreshView extends FrameLayout implements ISDPullToRefreshView
{
    public SDPullToRefreshView(@NonNull Context context)
    {
        super(context);
        initInternal();
    }

    public SDPullToRefreshView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        initInternal();
    }

    public SDPullToRefreshView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        initInternal();
    }

    private static final String TAG = "SDPullToRefreshView";

    private SDPullToRefreshRootView mRootLayout;

    private Mode mMode = Mode.BOTH;
    private State mState = State.RESET;
    private Direction mDirection = Direction.NONE;
    private Direction mLastDirection = Direction.NONE;
    /**
     * Reset状态下mRootLayout的top值
     */
    private int mRootTopReset;
    /**
     * 触发拦截拖动的最小移动距离，默认0
     */
    private int mTouchSlop;
    /**
     * 设置拖动的时候要消耗的拖动距离比例
     */
    private float mComsumeScrollPercent = DEFAULT_COMSUME_SCROLL_PERCENT;

    private ViewDragHelper mDragHelper;

    private boolean mHasOnLayout;
    private Runnable mUpdatePositionRunnable;

    private OnRefreshCallback mOnRefreshCallback;
    private OnStateChangedCallback mOnStateChangedCallback;
    private OnViewPositionChangedCallback mOnViewPositionChangedCallback;

    private boolean mIsDebug;

    private void initInternal()
    {
        addRootLayout();
        initViewDragHelper();
    }

    public void setDebug(boolean debug)
    {
        mIsDebug = debug;
    }

    private void addRootLayout()
    {
        mRootLayout = new SDPullToRefreshRootView(getContext());
        mRootLayout.setPullToRefreshView(this);
        addView(mRootLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mRootLayout.setHeaderView(new SDSimpleTextLoadingView(getContext()));
        mRootLayout.setFooterView(new SDSimpleTextLoadingView(getContext()));
    }

    /**
     * 初始化ViewDragHelper
     */
    private void initViewDragHelper()
    {
        mDragHelper = ViewDragHelper.create(this, new ViewDragHelper.Callback()
        {
            @Override
            public boolean tryCaptureView(View child, int pointerId)
            {
                if (child != mRootLayout)
                {
                    return false;
                }
                if (mState != State.RESET)
                {
                    return false;
                }
                if (mIsDebug)
                {
                    Log.i(TAG, "ViewDragHelper tryCaptureView when scroll distance:" + getScrollDistance());
                }
                if (getScrollDistance() != 0)
                {
                    return false;
                }
                return true;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy)
            {
                switch (mDirection)
                {
                    case HEADER_TO_FOOTER:
                        if (mMode == Mode.BOTH || mMode == Mode.PULL_FROM_HEADER)
                        {
                            final int comsumeDistance = (int) (Math.abs(dy) * getComsumeScrollPercent());
                            if (top > mRootTopReset)
                            {
                                top = top - comsumeDistance;
                            }
                            return Math.max(top, mRootTopReset);
                        } else
                        {
                            return mRootTopReset;
                        }
                    case FOOTER_TO_HEADER:
                        if (mMode == Mode.BOTH || mMode == Mode.PULL_FROM_FOOTER)
                        {
                            final int comsumeDistance = (int) (Math.abs(dy) * getComsumeScrollPercent());
                            if (top < mRootTopReset)
                            {
                                top = top + comsumeDistance;
                            }
                            return Math.min(top, mRootTopReset);
                        } else
                        {
                            return mRootTopReset;
                        }
                    case NONE:
                        return top;
                    default:
                        return top;
                }
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy)
            {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
                if (mTouchHelper.isNeedCosume() && isViewDragging())
                {
                    //设置方向
                    if (dy > 0)
                    {
                        setDirection(Direction.HEADER_TO_FOOTER);
                    } else if (dy < 0)
                    {
                        setDirection(Direction.FOOTER_TO_HEADER);
                    }
                    updateStateByScrollDistance();
                }

                mRootLayout.onViewPositionChanged(SDPullToRefreshView.this);
                if (mOnViewPositionChangedCallback != null)
                {
                    mOnViewPositionChangedCallback.onViewPositionChanged(SDPullToRefreshView.this);
                }
            }

            @Override
            public void onViewCaptured(View capturedChild, int activePointerId)
            {
                super.onViewCaptured(capturedChild, activePointerId);
                if (mIsDebug)
                {
                    Log.i(TAG, "ViewDragHelper onViewCaptured");
                }
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel)
            {
                if (mIsDebug)
                {
                    Log.i(TAG, "ViewDragHelper onViewReleased");
                }
                if (mState == State.PULL_TO_REFRESH)
                {
                    setState(State.RESET);
                } else if (mState == State.RELEASE_TO_REFRESH)
                {
                    setState(State.REFRESHING);
                }
                updateViewPositionByState();
            }
        });
    }

    /**
     * 根据状态更新view的位置
     */
    private void updateViewPositionByState()
    {
        if (mHasOnLayout)
        {
            updateViewPositionByStateReal();
        } else
        {
            mUpdatePositionRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    updateViewPositionByStateReal();
                }
            };
        }
    }

    /**
     * 根据状态更新view的位置
     */
    private void updateViewPositionByStateReal()
    {
        switch (mState)
        {
            case RESET:
            case PULL_TO_REFRESH:
                mDragHelper.smoothSlideViewTo(mRootLayout, 0, mRootTopReset);
                break;
            case RELEASE_TO_REFRESH:
            case REFRESHING:
                if (mDirection == Direction.HEADER_TO_FOOTER)
                {
                    mDragHelper.smoothSlideViewTo(mRootLayout, 0, mRootTopReset + mRootLayout.getHeaderRefreshHeight());
                } else if (mDirection == Direction.FOOTER_TO_HEADER)
                {
                    mDragHelper.smoothSlideViewTo(mRootLayout, 0, mRootTopReset - mRootLayout.getFooterRefreshHeight());
                }
                break;
        }
        invalidate();
    }

    /**
     * 更新当前状态
     */
    private void updateStateByScrollDistance()
    {
        int dis = getScrollDistance();
        if (mDirection == Direction.HEADER_TO_FOOTER)
        {
            if (dis <= mRootLayout.getHeaderRefreshHeight())
            {
                setState(State.PULL_TO_REFRESH);
            } else if (dis >= mRootLayout.getHeaderRefreshHeight())
            {
                setState(State.RELEASE_TO_REFRESH);
            }
        } else if (mDirection == Direction.FOOTER_TO_HEADER)
        {
            if (dis <= mRootLayout.getFooterRefreshHeight())
            {
                setState(State.PULL_TO_REFRESH);
            } else if (dis >= mRootLayout.getFooterRefreshHeight())
            {
                setState(State.RELEASE_TO_REFRESH);
            }
        }
    }

    /**
     * 设置状态
     *
     * @param state
     */
    private void setState(State state)
    {
        if (mState != state)
        {
            mState = state;

            if (mIsDebug)
            {
                Log.i(TAG, "setState:" + mState);
            }

            //通知view改变状态
            mRootLayout.onStateChanged(mState, this);

            //通知刷新回调
            if (mState == State.REFRESHING)
            {
                if (mOnRefreshCallback != null)
                {
                    if (mDirection == Direction.HEADER_TO_FOOTER)
                    {
                        mOnRefreshCallback.onRefreshingFromHeader(this);
                    } else if (mDirection == Direction.FOOTER_TO_HEADER)
                    {
                        mOnRefreshCallback.onRefreshingFromFooter(this);
                    }
                }
            }

            //通知状态变化回调
            if (mOnStateChangedCallback != null)
            {
                mOnStateChangedCallback.onStateChanged(mState, this);
            }

            if (mState == State.RESET)
            {
                setDirection(Direction.NONE);
            }
        }
    }


    /**
     * 设置拖动方向
     *
     * @param direction
     */
    private void setDirection(Direction direction)
    {
        if (direction != Direction.NONE)
        {
            if (mDirection == Direction.NONE)
            {
                mDirection = direction;
                mLastDirection = direction;
            }
        } else
        {
            mDirection = Direction.NONE;
        }
    }

    private float getComsumeScrollPercent()
    {
        return mComsumeScrollPercent;
    }

    //----------ISDPullToRefreshView implements start----------

    @Override
    public void setMode(Mode mode)
    {
        if (mode != null && mMode != mode)
        {
            mMode = mode;
        }
    }

    @Override
    public void setOnRefreshCallback(OnRefreshCallback onRefreshCallback)
    {
        mOnRefreshCallback = onRefreshCallback;
    }

    @Override
    public void setOnStateChangedCallback(OnStateChangedCallback onStateChangedCallback)
    {
        mOnStateChangedCallback = onStateChangedCallback;
    }

    @Override
    public void setOnViewPositionChangedCallback(OnViewPositionChangedCallback onViewPositionChangedCallback)
    {
        mOnViewPositionChangedCallback = onViewPositionChangedCallback;
    }

    @Override
    public void setComsumeScrollPercent(float comsumeScrollPercent)
    {
        if (comsumeScrollPercent < 0)
        {
            comsumeScrollPercent = 0;
        }
        if (comsumeScrollPercent > 1)
        {
            comsumeScrollPercent = 1;
        }
        mComsumeScrollPercent = comsumeScrollPercent;
    }

    @Override
    public void startRefreshingFromHeader()
    {
        if (mMode == Mode.DISABLE)
        {
            return;
        }
        if (isRefreshing())
        {
            return;
        }
        setDirection(Direction.HEADER_TO_FOOTER);
        setState(State.REFRESHING);
        updateViewPositionByState();
    }

    @Override
    public void startRefreshingFromFooter()
    {
        if (mMode == Mode.DISABLE)
        {
            return;
        }
        if (isRefreshing())
        {
            return;
        }
        setDirection(Direction.FOOTER_TO_HEADER);
        setState(State.REFRESHING);
        updateViewPositionByState();
    }

    @Override
    public void stopRefreshing()
    {
        if (mState != State.RESET)
        {
            setState(State.RESET);
            updateViewPositionByState();
        }
    }

    @Override
    public boolean isRefreshing()
    {
        return mState == State.REFRESHING;
    }

    @Override
    public State getState()
    {
        return mState;
    }

    @Override
    public SDPullToRefreshLoadingView getHeaderView()
    {
        return mRootLayout.getHeaderView();
    }

    @Override
    public void setHeaderView(SDPullToRefreshLoadingView headerView)
    {
        mRootLayout.setHeaderView(headerView);
    }

    @Override
    public SDPullToRefreshLoadingView getFooterView()
    {
        return mRootLayout.getFooterView();
    }

    @Override
    public void setFooterView(SDPullToRefreshLoadingView footerView)
    {
        mRootLayout.setFooterView(footerView);
    }

    @Override
    public View getRefreshView()
    {
        return mRootLayout.getRefreshView();
    }

    @Override
    public Direction getDirection()
    {
        return mLastDirection;
    }

    @Override
    public int getScrollDistance()
    {
        return Math.abs(mRootLayout.getTop() - mRootTopReset);
    }

    //----------ISDPullToRefreshView implements end----------

    @Override
    public void computeScroll()
    {
        if (mDragHelper.continueSettling(true))
        {
            invalidate();
        }
    }

    private SDTouchHelper mTouchHelper = new SDTouchHelper();

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        if (mIsDebug)
        {
            if (ev.getAction() == MotionEvent.ACTION_DOWN)
            {
                Log.i(TAG, "onInterceptTouchEvent:" + ev.getAction() + "--------------------");
            } else
            {
                Log.i(TAG, "onInterceptTouchEvent:" + ev.getAction());
            }
        }
        if (mMode == Mode.DISABLE || isRefreshing())
        {
            return false;
        }
        if (mTouchHelper.isNeedIntercept())
        {
            if (mIsDebug)
            {
                Log.e(TAG, "onInterceptTouchEvent Intercept success because isNeedIntercept is true with action----------" + ev.getAction());
            }
            return true;
        }

        mTouchHelper.processTouchEvent(ev);
        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                //触发ViewDragHelper的尝试捕捉
                mDragHelper.processTouchEvent(ev);
                mTouchHelper.setNeedIntercept(false);
                break;
            case MotionEvent.ACTION_MOVE:
                if (canPull())
                {
                    mTouchHelper.setNeedIntercept(true);
                    if (mIsDebug)
                    {
                        Log.e(TAG, "onInterceptTouchEvent Intercept success when isMoveDown:" + mTouchHelper.isMoveDown());
                    }
                }
                break;

            default:
                mDragHelper.processTouchEvent(ev);
                break;
        }
        return mTouchHelper.isNeedIntercept();
    }

    private boolean isViewDragging()
    {
        return mDragHelper.getViewDragState() == ViewDragHelper.STATE_DRAGGING;
    }

    private boolean checkMoveParams()
    {
        return Math.abs(mTouchHelper.getDistanceDownY()) > mTouchSlop && mTouchHelper.getDegreeY() < 30;
    }

    private boolean canPull()
    {
        return checkMoveParams() && (canPullFromHeader() || canPullFromFooter());
    }

    private boolean canPullFromHeader()
    {
        return mTouchHelper.isMoveDown()
                && (mMode == Mode.BOTH || mMode == Mode.PULL_FROM_HEADER)
                && SDTouchHelper.isScrollToTop(mRootLayout.getRefreshView());
    }

    private boolean canPullFromFooter()
    {
        return mTouchHelper.isMoveUp()
                && (mMode == Mode.BOTH || mMode == Mode.PULL_FROM_FOOTER)
                && SDTouchHelper.isScrollToBottom(mRootLayout.getRefreshView());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (mMode == Mode.DISABLE || isRefreshing())
        {
            return super.onTouchEvent(event);
        }

        mTouchHelper.processTouchEvent(event);
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if (!isViewDragging())
                {
                    //触发ViewDragHelper的尝试捕捉
                    mDragHelper.processTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchHelper.isNeedCosume())
                {
                    if (isViewDragging())
                    {
                        if (mIsDebug)
                        {
                            Log.i(TAG, "processTouchEvent ACTION_MOVE");
                        }
                        mDragHelper.processTouchEvent(event);
                    }
                } else
                {
                    if (canPull())
                    {
                        mTouchHelper.setNeedCosume(true);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mDragHelper.processTouchEvent(event);

                mTouchHelper.setNeedCosume(false);
                mTouchHelper.setNeedIntercept(false);
                break;
            default:
                mDragHelper.processTouchEvent(event);
                break;
        }

        return super.onTouchEvent(event) || mTouchHelper.isNeedCosume() || event.getAction() == MotionEvent.ACTION_DOWN;
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();

        if (getChildCount() < 2)
        {
            throw new IllegalArgumentException("you must add one child to SDPullToRefreshView in your xml file");
        } else if (getChildCount() > 2)
        {
            throw new IllegalArgumentException("you can only add one child to SDPullToRefreshView in your xml file");
        }

        View refreshView = getChildAt(1);
        ViewGroup.LayoutParams params = refreshView.getLayoutParams();
        removeView(refreshView);
        refreshView.setLayoutParams(params);
        mRootLayout.setRefreshView(refreshView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int headerHeight = mRootLayout.getHeaderHeight();
        int footerHeight = mRootLayout.getFooterHeight();
        height = height + headerHeight + footerHeight;

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, heightMode);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);
        int left = getPaddingLeft();
        int top = -mRootLayout.getHeaderHeight();
        int right = getWidth() - getPaddingRight();
        int bottom = getHeight() + mRootLayout.getFooterHeight();

        mRootTopReset = top;

        mRootLayout.layout(left, top, right, bottom);

        if (top != 0 && bottom != getHeight())
        {
            mHasOnLayout = true;
            runUpdatePositionRunnableIfNeed();
        }
    }

    private void runUpdatePositionRunnableIfNeed()
    {
        if (mHasOnLayout && mUpdatePositionRunnable != null)
        {
            mUpdatePositionRunnable.run();
            mUpdatePositionRunnable = null;
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        mHasOnLayout = false;
        mUpdatePositionRunnable = null;
    }
}