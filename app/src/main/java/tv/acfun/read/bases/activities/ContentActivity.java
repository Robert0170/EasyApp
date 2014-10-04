package tv.acfun.read.bases.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.harreke.easyapp.beans.ActionBarItem;
import com.harreke.easyapp.frameworks.bases.activity.ActivityFramework;
import com.harreke.easyapp.requests.IRequestCallback;
import com.harreke.easyapp.widgets.InfoView;

import java.util.ArrayList;

import tv.acfun.read.R;
import tv.acfun.read.api.API;
import tv.acfun.read.bases.fragments.ContentFragment;
import tv.acfun.read.beans.ArticleContent;
import tv.acfun.read.beans.Content;
import tv.acfun.read.parsers.ContentListParser;

/**
 * 由 Harreke（harreke@live.cn） 创建于 2014/09/25
 */
public class ContentActivity extends ActivityFramework {
    private View content_blank;
    private View content_comments_button;
    private View content_favourite_add_button;
    private View content_favourtie_remove_button;
    private ViewPager content_pager;
    private PagerTabStrip content_pager_indicator;
    private View content_share_button;
    private IRequestCallback<String> mCallback;
    private View.OnClickListener mClickListener;
    private Content mContent;
    private int mContentId;
    private ArrayList<ArrayList<ArticleContent>> mPageList;
    private ArrayList<String> mPageTitleList;
    private Task mTask;
    private int mTotalPage = 0;

    public static Intent create(Context context, int contentId) {
        Intent intent = new Intent(context, ContentActivity.class);

        intent.putExtra("contentId", contentId);

        return intent;
    }

    @Override
    public void assignEvents() {
        content_comments_button.setOnClickListener(mClickListener);
    }

    @Override
    public void initData(Intent intent) {
        mContentId = intent.getIntExtra("contentId", 0);
    }

    @Override
    public void newEvents() {
        mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.content_comments_button:
                        start(CommentActivity.create(getActivity(), mContent), false);
                        getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out);
                }
            }
        };
        mCallback = new IRequestCallback<String>() {
            @Override
            public void onFailure() {
                setInfoVisibility(InfoView.INFO_ERROR);
            }

            @Override
            public void onSuccess(String result) {
                mTask = new Task();
                mTask.execute(result);
            }
        };
    }

    @Override
    public void onActionBarItemClick(int position, ActionBarItem item) {

    }

    @Override
    public void onActionBarMenuCreate() {

    }

    @Override
    protected void onDestroy() {
        if (isRequestExecuting()) {
            cancelRequest();
        }
        if (mTask != null && mTask.isCancelled()) {
            mTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public void queryLayout() {
        content_pager = (ViewPager) findContentView(R.id.content_pager);
        content_pager_indicator = (PagerTabStrip) findContentView(R.id.content_pager_indicator);
        content_pager_indicator.setTabIndicatorColorResource(R.color.Theme);
        content_pager_indicator.setTextColor(getResources().getColor(R.color.Title));

        content_blank = findContentView(R.id.content_blank);
        content_comments_button = findContentView(R.id.content_comments_button);
        content_favourite_add_button = findContentView(R.id.content_favourite_add_button);
        content_favourtie_remove_button = findContentView(R.id.content_favourtie_remove_button);
        content_share_button = findContentView(R.id.content_share_button);
    }

    @Override
    public void setLayout() {
        setContentView(R.layout.activity_content);
    }

    @Override
    public void startAction() {
        setInfoVisibility(InfoView.INFO_LOADING);
        executeRequest(API.getArticleContent(mContentId), mCallback);
    }

    private class Adapter extends FragmentPagerAdapter {
        public Adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mTotalPage;
        }

        @Override
        public Fragment getItem(int position) {
            return ContentFragment.create(mContent, mPageList.get(position));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPageTitleList.get(position);
        }
    }

    private class Task extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            ContentListParser parser = ContentListParser.parse(params[0]);

            if (parser != null) {
                mContent = parser.getContent();
                mTotalPage = parser.getTotalPage();
                mPageList = parser.getPageList();
                mPageTitleList = parser.getPageTitleList();

                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onCancelled() {
            mTask = null;
            setInfoVisibility(InfoView.INFO_ERROR);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mTask = null;
            if (result) {
                if (mTotalPage == 0) {
                    setInfoVisibility(InfoView.INFO_ERROR);
                } else {
                    setInfoVisibility(InfoView.INFO_HIDE);
                    if (mTotalPage == 1) {
                        content_pager_indicator.setVisibility(View.GONE);
                        content_blank.setVisibility(View.GONE);
                    } else {
                        content_pager_indicator.setVisibility(View.VISIBLE);
                        content_blank.setVisibility(View.VISIBLE);
                    }
                    content_pager.setAdapter(new Adapter(getSupportFragmentManager()));
                }
            } else {
                setInfoVisibility(InfoView.INFO_ERROR);
            }
        }
    }
}