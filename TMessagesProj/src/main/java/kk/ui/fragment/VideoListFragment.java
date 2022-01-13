package kk.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import kk.adapter.VideoRvAdapter2;
import kk.event.DeleteVideoItemEvent;
import kk.ui.activity.VideoGroupActivity;
import kk.ui.activity.VideoPlayActivity;
import kk.ui.view.NSLoadMoreView;
import kk.utils.EventUtil;
import kk.utils.SystemUtil;
import kk.video.KKFileDownloadStatus;
import kk.video.KKVideoDataManager;
import kk.video.KKVideoDownloadListener;
import kk.video.KKVideoLoadListener;
import kk.video.KKVideoMessage;
import kk.video.NewVideoListener;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class VideoListFragment extends BaseFragment implements KKVideoLoadListener, KKVideoDownloadListener, NewVideoListener {
    private RecyclerView videoRv;
    private RelativeLayout rvNewVideo;
    private SmartRefreshLayout refreshLayout;
    private TextView tvNewVideo;
    private ImageView ivCloseTips;

    VideoRvAdapter2 videoRvAdapter;
    int page = 1;
    int lastPage = 1;
    boolean onPause = false;
    int newMessageId = -1;

    public static VideoListFragment instance() {
        VideoListFragment fragment = new VideoListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getContentView() {
        return R.layout.fragment_videolist;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (onPause) {
            KKVideoDataManager.getInstance().addListener(this);
            KKVideoDataManager.getInstance().addNewVideoListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        onPause = true;
        KKVideoDataManager.getInstance().removeListener(this);
        KKVideoDataManager.getInstance().removeNewVideoListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onViewCreated() {
        super.onViewCreated();
        EventBus.getDefault().register(this);
        KKVideoDataManager.getInstance().addListener(this);
        KKVideoDataManager.getInstance().addNewVideoListener(this);

        initView();
        refreshLayout.autoRefresh();
    }

    private void initView() {
        //下拉刷新
        refreshLayout = rootView.findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(refreshLayout -> {
            loadData();
        });
        refreshLayout.setEnableLoadMore(false);

        //view
        videoRv = rootView.findViewById(R.id.video_rv);
        ((SimpleItemAnimator) videoRv.getItemAnimator()).setSupportsChangeAnimations(false);
        //videoRv.setItemAnimator(null);
        videoRv.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        videoRv.setAdapter(videoRvAdapter = new VideoRvAdapter2(mContext));
        videoRvAdapter.getLoadMoreModule().setPreLoadNumber(2);
        videoRvAdapter.getLoadMoreModule().setLoadMoreView(new NSLoadMoreView());
        videoRvAdapter.setOnItemClickListener((adapter, view, position) -> {
            KKVideoMessage message = videoRvAdapter.getItem(position);
            if (!TextUtils.isEmpty(message.getDateObject())) return;
            startActivity(new Intent(mContext, VideoGroupActivity.class).putExtra("groupId", message.getDialogId()));
        });
        videoRvAdapter.addChildClickViewIds(R.id.iv_delete, R.id.iv_colloct, R.id.iv_centerimg, R.id.circle_progress, R.id.loading_view);
        videoRvAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            KKVideoMessage message = videoRvAdapter.getItem(position);
            switch (view.getId()) {
                case R.id.iv_delete:
                    KKVideoDataManager.getInstance().hideOnlineVideo(message);
                    videoRvAdapter.notifyItemRemoved(position);
                    break;
                case R.id.iv_colloct:
                    break;
                case R.id.loading_view:
                case R.id.iv_centerimg:
                case R.id.circle_progress:
                    if (message.getDownloadStatus() != null && message.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {
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
                        startActivity(new Intent(mContext, VideoPlayActivity.class).putExtra("position", p_position).putExtra("dataList", (Serializable) dataList));
                    } else if (message.getDownloadStatus() != null && message.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {
                        KKVideoDataManager.getInstance().pauseDownloadVideo(message);
                    } else {
                        EventUtil.post(mContext, EventUtil.Even.video页面视频下载点击, new HashMap<>());
                        KKVideoDataManager.getInstance().startDownloadVideo(message);
                    }
                    break;
            }
        });
        videoRvAdapter.getLoadMoreModule().setOnLoadMoreListener(() -> loadMore());
        videoRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(-1)) {
                    //EventBus.getDefault().post(new FloatLayoutEvent(true));
                } else if (dy < -10) {
                    //EventBus.getDefault().post(new FloatLayoutEvent(true));
                } else if (dy > 0) {
                    //EventBus.getDefault().post(new FloatLayoutEvent(false));
                }
            }
        });

        rvNewVideo = rootView.findViewById(R.id.rv_new_video);
        ivCloseTips = rootView.findViewById(R.id.iv_close_tips);
        ivCloseTips.setOnClickListener(view -> {
            rvNewVideo.setVisibility(View.GONE);
        });
        tvNewVideo = rootView.findViewById(R.id.tv_new_video);
        rvNewVideo.setOnClickListener(view -> {
            loadData();
            rvNewVideo.setVisibility(View.GONE);
        });
    }

    private void loadData() {
        page = 1;
        KKVideoDataManager.getInstance().refreshVideoMessages(0, this);
    }

    private void loadMore() {
        if (lastPage != 1) {//刷新视频，同时保持之前列表
            page = lastPage;
        }
        page++;
        lastPage = page;
        KKVideoDataManager.getInstance().loadMoreVideoMessages(0, this);
    }

    private List<KKVideoMessage> checkVideoMessages(List<KKVideoMessage> videoMessages) {
        List<KKVideoMessage> newList = new ArrayList<>();
        List<KKVideoMessage> dataList = videoRvAdapter.getData();
        if (dataList == null || dataList.size() == 0) {
            return videoMessages;
        }
        for (KKVideoMessage kkVideoMessage : videoMessages) {
            boolean has = false;
            for (KKVideoMessage temp : dataList) {
                if (kkVideoMessage.getId() == temp.getId()) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                newList.add(kkVideoMessage);
            }
        }
        return newList;
    }

    private void checkNewTips() {
        List<KKVideoMessage> list = videoRvAdapter.getData();
        if (list.size() > 0 && newMessageId != -1) {
            if (list.get(0).getId() != newMessageId) {
                rvNewVideo.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onMessagesLoad(int loadRequestId, List<KKVideoMessage> videoMessages) {
        boolean isEnd = videoMessages.size() < 20;
        AndroidUtilities.runOnUIThread(() -> {
            refreshLayout.finishRefresh();
            videoRvAdapter.getLoadMoreModule().loadMoreComplete();
            if (isEnd) videoRvAdapter.getLoadMoreModule().loadMoreEnd(true);
            if (videoMessages == null && videoMessages.size() == 0) return;

            //第一页新视频要加在顶部，不是重新加载
            if (page == 1) {
                List<KKVideoMessage> newList = checkVideoMessages(videoMessages);
                if (newList.size() > 0) {
                    videoRvAdapter.addData(0, newList);
                }
            } else {
                videoRvAdapter.addData(videoMessages);
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
        AndroidUtilities.runOnUIThread(() -> videoRvAdapter.notifyItemStatusChanged(fileName));
    }

    @Override
    public void onNewVideoMessage(int messageId) {
        newMessageId = messageId;
        AndroidUtilities.runOnUIThread(() -> checkNewTips());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteVideoItemEvent(DeleteVideoItemEvent event) {
        String fileName = SystemUtil.getFileName(event.filePath);
        KKVideoMessage kkVideoMessage = videoRvAdapter.getMessageByFileName(fileName);
        if (kkVideoMessage != null) {
            kkVideoMessage.getDownloadStatus().setStatus(KKFileDownloadStatus.Status.NOT_START);
            videoRvAdapter.notifyItemStatusChanged(fileName);
        }
    }

}
