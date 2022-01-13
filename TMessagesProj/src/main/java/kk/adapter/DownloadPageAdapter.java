package kk.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import kk.ui.fragment.BaseFragment;
import kk.ui.fragment.DownloadedFragment;
import kk.ui.fragment.DownloadingFragment;

public class DownloadPageAdapter extends FragmentPagerAdapter {
    List<String> list;
    Map<String, BaseFragment> placeHolder = new HashMap<>();

    public DownloadPageAdapter(FragmentManager fm, List<String> list) {
        super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.list = list;
    }

    @Override
    public Fragment getItem(int position) {
        if (placeHolder.get("" + position) != null) {
            return placeHolder.get(position + "");
        } else {
            BaseFragment fragment = null;
            if (position == 0) {
                fragment = DownloadingFragment.instance();
            } else if (position == 1) {
                fragment = DownloadedFragment.instance();
            }
            placeHolder.put(position + "", fragment);
            return fragment;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return list.get(position);
    }

    @Override
    public int getCount() {
        return null == list ? 0 : list.size();
    }

    public void notifyEditChange(boolean isEdit) {
        DownloadedFragment fragment = (DownloadedFragment) placeHolder.get("" + 1);
        fragment.notifyEditChange(isEdit);
    }
}
