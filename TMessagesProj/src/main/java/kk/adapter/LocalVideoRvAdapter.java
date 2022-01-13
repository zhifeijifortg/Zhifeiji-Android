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
import java.util.ArrayList;
import java.util.List;

import kk.files.KKFileTypes;
import kk.model.LocalVideoEntity;
import kk.utils.SystemUtil;

/***
 * 下载已完成视频列表
 */
public class LocalVideoRvAdapter extends BaseDelegateMultiAdapter<LocalVideoEntity, BaseViewHolder> implements LoadMoreModule {
    Context context;
    boolean delete;

    public LocalVideoRvAdapter(Context context) {
        this.context = context;
        initDelegate();
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

    private void initDelegate() {
        // 第一步，设置代理
        setMultiTypeDelegate(new BaseMultiTypeDelegate<LocalVideoEntity>() {
            @Override
            public int getItemType(@NotNull List<? extends LocalVideoEntity> data, int position) {
                if (data.get(position).fileType == KKFileTypes.MKV || data.get(position).fileType == KKFileTypes.MOV || data.get(position).fileType == KKFileTypes.MP4) {
                    return 1;
                } else {
                    return 2;
                }
            }
        });
        // 第二部，绑定 item 类型
        getMultiTypeDelegate()
                .addItemType(1, R.layout.view_downloaded_meida_item)
                .addItemType(2, R.layout.view_downloaded_file_item);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, LocalVideoEntity entity) {
        switch (baseViewHolder.getItemViewType()) {
            case 1:
                showMediaItem(baseViewHolder, entity);
                break;
            case 2:
                showFileItem(baseViewHolder, entity);
                break;
        }
    }

    public List<LocalVideoEntity> getDownloadedMedia() {
        List<LocalVideoEntity> result = new ArrayList<>();
        for (LocalVideoEntity localFile : getData()) {
            if (localFile.fileType == KKFileTypes.MKV || localFile.fileType == KKFileTypes.MOV || localFile.fileType == KKFileTypes.MP4) {
                result.add(localFile);
            }
        }
        return result;
    }

    private void showMediaItem(BaseViewHolder baseViewHolder, LocalVideoEntity entity) {
        RoundedImageView imageView = baseViewHolder.findView(R.id.ivCover);
        Glide.with(context).load(entity.path).into(imageView);

        ImageView ivCheck = baseViewHolder.findView(R.id.iv_check);
        if (delete) {
            ivCheck.setVisibility(View.VISIBLE);
        } else {
            ivCheck.setVisibility(View.GONE);
        }
        if (entity.deleteSelect) {
            ivCheck.setImageResource(R.drawable.ic_check_yes);
        } else {
            ivCheck.setImageResource(R.drawable.ic_check_no);
        }

        TextView tvTitleText = baseViewHolder.findView(R.id.tv_title_text);
        if (!TextUtils.isEmpty(entity.name)) {
            tvTitleText.setVisibility(View.VISIBLE);
            tvTitleText.setText(entity.name);
        } else {
            tvTitleText.setVisibility(View.GONE);
        }

        LinearLayout groupLayout = baseViewHolder.findView(R.id.group_layout);
        TextView tvGroupName = baseViewHolder.findView(R.id.tv_group_name);
        if (!TextUtils.isEmpty(entity.fromText)) {
            groupLayout.setVisibility(View.VISIBLE);
            tvGroupName.setText(context.getResources().getString(R.string.video_from) + entity.fromText);
        } else {
            groupLayout.setVisibility(View.GONE);
        }

        baseViewHolder.setText(R.id.tv_size, SystemUtil.getSizeFormat(entity.size));

        SimpleDateFormat formatter = new SimpleDateFormat(context.getResources().getString(R.string.dateformat2));
        baseViewHolder.setText(R.id.tv_finish_time, formatter.format(entity.time));
    }

    private void showFileItem(BaseViewHolder baseViewHolder, LocalVideoEntity entity) {
        ImageView imageView = baseViewHolder.findView(R.id.ivCover);
        //根据类型显示

        ImageView ivCheck = baseViewHolder.findView(R.id.iv_check);
        if (delete) {
            ivCheck.setVisibility(View.VISIBLE);
        } else {
            ivCheck.setVisibility(View.GONE);
        }
        if (entity.deleteSelect) {
            ivCheck.setImageResource(R.drawable.ic_check_yes);
        } else {
            ivCheck.setImageResource(R.drawable.ic_check_no);
        }

        TextView tvTitleText = baseViewHolder.findView(R.id.tv_title_text);
        if (!TextUtils.isEmpty(entity.name)) {
            tvTitleText.setVisibility(View.VISIBLE);
            tvTitleText.setText(entity.name);
        } else {
            tvTitleText.setVisibility(View.GONE);
        }

        LinearLayout groupLayout = baseViewHolder.findView(R.id.group_layout);
        TextView tvGroupName = baseViewHolder.findView(R.id.tv_group_name);
        if (!TextUtils.isEmpty(entity.fromText)) {
            groupLayout.setVisibility(View.VISIBLE);
            tvGroupName.setText(context.getResources().getString(R.string.video_from) + entity.fromText);
        } else {
            groupLayout.setVisibility(View.GONE);
        }

        baseViewHolder.setText(R.id.tv_size, SystemUtil.getSizeFormat(entity.size));

        SimpleDateFormat formatter = new SimpleDateFormat(context.getResources().getString(R.string.dateformat2));
        baseViewHolder.setText(R.id.tv_finish_time, formatter.format(entity.time));
    }
}
