package kk.ui.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;

import androidx.annotation.NonNull;

/**
 * Created by LSD on 2021/3/2.
 * Desc
 */
public class ContactView extends FrameLayout {
    private Context context;
    private FrameLayout rootView;

    public ContactView(@NonNull BaseFragment fragment) {
        super(fragment.getParentActivity());
        context = fragment.getParentActivity();
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.view_contact, this, true);
        rootView  = findViewById(R.id.rootView);
    }

    public void addView(View view){
        rootView.addView(view);
    }
}
