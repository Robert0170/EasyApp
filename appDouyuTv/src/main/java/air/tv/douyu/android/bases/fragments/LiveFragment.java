package air.tv.douyu.android.bases.fragments;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.harreke.easyapp.frameworks.bases.IFramework;
import com.harreke.easyapp.frameworks.bases.activity.ActivityFramework;
import com.harreke.easyapp.frameworks.bases.fragment.FragmentFramework;
import com.harreke.easyapp.frameworks.recyclerview.RecyclerFramework;
import com.harreke.easyapp.frameworks.recyclerview.RecyclerHolder;

import java.util.List;

import air.tv.douyu.android.R;
import air.tv.douyu.android.api.API;
import air.tv.douyu.android.bases.activities.RoomActivity;
import air.tv.douyu.android.beans.Room;
import air.tv.douyu.android.holders.RoomHolder;
import air.tv.douyu.android.parsers.LiveListParser;

/**
 * 由 Harreke（harreke@live.cn） 创建于 2014/12/18
 */
public class LiveFragment extends FragmentFramework {
    private LiveRecyclerHelper mLiveRecyclerHelper;

    public static LiveFragment create() {
        return new LiveFragment();
    }

    @Override
    protected void acquireArguments(Bundle bundle) {

    }

    @Override
    public void attachCallbacks() {

    }

    @Override
    public void enquiryViews() {
        mLiveRecyclerHelper = new LiveRecyclerHelper(this);
        mLiveRecyclerHelper.setHasFixedSize(true);
        mLiveRecyclerHelper.setLayoutManager(new GridLayoutManager(getContext(), 2));
        mLiveRecyclerHelper.attachAdapter();
    }

    @Override
    public void establishCallbacks() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_live;
    }

    @Override
    public void startAction() {
        mLiveRecyclerHelper.from(API.getLive(20, mLiveRecyclerHelper.getCurrentPage()));
    }

    private class LiveRecyclerHelper extends RecyclerFramework<Room> {
        public LiveRecyclerHelper(IFramework framework) {
            super(framework);
        }

        @Override
        protected RecyclerHolder<Room> createHolder(View itemView, int viewType) {
            return new RoomHolder(itemView);
        }

        @Override
        protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
            return inflater.inflate(R.layout.item_room, parent, false);
        }

        @Override
        public void onItemClick(int position, Room room) {
            start(RoomActivity.create(getContext(), room.getRoom_id()), ActivityFramework.Transition.Enter_Right);
        }

        @Override
        protected List<Room> onParse(String json) {
            LiveListParser parser = LiveListParser.parse(json);

            if (parser != null) {
                return parser.getData();
            } else {
                return null;
            }
        }

        @Override
        protected void setItemDecoration() {
        }
    }
}
