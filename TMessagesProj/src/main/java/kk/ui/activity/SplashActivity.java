package kk.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.jaeger.library.StatusBarUtil;
import com.ut.device.UTDevice;

import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

import kk.utils.LoginManager;
import kk.utils.PaperUtil;
import kk.utils.SystemUtil;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        StatusBarUtil.setTransparent(this);
        setContentView(R.layout.activity_splash);

        checkUserLogin();

        if (!PaperUtil.firstLoad()) {
            startActivity(new Intent(this, LaunchActivity.class));
            finish();
            return;
        }
        PaperUtil.firstLoad(false);
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, LaunchActivity.class));
            finish();
        }, 1500);
    }

    private void checkUserLogin() {
        if (LoginManager.isLogin()) {
            return;
        }
        String did = UTDevice.getUtdid(this);
        String countrycode = SystemUtil.getCountryZipCode(this);
        String simcode = SystemUtil.getTelContry(this);
    }
}
