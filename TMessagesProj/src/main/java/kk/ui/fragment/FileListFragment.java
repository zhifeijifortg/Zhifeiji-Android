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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import kk.KKLoger;
import kk.adapter.FileRvAdapter;
import kk.adapter.FileTabAdapter;
import kk.event.DeleteVideoItemEvent;
import kk.files.KKFileMessage;
import kk.files.KKFileMessageLoadListener;
import kk.files.KKFileMessageManager;
import kk.files.KKFileTypes;
import kk.ui.activity.FileGroupActivity;
import kk.ui.activity.VideoPlayActivity;
import kk.ui.view.NSLoadMoreView;
import kk.utils.EventUtil;
import kk.utils.SystemUtil;
import kk.video.KKFileDownloadStatus;
import kk.video.KKVideoDataManager;
import kk.video.KKVideoDownloadListener;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class FileListFragment extends BaseFragment implements KKFileMessageLoadListener, KKVideoDownloadListener {
    private RecyclerView fileRv;
    private RelativeLayout rvNew;
    private SmartRefreshLayout refreshLayout;
    private TextView tvNew;
    private ImageView ivCloseTips;

    FileRvAdapter fileRvAdapter;
    KKFileTypes currentTab;
    int page = 0;
    int pageIndex = 0;
    int newMessageId = -1;
    String lastMessageDate = "";
    int requestId = 0;
    boolean tabChange = false;


    public static FileListFragment instance(String suffix) {
        FileListFragment fragment = new FileListFragment();
        Bundle args = new Bundle();
        args.putString("suffix", suffix);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getContentView() {
        return R.layout.fragment_filelist;
    }

    public void tabChangeState() {
        tabChange = true;
    }

    public void beforeFragmentHidden() {
        KKFileMessageManager.getInstance().removeListener(this);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            KKFileMessageManager.getInstance().removeListener(this);
            refreshLayout.finishRefresh();
        } else {
            KKFileMessageManager.getInstance().addListener(this);
        }
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
        KKFileMessageManager.getInstance().addListener(this);//文件列表监听

        String suffix = getArguments().getString("suffix");
        currentTab = KKFileTypes.parseFileType(suffix);

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
        fileRv = rootView.findViewById(R.id.file_rv);
        ((SimpleItemAnimator) fileRv.getItemAnimator()).setSupportsChangeAnimations(false);
        fileRv.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        fileRv.setAdapter(fileRvAdapter = new FileRvAdapter(mContext));
        fileRvAdapter.getLoadMoreModule().setPreLoadNumber(2);
        fileRvAdapter.getLoadMoreModule().setEnableLoadMoreIfNotFullPage(true);//不满一屏自动加载
        fileRvAdapter.getLoadMoreModule().setLoadMoreView(new NSLoadMoreView());
        fileRvAdapter.setOnItemClickListener((adapter, view, position) -> {
            KKFileMessage message = fileRvAdapter.getItem(position);
            if (message.isDateMessage()) return;
            startActivity(new Intent(mContext, FileGroupActivity.class).putExtra("groupId", message.getDialogId()));
        });
        fileRvAdapter.addChildClickViewIds(R.id.iv_delete, R.id.iv_colloct, R.id.iv_centerimg, R.id.circle_progress, R.id.loading_view, R.id.tv_title);
        fileRvAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            KKFileMessage message = fileRvAdapter.getItem(position);
            switch (view.getId()) {
                case R.id.iv_delete:
                    KKFileMessageManager.getInstance().hideOnlineVideo(message);
                    fileRvAdapter.removeAt(position);
                    break;
                case R.id.iv_colloct:
                    break;
                case R.id.loading_view:
                case R.id.iv_centerimg:
                case R.id.circle_progress:
                case R.id.tv_title:
                    if (message.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {
                        if (message.getFileType() == KKFileTypes.VIDEO_FILTER || message.getFileType() == KKFileTypes.MKV || message.getFileType() == KKFileTypes.MOV || message.getFileType() == KKFileTypes.MP4) {
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
                            startActivity(new Intent(mContext, VideoPlayActivity.class).putExtra("position", p_position).putExtra("dataList", (Serializable) dataList));
                        } else {//其他
                            try {
                                AndroidUtilities.openForView(message.getMessageObject(), mContext);
                            } catch (Exception e) {
                                KKLoger.e("TTT", e.getMessage());
                            }
                        }
                    } else if (message.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {
                        KKFileMessageManager.getInstance().pauseDownloadVideo(message);
                    } else {
                        EventUtil.post(mContext, EventUtil.Even.video页面视频下载点击, new HashMap<>());
                        KKFileMessageManager.getInstance().startDownloadVideo(message);
                    }
                    break;
            }
        });
        fileRvAdapter.getLoadMoreModule().setOnLoadMoreListener(() -> loadMore());

        rvNew = rootView.findViewById(R.id.rv_new);
        ivCloseTips = rootView.findViewById(R.id.iv_close_tips);
        ivCloseTips.setOnClickListener(view -> {
            rvNew.setVisibility(View.GONE);
        });
        tvNew = rootView.findViewById(R.id.tv_new);
        rvNew.setOnClickListener(view -> {
            loadData();
            rvNew.setVisibility(View.GONE);
        });
    }

    private void loadData() {
        tabChange = false;
        page = 0;
        requestId = KKFileMessageManager.getInstance().loadFileMessages(0, currentTab, page, this);
    }

    private void loadMore() {
        tabChange = false;
        if (pageIndex != 0) {//刷新视频，同时保持之前列表
            page = pageIndex;
        }
        page++;
        pageIndex = page;
        requestId = KKFileMessageManager.getInstance().loadFileMessages(0, currentTab, page, this);
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

    private void checkNewTips() {
        List<KKFileMessage> list = fileRvAdapter.getData();
        if (list.size() > 0 && newMessageId != -1) {
            if (list.get(0).getId() != newMessageId) {
                rvNew.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onMessagesLoad(int loadRequestId, List<KKFileMessage> videoMessages) {
        if (tabChange) {
            tabChange = false;
            return;
        }
        if (requestId + 1 != loadRequestId) {
            return;
        }
        KKLoger.d("TTT", "【" + currentTab.name() + "】onMessagesLoad==> page:" + page + ",size:" + videoMessages.size() + "");
        boolean isEnd = videoMessages.size() < 20;
        if (page == 0) lastMessageDate = "";
        if (page == 0) {
            List<KKFileMessage> list = checkVideoMessages(videoMessages);
            list = dealMessageDate(list);
            List<KKFileMessage> flist = list;
            AndroidUtilities.runOnUIThread(() -> fileRvAdapter.setNewInstance(flist));
        } else {
            List<KKFileMessage> list = dealMessageDate(videoMessages);
            AndroidUtilities.runOnUIThread(() -> fileRvAdapter.addData(list));
        }

        AndroidUtilities.runOnUIThread(() -> {
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
        AndroidUtilities.runOnUIThread(() -> fileRvAdapter.notifyItemStatusChanged(fileName));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteVideoItemEvent(DeleteVideoItemEvent event) {
        String fileName = SystemUtil.getFileName(event.filePath);
        KKFileMessage kkVideoMessage = fileRvAdapter.getMessageByFileName(fileName);
        if (kkVideoMessage != null) {
            kkVideoMessage.getDownloadStatus().setStatus(KKFileDownloadStatus.Status.NOT_START);
            fileRvAdapter.notifyItemStatusChanged(fileName);
        }
    }

}
