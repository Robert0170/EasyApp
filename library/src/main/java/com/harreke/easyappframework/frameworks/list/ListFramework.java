package com.harreke.easyappframework.frameworks.list;

import android.content.Context;
import android.view.View;
import android.widget.AbsListView;

import com.harreke.easyappframework.R;
import com.harreke.easyappframework.frameworks.bases.IFramework;
import com.harreke.easyappframework.listeners.OnSlidableTriggerListener;
import com.harreke.easyappframework.loaders.ILoader;
import com.harreke.easyappframework.requests.IRequestCallback;
import com.harreke.easyappframework.requests.RequestBuilder;
import com.harreke.easyappframework.widgets.InfoView;
import com.harreke.easyappframework.widgets.slidableview.SlidableView;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * 由 Harreke（harreke@live.cn） 创建于 2014/07/31
 *
 * 列表框架
 *
 * 对SlidableView进行封装的列表框架，支持任意列表视图
 * 这是一个抽象类，对于不同的AbsListView（ListView、GridView等），需要继承这个类并实现对应的抽象方法
 *
 * 所谓的列表视图，是指必须依靠Adapter管理数据的视图，如AbsListView、RecyclerView等
 * 支持ListView、GridView，以及其他衍生于AbsListView的视图
 * 不支持不依靠Adapter管理数据的视图
 *
 * 注：
 * ListView和GridView架构相近（视图单元是一个简单单元），GridView可以直接使用ListView的实现类；
 * 虽然ExpandableListView同样衍生于AbsListView，但是由于架构的不同（视图单元是一个复合单元，由一个父单元和多个子单元复合而成），不能直接使用ListView的实现类，需要另外实现ExpandableListView的方法
 * 其他架构有别于ListView的视图，也需要另外实现对应的方法
 *
 * @param <ITEM>
 *         列表条目类型
 *
 *         列表视图单元的类型，是列表的最基本组成
 * @param <LOADER>
 *         列表Loader类型
 *         列表填充数据时，数据的结构类型
 *
 *         待填充进列表的数据需按一定结构储存，以便被框架识别与解析
 */
public abstract class ListFramework<ITEM, LOADER extends ILoader<ITEM>>
        implements IList<ITEM>, IListStatusChageListener<ITEM>, IRequestCallback<LOADER>, OnSlidableTriggerListener, View.OnClickListener {
    private static int ACTION_LOAD = 1;
    private static int ACTION_NONE = 0;

    private int mActionType = ACTION_NONE;
    private static int ACTION_REFRESH = 2;
    private Comparator<ITEM> mComparator = null;
    private String mCompleteText;
    private int mCurrentPage = 1;
    private String mErrorText;
    private IFramework mFramework;
    private InfoView mInfo = null;
    private String mLastText;
    private boolean mLoadEnabled = false;
    private int mPageSize = 0;
    private boolean mReverseScroll = false;
    private View mRoot = null;
    private SlidableView mSlidableView = null;
    private int mTotalPage = 1;

    public ListFramework(IFramework framework, int listId, int slidableViewId) {
        Context context;
        View listView;
        View slidableView;

        if (framework == null) {
            throw new IllegalArgumentException("Framework must not be null!");
        } else {
            listView = framework.queryContent(listId);
            if (listView == null || !(listView instanceof AbsListView)) {
                throw new IllegalArgumentException("Invalid listId!");
            }
            setListView(listView);
            if (slidableViewId > 0) {
                slidableView = framework.queryContent(slidableViewId);
                if (slidableView == null || !(slidableView instanceof SlidableView)) {
                    throw new IllegalArgumentException("Invalid slidableViewId!");
                }
                mSlidableView = (SlidableView) slidableView;
                mSlidableView.setSlidableContentView(listId);
                mSlidableView.setOnSlidableTriggerListener(this);
            }
            context = framework.getActivity();
            mErrorText = context.getString(R.string.info_retry);
            mLastText = context.getString(R.string.list_last);
            mCompleteText = context.getString(R.string.list_complete);
            mFramework = framework;
            setRootView(framework.getContent());
            setInfoView(framework.getInfo());
        }
    }

    /**
     * 停止加载列表
     */
    public final void cancel() {
        mFramework.cancelRequest();
        if (mActionType != ACTION_NONE) {
            mActionType = ACTION_NONE;
            setRefreshComplete();
            setLoadComplete(-1);
        }
    }

    /**
     * 清空列表
     */
    @Override
    public void clear() {
        cancel();
        mCurrentPage = 1;
    }

    /**
     * 销毁框架
     */
    public final void destroy() {
        cancel();
        clear();
        mFramework = null;
    }

    /**
     * 填充列表视图
     *
     * @param list
     *         项目列表
     */
    public final void from(ArrayList<ITEM> list) {
        from(list, false);
    }

    /**
     * 填充列表视图
     *
     * @param list
     *         项目列表
     * @param reverse
     *         是否倒转列表顺序
     */
    public final void from(ArrayList<ITEM> list, boolean reverse) {
        int i;

        if (list != null) {
            onPreAction();
            mPageSize = list.size();
            if (reverse) {
                for (i = mPageSize - 1; i > -1; i--) {
                    onParseItem(list.get(i));
                }
            } else {
                for (i = 0; i < mPageSize; i++) {
                    onParseItem(list.get(i));
                }
            }
            if (mComparator != null) {
                sort(mComparator);
            }
            onPostAction();
        }
    }

    /**
     * 填充列表视图
     *
     * @param list
     *         项目列表
     */
    public final void from(ITEM[] list) {
        from(list, false);
    }

    /**
     * 填充列表视图
     *
     * @param list
     *         项目列表
     * @param reverse
     *         是否倒转列表顺序
     */
    public final void from(ITEM[] list, boolean reverse) {
        int i;

        if (list != null) {
            onPreAction();
            mPageSize = list.length;
            if (reverse) {
                for (i = mPageSize - 1; i > -1; i--) {
                    onParseItem(list[i]);
                }
            } else {
                for (i = 0; i < mPageSize; i++) {
                    onParseItem(list[i]);
                }
            }
            if (mComparator != null) {
                sort(mComparator);
            }
            onPostAction();
        }
    }

    /**
     * 填充列表视图，从网络加载内容
     *
     * @param builder
     *         Http请求
     */
    public final void from(RequestBuilder builder) {
        if (builder != null) {
            onPreAction();
            mFramework.executeRequest(builder, this);
        }
    }

    /**
     * 获得动作类型
     *
     * {@link #ACTION_LOAD}
     * 加载列表
     * {@link #ACTION_REFRESH}
     * 刷新列表
     *
     * @return 动作类型
     */
    public final int getActionType() {
        return mActionType;
    }

    /**
     * 获得列表当前页面序号
     *
     * @return int
     */
    public final int getCurrentPage() {
        return mCurrentPage;
    }

    /**
     * 获得列表上一次加载的项目数量
     *
     * @return 上一次加载的项目数量
     */
    public final int getPageSize() {
        return mPageSize;
    }

    private void hideToast(boolean animate) {
        mFramework.hideToast(animate);
    }

    /**
     * 判断当前网络页面是否为第一页
     *
     * @return boolean
     */
    public final boolean isFirstPage() {
        return mCurrentPage <= 1;
    }

    /**
     * 判断当前网络页面是否为最后一页
     *
     * @return 是否为最后一页
     */
    public final boolean isLastPage() {
        return mCurrentPage >= mTotalPage;
    }

    /**
     * 判断是否启用加载更多功能
     *
     * @return 是否启用加载更多功能
     */
    public final boolean isLoadEnabled() {
        return mLoadEnabled;
    }

    /**
     * 判断是否正在访问网络
     *
     * @return 是否正在访问网络
     */
    public boolean isLoading() {
        return mFramework != null && mFramework.isRequestExecuting();
    }

    /**
     * 判断是否反转“加载更多”的触发条件
     *
     * @return 如果为false，则向下滑动至末尾会触发“加载更多”；否则为向上滑动至顶端会触发“加载更多”
     */
    public final boolean isReverseScroll() {
        return mReverseScroll;
    }

    public final boolean isShowingRetry() {
        return mInfo != null && mInfo.isShowingRetry();
    }

    /**
     * 列表翻至下一页
     */
    public final void nextPage() {
        if (mCurrentPage < mTotalPage) {
            mCurrentPage++;
        }
    }

    @Override
    public void onClick(View v) {
        onRefreshTrigger();
    }

    /**
     * 当列表加载错误时触发
     */
    @Override
    public void onError() {
        refresh();
        previousPage();
        if (isEmpty()) {
            if (mRoot != null) {
                mRoot.setVisibility(View.GONE);
            }
            if (mInfo != null) {
                mInfo.setInfoVisibility(InfoView.INFO_ERROR);
            }
        } else {
            if (mRoot != null) {
                mRoot.setVisibility(View.VISIBLE);
            }
            if (mInfo != null) {
                mInfo.setInfoVisibility(InfoView.INFO_HIDE);
            }
            mFramework.showToast(mErrorText, false);
        }
    }

    @Override
    public void onFailure() {
        onError();
    }

    /**
     * 触发开始加载
     */
    @Override
    public void onLoadTrigger() {
        if (!isLoading()) {
            setLoadStart();
            mActionType = ACTION_LOAD;
            nextPage();
            onAction();
        }
    }

    /**
     * 当列表加载完成时触发
     */
    @Override
    public void onPostAction() {
        refresh();
        if (mActionType == ACTION_REFRESH) {
            setRefreshComplete();
        } else if (mActionType == ACTION_LOAD && !isFirstPage()) {
            if (mSlidableView != null) {
                setLoadComplete(mPageSize);
            } else {
                showToast(String.format(mCompleteText, mPageSize));
            }
        }
        mActionType = ACTION_NONE;
        if (isEmpty()) {
            if (mRoot != null) {
                mRoot.setVisibility(View.GONE);
            }
            if (mInfo != null) {
                mInfo.setInfoVisibility(InfoView.INFO_EMPTY);
            }
        } else {
            if (mRoot != null) {
                mRoot.setVisibility(View.VISIBLE);
            }
            if (mInfo != null) {
                mInfo.setInfoVisibility(InfoView.INFO_HIDE);
            }
            if (mCurrentPage > 1 && isLastPage()) {
                showToast(mLastText);
            }
        }
    }

    /**
     * 当列表加载开始时触发
     */
    @Override
    public void onPreAction() {
        if (isEmpty()) {
            if (mRoot != null) {
                mRoot.setVisibility(View.GONE);
            }
            if (mInfo != null) {
                mInfo.setInfoVisibility(InfoView.INFO_LOADING);
            }
        } else if (mActionType == ACTION_LOAD) {
            setLoadStart();
        }
    }

    /**
     * 触发开始刷新
     */
    @Override
    public void onRefreshTrigger() {
        if (!isLoading()) {
            setRefreshStart();
            mActionType = ACTION_REFRESH;
            clear();
            refresh();
            onAction();
        }
    }

    @Override
    public void onSuccess(LOADER loader) {
        ArrayList<ITEM> list;
        int i;

        if (loader.isSuccess()) {
            loader.parse();
            list = loader.getList();
            if (list == null) {
                list = new ArrayList<ITEM>();
            }
            mPageSize = list.size();
            if (mPageSize == 0) {
                previousPage();
                mTotalPage = mCurrentPage;
            } else {
                for (i = 0; i < mPageSize; i++) {
                    onParseItem(list.get(i));
                }
            }
            if (mComparator != null) {
                sort(mComparator);
            }
            onPostAction();
        } else {
            onError();
        }
    }

    /**
     * 列表翻至前一页
     */
    public final void previousPage() {
        if (mCurrentPage > 1) {
            mCurrentPage--;
        }
    }

    /**
     * 设置列表的排序比较器
     *
     * @param comparator
     *         比较器
     */
    public final void setComparator(Comparator<ITEM> comparator) {
        mComparator = comparator;
    }

    /**
     * 设置覆盖层
     *
     * @param info
     *         覆盖层
     */
    public final void setInfoView(InfoView info) {
        mInfo = info;
        if (info != null) {
            info.setOnClickListener(this);
        }
    }

    /**
     * 手动触发结束加载状态
     *
     * @param pageSize
     *         页面加载的条目数
     *
     *         -1为不需要显示加载条目数
     */
    public final void setLoadComplete(int pageSize) {
        if (mSlidableView != null) {
            mSlidableView.setLoadComplete(pageSize);
        }
    }

    /**
     * 设置是否启用加载更多功能
     *
     * 当列表滑动至底部时，是否触发加载更多状态，以便自动加载下一页
     *
     * @param loadEnabled
     *         是否启用加载更多功能
     */
    public final void setLoadEnabled(boolean loadEnabled) {
        mLoadEnabled = loadEnabled;
    }

    /**
     * 手动触发开始加载状态
     */
    public final void setLoadStart() {
        if (mSlidableView != null) {
            mSlidableView.setLoadStart();
        }
    }

    /**
     * 手动触发结束刷新状态
     */
    public final void setRefreshComplete() {
        if (mSlidableView != null) {
            mSlidableView.setRefreshComplete();
        }
    }

    /**
     * 手动触发开始刷新状态
     */
    public final void setRefreshStart() {
        if (mSlidableView != null) {
            mSlidableView.setRefreshStart();
        }
    }

    /**
     * 设置是否反转“加载更多”的触发条件
     *
     * @param reverseScroll
     *         如果为false，则向下滑动至末尾会触发“加载更多”；否则为向上滑动至顶端会触发“加载更多”
     */
    public final void setReverseScroll(boolean reverseScroll) {
        mReverseScroll = reverseScroll;
    }

    /**
     * 设置内容层
     *
     * @param root
     *         内容层
     */
    public final void setRootView(View root) {
        mRoot = root;
    }

    /**
     * 设置“空内容”时是否显示“重试”按钮
     *
     * @param showRetryHintWhenEmpty
     *         “空内容”时是否需要显示“重试”按钮
     */
    public final void setShowRetryHintWhenEmpty(boolean showRetryHintWhenEmpty) {
        if (mInfo != null) {
            mInfo.setShowRetryWhenEmpty(showRetryHintWhenEmpty);
        }
    }

    /**
     * 设置列表的总页数
     *
     * @param totalPage
     *         总页数
     */
    public final void setTotalPage(int totalPage) {
        mTotalPage = totalPage;
    }

    private void showToast(String text) {
        mFramework.showToast(text);
    }
}