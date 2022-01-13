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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import at.blogc.android.views.ExpandableTextView;
import kk.KKLoger;
import kk.files.KKFileMessage;
import kk.files.KKFileTypes;
import kk.ui.view.CircleProgressBar;
import kk.utils.SystemUtil;
import kk.video.KKFileDownloadStatus;

/**
 * 群文件列表
 */
public class FileGroupRvAdapter extends BaseDelegateMultiAdapter<KKFileMessage, BaseViewHolder> implements LoadMoreModule {
    Context context;
    public static final int ITEM_STYLE_TITLE = 0;
    public static final int ITEM_STYLE_MEDIA = 1;
    public static final int ITEM_STYLE_FILE = 2;

    public FileGroupRvAdapter(Context context) {
        this.context = context;
        initDelegate();
    }

    private void initDelegate() {
        // 第一步，设置代理
        setMultiTypeDelegate(new BaseMultiTypeDelegate<KKFileMessage>() {
            @Override
            public int getItemType(@NotNull List<? extends KKFileMessage> data, int position) {
                KKFileMessage kkVideoMessage = data.get(position);
                if (kkVideoMessage.isDateMessage()) {
                    return ITEM_STYLE_TITLE;
                } else if (data.get(position).getFileType() == KKFileTypes.VIDEO_FILTER || data.get(position).getFileType() == KKFileTypes.MKV || data.get(position).getFileType() == KKFileTypes.MOV || data.get(position).getFileType() == KKFileTypes.MP4) {
                    return ITEM_STYLE_MEDIA;
                } else {
                    return ITEM_STYLE_FILE;
                }
            }
        });
        // 第二部，绑定 item 类型
        getMultiTypeDelegate()
                .addItemType(ITEM_STYLE_TITLE, R.layout.view_file_group_item_style_time)
                .addItemType(ITEM_STYLE_MEDIA, R.layout.view_file_group_item_style_media)
                .addItemType(ITEM_STYLE_FILE, R.layout.view_file_group_item_style);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, KKFileMessage entity) {
        switch (baseViewHolder.getItemViewType()) {
            case ITEM_STYLE_TITLE:
                baseViewHolder.setText(R.id.tv_title, entity.getDateObject());
                break;
            case ITEM_STYLE_MEDIA:
                try {
                    showMediaItem(baseViewHolder, entity);
                } catch (Exception e) {
                }
                break;
            case ITEM_STYLE_FILE:
                try {
                    showFileItem(baseViewHolder, entity);
                } catch (Exception e) {
                }
                break;
        }
    }

    public KKFileMessage getMessageByFileName(String fileName) {
        for (KKFileMessage kkVideoMessage : getData()) {
            if (!kkVideoMessage.isDateMessage() && fileName.equals(kkVideoMessage.getDownloadFileName())) {
                return kkVideoMessage;
            }
        }
        return null;
    }

    public void notifyItemStatusChanged(String fileName) {
        int position = -1;
        List<KKFileMessage> list = getData();
        for (int i = 0; i < list.size(); i++) {
            KKFileMessage kkVideoMessage = list.get(i);
            if (!kkVideoMessage.isDateMessage() && fileName.equals(kkVideoMessage.getDownloadFileName())) {
                position = i;
                break;
            }
        }
        if (position > -1) {
            notifyItemChanged(position);
        }
    }

    public List<KKFileMessage> getDownloadedMedia() {
        List<KKFileMessage> result = new ArrayList<>();
        for (KKFileMessage kkVideoMessage : getData()) {
            if (kkVideoMessage.getDownloadStatus() != null && KKFileDownloadStatus.Status.DOWNLOADED == kkVideoMessage.getDownloadStatus().getStatus()) {
                if (kkVideoMessage.getFileType() == KKFileTypes.MKV || kkVideoMessage.getFileType() == KKFileTypes.MOV || kkVideoMessage.getFileType() == KKFileTypes.MP4) {
                    result.add(kkVideoMessage);
                }
            }
        }
        return result;
    }

    public List<KKFileMessage> getStatusMessage(KKFileDownloadStatus.Status status) {
        if (status == null) return getData();
        List<KKFileMessage> result = new ArrayList<>();
        for (KKFileMessage kkVideoMessage : getData()) {
            if (!kkVideoMessage.isDateMessage() && kkVideoMessage.getDownloadStatus() != null && status == kkVideoMessage.getDownloadStatus().getStatus()) {
                result.add(kkVideoMessage);
            }
        }
        return result;
    }

    private void showMediaItem(BaseViewHolder baseViewHolder, KKFileMessage entity) {
        View image_bg = baseViewHolder.findView(R.id.image_bg);
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

        //大小
        baseViewHolder.setText(R.id.tv_size, SystemUtil.getSizeFormat(entity.getSize()));
        //长度
        if (entity.getMediaDuration() > 0) {
            baseViewHolder.setGone(R.id.tv_length, false);
            baseViewHolder.setText(R.id.tv_length, SystemUtil.timeTransfer(entity.getMediaDuration()));
        } else {
            baseViewHolder.setGone(R.id.tv_length, false);
        }

        //下载进度
        int pro = (int) (entity.getDownloadStatus().getDownloadedSize() * 1.00 / entity.getDownloadStatus().getTotalSize() * 100);

        //视频状态
        image_bg.setVisibility(View.GONE);
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
            image_bg.setVisibility(View.VISIBLE);
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
            image_bg.setVisibility(View.VISIBLE);
            iv_centerimg.setImageResource(R.drawable.ic_item_nostart);
            tv_download_size.setVisibility(View.VISIBLE);
            tv_download_size.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getDownloadedSize()) + "/" + SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));
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

    //普通文件
    private void showFileItem(BaseViewHolder baseViewHolder, KKFileMessage entity) {
        ImageView iv_centerimg = baseViewHolder.findView(R.id.iv_centerimg);
        View loading_view = baseViewHolder.findView(R.id.loading_view);
        CircleProgressBar circle_progress = baseViewHolder.findView(R.id.circle_progress);

        //标题
        String title = entity.getFileName();
        baseViewHolder.setText(R.id.tv_title, title);

        //大小
        baseViewHolder.setText(R.id.tv_desc, SystemUtil.getSizeFormat(entity.getSize()) + " " + entity.getFileType().name());

        //下载进度
        int pro = (int) (entity.getDownloadStatus().getDownloadedSize() * 1.00 / entity.getDownloadStatus().getTotalSize() * 100);

        //状态
        iv_centerimg.setVisibility(View.VISIBLE);
        loading_view.setVisibility(View.GONE);
        circle_progress.setVisibility(View.GONE);

        if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {//已完成
            if (entity.getFileType() == KKFileTypes.MKV || entity.getFileType() == KKFileTypes.MOV || entity.getFileType() == KKFileTypes.MP4) {
                iv_centerimg.setImageResource(R.drawable.ic_file_play);
            } else {
                iv_centerimg.setImageResource(R.drawable.ic_file_downloaded);
            }
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.NOT_START) {//未开始
            iv_centerimg.setImageResource(R.drawable.ic_file_nostart);
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {//下载中
            iv_centerimg.setImageResource(R.drawable.ic_file_download_parse);
            if (entity.getDownloadStatus().getDownloadedSize() == 0) {//loadingView
                loading_view.setVisibility(View.VISIBLE);
            } else {
                circle_progress.setVisibility(View.VISIBLE);
                circle_progress.setProgress(pro);
            }
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.PAUSE) {//暂停中
            iv_centerimg.setImageResource(R.drawable.ic_file_nostart);
            circle_progress.setVisibility(View.VISIBLE);
            circle_progress.setProgress(pro);
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.FAILED) {//已失败
            iv_centerimg.setImageResource(R.drawable.ic_file_nostart);
        }


        //来自
        //baseViewHolder.setText(R.id.tv_from, entity.getFromName());
        //时间
        SimpleDateFormat formatter = new SimpleDateFormat(context.getResources().getString(R.string.dateformat));
        long time = entity.getMessageObject().messageOwner.date;
        String formatDate = formatter.format(time * 1000);
        baseViewHolder.setText(R.id.tv_time, formatDate);
    }
}
