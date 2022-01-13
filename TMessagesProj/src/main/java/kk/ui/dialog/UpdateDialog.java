package kk.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.telegram.messenger.R;

import java.io.File;

import androidx.annotation.NonNull;
import kk.model.UpdateEntity;
import kk.utils.VersionManager;

/**
 * Desc 升级弹窗
 */
public class UpdateDialog extends Dialog {
    Context context;
    TextView tvTitle;
    UpdateEntity entity;
    File apkFile;

    public UpdateDialog(@NonNull Context context, UpdateEntity entity, File apkFile) {
        super(context, R.style.canceledOnTouch);
        this.context = context;
        this.entity = entity;
        this.apkFile = apkFile;
        setCanceledOnTouchOutside(true);
        setTranslucentStatus();
        setContentView(R.layout.dialog_update);
        initView();
    }

    private void setTranslucentStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0 全透明实现
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {//4.4 全透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    private void initView() {
        tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(entity.ver_title + "\n" + entity.ver_desc);
        findViewById(R.id.tv_cancel).setOnClickListener(view -> dismiss());
        findViewById(R.id.tv_install).setOnClickListener(view -> {
            VersionManager.updateStart(context, apkFile);
            dismiss();
        });
    }
}
