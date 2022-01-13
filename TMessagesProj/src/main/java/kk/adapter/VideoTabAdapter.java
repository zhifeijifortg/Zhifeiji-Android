package kk.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.R;

public class VideoTabAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    Context context;
    int selectIndex = 0;


    public VideoTabAdapter(Context context) {
        super(R.layout.layout_video_tab_item);
        this.context = context;
    }

    public void changeSelect(int position) {
        this.selectIndex = position;
        this.notifyDataSetChanged();
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, String entity) {
        TextView tv_video_tab = baseViewHolder.findView(R.id.tv_video_tab);
        tv_video_tab.setText(entity);

        if (selectIndex == baseViewHolder.getAdapterPosition()) {
            tv_video_tab.setSelected(true);
        } else {
            tv_video_tab.setSelected(false);
        }
    }
}
