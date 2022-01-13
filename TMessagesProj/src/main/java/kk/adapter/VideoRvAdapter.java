package kk.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

import kk.files.KKFileMessage;
import kk.files.KKFileTypes;
import kk.utils.SystemUtil;
import kk.video.KKFileDownloadStatus;

public class VideoRvAdapter extends BaseDelegateMultiAdapter<KKFileMessage, BaseViewHolder> implements LoadMoreModule {
    Context context;

    public VideoRvAdapter(Context context) {
        this.context = context;
        initDelegate();
    }

    private void initDelegate() {
        // 第一步，设置代理
        setMultiTypeDelegate(new BaseMultiTypeDelegate<KKFileMessage>() {
            @Override
            public int getItemType(@NotNull List<? extends KKFileMessage> data, int position) {
                KKFileMessage message = data.get(position);
                if (message.getFileType() == KKFileTypes.VIDEO_FILTER) {//媒体
                    return 1;
                } else {
                    return 2;
                }
            }
        });
        // 第二部，绑定 item 类型
        getMultiTypeDelegate()
                .addItemType(1, R.layout.view_downloading_media_item)
                .addItemType(2, R.layout.view_downloading_file_item);
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

    public void notifyItemStatusChanged(String fileName) {
        int position = -1;
        List<KKFileMessage> list = getData();
        for (int i = 0; i < list.size(); i++) {
            KKFileMessage message = list.get(i);
            if (!message.isDateMessage() && fileName.equals(message.getDownloadFileName())) {
                position = i;
                break;
            }
        }
        if (position > -1) {
            notifyItemChanged(position);
        }
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, KKFileMessage entity) {
        switch (baseViewHolder.getItemViewType()) {
            case 1:
                try {
                    showMediaItem(baseViewHolder, entity);
                } catch (Exception e) {
                }
                break;
            case 2:
                try {
                    showFileItem(baseViewHolder, entity);
                } catch (Exception e) {
                }
                break;
        }
    }

    private void showMediaItem(BaseViewHolder baseViewHolder, KKFileMessage entity) {
        FrameLayout imageLayout = baseViewHolder.findView(R.id.image_layout);
        ImageView ivRight = baseViewHolder.findView(R.id.iv_right);
        TextView tvDownloadState = baseViewHolder.findView(R.id.tv_download_state);
        TextView tvDownloadSize = baseViewHolder.findView(R.id.tv_download);
        TextView tvSpeed = baseViewHolder.findView(R.id.tv_speed);
        ProgressBar progressBar = baseViewHolder.findView(R.id.progressbar);
        TextView tvFinishTime = baseViewHolder.findView(R.id.tv_finish_time);
        View loadingView = baseViewHolder.findView(R.id.loading_view);


        BackupImageView ivThumb = new BackupImageView(context);
        if (entity.getDocument() != null && entity.getDocument().thumbs != null) {
            TLRPC.PhotoSize bigthumb = FileLoader.getClosestPhotoSizeWithSize(entity.getDocument().thumbs, 320);
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(entity.getDocument().thumbs, 40);
            if (thumb == bigthumb) {
                bigthumb = null;
            }
            ivThumb.setRoundRadius(AndroidUtilities.dp(6));
            ivThumb.getImageReceiver().setNeedsQualityThumb(bigthumb == null);
            ivThumb.getImageReceiver().setShouldGenerateQualityThumb(bigthumb == null);
            ivThumb.setImage(ImageLocation.getForDocument(bigthumb, entity.getDocument()), "40_40", ImageLocation.getForDocument(thumb, entity.getDocument()), "40_40_b", null, 0, 1, entity.getMessageObject());
            imageLayout.removeAllViews();
            imageLayout.addView(ivThumb);
        }

        //标题
        String titleText = "";
        if (entity.hasUserName()) {
            titleText = "@" + entity.getFromUserName();
        } else {
            titleText = entity.getMessage();
        }
        if (TextUtils.isEmpty(titleText)) {
            baseViewHolder.setGone(R.id.tv_send_name, true);
        } else {
            baseViewHolder.setGone(R.id.tv_send_name, false);
            baseViewHolder.setText(R.id.tv_send_name, titleText);
        }

        //来自
        baseViewHolder.setGone(R.id.group_layout, false);
        baseViewHolder.setText(R.id.tv_group_name, context.getResources().getString(R.string.video_from) + entity.getFromName());

        //计算下载速度
        float speed = SystemUtil.getDownloadSpeed(entity.getId(), entity.getDownloadStatus().getDownloadedSize());
        String unit = "KB/s";
        if (speed > 1024) {
            speed = speed / 1024.0f;
            unit = "MB/s";
        }
        tvSpeed.setText(String.format("%.1f", speed) + unit);

        //下载进度
        int pro = (int) (entity.getDownloadStatus().getDownloadedSize() * 1.00 / entity.getDownloadStatus().getTotalSize() * 100);

        tvDownloadState.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);
        ivRight.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        tvFinishTime.setVisibility(View.GONE);
        tvDownloadSize.setVisibility(View.VISIBLE);
        tvSpeed.setVisibility(View.GONE);

        //状态数据
        if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {//已完成
            ivRight.setImageResource(R.drawable.ic_do_play);
            tvDownloadState.setVisibility(View.GONE);
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));

            tvFinishTime.setVisibility(View.VISIBLE);
            SimpleDateFormat formatter = new SimpleDateFormat(context.getResources().getString(R.string.dateformat2));
            long time = entity.getDownloadTime();
            tvFinishTime.setText(formatter.format(time));
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.NOT_START) {//未开始
            ivRight.setImageResource(R.drawable.ic_do_download);
            tvDownloadState.setText(context.getResources().getString(R.string.download_nostart));
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {//下载中
            ivRight.setImageResource(R.drawable.ic_do_parse);
            tvDownloadState.setVisibility(View.GONE);
            tvSpeed.setVisibility(View.VISIBLE);
            if (entity.getDownloadStatus().getDownloadedSize() == 0) {
                loadingView.setVisibility(View.VISIBLE);
                ivRight.setVisibility(View.GONE);
            } else {
                loadingView.setVisibility(View.GONE);
                ivRight.setVisibility(View.VISIBLE);
            }
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(pro);
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getDownloadedSize()) + "/" + SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.PAUSE) {//暂停中
            ivRight.setImageResource(R.drawable.ic_do_download);
            tvDownloadState.setVisibility(View.VISIBLE);
            tvDownloadState.setText(context.getResources().getString(R.string.download_pause));

            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(pro);
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getDownloadedSize()) + "/" + SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.FAILED) {//已失败
            ivRight.setImageResource(R.drawable.ic_do_download);
            tvDownloadState.setText(context.getResources().getString(R.string.download_fail));
            progressBar.setProgress(pro);
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getDownloadedSize()) + "/" + SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));
        }
    }

    private void showFileItem(BaseViewHolder baseViewHolder, KKFileMessage entity) {
        FrameLayout imageLayout = baseViewHolder.findView(R.id.image_layout);
        ImageView ivRight = baseViewHolder.findView(R.id.iv_right);
        TextView tvDownloadState = baseViewHolder.findView(R.id.tv_download_state);
        TextView tvDownloadSize = baseViewHolder.findView(R.id.tv_download);
        TextView tvSpeed = baseViewHolder.findView(R.id.tv_speed);
        ProgressBar progressBar = baseViewHolder.findView(R.id.progressbar);
        TextView tvFinishTime = baseViewHolder.findView(R.id.tv_finish_time);
        View loadingView = baseViewHolder.findView(R.id.loading_view);


        //标题
        String titleText = entity.getFileName();
        if (TextUtils.isEmpty(titleText)) {
            baseViewHolder.setGone(R.id.tv_send_name, true);
        } else {
            baseViewHolder.setGone(R.id.tv_send_name, false);
            baseViewHolder.setText(R.id.tv_send_name, titleText);
        }

        //来自
        baseViewHolder.setGone(R.id.group_layout, false);
        baseViewHolder.setText(R.id.tv_group_name, context.getResources().getString(R.string.video_from) + entity.getFromName());

        //计算下载速度
        float speed = SystemUtil.getDownloadSpeed(entity.getId(), entity.getDownloadStatus().getDownloadedSize());
        String unit = "KB/s";
        if (speed > 1024) {
            speed = speed / 1024.0f;
            unit = "MB/s";
        }
        tvSpeed.setText(String.format("%.1f", speed) + unit);

        //下载进度
        int pro = (int) (entity.getDownloadStatus().getDownloadedSize() * 1.00 / entity.getDownloadStatus().getTotalSize() * 100);

        tvDownloadState.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);
        ivRight.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        tvFinishTime.setVisibility(View.GONE);
        tvDownloadSize.setVisibility(View.VISIBLE);
        tvSpeed.setVisibility(View.GONE);

        //状态数据
        if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {//已完成
            ivRight.setImageResource(R.drawable.ic_do_play);
            tvDownloadState.setVisibility(View.GONE);
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));

            tvFinishTime.setVisibility(View.VISIBLE);
            SimpleDateFormat formatter = new SimpleDateFormat(context.getResources().getString(R.string.dateformat2));
            long time = entity.getDownloadTime();
            tvFinishTime.setText(formatter.format(time));
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.NOT_START) {//未开始
            ivRight.setImageResource(R.drawable.ic_do_download);
            tvDownloadState.setText(context.getResources().getString(R.string.download_nostart));
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {//下载中
            ivRight.setImageResource(R.drawable.ic_do_parse);
            tvDownloadState.setVisibility(View.GONE);
            tvSpeed.setVisibility(View.VISIBLE);
            if (entity.getDownloadStatus().getDownloadedSize() == 0) {
                loadingView.setVisibility(View.VISIBLE);
                ivRight.setVisibility(View.GONE);
            } else {
                loadingView.setVisibility(View.GONE);
                ivRight.setVisibility(View.VISIBLE);
            }
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(pro);
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getDownloadedSize()) + "/" + SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.PAUSE) {//暂停中
            ivRight.setImageResource(R.drawable.ic_do_download);
            tvDownloadState.setVisibility(View.VISIBLE);
            tvDownloadState.setText(context.getResources().getString(R.string.download_pause));

            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(pro);
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getDownloadedSize()) + "/" + SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));
        } else if (entity.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.FAILED) {//已失败
            ivRight.setImageResource(R.drawable.ic_do_download);
            tvDownloadState.setText(context.getResources().getString(R.string.download_fail));
            progressBar.setProgress(pro);
            tvDownloadSize.setText(SystemUtil.getSizeFormat(entity.getDownloadStatus().getDownloadedSize()) + "/" + SystemUtil.getSizeFormat(entity.getDownloadStatus().getTotalSize()));
        }
    }
}
