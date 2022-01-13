package kk.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseDelegateMultiAdapter;
import com.chad.library.adapter.base.delegate.BaseMultiTypeDelegate;
import com.chad.library.adapter.base.module.LoadMoreModule;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.makeramen.roundedimageview.RoundedImageView;

import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.R;

import java.text.SimpleDateFormat;
import java.util.List;

import kk.model.LocalVideoEntity;
import kk.utils.SystemUtil;

/**
 * 首页已完成视频列表
 */
public class LocalVideoRvAdapter2 extends BaseDelegateMultiAdapter<LocalVideoEntity, BaseViewHolder> implements LoadMoreModule {
    Context context;
    boolean delete;

    public LocalVideoRvAdapter2(Context context) {
        this.context = context;
        initDelegate();
    }

    private void initDelegate() {
        // 第一步，设置代理
        setMultiTypeDelegate(new BaseMultiTypeDelegate<LocalVideoEntity>() {
            @Override
            public int getItemType(@NotNull List<? extends LocalVideoEntity> data, int position) {
                return position % 3;
            }
        });
        // 第二部，绑定 item 类型
        getMultiTypeDelegate()
                .addItemType(0, R.layout.view_localvideo_item_style1)
                .addItemType(1, R.layout.view_localvideo_item_style2)
                .addItemType(2, R.layout.view_localvideo_item_style3);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, LocalVideoEntity entity) {
        switch (baseViewHolder.getItemViewType()) {
            case 0:
            case 1:
            case 2:
                showItem(baseViewHolder, entity);
                break;
        }
    }

    public void deleteModel(boolean delete) {
        this.delete = delete;
        this.notifyDataSetChanged();
    }

    public void deleteItem(String path) {
        int position = -1;
        for (int i = 0; i < getData().size(); i++) {
            LocalVideoEntity entity = getItem(i);
            if (entity.path.equals(path)) {
                position = i;
                break;
            }
        }
        if (position > -1) {
            getData().remove(position);
            notifyItemRemoved(position);
        }
    }

    private void showItem(BaseViewHolder baseViewHolder, LocalVideoEntity entity) {
        RoundedImageView imageView = baseViewHolder.findView(R.id.iv_cover);
        Glide.with(context).load(entity.path).into(imageView);

        View holdView = baseViewHolder.findView(R.id.holdView);
        ImageView ivItemPlay = baseViewHolder.findView(R.id.iv_itemplay);
        ImageView ivCheck = baseViewHolder.findView(R.id.iv_check);
        if (delete) {
            ivItemPlay.setVisibility(View.GONE);
            holdView.setVisibility(View.VISIBLE);
            ivCheck.setVisibility(View.VISIBLE);
        } else {
            ivItemPlay.setVisibility(View.VISIBLE);
            holdView.setVisibility(View.GONE);
            ivCheck.setVisibility(View.GONE);
        }
        if (entity.deleteSelect) {
            ivCheck.setImageResource(R.drawable.ic_check_yes);
        } else {
            ivCheck.setImageResource(R.drawable.ic_check_no);
        }

        String unit = "MB";
        double size = (entity.size / 1024 / 1024.0);
        if (size > 1024) {
            size = size / 1024.0;
            unit = "GB";
        }
        baseViewHolder.setText(R.id.tv_size, String.format("%.1f", size) + unit);
        if (entity.duration > 0) {
            baseViewHolder.setVisible(R.id.tv_length, true);
            baseViewHolder.setText(R.id.tv_length, SystemUtil.timeTransfer(entity.duration));
        } else {
            baseViewHolder.setVisible(R.id.tv_length, false);
        }
        baseViewHolder.setText(R.id.tv_from, context.getResources().getString(R.string.video_from) + entity.fromText);
    }
}
