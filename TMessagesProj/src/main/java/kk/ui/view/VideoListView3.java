package kk.ui.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;
import kk.KKLoger;
import kk.adapter.VideoPageAdapter;
import kk.event.ChangeEditEvent;
import kk.event.DeleteVideoOkEvent;
import kk.files.KKFileMessage;
import kk.files.KKLocalFileManager;
import kk.ui.activity.DownloadActivity;
import kk.utils.DensityUtil;
import kk.utils.EventUtil;
import kk.video.KKFileDownloadStatus;
import kk.video.KKLocalVideoFileManager;
import kk.video.KKVideoDownloadListener;
import kk.video.KKVideoMessage;

/**
 * Created by LSD on 2021/3/2.
 * Desc
 */
public class VideoListView3 extends FrameLayout implements KKLocalFileManager.Listener<KKFileMessage>, KKVideoDownloadListener {
    FragmentActivity activity;
    MagicIndicator magicIndicator;
    ViewPager2 viewpager2;
    RelativeLayout floatLayout;
    TextView tvDownloadingNum;
    TextView tvEdit;

    VideoPageAdapter videoPageAdapter;
    private boolean isAnimatorEnd = true;
    boolean edit = false;

    public void onResume() {
    }

    public void onPause() {
    }

    public VideoListView3(@NonNull BaseFragment fragment) {
        super(fragment.getParentActivity());
        EventBus.getDefault().register(this);
        activity = fragment.getParentActivity();
        LayoutInflater.from(activity).inflate(R.layout.view_video_list3, this, true);

        initView();

        //添加文件状态监听 - 处理下载中个数
        KKLocalFileManager.getInstance().addLocalVideoFilesListener(this, this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        tvDownloadingNum = findViewById(R.id.tv_download_num);
        floatLayout = findViewById(R.id.float_layout);
        magicIndicator = findViewById(R.id.magic_indicator);
        viewpager2 = findViewById(R.id.viewpager);
        tvEdit = findViewById(R.id.tv_edit);

        floatLayout.setOnClickListener(view -> {
            EventUtil.post(activity, EventUtil.Even.video页面浮动按钮点击, new HashMap<>());
            activity.startActivity(new Intent(activity, DownloadActivity.class));
        });
        tvEdit.setOnClickListener(view -> {
            changeEdit();
        });

        //magicIndicator.setBackgroundColor(Color.parseColor("#ffffff"));
        String[] tabs = activity.getResources().getStringArray(R.array.video_list_tables);//tabs
        CommonNavigator commonNavigator = new CommonNavigator(activity);
        //commonNavigator.setAdjustMode(true);
        commonNavigator.setAdapter(new CommonNavigatorAdapter() {
            @Override
            public int getCount() {
                return tabs.length;
            }

            @Override
            public IPagerTitleView getTitleView(Context context, final int index) {
                SimplePagerTitleView simplePagerTitleView = new ColorTransitionPagerTitleView(context);
                simplePagerTitleView.setNormalColor(Color.parseColor("#B3FFFFFF"));
                simplePagerTitleView.setSelectedColor(Color.parseColor("#FFFFFF"));
                simplePagerTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                simplePagerTitleView.setText(tabs[index]);
                simplePagerTitleView.setWidth(DensityUtil.dip2px(context,110));
                TextPaint tp = simplePagerTitleView.getPaint();
                tp.setFakeBoldText(true);
                simplePagerTitleView.setOnClickListener(v -> viewpager2.setCurrentItem(index));
                return simplePagerTitleView;
            }

            @Override
            public IPagerIndicator getIndicator(Context context) {
                LinePagerIndicator linePagerIndicator = new LinePagerIndicator(context);
                linePagerIndicator.setMode(LinePagerIndicator.MODE_EXACTLY);
                linePagerIndicator.setLineHeight(10);
                linePagerIndicator.setLineWidth(DensityUtil.dip2px(context,62));
                linePagerIndicator.setColors(Color.parseColor("#ffffff"));
                return linePagerIndicator;
            }
        });
        magicIndicator.setNavigator(commonNavigator);
        viewpager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                magicIndicator.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                magicIndicator.onPageSelected(position);
                if (position == 0) {
                    EventUtil.post(activity, EventUtil.Even.video页面展示, new HashMap<>());
                    tvEdit.setVisibility(View.INVISIBLE);
                } else {
                    EventUtil.post(activity, EventUtil.Even.video页面缓存展示, new HashMap<>());
                    tvEdit.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                magicIndicator.onPageScrollStateChanged(state);
            }
        });
        //viewpager2.setUserInputEnabled(false);//禁止左右滑动
        viewpager2.setOffscreenPageLimit(1);
        viewpager2.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewpager2.setAdapter(videoPageAdapter = new VideoPageAdapter(activity, Arrays.asList(tabs)));
    }

    //显示浮窗
    private void showFloatView() {
        if (!isAnimatorEnd) return;
        if (floatLayout.getVisibility() == View.VISIBLE) return;
        floatLayout.setVisibility(View.VISIBLE);
        ObjectAnimator animator = ObjectAnimator.ofFloat(floatLayout, "translationY", 0);
        animator.setDuration(400);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimatorEnd = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimatorEnd = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    //隐藏浮窗
    private void hideFloatView() {
        if (!isAnimatorEnd) return;
        if (floatLayout.getVisibility() == GONE) return;
        ObjectAnimator animator = ObjectAnimator.ofFloat(floatLayout, "translationY", DensityUtil.dip2px(activity, 80));
        animator.setDuration(400);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimatorEnd = false;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isAnimatorEnd = true;
                floatLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }

    private void changeEdit() {
        edit = !edit;
        if (edit) {
            floatLayout.setVisibility(GONE);
            tvEdit.setText(activity.getResources().getString(R.string.ac_down_cancel));
        } else {
            floatLayout.setVisibility(VISIBLE);
            tvEdit.setText(activity.getResources().getString(R.string.ac_down_edit));
        }
        videoPageAdapter.notifyEditChange(edit);
    }

    @Override
    public void updateVideoDownloadStatus(String fileName, KKFileDownloadStatus fileDownloadStatus) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteVideoEvent(DeleteVideoOkEvent event) {
        edit = true;
        changeEdit();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChangeEditEvent(ChangeEditEvent event) {
        changeEdit();
    }

    @Override
    public void onLocalVideoFilesUpdate(List<KKFileMessage> videoMessages) {
        List<KKFileMessage> result = new ArrayList<>();
        for (KKFileMessage kkFileMessage : videoMessages) {
            if (!kkFileMessage.isDateMessage() && KKFileDownloadStatus.Status.DOWNLOADED != kkFileMessage.getDownloadStatus().getStatus()) {
                result.add(kkFileMessage);
            }
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (result.size() == 0) {
                tvDownloadingNum.setVisibility(INVISIBLE);
            } else {
                tvDownloadingNum.setVisibility(VISIBLE);
                tvDownloadingNum.setText(result.size() + "");
            }
        });
    }
}
