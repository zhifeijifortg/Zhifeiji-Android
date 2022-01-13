package kk.ui.activity;

import android.app.Activity;
import android.os.Bundle;

import com.flurry.android.FlurryAgent;
import com.umeng.analytics.MobclickAgent;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Created by LSD on 2021/4/7.
 * Desc
 */
public class BaseActivity extends FragmentActivity{
    public Activity mActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        FlurryAgent.onStartSession(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        FlurryAgent.onEndSession(this);
    }
}
