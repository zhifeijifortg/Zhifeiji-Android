package kk.player;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.widget.ImageView;

import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import org.telegram.messenger.R;

import moe.codeest.enviews.ENDownloadView;

/**
 * Created by LSD on 2021/2/24.
 * Desc
 */
public class IVideoPlayer extends StandardGSYVideoPlayer {
    ImageView mCoverImage;
    boolean blankClick = false;

    public IVideoPlayer(Context context) {
        super(context);
    }

    public IVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getLayoutId() {
        return R.layout.ivideo_layout;
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        mCoverImage = (ImageView) findViewById(R.id.thumbImage);
        if (mThumbImageViewLayout != null && (mCurrentState == -1 || mCurrentState == CURRENT_STATE_NORMAL || mCurrentState == CURRENT_STATE_ERROR)) {
            mThumbImageViewLayout.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void updateStartImage() {
        if (mStartButton instanceof ImageView) {
            ImageView imageView = (ImageView) mStartButton;
            if (mCurrentState == CURRENT_STATE_PAUSE) {
                imageView.setImageResource(com.shuyu.gsyvideoplayer.R.drawable.video_click_play_selector);
            } else if (mCurrentState == CURRENT_STATE_ERROR) {
                imageView.setImageResource(com.shuyu.gsyvideoplayer.R.drawable.video_click_error_selector);
            } else {
                imageView.setImageResource(com.shuyu.gsyvideoplayer.R.drawable.video_click_pause_selector);
            }
        }
    }

    @Override
    public void onSurfaceAvailable(Surface surface) {
        super.onSurfaceAvailable(surface);
        if (GSYVideoType.getRenderType() != GSYVideoType.TEXTURE) {
            if (mThumbImageViewLayout != null && mThumbImageViewLayout.getVisibility() == VISIBLE) {
                mThumbImageViewLayout.setVisibility(INVISIBLE);
            }
        }
    }

    @Override
    protected void hideAllWidget() {
        super.hideAllWidget();
        setViewShowState(mBottomProgressBar, INVISIBLE);
    }

    @Override
    protected void changeUiToPlayingShow() {
        super.changeUiToPlayingShow();
        if (!blankClick) {
            hideAllWidget();
        } else {
            setViewShowState(mProgressBar, VISIBLE);
        }
        blankClick = false;
    }

    public void onClickBlankShowUI() {
        blankClick = true;
    }

    public void onAutoCompleteShowUI() {
        ImageView imageView = (ImageView) mStartButton;
        imageView.setImageResource(com.shuyu.gsyvideoplayer.R.drawable.video_click_play_selector);
        setViewShowState(mProgressBar, VISIBLE);
    }

    @Override
    protected void changeUiToNormal() {
        super.changeUiToNormal();
        setViewShowState(mStartButton, INVISIBLE);
    }

    @Override
    protected void changeUiToPreparingShow() {
        super.changeUiToPreparingShow();
        setViewShowState(mProgressBar, INVISIBLE);
    }
}
