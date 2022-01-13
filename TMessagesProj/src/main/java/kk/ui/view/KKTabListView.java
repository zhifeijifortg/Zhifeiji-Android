package kk.ui.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.messenger.R;

public class KKTabListView {

    private final View view;

    public KKTabListView(Context context, View chatListView, ViewGroup parent) {
//        this.chatListView = chatListView;
        this.view = LayoutInflater.from(context).inflate(R.layout.kk_tab, parent, true);
//        FrameLayout container = this.view.findViewById(R.id.kk_list_container);
//        container.addView(chatListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }
}
