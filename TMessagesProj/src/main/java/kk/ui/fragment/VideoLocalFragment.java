package kk.ui.fragment;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import kk.KKLoger;
import kk.adapter.LocalVideoRvAdapter2;
import kk.event.ChangeEditEvent;
import kk.event.DeleteVideoItemEvent;
import kk.event.DeleteVideoOkEvent;
import kk.model.LocalVideoEntity;
import kk.ui.activity.VideoPlayActivity;
import kk.utils.DensityUtil;
import kk.utils.EventUtil;
import kk.utils.FileScanManager;
import kk.utils.SystemUtil;
import kk.utils.sort.LastModifiedFileComparator;
import kk.utils.sort.LocalVideoSort;
import kk.video.KKFileDownloadStatus;
import kk.video.KKLocalVideoFileManager;
import kk.video.KKVideoDataManager;
import kk.video.KKVideoDownloadListener;
import kk.video.KKVideoMessage;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class VideoLocalFragment extends BaseFragment implements KKLocalVideoFileManager.Listener<KKVideoMessage>, KKVideoDownloadListener {
    private RecyclerView videoRv;
    private SmartRefreshLayout refreshLayout;
    private LinearLayout nullLayout;
    private LinearLayout bottomLayout;
    private TextView tvCheckAll;
    private TextView tvDelete;

    private LocalVideoRvAdapter2 localVideoRvAdapter;
    private List<String> deleteList = new ArrayList<>();

    public static VideoLocalFragment instance() {
        VideoLocalFragment fragment = new VideoLocalFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getContentView() {
        return R.layout.fragment_videolocal;
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

        initView();

        loadLocalData();
        //添加监听已完成的
        KKLocalVideoFileManager.getInstance().addLocalVideoFilesListener(this, this);
    }

    private void initView() {
        bottomLayout = rootView.findViewById(R.id.bottom_layout);
        tvCheckAll = rootView.findViewById(R.id.tv_checkall);
        tvDelete = rootView.findViewById(R.id.tv_dodelete);
        nullLayout = rootView.findViewById(R.id.null_layout);
        videoRv = rootView.findViewById(R.id.video_rv);
        rootView.findViewById(R.id.bottom_layout);

        tvCheckAll.setOnClickListener(view -> {
            List<LocalVideoEntity> list = localVideoRvAdapter.getData();

            boolean lastCheckAll = false;
            if (deleteList.size() == list.size()) {
                lastCheckAll = true;
            }
            boolean checkAll = !lastCheckAll;
            deleteList.clear();
            for (LocalVideoEntity entity : list) {
                if (checkAll) {
                    deleteList.add(entity.path);
                }
                entity.deleteSelect = checkAll;
            }
            String fixText = deleteList.size() > 0 ? "(" + deleteList.size() + ")" : "";
            tvDelete.setText(mContext.getResources().getString(R.string.ac_downed_text_delete) + fixText);
            localVideoRvAdapter.notifyDataSetChanged();
        });
        tvDelete.setOnClickListener(view -> {
            if (deleteList.size() > 0) {
                for (String path : deleteList) {
                    SystemUtil.deleteFile(path);
                    String fileName = SystemUtil.getFileName(path);
                    KKVideoDataManager.getInstance().removeLocalFile(fileName);
                    EventBus.getDefault().post(new DeleteVideoItemEvent(path));
                    localVideoRvAdapter.deleteItem(path);
                }
                deleteList.clear();
                Toast.makeText(mContext, mContext.getResources().getString(R.string.ac_downed_delete_ok), Toast.LENGTH_LONG).show();
                tvDelete.setText(mContext.getResources().getString(R.string.ac_downed_text_delete));
                EventBus.getDefault().post(new DeleteVideoOkEvent());

                List<LocalVideoEntity> list = localVideoRvAdapter.getData();
                if (list.size() == 0) {
                    nullLayout.setVisibility(View.VISIBLE);
                } else {
                    nullLayout.setVisibility(View.GONE);
                }
            }
        });

        //下拉刷新
        refreshLayout = rootView.findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(refreshLayout -> {
            loadLocalData();
        });
        refreshLayout.setEnableLoadMore(false);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        videoRv.setLayoutManager(layoutManager);
        videoRv.addItemDecoration(new RecyclerView.ItemDecoration() {
            int spacing = DensityUtil.dip2px(mContext, 12);
            int halfSpacing = spacing >> 1;

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.top = halfSpacing;
                outRect.right = halfSpacing;
                outRect.bottom = halfSpacing;
                outRect.left = halfSpacing;
            }
        });
        videoRv.setAdapter(localVideoRvAdapter = new LocalVideoRvAdapter2(mContext));
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
        localVideoRvAdapter.getLoadMoreModule().setEnableLoadMore(false);
        localVideoRvAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            EventBus.getDefault().post(new ChangeEditEvent());
            return true;
        });
        localVideoRvAdapter.setOnItemClickListener((adapter, view, position) -> {
            LocalVideoEntity itemEntity = localVideoRvAdapter.getItem(position);
            if (itemEntity.deleteSelect) {
                itemEntity.deleteSelect = false;
                deleteList.remove(itemEntity.path);
                String fixText = deleteList.size() > 0 ? "(" + deleteList.size() + ")" : "";
                tvDelete.setText(mContext.getResources().getString(R.string.ac_downed_text_delete) + fixText);
                localVideoRvAdapter.notifyItemChanged(position);
                return;
            }
            List<String> dataList = new ArrayList<>();
            List<LocalVideoEntity> list = localVideoRvAdapter.getData();
            for (LocalVideoEntity videoEntity : list) {
                dataList.add(videoEntity.path);
            }
            startActivity(new Intent(mContext, VideoPlayActivity.class).putExtra("position", position).putExtra("dataList", (Serializable) dataList));
        });
        localVideoRvAdapter.addChildClickViewIds(R.id.iv_check);
        localVideoRvAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            LocalVideoEntity videoEntity = localVideoRvAdapter.getItem(position);
            videoEntity.deleteSelect = !videoEntity.deleteSelect;
            if (videoEntity.deleteSelect) {
                deleteList.add(videoEntity.path);
            } else {
                deleteList.remove(videoEntity.path);
            }
            String fixText = deleteList.size() > 0 ? "(" + deleteList.size() + ")" : "";
            tvDelete.setText(mContext.getResources().getString(R.string.ac_downed_text_delete) + fixText);
            localVideoRvAdapter.notifyItemChanged(position);
        });
    }

    public void notifyEditChange(boolean isEdit) {
        localVideoRvAdapter.deleteModel(isEdit);
        if (isEdit) {
            bottomLayout.setVisibility(View.VISIBLE);
        } else {
            bottomLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLocalVideoFilesUpdate(List<KKVideoMessage> videoMessages) {
        KKLoger.d("TTT", "VideoLocalFragment=> onLocalVideoFilesUpdate");
        List<LocalVideoEntity> videoList = new ArrayList<>();
        for (KKVideoMessage kkVideoMessage : videoMessages) {
            if (kkVideoMessage.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {
                File downloadFile = kkVideoMessage.getDownloadStatus().getVideoFile();
                String name = downloadFile.getName();
                String path = downloadFile.getAbsolutePath();
                long time = downloadFile.lastModified();
                long size = kkVideoMessage.getDownloadStatus().getTotalSize();
                LocalVideoEntity videoEntity = new LocalVideoEntity(name, path, time, size);
                videoEntity.duration = kkVideoMessage.getVideoDuration();
                videoEntity.fromText = kkVideoMessage.getFromName();
                videoList.add(videoEntity);
            }
        }
        AndroidUtilities.runOnUIThread(() -> {
            checkVideo(videoList);
        });
    }

    @Override
    public void updateVideoDownloadStatus(String fileName, KKFileDownloadStatus fileDownloadStatus) {
    }

    private void loadLocalData() {
        String[] dirs = {
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/Telegram/Telegram Video",
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/Telegram/Telegram Documents"
        };
        FileScanManager.scanFile(dirs, new FileScanManager.FileListener() {
            @Override
            public void onFind(List<LocalVideoEntity> list) {
            }

            @Override
            public void onFinish(List<LocalVideoEntity> list) {
                refreshLayout.finishRefresh();
                AndroidUtilities.runOnUIThread(() -> {
                    checkVideo(list);
                });
            }
        });
    }

    private void checkVideo(List<LocalVideoEntity> list) {
        List<LocalVideoEntity> dataList = localVideoRvAdapter.getData();
        List<LocalVideoEntity> newList = new ArrayList<>();
        newList.addAll(dataList);

        for (LocalVideoEntity entity : list) {
            boolean has = false;
            for (LocalVideoEntity temp : dataList) {
                if (entity.name.equals(temp.name)) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                newList.add(entity);
            }
        }
        Collections.sort(newList, new LocalVideoSort());//排序
        if (newList.size() == 0) {
            localVideoRvAdapter.setList(null);
            nullLayout.setVisibility(View.VISIBLE);
        } else {
            nullLayout.setVisibility(View.GONE);
            localVideoRvAdapter.setList(newList);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteVideoItemEvent(DeleteVideoItemEvent event) {
        localVideoRvAdapter.deleteItem(event.filePath);
        List<LocalVideoEntity> list = localVideoRvAdapter.getData();
        if (list.size() == 0) {
            nullLayout.setVisibility(View.VISIBLE);
        } else {
            nullLayout.setVisibility(View.GONE);
        }
    }
}
