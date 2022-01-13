package kk.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.shuyu.gsyvideoplayer.GSYVideoManager;

import org.greenrobot.eventbus.EventBus;
import org.telegram.messenger.R;

import java.util.HashMap;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import kk.adapter.VideoPlayAdapter;
import kk.event.DeleteVideoItemEvent;
import kk.player.IVideoPlayer;
import kk.utils.EventUtil;
import kk.utils.ShareUtil;
import kk.utils.SystemUtil;
import kk.video.KKVideoDataManager;

/**
 * Created by LSD on 2021/3/9.
 * Desc
 */
public class VideoPlayActivity extends BaseActivity {
    ViewPager2 viewPager2;
    VideoPlayAdapter videoPlayAdapter;
    List<String> list;
    int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video_play);
        EventUtil.post(mActivity, EventUtil.Even.视频播放, new HashMap<>());

        initView();
        initData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        GSYVideoManager.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GSYVideoManager.onResume(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GSYVideoManager.releaseAllVideos();
    }

    private void initView() {
        findViewById(R.id.iv_back).setOnClickListener(view -> finish());
        findViewById(R.id.iv_delete).setOnClickListener(view -> {
            int position = viewPager2.getCurrentItem();
            String path = videoPlayAdapter.getItem(position);
            String fileName = SystemUtil.getFileName(path);
            KKVideoDataManager.getInstance().removeLocalFile(fileName);
            SystemUtil.deleteFile(path);
            EventBus.getDefault().post(new DeleteVideoItemEvent(path));
            Toast.makeText(this, this.getResources().getString(R.string.ac_downed_delete_ok), Toast.LENGTH_LONG).show();
            finish();
        });
        findViewById(R.id.iv_share).setOnClickListener(view -> {
            int position = viewPager2.getCurrentItem();
            String path = videoPlayAdapter.getItem(position);
            ShareUtil.shareVideo2(this, path);
        });
        viewPager2 = findViewById(R.id.viewPager2);
        viewPager2.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        viewPager2.setOffscreenPageLimit(1);
        viewPager2.setAdapter(videoPlayAdapter = new VideoPlayAdapter(this));
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                //自动播放
                int playPosition = GSYVideoManager.instance().getPlayPosition();
                if (playPosition >= 0 && playPosition != position) {
                    play(position);
                }
            }
        });
    }

    private void initData() {
        list = (List<String>) getIntent().getSerializableExtra("dataList");
        position = getIntent().getIntExtra("position", 0);

        videoPlayAdapter.setList(list);
        viewPager2.setCurrentItem(position, false);
        viewPager2.post(() -> {
            play(position);
        });
    }

    private void play(int position) {
        //开始新播放
        RecyclerView.ViewHolder viewHolder = ((RecyclerView) viewPager2.getChildAt(0)).findViewHolderForAdapterPosition(position);
        if (viewHolder != null) {
            BaseViewHolder baseViewHolder = (BaseViewHolder) viewHolder;
            IVideoPlayer iVideoPlayer = baseViewHolder.findView(R.id.i_player);
            iVideoPlayer.startPlayLogic();
        }
    }
}
