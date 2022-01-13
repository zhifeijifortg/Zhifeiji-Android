package kk.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
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
import org.telegram.messenger.R;

import java.util.Arrays;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import kk.adapter.DownloadPageAdapter;
import kk.event.ChangeEditEvent;
import kk.event.DeleteVideoOkEvent;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class DownloadActivity extends BaseActivity {
    LinearLayout layoutTitle;
    TextView tvEdit;
    MagicIndicator magicIndicator;
    ViewPager viewpager;
    DownloadPageAdapter downloadPageAdapter;
    boolean edit = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_video_download);

        initView();
    }

    private void initView() {
        layoutTitle = findViewById(R.id.layout_title);
        //layoutTitle.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        //int defaultBackground = Theme.getDefaultAccentColor(Theme.key_chat_wallpaper);
        //StatusBarUtil.setColor(this, defaultBackground);

        tvEdit = findViewById(R.id.tv_edit);
        tvEdit.setOnClickListener(view -> {
            changeEdit();
        });
        findViewById(R.id.iv_back).setOnClickListener(view -> finish());
        magicIndicator = findViewById(R.id.magic_indicator);
        viewpager = findViewById(R.id.viewpager);

        String[] tabs = this.getResources().getStringArray(R.array.download_tables);//tabs
        //magicIndicator.setBackgroundColor(getResources().getColor(R.color.white));
        CommonNavigator commonNavigator = new CommonNavigator(this);
        commonNavigator.setAdjustMode(true);
        commonNavigator.setAdapter(new CommonNavigatorAdapter() {
            @Override
            public int getCount() {
                return tabs.length;
            }

            @Override
            public IPagerTitleView getTitleView(Context context, final int index) {
                SimplePagerTitleView simplePagerTitleView = new ColorTransitionPagerTitleView(context);
                simplePagerTitleView.setNormalColor(Color.parseColor("#656A6E"));
                simplePagerTitleView.setSelectedColor(Color.parseColor("#69B1F2"));
                simplePagerTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                simplePagerTitleView.setText(tabs[index]);
                simplePagerTitleView.setOnClickListener(v -> viewpager.setCurrentItem(index));
                return simplePagerTitleView;
            }

            @Override
            public IPagerIndicator getIndicator(Context context) {
                LinePagerIndicator linePagerIndicator = new LinePagerIndicator(context);
                linePagerIndicator.setMode(LinePagerIndicator.MODE_WRAP_CONTENT);
                linePagerIndicator.setLineHeight(10);
                linePagerIndicator.setColors(Color.parseColor("#69B1F2"));
                return linePagerIndicator;
            }
        });
        magicIndicator.setNavigator(commonNavigator);
        viewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                magicIndicator.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                magicIndicator.onPageSelected(position);
                if (position == 0) {
                    tvEdit.setVisibility(View.INVISIBLE);
                } else {
                    tvEdit.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                magicIndicator.onPageScrollStateChanged(state);
            }
        });
        viewpager.setAdapter(downloadPageAdapter = new DownloadPageAdapter(getSupportFragmentManager(), Arrays.asList(tabs)));
    }

    private void changeEdit() {
        edit = !edit;
        if (edit) {
            tvEdit.setText(this.getResources().getString(R.string.ac_down_cancel));
        } else {
            tvEdit.setText(this.getResources().getString(R.string.ac_down_edit));
        }
        downloadPageAdapter.notifyEditChange(edit);
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
}
