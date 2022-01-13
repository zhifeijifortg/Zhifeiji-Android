package kk.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import kk.adapter.VideoGroupRvAdapter;
import kk.ui.view.NSLoadMoreView;
import kk.video.KKFileDownloadStatus;
import kk.video.KKVideoDataManager;
import kk.video.KKVideoDownloadListener;
import kk.video.KKVideoLoadListener;
import kk.video.KKVideoMessage;

/**
 * Created by LSD on 2021/3/3.
 * Desc
 */
public class VideoGroupActivity extends BaseActivity implements KKVideoDownloadListener, KKVideoLoadListener {
    LinearLayout layoutTitle;
    SmartRefreshLayout refreshLayout;
    TextView tvGroupName;
    RecyclerView videoRv;
    FrameLayout groupAvstarLayout;

    AvatarDrawable avatarDrawable = new AvatarDrawable();
    BackupImageView avatarImageView;
    String lastMessageDate = "";

    VideoGroupRvAdapter videoRvAdapter;
    long groupId;
    int page = 1;
    int lastPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_group_video);
        KKVideoDataManager.getInstance().addListener(this);

        init();
        initView();
        loadChat();
        refreshLayout.autoRefresh();
    }

    @Override
    protected void onDestroy() {
        KKVideoDataManager.getInstance().removeListener(this);
        super.onDestroy();
    }

    private void init() {
        groupId = getIntent().getLongExtra("groupId", 0);
    }

    private void initView() {
        layoutTitle = findViewById(R.id.layout_title);
        avatarImageView = new BackupImageView(this);
        groupAvstarLayout = findViewById(R.id.group_avstar_layout);
        refreshLayout = findViewById(R.id.refreshLayout);
        videoRv = findViewById(R.id.video_rv);
        tvGroupName = findViewById(R.id.tv_group_name);
        ImageView ivBack = findViewById(R.id.iv_back);

        //默认背景
        //layoutTitle.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        //int defaultBackground = Theme.getDefaultAccentColor(Theme.key_chat_wallpaper);
        //StatusBarUtil.setColor(this, defaultBackground);
        //默认字体颜色
        //tvGroupName.setTextColor(Theme.getColor(Theme.key_actionBarDefaultTitle));

        ivBack.setOnClickListener(view -> {
            finish();
        });

        //下拉刷新
        refreshLayout.setOnRefreshListener(refreshLayout -> {
            loadVideo();
        });
        refreshLayout.setEnableLoadMore(false);

        //RecyclerView
        videoRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        videoRv.setAdapter(videoRvAdapter = new VideoGroupRvAdapter(this));
        videoRvAdapter.getLoadMoreModule().setPreLoadNumber(2);
        videoRvAdapter.getLoadMoreModule().setLoadMoreView(new NSLoadMoreView());
        videoRv.setItemAnimator(null);//取消item动画
        videoRvAdapter.addChildClickViewIds(R.id.iv_centerimg, R.id.circle_progress, R.id.loading_view);
        videoRvAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            KKVideoMessage message = videoRvAdapter.getItem(position);
            switch (view.getId()) {
                case R.id.loading_view:
                case R.id.iv_centerimg:
                case R.id.circle_progress:
                    if (message.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {
                        int p_position = 0;
                        List<String> dataList = new ArrayList<>();
                        List<KKVideoMessage> list = videoRvAdapter.getStatusMessage(KKFileDownloadStatus.Status.DOWNLOADED);
                        for (int i = 0; i < list.size(); i++) {
                            KKVideoMessage temp = list.get(i);
                            dataList.add(temp.getDownloadStatus().getVideoFile().getAbsolutePath());
                            if (message.getId() == temp.getId()) {
                                p_position = i;
                            }
                        }
                        if (dataList.size() == 0) return;
                        startActivity(new Intent(this, VideoPlayActivity.class).putExtra("position", p_position).putExtra("dataList", (Serializable) dataList));
                    } else if (message.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {
                        KKVideoDataManager.getInstance().pauseDownloadVideo(message);
                    } else {
                        KKVideoDataManager.getInstance().startDownloadVideo(message);
                    }
                    break;
            }
        });
        videoRvAdapter.getLoadMoreModule().setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                loadMoreVideo();
            }
        });
    }

    private void loadChat() {
        TLRPC.Chat chat = KKVideoDataManager.getInstance().getChat(groupId);
        if (chat == null) return;
        tvGroupName.setText(chat.title);

        avatarDrawable.setInfo(chat);
        if (avatarImageView != null) {
            avatarImageView.setRoundRadius(AndroidUtilities.dp(50));
            avatarImageView.setImage(ImageLocation.getForChat(chat, false), "50_50", avatarDrawable, chat);
        }
        groupAvstarLayout.addView(avatarImageView);
    }

    private void loadVideo() {
        page = 1;
        KKVideoDataManager.getInstance().refreshVideoMessages(groupId, this);
    }

    private void loadMoreVideo() {
        if (lastPage != 1) {//刷新视频，同时保持之前列表
            page = lastPage;
        }
        page++;
        lastPage = page;
        KKVideoDataManager.getInstance().loadMoreVideoMessages(groupId, this);
    }

    @Override
    public void onMessagesLoad(int loadRequestId, List<KKVideoMessage> videoMessages) {
        boolean isEnd = videoMessages.size() < 20;
        if (page == 1) lastMessageDate = "";
        List<KKVideoMessage> videoList;
        if (page == 1) {
            videoList = checkVideoMessages(videoMessages);
            videoList = dealMessageDate(videoList);
        } else {
            videoList = dealMessageDate(videoMessages);
        }
        final List<KKVideoMessage> fvideoList = videoList;
        AndroidUtilities.runOnUIThread(() -> {
            refreshLayout.finishRefresh();
            videoRvAdapter.getLoadMoreModule().loadMoreComplete();
            if (isEnd) videoRvAdapter.getLoadMoreModule().loadMoreEnd(true);
            if (fvideoList.size() == 0) return;
            if (page == 1) {
                videoRvAdapter.setList(fvideoList);
            } else {
                videoRvAdapter.addData(fvideoList);
            }
        });
    }

    @Override
    public void onError(int loadRequestId, int errorCode, String msg) {
        AndroidUtilities.runOnUIThread(() -> {
            if (page != 1) {
                videoRvAdapter.getLoadMoreModule().loadMoreFail();
            } else {
                refreshLayout.finishRefresh(false);
            }
        });
    }

    @Override
    public void updateVideoDownloadStatus(String fileName, KKFileDownloadStatus fileDownloadStatus) {
        AndroidUtilities.runOnUIThread(() -> {
            videoRvAdapter.notifyItemStatusChanged(fileName);
        });
    }

    private List<KKVideoMessage> checkVideoMessages(List<KKVideoMessage> videoMessages) {
        List<KKVideoMessage> newList = new ArrayList<>();
        List<KKVideoMessage> oldList = new ArrayList<>();
        List<KKVideoMessage> dataList = videoRvAdapter.getData();
        if (dataList == null || dataList.size() == 0) return videoMessages;

        for (KKVideoMessage oldItem : dataList) {
            if (!oldItem.isDateMessage()) {
                oldList.add(oldItem);
            } else {
                continue;
            }
        }

        for (KKVideoMessage kkVideoMessage : videoMessages) {
            boolean has = false;
            for (KKVideoMessage temp : oldList) {
                if (kkVideoMessage.getId() == temp.getId()) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                newList.add(kkVideoMessage);
            }
        }
        newList.addAll(oldList);
        return newList;
    }

    private List<KKVideoMessage> dealMessageDate(List<KKVideoMessage> videoMessages) {
        List<KKVideoMessage> videoList = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat(this.getResources().getString(R.string.dateformat));
        for (KKVideoMessage kkVideoMessage : videoMessages) {
            long time = kkVideoMessage.getMessageObject().messageOwner.date;
            String formatDate = LocaleController.formatDateChat(time);
            //String formatDate = formatter.format(time * 1000);
            if (TextUtils.isEmpty(lastMessageDate) || !lastMessageDate.equals(formatDate)) {
                lastMessageDate = formatDate;
                KKVideoMessage dateMessage = new KKVideoMessage(formatDate);
                videoList.add(dateMessage);
            }
            videoList.add(kkVideoMessage);
        }
        return videoList;
    }
}
