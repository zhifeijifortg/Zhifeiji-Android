package kk.ui.fragment;

import android.os.Bundle;

import org.greenrobot.eventbus.EventBus;
import org.telegram.messenger.R;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import kk.adapter.FileTabAdapter;
import kk.files.KKFileTypes;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class FileContainerFragment extends BaseFragment {
    private RecyclerView tabLayout;
    FileTabAdapter fileTabAdapter;
    KKFileTypes currentTab = KKFileTypes.ALL;
    KKFileTypes extTab;


    public static FileContainerFragment instance() {
        FileContainerFragment fragment = new FileContainerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getContentView() {
        return R.layout.fragment_filecontainer;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onViewCreated() {
        super.onViewCreated();
        initView();
    }

    private void initView() {
        tabLayout = rootView.findViewById(R.id.tab_layout);
        tabLayout.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        tabLayout.setAdapter(fileTabAdapter = new FileTabAdapter(mContext));
        fileTabAdapter.setOnItemClickListener((adapter, view, position) -> {  //tab切换
            KKFileTypes type = fileTabAdapter.getItem(position);
            if (type == currentTab) return;
            currentTab = type;

            FragmentManager manager = mContext.getSupportFragmentManager();
            if (extTab != null) {
                FileListFragment ex_fragment = (FileListFragment) manager.findFragmentByTag(extTab.getSuffix());
                ex_fragment.beforeFragmentHidden();
            }
            FileListFragment fragment = (FileListFragment) manager.findFragmentByTag(currentTab.getSuffix());
            if(fragment != null){
                fragment.tabChangeState();
            }

            fileTabAdapter.changeSelect(position);
            fragmentLoop();
        });
        fileTabAdapter.setList(KKFileTypes.getTabShowTypes());
        fragmentLoop();
    }

    private void fragmentLoop() {
        FragmentManager manager = mContext.getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        Fragment fragment = manager.findFragmentByTag(currentTab.getSuffix());
        if (fragment == null) {
            fragment = FileListFragment.instance(currentTab.getSuffix());
            transaction.add(R.id.list_frame_layout, fragment, currentTab.getSuffix());
        }
        if (extTab != null) {
            Fragment ex_fragment = manager.findFragmentByTag(extTab.getSuffix());
            if (ex_fragment != null && !ex_fragment.isHidden()) {
                transaction.hide(ex_fragment);
            }
        }
        transaction.show(fragment);
        transaction.commitAllowingStateLoss();
        extTab = currentTab;
    }
}
