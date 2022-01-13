package kk.files;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.concurrent.CountDownLatch;

public abstract class TagDialogSearch extends TagSearch {

    protected final int dialogId;

    public TagDialogSearch(KKFileTypes fileType, int dialogId) {
        super(fileType);
        this.dialogId = dialogId;
    }

    @Override
    protected TLObject getSearchRequest() {
        TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(parseChatId(dialogId));
        if (chat == null) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            final MessagesStorage messagesStorage = MessagesStorage.getInstance(UserConfig.selectedAccount);
            messagesStorage.getStorageQueue().postRunnable(() -> {
                TLRPC.Chat c = messagesStorage.getChat(parseChatId(dialogId));
                MessagesController.getInstance(UserConfig.selectedAccount).putChat(c, true);
                countDownLatch.countDown();
            });
            try {
                countDownLatch.await();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        final TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = AccountInstance.getInstance(UserConfig.selectedAccount).getMessagesController().getInputPeer(dialogId);
        if (req.peer == null) {
            return null;
        }
        req.limit = 20;
        req.q = fileType.getQueryString();
        req.filter = getMessagesFilter();
        if (lastMessage == null) {
            req.offset_id = 0;
        } else {
            req.offset_id = lastMessage.getId();
        }
        return req;
    }



    private int parseChatId(long dialogId) {
        int lower_part = (int) dialogId;
        if (lower_part != 0) {
            if (lower_part > 0) {
                return lower_part;
            } else if (lower_part < 0) {
                return -lower_part;
            }
        }
        throw new IllegalArgumentException("Can't parse chatId, dialogId=" + dialogId);
    }
}
