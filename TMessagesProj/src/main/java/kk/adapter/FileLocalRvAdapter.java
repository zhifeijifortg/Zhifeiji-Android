package kk.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseDelegateMultiAdapter;
import com.chad.library.adapter.base.delegate.BaseMultiTypeDelegate;
import com.chad.library.adapter.base.module.LoadMoreModule;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.makeramen.roundedimageview.RoundedImageView;

import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.List;

import kk.files.KKFileMessage;
import kk.files.KKFileTypes;
import kk.model.LocalVideoEntity;
import kk.utils.SystemUtil;
import kk.video.KKFileDownloadStatus;

/**
 * 首页已完成视频列表
 */
public class FileLocalRvAdapter extends BaseDelegateMultiAdapter<LocalVideoEntity, BaseViewHolder> implements LoadMoreModule {
    Context context;
    boolean delete;

    public FileLocalRvAdapter(Context context) {
        this.context = context;
        initDelegate();
    }

    private void initDelegate() {
        // 第一步，设置代理
        setMultiTypeDelegate(new BaseMultiTypeDelegate<LocalVideoEntity>() {
            @Override
            public int getItemType(@NotNull List<? extends LocalVideoEntity> data, int position) {
                if (data.get(position).fileType == KKFileTypes.MKV || data.get(position).fileType == KKFileTypes.MOV || data.get(position).fileType == KKFileTypes.MP4) {
                    return position % 3;
                } else {
                    return 3;
                }
            }
        });
        // 第二部，绑定 item 类型
        getMultiTypeDelegate()
                .addItemType(0, R.layout.view_local_file_item_style1)
                .addItemType(1, R.layout.view_local_file_item_style2)
                .addItemType(2, R.layout.view_local_file_item_style3)
                .addItemType(3, R.layout.view_local_file_item_style_comm);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, LocalVideoEntity entity) {
        switch (baseViewHolder.getItemViewType()) {
            case 0:
            case 1:
            case 2:
                showItem(baseViewHolder, entity);
                break;
            case 3:
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
            ivCheck.setImageResource(R.drawable.ic_check_no2);
        }

        baseViewHolder.setText(R.id.tv_size, SystemUtil.getSizeFormat(entity.size));
        if (entity.duration > 0) {
            baseViewHolder.setVisible(R.id.tv_length, true);
            baseViewHolder.setText(R.id.tv_length, SystemUtil.timeTransfer(entity.duration));
        } else {
            baseViewHolder.setVisible(R.id.tv_length, false);
        }
        baseViewHolder.setText(R.id.tv_from, entity.fromText);
    }

    private void showFileItem(BaseViewHolder baseViewHolder, LocalVideoEntity entity) {
        View checkView = baseViewHolder.findView(R.id.rl_check);
        ImageView ivItemPlay = baseViewHolder.findView(R.id.iv_itemplay);
        ImageView ivCheck = baseViewHolder.findView(R.id.iv_check);

        if (delete) {
            checkView.setVisibility(View.VISIBLE);
        } else {
            checkView.setVisibility(View.GONE);
        }
        if (entity.deleteSelect) {
            ivCheck.setImageResource(R.drawable.ic_check_yes);
        } else {
            ivCheck.setImageResource(R.drawable.ic_check_no);
        }

        baseViewHolder.setText(R.id.tv_title, entity.name);
        baseViewHolder.setText(R.id.tv_desc, SystemUtil.getSizeFormat(entity.size) + " " + entity.fileType.name());
        baseViewHolder.setText(R.id.tv_from, entity.fromText);

        //文件类型
        if (entity.fileType == KKFileTypes.MKV || entity.fileType == KKFileTypes.MOV || entity.fileType == KKFileTypes.MP4) {
            ivItemPlay.setImageResource(R.drawable.ic_file_play);
        } else {
            ivItemPlay.setImageResource(R.drawable.ic_file_downloaded);
        }
    }
}
