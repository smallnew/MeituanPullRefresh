package com.smallnew.meituan.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.smallnew.meituan.PullToRefreshView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PullToRefreshActivity extends AppCompatActivity {

    public static final int REFRESH_DELAY = 4000;

    private PullToRefreshView mPullToRefreshView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pull_to_refresh);

        mPullToRefreshView = (PullToRefreshView) findViewById(R.id.pull_to_refresh);
        mPullToRefreshView.setOnRefreshListener(new PullToRefreshView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPullToRefreshView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPullToRefreshView.setRefreshing(false);
                    }
                }, REFRESH_DELAY);
            }
        });
    }


}
