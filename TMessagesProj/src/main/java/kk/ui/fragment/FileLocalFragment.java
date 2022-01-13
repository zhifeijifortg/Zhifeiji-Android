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
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import kk.KKLoger;
import kk.adapter.FileLocalRvAdapter;
import kk.adapter.FileTabAdapter;
import kk.event.ChangeEditEvent;
import kk.event.DeleteVideoItemEvent;
import kk.event.DeleteVideoOkEvent;
import kk.files.KKFileMessage;
import kk.files.KKFileTypes;
import kk.files.KKLocalFileManager;
import kk.model.LocalVideoEntity;
import kk.ui.activity.VideoPlayActivity;
import kk.utils.DensityUtil;
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
public class FileLocalFragment extends BaseFragment implements KKLocalFileManager.Listener<KKFileMessage>, KKVideoDownloadListener {
    private RecyclerView tabLayout;
    private RecyclerView fileRv;
    private SmartRefreshLayout refreshLayout;
    private LinearLayout nullLayout;
    private LinearLayout bottomLayout;
    private TextView tvCheckAll;
    private TextView tvDelete;

    private FileTabAdapter fileTabAdapter;
    private FileLocalRvAdapter localFileRvAdapter;
    private List<String> deleteList = new ArrayList<>();
    private List<LocalVideoEntity> tempDataList;
    private KKFileTypes type = KKFileTypes.ALL;

    public static FileLocalFragment instance() {
        FileLocalFragment fragment = new FileLocalFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getContentView() {
        return R.layout.fragment_filelocal;
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

        //添加监听已完成的
        KKLocalFileManager.getInstance().addLocalVideoFilesListener(this, this);
    }

    private void initView() {
        tabLayout = rootView.findViewById(R.id.tab_layout);
        tabLayout.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        tabLayout.setAdapter(fileTabAdapter = new FileTabAdapter(mContext));
        fileTabAdapter.setOnItemClickListener((adapter, view, position) -> {  //tab切换
            fileTabAdapter.changeSelect(position);
            type = fileTabAdapter.getItem(position);
            localFileRvAdapter.setList(filterList());
        });
        fileTabAdapter.setList(KKFileTypes.getTabShowTypes());

        bottomLayout = rootView.findViewById(R.id.bottom_layout);
        tvCheckAll = rootView.findViewById(R.id.tv_checkall);
        tvDelete = rootView.findViewById(R.id.tv_dodelete);
        nullLayout = rootView.findViewById(R.id.null_layout);
        fileRv = rootView.findViewById(R.id.file_rv);
        rootView.findViewById(R.id.bottom_layout);

        tvCheckAll.setOnClickListener(view -> {
            List<LocalVideoEntity> list = localFileRvAdapter.getData();

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
            localFileRvAdapter.notifyDataSetChanged();
        });
        tvDelete.setOnClickListener(view -> {
            if (deleteList.size() > 0) {
                for (String path : deleteList) {
                    SystemUtil.deleteFile(path);
                    String fileName = SystemUtil.getFileName(path);
                    KKVideoDataManager.getInstance().removeLocalFile(fileName);
                    EventBus.getDefault().post(new DeleteVideoItemEvent(path));
                    localFileRvAdapter.deleteItem(path);
                }
                deleteList.clear();
                Toast.makeText(mContext, mContext.getResources().getString(R.string.ac_downed_delete_ok), Toast.LENGTH_LONG).show();
                tvDelete.setText(mContext.getResources().getString(R.string.ac_downed_text_delete));
                EventBus.getDefault().post(new DeleteVideoOkEvent());

                List<LocalVideoEntity> list = localFileRvAdapter.getData();
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
        fileRv.setLayoutManager(layoutManager);
        fileRv.addItemDecoration(new RecyclerView.ItemDecoration() {
            int spacing = DensityUtil.dip2px(mContext, 5);
            int halfSpacing = spacing >> 1;

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.top = halfSpacing;
                outRect.right = halfSpacing;
                outRect.bottom = halfSpacing;
                outRect.left = halfSpacing;
            }
        });
        fileRv.setAdapter(localFileRvAdapter = new FileLocalRvAdapter(mContext));
        localFileRvAdapter.getLoadMoreModule().setEnableLoadMore(false);
        localFileRvAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            EventBus.getDefault().post(new ChangeEditEvent());
            return true;
        });
        localFileRvAdapter.setOnItemClickListener((adapter, view, position) -> {
            LocalVideoEntity itemEntity = localFileRvAdapter.getItem(position);
            if (itemEntity.deleteSelect) {//删除模式
                itemEntity.deleteSelect = false;
                deleteList.remove(itemEntity.path);
                String fixText = deleteList.size() > 0 ? "(" + deleteList.size() + ")" : "";
                tvDelete.setText(mContext.getResources().getString(R.string.ac_downed_text_delete) + fixText);
                localFileRvAdapter.notifyItemChanged(position);
            } else {//普通模式点击
                if (itemEntity.fileType == KKFileTypes.VIDEO_FILTER || itemEntity.fileType == KKFileTypes.MKV || itemEntity.fileType == KKFileTypes.MOV || itemEntity.fileType == KKFileTypes.MP4) {
                    //媒体
                    int p_position = 0;
                    List<String> dataList = new ArrayList<>();
                    List<LocalVideoEntity> list = localFileRvAdapter.getDownloadedMedia();
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
        localFileRvAdapter.addChildClickViewIds(R.id.iv_check);
        localFileRvAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            LocalVideoEntity videoEntity = localFileRvAdapter.getItem(position);
            videoEntity.deleteSelect = !videoEntity.deleteSelect;
            if (videoEntity.deleteSelect) {
                deleteList.add(videoEntity.path);
            } else {
                deleteList.remove(videoEntity.path);
            }
            String fixText = deleteList.size() > 0 ? "(" + deleteList.size() + ")" : "";
            tvDelete.setText(mContext.getResources().getString(R.string.ac_downed_text_delete) + fixText);
            localFileRvAdapter.notifyItemChanged(position);
        });
    }

    public void notifyEditChange(boolean isEdit) {
        localFileRvAdapter.deleteModel(isEdit);
        if (isEdit) {
            bottomLayout.setVisibility(View.VISIBLE);
        } else {
            bottomLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void updateVideoDownloadStatus(String fileName, KKFileDownloadStatus fileDownloadStatus) {
    }

    @Override
    public void onLocalVideoFilesUpdate(List<KKFileMessage> fileMessages) {
        List<LocalVideoEntity> videoList = new ArrayList<>();
        for (KKFileMessage kkFileMessage : fileMessages) {
            if (kkFileMessage.getDownloadStatus().getStatus() == KKFileDownloadStatus.Status.DOWNLOADED) {
                File downloadFile = kkFileMessage.getDownloadStatus().getVideoFile();
                String name = kkFileMessage.getFileName();
                String path = downloadFile.getAbsolutePath();
                long time = kkFileMessage.getDownloadTime();
                long size = kkFileMessage.getDownloadStatus().getTotalSize();
                LocalVideoEntity videoEntity = new LocalVideoEntity(name, path, time, size);
                videoEntity.fileType = kkFileMessage.getFileType();
                videoEntity.duration = kkFileMessage.getMediaDuration();
                videoEntity.fromText = kkFileMessage.getFromName();
                videoList.add(videoEntity);
            }
        }
        AndroidUtilities.runOnUIThread(() -> {
            checkVideo(videoList);

            //加载本地的
            loadLocalData();
        });
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
        List<LocalVideoEntity> dataList = localFileRvAdapter.getData();
        List<LocalVideoEntity> newList = new ArrayList<>();
        newList.addAll(dataList);

        for (LocalVideoEntity entity : list) {
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
            localFileRvAdapter.setList(null);
            nullLayout.setVisibility(View.VISIBLE);
        } else {
            tempDataList = newList;
            nullLayout.setVisibility(View.GONE);

            List<LocalVideoEntity> finalData = filterList();
            localFileRvAdapter.setList(finalData);
        }
    }

    //tab过滤数据
    private List<LocalVideoEntity> filterList() {
        if (type == KKFileTypes.ALL) {
            return tempDataList;
        }
        List<String> arrays = new ArrayList<>();
        if (type == KKFileTypes.TAG_VIDEO) {
            arrays.add("mp4");
            arrays.add("mov");
            arrays.add("mkv");
        } else if (type == KKFileTypes.TAG_FILES) {
            arrays.add("txt");
            arrays.add("doc");
        } else if (type == KKFileTypes.TAG_OTHERS) {
            arrays.add("apk");
        }
        if (tempDataList == null && tempDataList.size() == 0) {
            return new ArrayList<>();
        }
        return tempDataList.stream().filter(new Predicate<LocalVideoEntity>() {
            @Override
            public boolean test(LocalVideoEntity item) {
                String extensionName = SystemUtil.getExtensionName(item.path);
                extensionName = extensionName.toLowerCase();
                return arrays.contains(extensionName);
            }
        }).collect(Collectors.toList());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteVideoItemEvent(DeleteVideoItemEvent event) {
        localFileRvAdapter.deleteItem(event.filePath);
        List<LocalVideoEntity> list = localFileRvAdapter.getData();
        if (list.size() == 0) {
            nullLayout.setVisibility(View.VISIBLE);
        } else {
            nullLayout.setVisibility(View.GONE);
        }
    }
}
