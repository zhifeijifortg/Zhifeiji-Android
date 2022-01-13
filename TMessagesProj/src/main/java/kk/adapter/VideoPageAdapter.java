package kk.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import kk.ui.fragment.BaseFragment;
import kk.ui.fragment.FileContainerFragment;
import kk.ui.fragment.FileListFragment;
import kk.ui.fragment.FileLocalFragment;
import kk.ui.fragment.VideoLocalFragment;

public class VideoPageAdapter extends FragmentStateAdapter {
    List<String> list;
    Map<String, BaseFragment> placeHolder = new HashMap<>();

    public VideoPageAdapter(@NonNull FragmentActivity fragmentActivity, List<String> list) {
        super(fragmentActivity);
        this.list = list;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (placeHolder.get("" + position) != null) {
            return placeHolder.get(position + "");
        } else {
            BaseFragment fragment = null;
            if (position == 0) {
                fragment = FileContainerFragment.instance();
            } else if (position == 1) {
                fragment = FileLocalFragment.instance();
            }
            placeHolder.put(position + "", fragment);
            return fragment;
        }
    }

    @Override
    public int getItemCount() {
        return null == list ? 0 : list.size();
    }

    public void notifyEditChange(boolean isEdit) {
        FileLocalFragment fragment = (FileLocalFragment) placeHolder.get("" + 1);
        fragment.notifyEditChange(isEdit);
    }
}
