package kk.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import kk.adapter.VideoRvAdapter;
import kk.files.KKFileMessage;
import kk.files.KKFileMessageManager;
import kk.files.KKLocalFileManager;
import kk.ui.activity.VideoGroupActivity;
import kk.utils.NetUtil;
import kk.utils.SystemUtil;
import kk.video.KKFileDownloadStatus;
import kk.video.KKVideoDownloadListener;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class DownloadingFragment extends BaseFragment implements KKLocalFileManager.Listener<KKFileMessage>, KKVideoDownloadListener {
    private RecyclerView videoRv;
    private LinearLayout nullLayout;
    private TextView tvWifiSpeed;
    private VideoRvAdapter videoRvAdapter;
    List<KKFileMessage> videos;
    boolean isActive;

    public static DownloadingFragment instance() {
        DownloadingFragment fragment = new DownloadingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getContentView() {
        return R.layout.fragment_downloading;
    }

    @Override
    protected void onViewCreated() {
        super.onViewCreated();
        initView();
        isActive = true;

        //添加监听下载中的文件
        KKLocalFileManager.getInstance().addLocalVideoFilesListener(this, this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isActive = false;
    }

    private void initView() {
        nullLayout = rootView.findViewById(R.id.null_layout);
        tvWifiSpeed = rootView.findViewById(R.id.tv_wifi_speed);
        videoRv = rootView.findViewById(R.id.video_rv);
        videoRv.setItemAnimator(null);
        videoRv.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        videoRv.setAdapter(videoRvAdapter = new VideoRvAdapter(mContext));
        videoRvAdapter.getLoadMoreModule().setEnableLoadMore(false);
        videoRvAdapter.setOnItemClickListener((adapter, view, position) -> {
            KKFileMessage message = videoRvAdapter.getItem(position);
            startActivity(new Intent(mContext, VideoGroupActivity.class).putExtra("groupId", message.getDialogId()));
        });
        videoRvAdapter.addChildClickViewIds(R.id.iv_right, R.id.image_layout, R.id.loading_view);
        videoRvAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            KKFileMessage message = videoRvAdapter.getItem(position);
            if (message.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {
                KKFileMessageManager.getInstance().pauseDownloadVideo(message);
            } else {
                KKFileMessageManager.getInstance().startDownloadVideo(message);
            }
        });
    }

    private void setNetSpeed() {
        int net = NetUtil.getNetworkState(mContext);
        Drawable drawableLeft;
        if (net == 1) {
            drawableLeft = mContext.getResources().getDrawable(R.drawable.icon_wifi);
            tvWifiSpeed.setTextColor(Color.parseColor("#25BD45"));
            long netSpeed = SystemUtil.getNetSpeed();
            if (netSpeed > 1024) {
                tvWifiSpeed.setText(mContext.getResources().getString(R.string.wifi_net) + " " + String.format("%.1f", netSpeed / 1024.0) + "MB/s");
            } else {
                tvWifiSpeed.setText(mContext.getResources().getString(R.string.wifi_net) + " " + netSpeed + "KB/s");
            }
        } else {
            drawableLeft = mContext.getResources().getDrawable(R.drawable.icon_netmobile);
            tvWifiSpeed.setTextColor(Color.parseColor("#FF4343"));
            long netSpeed = SystemUtil.getNetSpeed();
            if (netSpeed > 1024) {
                tvWifiSpeed.setText(mContext.getResources().getString(R.string.mobile_net) + " " + String.format("%.1f", netSpeed / 1024.0) + "MB/s");
            } else {
                tvWifiSpeed.setText(mContext.getResources().getString(R.string.mobile_net) + " " + netSpeed + "KB/s");
            }
        }
        tvWifiSpeed.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, null, null);
        tvWifiSpeed.setCompoundDrawablePadding(10);
    }

    //下载中的数据
    private List<KKFileMessage> getStatusMessage() {
        List<KKFileMessage> result = new ArrayList<>();
        for (KKFileMessage kkVideoMessage : videos) {
            if (!kkVideoMessage.isDateMessage() && KKFileDownloadStatus.Status.DOWNLOADED != kkVideoMessage.getDownloadStatus().getStatus()) {
                result.add(kkVideoMessage);
            }
        }
        return result;
    }

    //DOWNLOADING个数
    private int getDownloadingCount() {
        List<KKFileMessage> result = new ArrayList<>();
        for (KKFileMessage kkVideoMessage : videoRvAdapter.getData()) {
            if (!kkVideoMessage.isDateMessage() && KKFileDownloadStatus.Status.DOWNLOADING == kkVideoMessage.getDownloadStatus().getStatus()) {
                result.add(kkVideoMessage);
            }
        }
        return result.size();
    }

    @Override
    public void onLocalVideoFilesUpdate(List<KKFileMessage> videoMessages) {
        if (!isActive) return;
        this.videos = videoMessages;
        List<KKFileMessage> temp = getStatusMessage();
        AndroidUtilities.runOnUIThread(() -> {
            if (temp.size() == 0) {
                nullLayout.setVisibility(View.VISIBLE);
                videoRvAdapter.setList(null);
            } else {
                nullLayout.setVisibility(View.GONE);
                videoRvAdapter.setList(temp);
            }
        });
    }

    @Override
    public void updateVideoDownloadStatus(String fileName, KKFileDownloadStatus fileDownloadStatus) {
        if (!isActive) return;
        AndroidUtilities.runOnUIThread(() -> {
            videoRvAdapter.notifyItemStatusChanged(fileName);
            int count = getDownloadingCount();
            if (count > 0) {
                tvWifiSpeed.setVisibility(View.VISIBLE);
                setNetSpeed();
            } else {
                tvWifiSpeed.setVisibility(View.GONE);
            }
        });
    }
}
