package kk.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.StatisticActivity;

import androidx.annotation.NonNull;
import kk.model.SystemEntity;
import kk.utils.PaperUtil;

/**
 * Created by LSD on 2021/3/2.
 * Desc
 */
public class MyView extends FrameLayout {
    BaseFragment fragment;
    private Context context;

    public MyView(@NonNull BaseFragment fragment) {
        super(fragment.getParentActivity());
        this.fragment = fragment;
        context = fragment.getParentActivity();
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.view_my, this, true);
        setOnClickListener(view -> {//屏蔽点击穿透
        });
        SystemEntity entity = PaperUtil.systemInfo();
        String tgLink = entity == null ? "" : entity.telegram;
        findViewById(R.id.tv_feeback).setOnClickListener(view -> {
            Browser.openUrl(context, tgLink, true);
        });
        findViewById(R.id.tv_follow).setOnClickListener(view -> {
            Browser.openUrl(context, tgLink, true);
        });
        findViewById(R.id.tv_contact_us).setOnClickListener(view -> {
            Browser.openUrl(context, tgLink, true);
        });

        findViewById(R.id.tv_static).setOnClickListener(view->{
            //统计
            TLRPC.Chat chat = fragment.getMessagesController().getChat(1201177205);
            Bundle args1 = new Bundle();
            args1.putInt("chat_id",1201177205);
            args1.putBoolean("is_megagroup", chat.megagroup);
            StatisticActivity activity = new StatisticActivity(args1);
            fragment.presentFragment(activity);
        });
    }
}
