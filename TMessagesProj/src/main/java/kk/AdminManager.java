package kk;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

public class AdminManager {

    private static final String TAG = "AdminManager";

    private static volatile AdminManager instance;

    private DialogsFilter dialogsFilter = DialogsFilter.FILTER_NO;
    private List<NotificationCenter.NotificationCenterDelegate> observers = new ArrayList<>();

    public enum DialogsFilter{
        FILTER_NO,
        FILTER_CREATOR;
    }

    public String getMenuName() {
        switch (dialogsFilter) {
            case FILTER_CREATOR:
                return "Switch To Normal Mode";
            case FILTER_NO:
                return "Switch To Admin Mode";
        }
        return "Error";
    }

    public DialogsFilter getDialogsFilter() {
        return dialogsFilter;
    }

    public void setDialogsFilter(DialogsFilter dialogsFilter) {
        this.dialogsFilter = dialogsFilter;
        for (NotificationCenter.NotificationCenterDelegate observer : observers) {
            observer.didReceivedNotification(NotificationCenter.dialogsNeedReload, UserConfig.selectedAccount);
        }
    }

    public void addObserver(NotificationCenter.NotificationCenterDelegate observer) {
        observers.add(observer);
    }

    private AdminManager() {
    }

    public static AdminManager getInstance() {
        if (instance == null) {
            synchronized (AdminManager.class) {
                if (instance == null) {
                    instance = new AdminManager();
                }
            }
        }
        return instance;
    }

    public List<TLRPC.Dialog> filterDialogs(List<TLRPC.Dialog> dialogs, int account) {
        switch (this.dialogsFilter) {
            case FILTER_NO:
                return dialogs;
            case FILTER_CREATOR:
                return filterCreatorDialogs(dialogs, account);
            default:
                return null;
        }
    }

    private List<TLRPC.Dialog> filterCreatorDialogs(List<TLRPC.Dialog> dialogs, int account) {
        List<TLRPC.Dialog> result = new ArrayList<>();

        for (TLRPC.Dialog dialog : dialogs) {
            long dialogID = dialog.id;
            int lower_id = (int) dialogID;
            int high_id = (int) (dialogID >> 32);
            if (lower_id != 0) {
                if (lower_id < 0) {
                    TLRPC.Chat chat = MessagesController.getInstance(account).getChat(-lower_id);
                    if (chat.creator) {
                        result.add(dialog);
                    }
//                    Log.d(TAG, "chat:" + chat.title + " creator:" + chat.creator);
                } else {
                    TLRPC.User user = MessagesController.getInstance(account).getUser(lower_id);
//                    Log.d(TAG, "user:" + user.username);
                }
            } else {
//                Log.d(TAG, "lower_id == 0");
            }
        }
//        Log.d(TAG, "====filterCreatorDialogs count:" + result.size());
        return result;
    }
}
