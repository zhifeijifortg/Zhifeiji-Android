package kk.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.R;

import kk.files.KKFileTypes;

public class FileTabAdapter extends BaseQuickAdapter<KKFileTypes, BaseViewHolder> {
    Context context;
    int selectIndex = 0;


    public FileTabAdapter(Context context) {
        super(R.layout.layout_file_tab_item);
        this.context = context;
    }

    public void changeSelect(int position) {
        this.selectIndex = position;
        this.notifyDataSetChanged();
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, KKFileTypes entity) {
        TextView tv_video_tab = baseViewHolder.findView(R.id.tv_file_tab);
        String title = entity.getSuffix();
        if (TextUtils.isEmpty(title)) {
            title = "All";
        } else if ("_tag_video".equals(title)) {
            title = "Videos";
        } else if ("_tag_files".equals(title)) {
            title = "Files";
        } else if ("_tag_others".equals(title)) {
            title = "Others";
        }
        title = title.replace(".", "");
        tv_video_tab.setText(title);

        if (selectIndex == baseViewHolder.getAdapterPosition()) {
            tv_video_tab.setSelected(true);
        } else {
            tv_video_tab.setSelected(false);
        }
    }
}
