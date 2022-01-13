package kk.adapter;

import android.content.Context;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;

import org.jetbrains.annotations.NotNull;
import org.telegram.messenger.R;

import kk.player.IVideoPlayer;

public class VideoPlayAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    Context context;

    public VideoPlayAdapter(Context context) {
        super(R.layout.layout_video_play_item);
        this.context = context;
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, String entity) {
        IVideoPlayer iVideoPlayer = baseViewHolder.findView(R.id.i_player);
        iVideoPlayer.getTitleTextView().setVisibility(View.GONE);
        iVideoPlayer.getBackButton().setVisibility(View.GONE);
        GSYVideoOptionBuilder gsyVideoOptionBuilder = new GSYVideoOptionBuilder();
        gsyVideoOptionBuilder
                .setIsTouchWiget(false)
                .setUrl("file://" + entity)
                .setLooping(false)
                .setPlayPosition(baseViewHolder.getAdapterPosition())
                .setRotateViewAuto(false)
                .setLockLand(true)
                .setShowFullAnimation(true)
                .setNeedLockFull(true)
                .setVideoAllCallBack(new GSYSampleCallBack() {
                    @Override
                    public void onClickBlank(String url, Object... objects) {
                        //GSYVideoManager.onPause();
                        IVideoPlayer player = (IVideoPlayer) objects[1];
                        player.onClickBlankShowUI();
                    }

                    @Override
                    public void onAutoComplete(String url, Object... objects) {
                        IVideoPlayer player = (IVideoPlayer) objects[1];
                        player.onAutoCompleteShowUI();
                    }
                }).build(iVideoPlayer);
    }
}
