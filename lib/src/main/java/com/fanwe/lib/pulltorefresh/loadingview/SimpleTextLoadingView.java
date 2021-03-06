/*
 * Copyright (C) 2017 zhengjun, fanwe (http://www.fanwe.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fanwe.lib.pulltorefresh.loadingview;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.fanwe.lib.pulltorefresh.ISDPullToRefreshView;
import com.fanwe.lib.pulltorefresh.R;
import com.fanwe.lib.pulltorefresh.SDPullToRefreshView;

public class SimpleTextLoadingView extends SDPullToRefreshLoadingView
{
    public SimpleTextLoadingView(@NonNull Context context)
    {
        super(context);
        init();
    }

    public SimpleTextLoadingView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public SimpleTextLoadingView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    private TextView tv_content;

    private void init()
    {
        LayoutInflater.from(getContext()).inflate(R.layout.view_simple_text_loading, this, true);
        tv_content = (TextView) findViewById(R.id.tv_content);
    }

    public TextView getTextView()
    {
        return tv_content;
    }

    @Override
    public void onStateChanged(ISDPullToRefreshView.State newState, ISDPullToRefreshView.State oldState, SDPullToRefreshView view)
    {
        switch (newState)
        {
            case RESET:
            case PULL_TO_REFRESH:
                if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.HEADER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_pull_to_refresh_header));
                } else if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.FOOTER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_pull_to_refresh_footer));
                }
                break;
            case RELEASE_TO_REFRESH:
                if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.HEADER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_release_to_refresh_header));
                } else if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.FOOTER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_release_to_refresh_footer));
                }
                break;
            case REFRESHING:
                if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.HEADER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_refreshing_header));
                } else if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.FOOTER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_refreshing_footer));
                }
                break;
            case REFRESH_SUCCESS:
                if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.HEADER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_refreshing_success_header));
                } else if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.FOOTER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_refreshing_success_footer));
                }
                break;
            case REFRESH_FAILURE:
                if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.HEADER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_refreshing_failure_header));
                } else if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.FOOTER)
                {
                    getTextView().setText(getResources().getString(R.string.lib_ptr_state_refreshing_failure_footer));
                }
                break;
            case REFRESH_FINISH:
                if (oldState == ISDPullToRefreshView.State.REFRESHING)
                {
                    if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.HEADER)
                    {
                        getTextView().setText(getResources().getString(R.string.lib_ptr_state_pull_to_refresh_header));
                    } else if (getLoadingViewType() == ISDPullToRefreshView.LoadingViewType.FOOTER)
                    {
                        getTextView().setText(getResources().getString(R.string.lib_ptr_state_pull_to_refresh_footer));
                    }
                }
                break;
        }
    }
}
