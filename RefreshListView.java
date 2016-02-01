package com.itheima.zhbj_17.views;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.itheima.zhbj_17.R;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

/**
 * @创建者	 伍碧林
 * @创时间 	 2015-12-21 下午4:49:51
 * @描述	     TODO
 *
 * @版本       $Rev: 64 $
 * @更新者     $Author: admin $
 * @更新时间    $Date: 2015-12-24 09:58:25 +0800 (周四, 24 十二月 2015) $
 * @更新描述    TODO
 */
public class RefreshListView extends ListView implements OnScrollListener, OnClickListener {
	private static final String	TAG						= "RefreshListView";
	private FrameLayout			mCustomContainer;
	private ProgressBar			mHeaderViewPb;
	private ImageView			mHeaderViewArrow;
	private TextView			mHeaderViewState;
	private TextView			mHeaderViewTime;
	private int					mRefreshHeaderViewMeasuredHeight;
	private int					mInitPaddingTop;
	private float				mDownX;
	private float				mDownY;
	private View				mHeaderView;

	// 下拉刷新
	public static final int		STATE_PULL_REFRESH		= 0;
	// 松开刷新
	public static final int		STATE_RELEASE_REFRESH	= 1;
	// 正在刷新
	public static final int		STATE_REFRESHING		= 2;

	//定义变量记录最新的状态
	public int					mCurState				= STATE_PULL_REFRESH;
	private RotateAnimation		mUp2DownAnimation;
	private RotateAnimation		mDown2UpAnimation;
	private View				mRefreshHeaderView;

	//表示是否即将显示出 下拉刷新头布局
	private boolean				isRefreshViewBeginShow	= true;
	private OnItemClickListener	mListener;
	private Drawable			mSel;

	//在代码中直接new的时候
	public RefreshListView(Context context) {
		this(context, null);
	}

	//在xml中使用的时候
	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		addHeaderView(context);
		addFooterView(context);
		initAnimation();
	}

	private void addFooterView(Context context) {
		View footerView = View.inflate(context, R.layout.inflate_refresh_footer, null);
		//找孩子
		mFooterViewPb = (ProgressBar) footerView.findViewById(R.id.footerview_pb);
		mFooterViewState = (TextView) footerView.findViewById(R.id.footerview_state);
		addFooterView(footerView);

		//监听listView的滚动
		this.setOnScrollListener(this);

		//给footerView设置点击事件
		footerView.setOnClickListener(this);
	}

	public void addHeaderView(Context context) {
		//下拉刷新头布局的容器
		mHeaderView = View.inflate(context, R.layout.inflate_refreshlistview_headerview, null);

		//找出下拉刷新的头布局
		mRefreshHeaderView = mHeaderView.findViewById(R.id.refreshlistview_refresh_headerview);

		//下拉刷新头布局里面的孩子
		mHeaderViewPb = (ProgressBar) mHeaderView.findViewById(R.id.refreshlistview_headerview_pb);
		mHeaderViewArrow = (ImageView) mHeaderView.findViewById(R.id.refreshlistview_headerview_arrow);
		mHeaderViewState = (TextView) mHeaderView.findViewById(R.id.refreshlistview_headerview_state);
		mHeaderViewTime = (TextView) mHeaderView.findViewById(R.id.refreshlistview_headerview_updatetime);
		this.addHeaderView(mHeaderView);

		//得到下拉刷新头布局的高度
		/**
		 UNSPECIFIED 不确定 wrap_content-->应有多大,系统就分配多大
		 EXACTLY  精确的 match_parent 100px 100dp-->不管自身有多大,要求系统给我分配这么多
		 AT_MOST  最大 情况用的比较少
		 */
		/*
		 int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);//自身有多大,系统就分配多大给我
		int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);//自身有多大,系统就分配多大给我
		refreshHeaderView.measure(widthMeasureSpec, heightMeasureSpec);*/

		mRefreshHeaderView.measure(0, 0);
		mRefreshHeaderViewMeasuredHeight = mRefreshHeaderView.getMeasuredHeight();

		//完全隐藏下拉刷新头布局
		mInitPaddingTop = -mRefreshHeaderViewMeasuredHeight;
		//下拉刷新头布局的父容器设置了paddingTop
		mHeaderView.setPadding(0, mInitPaddingTop, 0, 0);

		mCustomContainer = (FrameLayout) mHeaderView.findViewById(R.id.refreshlistview_headerview_custom_container);
	}

	private void initAnimation() {
		mUp2DownAnimation =
				new RotateAnimation(180, 360, RotateAnimation.RELATIVE_TO_SELF, .5f, RotateAnimation.RELATIVE_TO_SELF,
						.5f);
		mUp2DownAnimation.setDuration(400);
		mUp2DownAnimation.setFillAfter(true);

		mDown2UpAnimation =
				new RotateAnimation(0, 180, RotateAnimation.RELATIVE_TO_SELF, .5f, RotateAnimation.RELATIVE_TO_SELF,
						.5f);
		mDown2UpAnimation.setDuration(400);
		mDown2UpAnimation.setFillAfter(true);
	}

	/**
	 * 为ListView添加自定义的头布局
	 * @param customHeaderView
	 */
	public void addCustomHeaderView(View customHeaderView) {
		mCustomContainer.addView(customHeaderView);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.i(TAG, "MotionEvent.ACTION_DOWN");
			mDownX = ev.getRawX();
			mDownY = ev.getRawY();
			break;
		case MotionEvent.ACTION_MOVE:

			//判断如果当前的状态是正在刷新,我们就直接返回
			if (mCurState == STATE_REFRESHING) {
				return super.onTouchEvent(ev);
			}

			Log.i(TAG, "MotionEvent.ACTION_MOVE");
			float moveX = ev.getRawX();
			float moveY = ev.getRawY();

			if (mDownX == 0 && mDownY == 0) {
				mDownX = moveX;
				mDownY = moveY;
			}

			int diffX = (int) (moveX - mDownX + .5f);//水平拖动的距离
			int diffY = (int) (moveY - mDownY + .5f);//垂直拖动的距离-->和它有关系

			//分情况处理action_move
			//下拉刷新布局左下角.Y>=ListView左上角.Y并且是下拉操作

			//下拉刷新布局左上角.Y
			int[] mRefreshHeaderViewArr = new int[2];
			mRefreshHeaderView.getLocationOnScreen(mRefreshHeaderViewArr);
			int mRefreshHeaderViewTopY = mRefreshHeaderViewArr[1];

			//下拉刷新布局左下角.Y
			int mRefreshHeaderViewBottomY = mRefreshHeaderViewTopY + mRefreshHeaderViewMeasuredHeight;

			//ListView左上角.Y
			int[] refreshListViewLocationArr = new int[2];
			this.getLocationOnScreen(refreshListViewLocationArr);
			int refreshListViewTopY = refreshListViewLocationArr[1];

			if (mRefreshHeaderViewBottomY >= refreshListViewTopY && diffY > 0) {//看不到滚动条

				//不希望触发item的点击事件-->置空的点击事件对象
				super.setOnItemClickListener(null);
				//不希望item产生selector效果
				super.setSelector(new ColorDrawable(Color.TRANSPARENT));

				if (isRefreshViewBeginShow) {
					//重置downY,重置diffY
					mDownY = moveY;
					diffY = (int) (moveY - mDownY + .5f);
					isRefreshViewBeginShow = false;
				}

				//paddingTop可以控制下拉刷新头布局的显示和隐藏
				int paddingTop = mInitPaddingTop + diffY;

				//设置最新的paddingTop
				mHeaderView.setPadding(0, paddingTop, 0, 0);

				//根据paddintTop的正负值切换当前的下拉刷新的状态
				if (paddingTop > 0 && mCurState != STATE_RELEASE_REFRESH) {//已经完全显示除了下拉刷新头布局-->松开刷新
					Log.i(TAG, "松开刷新");
					mCurState = STATE_RELEASE_REFRESH;
					refreshHeaderViewByState();
				} else if (paddingTop <= 0 && mCurState != STATE_PULL_REFRESH) {//下拉刷新头布局,还有一部分没有完全显示出来-->下拉刷新
					Log.i(TAG, "下拉刷新");
					mCurState = STATE_PULL_REFRESH;
					refreshHeaderViewByState();
				}
				return true;
			} else {//listView自己处理事件
				return super.onTouchEvent(ev);
			}

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			//重置相关的值
			if (isRefreshViewBeginShow) {
				//			还原item的点击事件
				super.setOnItemClickListener(mListener);

				//			还原selector设置效果
				super.setSelector(mSel);
			}

			/**
			 isRefreshViewBeginShow = true-->下拉刷新头布局现在是什么情况-->完成隐藏的
			 */
			isRefreshViewBeginShow = true;

			/**
			1.下拉刷新-->下拉刷新
			2.松开刷新-->正在刷新
			 */
			if (mCurState == STATE_PULL_REFRESH) {
				mCurState = STATE_PULL_REFRESH;
				refreshHeaderViewByState();

				//修改paddingtop->控制下拉刷新头布局的显示隐藏-->完全隐藏
				//				mHeaderView.setPadding(0, mInitPaddingTop, 0, 0);

				changePaddingTopAnimation(mHeaderView.getPaddingTop(), mInitPaddingTop);

			} else if (mCurState == STATE_RELEASE_REFRESH) {
				mCurState = STATE_REFRESHING;
				refreshHeaderViewByState();

				//修改paddingtop->控制下拉刷新头布局的显示隐藏-->完全显示
				//				mHeaderView.setPadding(0, 0, 0, 0);

				changePaddingTopAnimation(mHeaderView.getPaddingTop(), 0);

				//通知外界.现在已经是正在刷新的状态
				if (mOnRefreshListener != null) {
					mOnRefreshListener.onRefresh(this);
				}
			}

			break;

		default:
			break;
		}
		return super.onTouchEvent(ev);
	}

	/**
	 * 根据最新的下拉刷新的状态更新下拉刷新布局的ui
	 */
	private void refreshHeaderViewByState() {

		switch (mCurState) {
		case STATE_PULL_REFRESH://切换为 下拉刷新
			//进度条
			mHeaderViewPb.setVisibility(View.INVISIBLE);
			//箭头
			mHeaderViewArrow.setVisibility(View.VISIBLE);
			//箭头由下转到上

			mHeaderViewArrow.startAnimation(mUp2DownAnimation);

			//下拉刷新的状态
			mHeaderViewState.setText("下拉刷新");
			//更新时间-->不做处理
			break;
		case STATE_RELEASE_REFRESH://切换为 松开刷新
			//进度条
			mHeaderViewPb.setVisibility(View.INVISIBLE);
			//箭头
			mHeaderViewArrow.setVisibility(View.VISIBLE);
			//箭头由下转到上
			mHeaderViewArrow.startAnimation(mDown2UpAnimation);

			//下拉刷新的状态
			mHeaderViewState.setText("松开刷新");
			//更新时间-->不做处理
			break;
		case STATE_REFRESHING://切换为正在刷新
			//清空动画
			mHeaderViewArrow.clearAnimation();

			//进度条
			mHeaderViewPb.setVisibility(View.VISIBLE);
			//箭头
			mHeaderViewArrow.setVisibility(View.INVISIBLE);
			//下拉刷新的状态
			mHeaderViewState.setText("正在刷新");
			//更新时间
			mHeaderViewTime.setText(getUpdateTime());
			break;

		default:
			break;
		}
	}

	private CharSequence getUpdateTime() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

	@Override
	public void setOnItemClickListener(android.widget.AdapterView.OnItemClickListener listener) {
		mListener = listener;
		super.setOnItemClickListener(listener);
	}

	@Override
	public void setSelector(Drawable sel) {
		mSel = sel;
		super.setSelector(sel);
	}

	/**
	 * 以动画的方式修改paddingTop
	 */
	public void changePaddingTopAnimation(int start, int end) {
		//ValueAnimator-->帮助我们生成我们想要的渐变值
		ValueAnimator valueAnimator = ValueAnimator.ofInt(start, end);
		valueAnimator.setDuration(400);
		valueAnimator.start();

		//监听动画执行过程中的渐变值
		valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator value) {
				int paddintTop = (Integer) value.getAnimatedValue();
				mHeaderView.setPadding(0, paddintTop, 0, 0);
			}
		});
	}

	/**
	 * 1.开启下拉刷新的效果
	 */
	public void startRefresh() {
		mCurState = STATE_REFRESHING;
		//状态改变了.刷新ui
		refreshHeaderViewByState();

		//修改paddingTop
		changePaddingTopAnimation(mHeaderView.getPaddingTop(), 0);
	}

	/**
	 * 2.结束下拉刷新的效果
	 */
	public void stopRefresh() {
		mCurState = STATE_RELEASE_REFRESH;
		//状态改变了.刷新ui
		refreshHeaderViewByState();

		//修改paddingTop
		changePaddingTopAnimation(mHeaderView.getPaddingTop(), mInitPaddingTop);
	}

	/**
	 * 3.监听用户的下拉操作-->触发重新加载数据
	 */
	/**
	 	1.定义接口,接口方法
		2.声明接口对象
		3.在某一个时刻,使用接口对象,调用接口方法,开始传值
		4.暴露公共的方法
	 */
	public interface OnRefreshListener {
		void onRefresh(RefreshListView refreshListView);

		void onLoadMore(RefreshListView refreshListView);
	}

	OnRefreshListener	mOnRefreshListener;
	private ProgressBar	mFooterViewPb;
	private TextView	mFooterViewState;
	private boolean		isLoadMoreSuccess;
	private boolean		isLoadingMore;

	public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
		mOnRefreshListener = onRefreshListener;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		//如何得知滑到底部呢?
		if (getLastVisiblePosition() == getAdapter().getCount() - 1) {
			if (scrollState == SCROLL_STATE_IDLE || scrollState == SCROLL_STATE_FLING) {

				if (isLoadingMore) {
					return;
				}
				//现在就可以出发加载更多的数据
				if (mOnRefreshListener != null) {
					mOnRefreshListener.onLoadMore(this);
					isLoadingMore = true;
				}
			}
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// TODO

	}

	/**
	 * 是否有加载更多
	 * @param b
	 */
	public void setHasLoadMore(boolean hasLoadMore) {
		if (hasLoadMore) {
			//pb
			mFooterViewPb.setVisibility(View.VISIBLE);
			//state
			mFooterViewState.setText("正在加载更多...");
		} else {
			//pb
			mFooterViewPb.setVisibility(View.GONE);
			//state
			mFooterViewState.setText("没有加载更多");

		}

	}

	/**
	 * 加载更多完成
	 * @param isLoadMoreSuccess
	 */
	public void stopLoadMore(boolean isLoadMoreSuccess) {
		isLoadingMore = false;

		//把是否加载失败,保存为成员变量
		this.isLoadMoreSuccess = isLoadMoreSuccess;

		if (isLoadMoreSuccess) {//加载更多成功
			//pb
			mFooterViewPb.setVisibility(View.VISIBLE);
			//state
			mFooterViewState.setText("正在加载更多...");
		} else {//加载更多失败
			//pb
			mFooterViewPb.setVisibility(View.GONE);
			//state
			mFooterViewState.setText("点击重试");
		}

	}

	@Override
	public void onClick(View v) {

		if (!isLoadMoreSuccess) {
			//再一次触发,现在可以加载更多的数据
			if (mOnRefreshListener != null) {
				mOnRefreshListener.onLoadMore(this);

				//更新ui
				//pb
				mFooterViewPb.setVisibility(View.VISIBLE);
				//state
				mFooterViewState.setText("正在加载更多...");
			}
		}
	}

}
