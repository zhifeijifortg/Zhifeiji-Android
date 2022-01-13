package kk.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseDelegateMultiAdapter;
import com.chad.library.adapter.base.delegate.BaseMultiTypeDelegate;
import com.chad.library.adapter.base.module.LoadMoreModule;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.BackupImageView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import at.blogc.android.views.ExpandableTextView;
import kk.ui.view.CircleProgressBar;
import kk.utils.SystemUtil;
import kk.video.KKFileDownloadStatus;
import kk.video.KKVideoMessage;

/**
 * 群视频列表
 */
public class VideoGroupRvAdapter extends BaseDelegateMultiAdapter<KKVideoMessage, BaseViewHolder> implements LoadMoreModule {
    Context context;
    public static final int VIDEO_ITEM_STYLE_TITLE = 0;
    public static final int VIDEO_ITEM_STYLE_CONTENT = 1;

    public VideoGroupRvAdapter(Context context) {
        this.context = context;
        initDelegate();
    }

    private void initDelegate() {
        // 第一步，设置代理
        setMultiTypeDelegate(new BaseMultiTypeDelegate<KKVideoMessage>() {
            @Override
            public int getItemType(@NotNull List<? extends KKVideoMessage> data, int position) {
                KKVideoMessage kkVideoMessage = data.get(position);
                if (kkVideoMessage.isDateMessage()) {
                    return VIDEO_ITEM_STYLE_TITLE;
                } else {
                    return VIDEO_ITEM_STYLE_CONTENT;
                }
            }
        });
        // 第二部，绑定 item 类型
        getMultiTypeDelegate()
                .addItemType(VIDEO_ITEM_STYLE_TITLE, R.layout.view_video_item_style_grop_time)
                .addItemType(VIDEO_ITEM_STYLE_CONTENT, R.layout.view_video_item_style_group);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, KKVideoMessage entity) {
        switch (baseViewHolder.getItemViewType()) {
            case VIDEO_ITEM_STYLE_TITLE:
                baseViewHolder.setText(R.id.tv_title, entity.getDateObject());
                break;
            case VIDEO_ITEM_STYLE_CONTENT:
                try {
                    showItemContent(baseViewHolder, entity);
                } catch (Exception e) {
                }
                break;
        }
    }

    public KKVideoMessage getMessageByFileName(String fileName) {
        for (KKVideoMessage kkVideoMessage : getData()) {
            if (!kkVideoMessage.isDateMessage() && fileName.equals(kkVideoMessage.getDownloadFileName())) {
                return kkVideoMessage;
            }
        }
        return null;
    }

    public void notifyItemStatusChanged(String fileName) {
        int position = -1;
        List<KKVideoMessage> list = getData();
        for (int i = 0; i < list.size(); i++) {
            KKVideoMessage kkVideoMessage = list.get(i);
            if (!kkVideoMessage.isDateMessage() && fileName.equals(kkVideoMessage.getDownloadFileName())) {
                position = i;
                break;
            }
        }
        if (position > -1) {
            notifyItemChanged(position);
        }
    }

    public List<KKVideoMessage> getStatusMessage(KKFileDownloadStatus.Status status) {
        if (status == null) return getData();
        List<KKVideoMessage> result = new ArrayList<>();
        for (KKVideoMessage kkVideoMessage : getData()) {
            if (!kkVideoMessage.isDateMessage() && kkVideoMessage.getDownloadStatus() != null && status == kkVideoMessage.getDownloadStatus().getStatus()) {
                result.add(kkVideoMessage);
            }
        }
        return result;
    }

    private void showItemContent(BaseViewHolder baseViewHolder, KKVideoMessage entity) {
        FrameLayout imageLayout = baseViewHolder.findView(R.id.image_layout);
        ExpandableTextView expandableTextView = baseViewHolder.findView(R.id.expandableTextView);
        TextView tv_expand = baseViewHolder.findView(R.id.tv_expand);
        LinearLayout layout_cover_bottom = baseViewHolder.findView(R.id.layout_cover_bottom);
        ImageView iv_centerimg = baseViewHolder.findView(R.id.iv_centerimg);
        View loading_view = baseViewHolder.findView(R.id.loading_view);
        TextView tv_download_size = baseViewHolder.findView(R.id.tv_download_size);
        CircleProgressBar circle_progress = baseViewHolder.findView(R.id.circle_progress);


        BackupImageView ivThumb = new BackupImageView(context);
        if (entity.getDocument() != null && entity.getDocument().thumbs != null) {
            TLRPC.PhotoSize bigthumb = FileLoader.getClosestPhotoSizeWithSize(entity.getDocument().thumbs, 320);
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(entity.getDocument().thumbs, 40);
            if (thumb == bigthumb) {
                bigthumb = null;
            }
            ivThumb.setRoundRadius(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
            ivThumb.getImageReceiver().setNeedsQualityThumb(bigthumb == null);
            ivThumb.getImageReceiver().setShouldGenerateQualityThumb(bigthumb == null);
            ivThumb.setImage(ImageLocation.getForDocument(bigthumb, entity.getDocument()), "256_142", ImageLocation.getForDocument(thumb, entity.getDocument()), "256_142_b", null, 0, 1, entity.getMessageObject());
            imageLayout.removeAllViews();
            imageLayout.addView(ivThumb);
        }

        //视频大小
        String unit = "MB";
        double size = (entity.getSize() / 1024 / 1024.0);
        if (size > 1024) {
            size = size / 1024.0;
            unit = "GB";
        }
        baseViewHolder.setText(R.id.tv_size, String.format("%.1f", size) + unit);
        baseViewHolder.setText(R.id.tv_length, SystemUtil.timeTransfer(entity.getVideoDuration()));

        //下载进度
        int pro = (int) (entity.getDownloadStatus().getDownloadedSize() * 1.00 / entity.getDownloadStatus().getTotalSize() * 100);

        //视频状态
        tv_download_size.setVisibility(View.GONE);
        iv_centerimg.setVisibility(View.VISIBLE);
        loading_view.setVisibility(View.GONE);
        layout_cover_bottom.setVisibility(View.GONE);
        circle_progress.setVisibility(View.GONE);

        if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {//已完成
            layout_cover_bottom.setVisibility(View.VISIBLE);
            iv_centerimg.setImageResource(R.drawable.ic_item_play);
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.NOT_START) {//未开始
            layout_cover_bottom.setVisibility(View.VISIBLE);
            iv_centerimg.setImageResource(R.drawable.ic_item_nostart);
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {//下载中
            iv_centerimg.setImageResource(R.drawable.ic_item_pause);
            tv_download_size.setVisibility(View.VISIBLE);
            tv_download_size.setText(String.format("%.1f", entity.getDownloadStatus().getDownloadedSize() / 1024 / 1024.0) + "MB/" + String.format("%.1f", entity.getDownloadStatus().getTotalSize() / 1024 / 1024.0) + "MB");
            if (entity.getDownloadStatus().getDownloadedSize() == 0) {//loadingView
                iv_centerimg.setVisibility(View.GONE);
                loading_view.setVisibility(View.VISIBLE);
            } else {
                circle_progress.setVisibility(View.VISIBLE);
                circle_progress.setProgress(pro);
            }
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.PAUSE) {//暂停中
            iv_centerimg.setImageResource(R.drawable.ic_item_nostart);
            tv_download_size.setVisibility(View.VISIBLE);
            tv_download_size.setText(String.format("%.1f", entity.getDownloadStatus().getDownloadedSize() / 1024 / 1024.0) + "MB/" + String.format("%.1f", entity.getDownloadStatus().getTotalSize() / 1024 / 1024.0) + "MB");
            circle_progress.setVisibility(View.VISIBLE);
            circle_progress.setProgress(pro);
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.FAILED) {//已失败
            iv_centerimg.setImageResource(R.drawable.ic_item_nostart);
        }

        //标题
        boolean showExpand;
        String titleText = "";
        if (entity.hasUserName()) {
            titleText = "@" + entity.getFromUserName();
            showExpand = false;
        } else {
            titleText = entity.getMessage();
            if (TextUtils.isEmpty(titleText)) {
                showExpand = false;
            } else {
                showExpand = true;
            }
        }
        int length = SystemUtil.String_length(titleText);
        if (length < 50) {
            showExpand = false;
        }
        expandableTextView.setText(titleText);
        tv_expand.setVisibility(showExpand ? View.VISIBLE : View.GONE);
        tv_expand.setOnClickListener(view -> {
            if (expandableTextView.isExpanded()) {
                expandableTextView.collapse();
                tv_expand.setText(context.getResources().getString(R.string.fg_textview_expand));
            } else {
                expandableTextView.expand();
                tv_expand.setText(context.getResources().getString(R.string.fg_textview_collapse));
            }
        });
        expandableTextView.addOnExpandListener(new ExpandableTextView.OnExpandListener() {
            @Override
            public void onExpand(@NonNull ExpandableTextView view) {
                tv_expand.setText(context.getResources().getString(R.string.fg_textview_collapse));
            }

            @Override
            public void onCollapse(@NonNull ExpandableTextView view) {
                tv_expand.setText(context.getResources().getString(R.string.fg_textview_expand));
            }
        });
    }
}
