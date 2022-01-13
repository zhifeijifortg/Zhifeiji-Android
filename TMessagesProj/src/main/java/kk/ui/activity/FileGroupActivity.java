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
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import kk.KKLoger;
import kk.adapter.FileGroupRvAdapter;
import kk.files.KKFileMessage;
import kk.files.KKFileMessageLoadListener;
import kk.files.KKFileMessageManager;
import kk.files.KKFileTypes;
import kk.ui.view.NSLoadMoreView;
import kk.video.KKFileDownloadStatus;
import kk.video.KKVideoDataManager;
import kk.video.KKVideoDownloadListener;

/**
 * Created by LSD on 2021/3/3.
 * Desc
 */
public class FileGroupActivity extends BaseActivity implements KKVideoDownloadListener, KKFileMessageLoadListener {
    LinearLayout layoutTitle;
    SmartRefreshLayout refreshLayout;
    TextView tvGroupName;
    RecyclerView fileRv;
    FrameLayout groupAvstarLayout;

    AvatarDrawable avatarDrawable = new AvatarDrawable();
    BackupImageView avatarImageView;
    String lastMessageDate = "";

    FileGroupRvAdapter fileRvAdapter;
    long groupId;
    int page = 0;
    int pageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_file_group);
        KKFileMessageManager.getInstance().addListener(this);

        init();
        initView();
        loadChat();
        refreshLayout.autoRefresh();
    }

    @Override
    protected void onDestroy() {
        KKFileMessageManager.getInstance().removeListener(this);
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
        fileRv = findViewById(R.id.video_rv);
        tvGroupName = findViewById(R.id.tv_group_name);
        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(view -> {
            finish();
        });

        //下拉刷新
        refreshLayout.setOnRefreshListener(refreshLayout -> {
            loadData();
        });
        refreshLayout.setEnableLoadMore(false);

        //RecyclerView
        fileRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        fileRv.setAdapter(fileRvAdapter = new FileGroupRvAdapter(this));
        fileRvAdapter.getLoadMoreModule().setPreLoadNumber(2);
        fileRvAdapter.getLoadMoreModule().setLoadMoreView(new NSLoadMoreView());
        fileRv.setItemAnimator(null);//取消item动画
        fileRvAdapter.addChildClickViewIds(R.id.iv_centerimg, R.id.circle_progress, R.id.loading_view);
        fileRvAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            KKFileMessage message = fileRvAdapter.getItem(position);
            switch (view.getId()) {
                case R.id.loading_view:
                case R.id.iv_centerimg:
                case R.id.circle_progress:
                    if (message.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {
                        if (message.getFileType() == KKFileTypes.MKV || message.getFileType() == KKFileTypes.MOV || message.getFileType() == KKFileTypes.MP4) {
                            //媒体
                            int p_position = 0;
                            List<String> dataList = new ArrayList<>();
                            List<KKFileMessage> list = fileRvAdapter.getDownloadedMedia();
                            for (int i = 0; i < list.size(); i++) {
                                KKFileMessage temp = list.get(i);
                                dataList.add(temp.getDownloadStatus().getVideoFile().getAbsolutePath());
                                if (message.getId() == temp.getId()) {
                                    p_position = i;
                                }
                            }
                            if (dataList.size() == 0) return;
                            startActivity(new Intent(this, VideoPlayActivity.class).putExtra("position", p_position).putExtra("dataList", (Serializable) dataList));
                        } else {//其他

                        }
                    } else if (message.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {
                        KKFileMessageManager.getInstance().pauseDownloadVideo(message);
                    } else {
                        KKFileMessageManager.getInstance().startDownloadVideo(message);
                    }
                    break;
            }
        });
        fileRvAdapter.getLoadMoreModule().setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                loadMore();
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

    private void loadData() {
        page = 0;
        KKFileMessageManager.getInstance().loadFileMessages(groupId, KKFileTypes.ALL, page, this);
    }

    private void loadMore() {
        if (pageIndex != 0) {//刷新视频，同时保持之前列表
            page = pageIndex;
        }
        page++;
        pageIndex = page;
        KKFileMessageManager.getInstance().loadFileMessages(groupId, KKFileTypes.ALL, page, this);
    }

    @Override
    public void onMessagesLoad(int loadRequestId, List<KKFileMessage> fileMessages) {
        boolean isEnd = fileMessages.size() < 20;
        if (page == 0) lastMessageDate = "";
        List<KKFileMessage> list;
        if (page == 0) {
            list = checkVideoMessages(fileMessages);
            list = dealMessageDate(list);
        } else {
            list = dealMessageDate(fileMessages);
        }
        final List<KKFileMessage> fList = list;
        AndroidUtilities.runOnUIThread(() -> {
            if (fList.size() > 0) {
                if (page == 0) {
                    fileRvAdapter.setList(fList);
                } else {
                    fileRvAdapter.addData(fList);
                }
            }
            refreshLayout.finishRefresh();
            fileRvAdapter.getLoadMoreModule().loadMoreComplete();
            if (isEnd) fileRvAdapter.getLoadMoreModule().loadMoreEnd(true);
        });
    }

    @Override
    public void onError(int loadRequestId, int errorCode, String msg) {
        AndroidUtilities.runOnUIThread(() -> {
            if (page != 0) {
                fileRvAdapter.getLoadMoreModule().loadMoreFail();
            } else {
                refreshLayout.finishRefresh(false);
            }
        });
    }

    @Override
    public void updateVideoDownloadStatus(String fileName, KKFileDownloadStatus fileDownloadStatus) {
        AndroidUtilities.runOnUIThread(() -> {
            fileRvAdapter.notifyItemStatusChanged(fileName);
        });
    }

    private List<KKFileMessage> checkVideoMessages(List<KKFileMessage> videoMessages) {
        List<KKFileMessage> newList = new ArrayList<>();
        List<KKFileMessage> oldList = new ArrayList<>();
        List<KKFileMessage> dataList = fileRvAdapter.getData();
        if (dataList == null || dataList.size() == 0) return videoMessages;

        for (KKFileMessage oldItem : dataList) {
            if (!oldItem.isDateMessage()) {
                oldList.add(oldItem);
            } else {
                continue;
            }
        }

        for (KKFileMessage kkVideoMessage : videoMessages) {
            boolean has = false;
            for (KKFileMessage temp : oldList) {
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

    private List<KKFileMessage> dealMessageDate(List<KKFileMessage> videoMessages) {
        List<KKFileMessage> videoList = new ArrayList<>();
        for (KKFileMessage kkVideoMessage : videoMessages) {
            long time = kkVideoMessage.getMessageObject().messageOwner.date;
            String formatDate = LocaleController.formatDateChat(time, true);
            if (TextUtils.isEmpty(lastMessageDate) || !lastMessageDate.equals(formatDate)) {
                lastMessageDate = formatDate;
                KKFileMessage dateMessage = new KKFileMessage(formatDate);
                videoList.add(dateMessage);
            }
            videoList.add(kkVideoMessage);
        }
        return videoList;
    }
}
