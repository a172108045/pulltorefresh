package com.sd.demo.pulltorefresh.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.fanwe.library.activity.SDBaseActivity;
import com.fanwe.lib.pulltorefresh.ISDPullToRefreshView;
import com.fanwe.lib.pulltorefresh.SDPullToRefreshView;
import com.sd.demo.pulltorefresh.R;

public class ScrollViewActivity extends SDBaseActivity
{
    private static final String TAG = "ScrollViewActivity";

    private SDPullToRefreshView view_pull;
    private Button btn;

    @Override
    protected void init(Bundle savedInstanceState)
    {
        setContentView(R.layout.activity_scrollview);
        view_pull = (SDPullToRefreshView) findViewById(R.id.view_pull);
        btn = (Button) findViewById(R.id.btn);

        view_pull.setDebug(true);
        view_pull.setOnStateChangedCallback(new ISDPullToRefreshView.OnStateChangedCallback()
        {
            @Override
            public void onStateChanged(ISDPullToRefreshView.State newState, ISDPullToRefreshView.State oldState, SDPullToRefreshView view)
            {
                //状态变化回调
                btn.setText(String.valueOf(view.getDirection()) + "->" + String.valueOf(newState));
            }
        });
        view_pull.setOnViewPositionChangedCallback(new ISDPullToRefreshView.OnViewPositionChangedCallback()
        {
            @Override
            public void onViewPositionChanged(SDPullToRefreshView view)
            {
                //view被拖动回调
                Log.i(TAG, "onViewPositionChanged getScrollDistance:" + view.getScrollDistance());
            }
        });
        view_pull.setOnRefreshCallback(new ISDPullToRefreshView.OnRefreshCallback()
        {
            @Override
            public void onRefreshingFromHeader(final SDPullToRefreshView view)
            {
                //头部刷新回调
                view.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        view.stopRefreshing();
                    }
                }, 1000);
            }

            @Override
            public void onRefreshingFromFooter(final SDPullToRefreshView view)
            {
                //底部加载回调
                view.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        view.stopRefreshing();
                    }
                }, 1000);
            }
        });
        view_pull.startRefreshingFromFooter();
    }

}
