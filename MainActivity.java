package com.yuqirong.cardswipeview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import me.yuqirong.cardswipelayout.CardItemTouchHelperCallback;


/**
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final float CHILD_SCALE_LARGE = 1.2f;
    private static final float CHILD_SCALE_NORMAL = 1.0f;
    public ViewDragHelper mDragHelper; // 这个跟原生的ViewDragHelper差不多，仅仅只是修改了Interpolator
    private GestureDetectorCompat moveDetector;
    private int lastDragState;
    private WellChosenRecyclerView recyclerView;
    private boolean isTouchBottom, isLastTouchBottom, swipeLeft, btnLock, isCardVanish;
    private int allWidth, allHeight, centerX, centerY, childWith;    //中心点、面板的高宽、每一个子View对应的宽度

    private List<Integer> list = new ArrayList<>();

    private final static int ROTATION = 20, ANIM_SPEED_DEFAULT = 100;
    private static final int X_VEL_THRESHOLD = 900;
    private static final int X_DISTANCE_THRESHOLD = 400;
    private final static int DRAG_SWIPE_SLOP = 30;
    private final static int SCORLL_SLOP = 20;
    private static final long LONG_PRESS_TIME = 300;

    public static final int VANISH_TYPE_LEFT = 0;
    public static final int VANISH_TYPE_RIGHT = 1;
    public static final int VANISH_TYPE_UP = 2;
    private View mOverdrawChild;
    private Object obj1 = new Object();
    private int mOverdrawChildPosition = -1, initCenterViewX = 0, initCenterViewY = 0; // 最初时，中间View的x位置,y位置
    private float lastDownX, lastDownY;
    private boolean animating, handleByDragHelper;
    private SparseArray<Rect> mLoctions;
    private GridLayoutManager manager;
    private long lastDownTime;
    private boolean isLongPress;
    private WellChosenAdapter adapter;

    // 引导动画相关
    private static final int DEFAULT_ANIM_ROTATE_ANGLE = 10; // 旋转角度
    private static final int DEFAULT_ANIM_ROTATE_X_OFFSET = 200; // 旋转x偏移量
    private static final int DEFAULT_ANIM_ROTATE_Y_OFFSET = 100; // 旋转y偏移量
    private static final int DEFAULT_ANIM_STAY_TIME = 1000; // 每一小段动画停留时间
    private static final int DEFAULT_ANIM_EXEC_TIME = 1500; // 每一小段动画执行时间
    private AtomicBoolean mNeedShowGuide = new AtomicBoolean(false); // 是否需要显示引导
    private ValueAnimator mGuideAnim;
    private TextView mImgLike, mTvLikeTips;
    private View mViewGuideLayer;
    private View rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diandian_well_chosen_cards);
        AppContext.init(this);
        initView();
        initData();
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                initGuideAnim();
                showRightSwipeGuide();
            }
        });
    }

    private void showRightSwipeGuide() {
        if (mGuideAnim != null && !mGuideAnim.isRunning()) {
            mGuideAnim.start();
            setSlideContentVisible(false);
        }
    }

    private Context getContext() {
        return MainActivity.this;
    }

    private void initGuideAnim() {
        mNeedShowGuide.set(true);
        if (!mNeedShowGuide.get() || mGuideAnim != null || recyclerView == null)
            return;
        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(1);
        if (viewHolder == null || adapter.isHeaderView(viewHolder)) {
            return;
        }
        final View targetView = viewHolder.itemView;
        final ViewGroup viewGroup = (ViewGroup) viewHolder.itemView;
        FrameLayout background = (FrameLayout) rootLayout;//((FrameLayout) getWindow().getDecorView());//(ViewGroup) getWindow().findViewById(Window.ID_ANDROID_CONTENT);
        if (viewGroup == null || background == null) {
            return;
        }

        mTvLikeTips = new TextView(getContext());
        final int tvWidth = UIUtils.getPixels(190);
        final int tvHeight = UIUtils.getPixels(55);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(tvWidth, tvHeight);
        mTvLikeTips.setLayoutParams(params);
        mTvLikeTips.setTranslationY(targetView.getTop() + targetView.getHeight() / 2 + tvHeight);
        mTvLikeTips.setTranslationX(targetView.getLeft() + targetView.getWidth() - tvWidth / 2);
        mTvLikeTips.setAlpha(0f);
        mTvLikeTips.setBackgroundResource(R.drawable.ic_well_chosen_right_slide_guide_tips);
//        mImgLike = new ImageView(getContext());
//        mImgLike.setLayoutParams(new ViewGroup.LayoutParams(UIUtils.getPixels(66),
//                UIUtils.getPixels(66)));
//        mImgLike.setBackgroundResource(R.drawable.img_like);
//        mImgLike.setScaleX(0.0F);
//        mImgLike.setScaleY(0.0F);

        mViewGuideLayer = new View(getContext());
        mViewGuideLayer.setBackgroundColor(Color.parseColor("#aa626567"));
        mViewGuideLayer.setAlpha(0);
        background.addView(mViewGuideLayer);
        background.addView(mTvLikeTips);
//        background.addView(mImgLike);

        mGuideAnim = ValueAnimator.ofFloat(0, 1, 1, 0);
        mGuideAnim.setStartDelay(DEFAULT_ANIM_STAY_TIME);
        mGuideAnim.setDuration(DEFAULT_ANIM_EXEC_TIME * 1 + DEFAULT_ANIM_STAY_TIME * 1);
        mGuideAnim.setInterpolator(new LinearInterpolator());
        mGuideAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                animating = true;
                super.onAnimationStart(animation);
                preViewSelect(targetView);

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                onEnd();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                onEnd();
            }

            private void onEnd() {
                removeChildDrawingOrderCallbackIfNecessary(targetView);
                // 恢复各种状态
//                if (mGuideAnimStatusListener != null) {
//                    mGuideAnimStatusListener.onEnd();
//                }
                mNeedShowGuide.set(false);
                hideAllGuideViews();
                setSlideContentVisible(true);
                animating = false;
            }
        });
        mGuideAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (adapter == null || adapter.isEmpty() || adapter.getItem(1) == null) {
                    return;
                }

                float animValue = (float) valueAnimator.getAnimatedValue();
                float absAnimValue = Math.abs(animValue);

//                if (mGuideAnimStatusListener != null) {
//                    mGuideAnimStatusListener.onAnimating(animValue);
//                }

                // 第一张的旋转
                targetView.setRotation(animValue * DEFAULT_ANIM_ROTATE_ANGLE);
                targetView.setTranslationX(animValue * DEFAULT_ANIM_ROTATE_X_OFFSET);
                targetView.setTranslationY(absAnimValue * DEFAULT_ANIM_ROTATE_Y_OFFSET);

                // 提示
                if (animValue >= 0) { // 喜欢的提示
                    if (mTvLikeTips != null && absAnimValue >= 0.5) {
                        float alphaPercent = (absAnimValue - 0.5F) * 2;
                        if (alphaPercent > 0.9F) {
                            alphaPercent = 1.0F;
                        }
                        if (alphaPercent < 0.1F) {
                            alphaPercent = 0.0F;
                        }
                        mTvLikeTips.setAlpha(alphaPercent);
                    }
                    if (mImgLike != null) {
                        mImgLike.setScaleX(absAnimValue);
                        mImgLike.setScaleY(absAnimValue);
                        mImgLike.setTranslationX((childWith / 2.0F - 20 - mImgLike.getMeasuredWidth() / 2.0F) * absAnimValue - mImgLike.getScaleX());
                        mImgLike.setTranslationY(absAnimValue * mImgLike.getMeasuredHeight() / 2.0F);
                    }
                }
                if (animValue == 1) {
                    overDrawChildZoom(targetView, false);
                }

                // 背景透明度
                mViewGuideLayer.setAlpha(absAnimValue);

                // 设置各种不能点击，包括上面

            }
        });
    }

    private void setSlideContentVisible(RecyclerView.ViewHolder viewHolder, boolean visible) {
        if (viewHolder != null && viewHolder instanceof WellChosenAdapter.ImageViewHolder) {

        }
    }

    private void setSlideContentVisible(boolean visible) {
        if (recyclerView == null) return;
        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(1);
        setSlideContentVisible(viewHolder, visible);
    }

    private void hideAllGuideViews() {
        if (mTvLikeTips != null) {
            mTvLikeTips.setVisibility(View.GONE);
        }
        if (mViewGuideLayer != null) {
            mViewGuideLayer.setVisibility(View.GONE);
        }
        if (mImgLike != null) {
            mImgLike.setVisibility(View.GONE);
        }
    }

    private void overDrawChildZoom(View child, boolean zoom) {
        child = child == null ? mOverdrawChild : child;
        if (child != null) {
            mOverdrawChild.animate().scaleY(zoom ? CHILD_SCALE_LARGE : CHILD_SCALE_NORMAL)
                    .scaleX(zoom ? CHILD_SCALE_LARGE : CHILD_SCALE_NORMAL)
                    .start();
        }
    }

    private void initView() {
        rootLayout = findViewById(R.id.well_chosen_rootlayout);
        recyclerView = (WellChosenRecyclerView) findViewById(R.id.well_chosen_recyclerview);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new WellChosenAdapter(list);
        recyclerView.setAdapter(adapter);
        CardItemTouchHelperCallback cardCallback = new CardItemTouchHelperCallback(recyclerView.getAdapter(), list);
        BringToFrontItemTouchHelperCallback callback = new BringToFrontItemTouchHelperCallback();
        final ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        mDragHelper = ViewDragHelper.create(recyclerView, 10f, new DragHelperCallback());
        moveDetector = new GestureDetectorCompat(MainActivity.this, new MoveDetector());
        recyclerView.setDragHelper(mDragHelper);
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                boolean shouldIntercept = mDragHelper.shouldInterceptTouchEvent(e);
                boolean moveFlag = moveDetector.onTouchEvent(e);
                int action = e.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    initLocs();
                    lastDownX = e.getX();
                    lastDownY = e.getY();
                    lastDownTime = e.getDownTime();
                    isLongPress = false;
                    // ACTION_DOWN时就让mDragHelper开始工作，否则有时候导致异常 (此为原作注释，未经试验 by jinxiao)
                } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                    // 由于moveDetector的存在（超过阈值才能拖动）
                    // ViewDragHelper无法接受到按下后不拖动直接抬起的事件 （onViewRelease处收不到）
                    // 所以在此拦截处处理缩放回原大小
                    overDrawChildZoom(null, false);
                } else if (action == MotionEvent.ACTION_MOVE) { // 长按放大
                    isLongPress = !isLongPress ? isLongPressed(e) : isLongPress;
                    if (isLongPress) {
                        final int actionIndex = MotionEventCompat.getActionIndex(e);
                        View view = mDragHelper.findTopChildUnder((int) e.getX(), (int) e.getY());
                        if (isDraggable(view) && mDragHelper.getCapturedView() != view) {
                            final int pointerId = e.getPointerId(actionIndex);
                            mDragHelper.captureChildView(view, pointerId);
                        }
                    }
                }

                handleByDragHelper = moveFlag;
                return shouldIntercept;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP ||
                        e.getAction() == MotionEvent.ACTION_CANCEL) {
                    isTouchBottom = false;
                }
                try {
                    mDragHelper.processTouchEvent(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }

            public boolean isLongPressed(MotionEvent e) {
                if ((Math.abs(e.getX() - lastDownX) < DRAG_SWIPE_SLOP
                        && Math.abs(e.getX() - lastDownX) < SCORLL_SLOP)
                        && Math.abs(e.getEventTime() - lastDownTime) >= LONG_PRESS_TIME) {
                    return true;
                }
                return false;
            }

        });
        manager = new GridLayoutManager(MainActivity.this, 2) {
            @Override
            public boolean canScrollVertically() {
                return !handleByDragHelper && !animating && super.canScrollVertically();
            }

        };
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.isHeaderView(position)) {
                    return 2;
                } else {
                    return 1;
                }
            }
        });
        recyclerView.setLayoutManager(manager);
//        touchHelper.attachToRecyclerView(recyclerView);


    }

    private void initData() {
        list.add(R.drawable.img_avatar_01);
        list.add(R.drawable.img_avatar_02);
        list.add(R.drawable.img_avatar_03);
        list.add(R.drawable.img_avatar_04);
        list.add(R.drawable.img_avatar_05);
        list.add(R.drawable.img_avatar_06);
        list.add(R.drawable.img_avatar_07);
        list.add(R.drawable.img_avatar_01);
        list.add(R.drawable.img_avatar_02);
        list.add(R.drawable.img_avatar_03);
        list.add(R.drawable.img_avatar_04);
        list.add(R.drawable.img_avatar_05);
        list.add(R.drawable.img_avatar_06);
        list.add(R.drawable.img_avatar_07);
    }

    private RecyclerView.ChildDrawingOrderCallback mChildDrawingOrderCallback = null;

    void removeChildDrawingOrderCallbackIfNecessary(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (view != null) {
                view.setTranslationZ(0);
            }
            if (view != mOverdrawChild && mOverdrawChild != null) {
                mOverdrawChild.setTranslationZ(0);
            }
            return;
        }
        if (view == mOverdrawChild) {
            mOverdrawChild = null;
            // only remove if we've added
            if (recyclerView != null && mChildDrawingOrderCallback != null) {
                recyclerView.setChildDrawingOrderCallback(null);
            }
        }
    }

    private void addChildDrawingOrderCallback() {
        if (Build.VERSION.SDK_INT >= 21) {// we use elevation on Lollipop
            if (mOverdrawChild != null) {
                mOverdrawChild.setTranslationZ(10);
            }
            return;
        }
        if (mChildDrawingOrderCallback == null) {
            mChildDrawingOrderCallback = new RecyclerView.ChildDrawingOrderCallback() {
                @Override
                public int onGetChildDrawingOrder(int childCount, int i) {
                    if (childCount == i) {
                        return childCount - 1;
                    }
                    if (mOverdrawChild == null) {
                        return i;
                    }
                    int childPosition = mOverdrawChildPosition;
                    if (childPosition == -1) {
                        childPosition = recyclerView.indexOfChild(mOverdrawChild);
                        mOverdrawChildPosition = childPosition;
                    }
                    if (childPosition >= childCount || childPosition == -1) {
                        return i;
                    }
                    Log.d("clip:", mOverdrawChildPosition + "," + childCount + "," + i);
                    if (i == childCount - 1) {
                        return childPosition;
                    }
                    return i < childPosition ? i : i + 1;
                }
            };
        }
        recyclerView.setChildDrawingOrderCallback(mChildDrawingOrderCallback);
    }

    /**
     * 松手时处理滑动到边缘的动画
     */
    private void animToSide(View changedView, float xvel, float yvel) {
        int finalX = initCenterViewX;
        int finalY = initCenterViewY;
        int flyType = -1;

        int dx = changedView.getLeft() - initCenterViewX;
        int dy = changedView.getTop() - initCenterViewY;
        if (dx == 0) {
            // 由于dx作为分母，此处保护处理
            dx = 1;
        }
        if (xvel > X_VEL_THRESHOLD || dx > X_DISTANCE_THRESHOLD) {
            flyType = VANISH_TYPE_RIGHT;
            isCardVanish = true;
            finalX = changedView.getLeft() + changedView.getWidth() * 2;
            finalY = dy * (changedView.getWidth() + initCenterViewX) / Math.abs(dx) + initCenterViewY;
        } else if (xvel < -X_VEL_THRESHOLD || dx < -X_DISTANCE_THRESHOLD) {
            flyType = VANISH_TYPE_LEFT;
            isCardVanish = false;
        } else {
            isCardVanish = false;
        }

        if (mDragHelper.smoothSlideViewTo(changedView, finalX, finalY)) {
            recyclerView.invalidate();
        }

    }

    /**
     * 点击按钮消失动画
     *
     * @param view RecycleView.holder的itemView
     */
    public void vanishOnBtnClick(View view) {
        synchronized (obj1) {
            isCardVanish = true;
            preViewSelect(view);
            int finalX = view.getLeft() + view.getWidth() * 2;
            int finalY = view.getTop();
            if (mDragHelper.smoothSlideViewTo(view, finalX, finalY)) {
                ViewCompat.postInvalidateOnAnimation(recyclerView);
            }
            itemVanished(view);
            isCardVanish = false;
        }
    }

    private void initLocs() {
        if (mLoctions == null && adapter.datalist != null) {
            mLoctions = new SparseArray<>();
            for (int i = 0; i < adapter.datalist.size(); i++) {
                View view = recyclerView.getChildAt(i);
                if (view != null) {
                    mLoctions.put(i, new Rect(view.getTop(), view.getLeft(), view.getBottom(), view.getRight()));
                }
            }
        }
    }

    public void itemVanished(View view) {
        if (view == null || recyclerView == null) {
            return;
        }
        RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
        if (viewHolder == null) return;

        int adapterPosition = viewHolder.getAdapterPosition();
        adapter.removeItem(adapterPosition);
        // 当没有数据时回调 mListener
        if (adapter.isEmpty()) {
            onCardsRanOut();
        }
    }

    public void onCardsRanOut() {
        Toast.makeText(MainActivity.this, "data clear", Toast.LENGTH_SHORT).show();
        recyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                initData();
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        }, 3000L);
    }

    public void scaleIconInCard(RecyclerView.ViewHolder viewHolder, float ratio, int direction) {
        WellChosenAdapter.ImageViewHolder myHolder = (WellChosenAdapter.ImageViewHolder) viewHolder;
        if (direction == VANISH_TYPE_LEFT) {
            myHolder.dislikeImageView.setAlpha(Math.abs(ratio));
        } else if (direction == VANISH_TYPE_RIGHT) {
            myHolder.likeImageView.setAlpha(Math.abs(ratio));
        }
    }

    public void resetIconInCard(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder == null) {
            return;
        }
        WellChosenAdapter.ImageViewHolder myHolder = (WellChosenAdapter.ImageViewHolder) viewHolder;
        viewHolder.itemView.setAlpha(1f);
        myHolder.dislikeImageView.setAlpha(0f);
        myHolder.likeImageView.setAlpha(0f);
    }

    private void resetBackupCardView(View changedView) {
        float rot = 0;//1.0f - SCALE_STEP * 2;
        changedView.setRotation(rot);
        changedView.setRotation(rot);
        changedView.setScaleX(1.0f);
        changedView.setScaleY(1.0f);
    }

    public boolean isDraggable(View view) {
        if (view == null || recyclerView == null || adapter == null) return false;
        RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
        return !adapter.isHeaderView(viewHolder);
    }

    private void preViewSelect(View child) {
        mOverdrawChildPosition = -1;
        if (mOverdrawChild != child) {
            removeChildDrawingOrderCallbackIfNecessary(mOverdrawChild);
            mOverdrawChild = child;
        }
        addChildDrawingOrderCallback();
        initCenterViewX = child.getLeft();
        initCenterViewY = child.getTop();
        overDrawChildZoom(child, true);
    }

    class MoveDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx,
                                float dy) {
            // 拖动了，touch不往下传递
            return Math.abs(dx) > Math.abs(dy) && (Math.abs(dx) > DRAG_SWIPE_SLOP && Math.abs(dy) < SCORLL_SLOP);
        }
    }


    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public void onViewDragStateChanged(int state) {
            if (state == ViewDragHelper.STATE_IDLE && lastDragState != state) { // 动画结束后重置
                animating = false;
            }
            lastDragState = state;
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            preViewSelect(capturedChild);
            isTouchBottom = lastDownY > capturedChild.getY() + capturedChild.getHeight() / 2;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            animating = true;
            allWidth = changedView.getMeasuredWidth();
            allHeight = changedView.getMeasuredHeight();
            centerX = allWidth / 2 + initCenterViewX;
            centerY = allHeight / 2;
            getCenterX(changedView);
            if (lastDragState == ViewDragHelper.STATE_DRAGGING) {
                isLastTouchBottom = isTouchBottom;
                changeViewRatation(changedView,
                        (swipeLeft ^ isTouchBottom ? -1 : 1) * getCenterX(changedView) * ROTATION);
            } else if (lastDragState == ViewDragHelper.STATE_SETTLING) {
                changeViewRatation(changedView,
                        (swipeLeft ^ isLastTouchBottom ? -1 : 1) * getCenterX(changedView) * ROTATION);
            }
            scaleIconInCard(recyclerView.getChildViewHolder(changedView), (left - initCenterViewX) * 1.0f / allWidth, swipeLeft ? VANISH_TYPE_LEFT : VANISH_TYPE_RIGHT);
        }

        private float getCenterX(View child) {
            int index = recyclerView.indexOfChild(mOverdrawChild);
            if (child.getWidth() / 2 + child.getX() - centerX < 0) {
                swipeLeft = true;
            } else {
                swipeLeft = false;
            }
            float width = Math.abs(child.getWidth() / 2 + child.getX() - centerX);
            if (width > centerX) {
                width = centerX;
            }
            return width / centerX;
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (animating) {
                return false;
            }

            return handleByDragHelper && isDraggable(child);
        }

        @Override
        public int getViewHorizontalDragRange(View child, int dx) {
            // 卡片实际能够水平移动的距离受clampViewPositionHorizontal()方法限制，并不受此方法限制
            // 此处的值用来控制拖拽过程中松手后，自动滑行的速度
//            if (isCardVanish) {
//                return Math.abs(dx) / 3;
//            }
            return Math.abs(dx) * ANIM_SPEED_DEFAULT;
        }


        @Override
        public int getViewVerticalDragRange(View child, int dy) {
//            if (isCardVanish) {
//                return Math.abs(dy) / 3;
//            }
            return Math.abs(dy) * ANIM_SPEED_DEFAULT;
        }

        @Override
        public void onViewReleased(final View releasedChild, float xvel, float yvel) {
            animating = true;
            animToSide(releasedChild, xvel, yvel);
            overDrawChildZoom(null, false);
            // 200L后进行remove操作，立即remove掉会导致view不再是recyclview的child
            // ChildDrawingOrderCallback失效，view将会在动画未完成前被绘制在底部
            recyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isCardVanish) {
                        resetIconInCard(recyclerView.getChildViewHolder(releasedChild));
                        itemVanished(releasedChild);
                        isCardVanish = false;
                    }
                }
            }, 200L);
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return left;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return top;
        }

        private void changeViewRatation(View changedView, float rotation) {
            if (Math.abs(Math.abs(rotation) - 0.037037037) <= 0.00001
                    || Math.abs(Math.abs(rotation) - 0.055555556) <= 0.00001) {//旋转角度为0.037037037或0.055555556时，部分手机布局内容会闪烁
                return;
            }
            changedView.setRotation(rotation);
        }

    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeChildDrawingOrderCallbackIfNecessary(mOverdrawChild);
        if (mGuideAnim != null) {
            mGuideAnim.cancel();
            mGuideAnim.removeAllListeners();
            mGuideAnim = null;
        }
    }

    private class WellChosenAdapter<T> extends RecyclerView.Adapter {
        private List<T> datalist;
        private final static int ITEM_TYPE_HEAD = 1;
        private final static int ITEM_TYPE_IMG = 2;

        WellChosenAdapter(List<T> datalist) {
            this.datalist = datalist;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ITEM_TYPE_HEAD) {
                return new QuoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.include_diandian_well_chosen_quote, parent, false));
            } else if (viewType == ITEM_TYPE_IMG) {
                return new ImageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diandian_well_chosen, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof WellChosenAdapter.ImageViewHolder) {
                resetBackupCardView(holder.itemView);
                resetIconInCard(holder);
                ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
                ImageView avatarImageView = imageViewHolder.avatarImageView;
                avatarImageView.setImageResource((Integer) (getItem(position)));
                imageViewHolder.btnLike.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        vanishOnBtnClick(holder.itemView);
                    }
                });
            }
        }

        T getItem(int position) {
            int pos = getRealPosition(position);
            return datalist != null && pos >= 0 && pos < datalist.size() ? datalist.get(pos) : null;
        }

        int getRealPosition(int pos) {
            return pos - getHeaderCount();
        }


        boolean isEmpty() {
            return datalist != null && datalist.size() == 0;
        }

        @Override
        public int getItemCount() {
            return getHeaderCount() + (datalist != null ? datalist.size() : 0);
        }

        public int getHeaderCount() {
            return 1;
        }

        public boolean isHeaderView(int pos) {
            return pos >= 0 && pos < getHeaderCount();
        }

        public boolean isHeaderView(RecyclerView.ViewHolder viewHolder) {
            return viewHolder instanceof WellChosenAdapter.QuoteViewHolder;
        }


        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return ITEM_TYPE_HEAD;
            } else {
                return ITEM_TYPE_IMG;
            }
        }

        public void removeItem(int pos) {
            if (getItem(pos) == null) {
                return;
            }
            int realPosition = getRealPosition(pos);
            datalist.remove(realPosition);
            notifyItemRemoved(pos);
        }

        public void addItem(T data) {
            datalist.add(data);
            int realPosition = datalist.size();
            notifyItemInserted(realPosition);
        }


        class QuoteViewHolder extends RecyclerView.ViewHolder {
            QuoteViewHolder(View itemView) {
                super(itemView);
            }
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {

            ImageView avatarImageView;
            ImageView likeImageView;
            ImageView dislikeImageView;
            ImageView btnLike;

            ImageViewHolder(View itemView) {
                super(itemView);
                avatarImageView = (ImageView) itemView.findViewById(R.id.iv_well_chosen_avatar);
                likeImageView = (ImageView) itemView.findViewById(R.id.iv_well_chosen_like_icon);
                dislikeImageView = (ImageView) itemView.findViewById(R.id.iv_well_chosen_dislike_icon);
                btnLike = (ImageView) itemView.findViewById(R.id.btn_well_chosen_like_vanish);
            }

        }
    }

}
