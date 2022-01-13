package kk.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import kk.KKLoger;
import kk.adapter.LocalVideoRvAdapter;
import kk.event.ChangeEditEvent;
import kk.event.DeleteVideoItemEvent;
import kk.event.DeleteVideoOkEvent;
import kk.files.KKFileMessage;
import kk.files.KKFileTypes;
import kk.files.KKLocalFileManager;
import kk.model.LocalVideoEntity;
import kk.ui.activity.VideoPlayActivity;
import kk.utils.FileScanManager;
import kk.utils.SystemUtil;
import kk.utils.sort.LocalVideoSort;
import kk.video.KKFileDownloadStatus;
import kk.video.KKVideoDataManager;
import kk.video.KKVideoDownloadListener;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class DownloadedFragment extends BaseFragment implements KKLocalFileManager.Listener<KKFileMessage>, KKVideoDownloadListener {
    private RecyclerView videoRv;
    private LinearLayout nullLayout;
    private LinearLayout bottomLayout;
    private TextView tvCheckAll;
    private TextView tvDelete;
    private LocalVideoRvAdapter localVideoRvAdapter;
    private List<String> deleteList = new ArrayList<>();
    boolean isActive;

    public static DownloadedFragment instance() {
        DownloadedFragment fragment = new DownloadedFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getContentView() {
        return R.layout.fragment_downloaded;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isActive = false;
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onViewCreated() {
        super.onViewCreated();
        isActive = true;
        EventBus.getDefault().register(this);
        initView();

        KKLocalFileManager.getInstance().addLocalVideoFilesListener(this, this);
    }


    private void initView() {
        bottomLayout = rootView.findViewById(R.id.bottom_layout);
        tvCheckAll = rootView.findViewById(R.id.tv_checkall);
        tvDelete = rootView.findViewById(R.id.tv_dodelete);
        nullLayout = rootView.findViewById(R.id.null_layout);
        videoRv = rootView.findViewById(R.id.video_rv);

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
            if (checkAll) {
                tvCheckAll.setText(mContext.getResources().getString(R.string.ac_downed_text_checkno));
            } else {
                tvCheckAll.setText(mContext.getResources().getString(R.string.ac_downed_text_checkall));
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

        videoRv.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        videoRv.setAdapter(localVideoRvAdapter = new LocalVideoRvAdapter(mContext));
        localVideoRvAdapter.getLoadMoreModule().setEnableLoadMore(false);
        localVideoRvAdapter.setOnItemClickListener((adapter, view, position) -> {
            LocalVideoEntity itemEntity = localVideoRvAdapter.getItem(position);
            if (itemEntity.deleteSelect) {//删除模式
                itemEntity.deleteSelect = false;
                deleteList.remove(itemEntity.path);
                String fixText = deleteList.size() > 0 ? "(" + deleteList.size() + ")" : "";
                tvDelete.setText(mContext.getResources().getString(R.string.ac_downed_text_delete) + fixText);
                localVideoRvAdapter.notifyItemChanged(position);
            } else {//普通模式点击
                if (itemEntity.fileType == KKFileTypes.VIDEO_FILTER || itemEntity.fileType == KKFileTypes.MKV || itemEntity.fileType == KKFileTypes.MOV || itemEntity.fileType == KKFileTypes.MP4) {
                    //媒体
                    int p_position = 0;
                    List<String> dataList = new ArrayList<>();
                    List<LocalVideoEntity> list = localVideoRvAdapter.getDownloadedMedia();
                    for (int i = 0; i < list.size(); i++) {
                        LocalVideoEntity temp = list.get(i);
                        dataList.add(temp.path);
                        if (itemEntity.path.equals(temp.path)) {
                            p_position = i;
                        }
                    }
                    if (dataList.size() == 0) return;
                    startActivity(new Intent(mContext, VideoPlayActivity.class).putExtra("position", p_position).putExtra("dataList", (Serializable) dataList));
                } else {//其他
                    try {
                        AndroidUtilities.openForView(new File(itemEntity.path), mContext);
                    } catch (Exception e) {
                        KKLoger.e("TTT", e.getMessage());
                    }
                }
            }
        });
        localVideoRvAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            EventBus.getDefault().post(new ChangeEditEvent());
            return true;
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
    public void onLocalVideoFilesUpdate(List<KKFileMessage> fileMessages) {
        if (!isActive) return;
        List<LocalVideoEntity> videoList = new ArrayList<>();
        for (KKFileMessage kkFileMessage : fileMessages) {
            if (kkFileMessage.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {
                File downloadFile = kkFileMessage.getDownloadStatus().getVideoFile();
                String name = kkFileMessage.getFileName();
                String path = downloadFile.getAbsolutePath();
                long time = kkFileMessage.getDownloadTime();
                long size = kkFileMessage.getDownloadStatus().getTotalSize();
                LocalVideoEntity videoEntity = new LocalVideoEntity(name, path, time, size);
                videoEntity.videoId = kkFileMessage.getId();
                videoEntity.fileType = kkFileMessage.getFileType();
                videoEntity.duration = kkFileMessage.getMediaDuration();
                videoEntity.fromText = kkFileMessage.getFromName();
                videoList.add(videoEntity);
            }
        }
        AndroidUtilities.runOnUIThread(() -> {
            checkVideo(false, videoList);

            //加载本地文件
            loadLocalVideo();
        });
    }

    @Override
    public void updateVideoDownloadStatus(String fileName, KKFileDownloadStatus fileDownloadStatus) {
    }

    private void loadLocalVideo() {
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
                AndroidUtilities.runOnUIThread(() -> {
                    checkVideo(true, list);
                });
            }
        });
    }

    private void checkVideo(boolean isLocal, List<LocalVideoEntity> list) {
        List<LocalVideoEntity> dataList = localVideoRvAdapter.getData();
        List<LocalVideoEntity> newList = new ArrayList<>();
        newList.addAll(dataList);

        for (LocalVideoEntity entity : list) {
            if (!isLocal && entity.videoId != -1) {
                SystemUtil.removeDownloadData(entity.videoId);
            }
            boolean has = false;
            for (LocalVideoEntity temp : dataList) {
                if (entity.path.equals(temp.path)) {
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
