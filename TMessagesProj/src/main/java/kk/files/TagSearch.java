package kk.files;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import kk.KKLoger;
import kk.video.KKFileDownloadStatus;
import kk.video.KKFileDownloadStatusManager;
import kk.video.KKVideoDownloadListener;

public abstract class TagSearch extends BaseTagSearch {

    protected final KKFileTypes fileType;
    protected MessageObject lastMessage = null;
    private List<KKFileMessage> messages = new ArrayList<>();
    private int index = 0;
    private boolean needLoadMore = true;

    public TagSearch(KKFileTypes fileType) {
        this.fileType = fileType;
    }

    public synchronized KKFileMessage getNextMessage(KKVideoDownloadListener downloadListener) {
        int size = messages.size();
        if (index < size) {
            return messages.get(index);
        } else {
            if (!needLoadMore) {
                return null;
            }
            CountDownLatch countDownLatch = new CountDownLatch(1);
            load(downloadListener, countDownLatch);
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (index < messages.size()) {
                return messages.get(index);
            } else {
                return null;
            }
        }
    }

    public synchronized void resetData() {
        messages.clear();
        index = 0;
        needLoadMore = true;
        lastMessage = null;
    }

    public synchronized void increaseIndex() {
        index++;
    }

    private void load(KKVideoDownloadListener downloadListener, CountDownLatch countDownLatch) {
        TLObject searchRequest = getSearchRequest();
        if (searchRequest == null) {
            return;
        }
        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(searchRequest, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                if (error == null) {
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    int size = res.messages.size();
                    if (size == 0) {
                        needLoadMore = false;
                    } else {
                        needLoadMore = true;
                    }
                    int dataCount = 0;
                    for (int i = 0; i < size; i++) {
                        lastMessage = new MessageObject(UserConfig.selectedAccount, res.messages.get(i), false, true);
                        if (lastMessage.getDocument() == null) {
                            continue;
                        }
                        dataCount++;
                        lastMessage.setQuery(fileType.getQueryString());
                        KKFileDownloadStatus status = KKFileDownloadStatusManager.getInstance().addWatch(lastMessage);
                        KKFileDownloadStatusManager.getInstance().addFileDownloadListener(lastMessage, downloadListener);
                        KKFileMessage fileMessage = new KKFileMessage(lastMessage, status, fileType);
                        messages.add(fileMessage);
                    }
                    KKLoger.logFileData("------load messages for " + fileType + ", size = " + size + ", dataCount = " + dataCount);
                    MessagesStorage.getInstance(UserConfig.selectedAccount).putUsersAndChats(res.users, res.chats, true, true);
                    MessagesController.getInstance(UserConfig.selectedAccount).putUsers(res.users, false);
                    MessagesController.getInstance(UserConfig.selectedAccount).putChats(res.chats, false);
                    countDownLatch.countDown();
                } else {
                    countDownLatch.countDown();
                    needLoadMore = false;
                }

            }
        });
    }

    protected abstract TLObject getSearchRequest();

    protected abstract TLRPC.MessagesFilter getMessagesFilter();

    public void loadFileMessages(
            int page,
            KKVideoDownloadListener downloadListener,
            final List<KKFileMessage> result,
            CountDownLatch totalLatch
    ) {
        final TLRPC.TL_messages_searchGlobal req = new TLRPC.TL_messages_searchGlobal();
        req.limit = 100;
        req.q = fileType.getQueryString();
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        if (lastMessage == null) {
            req.offset_rate = 0;
            req.offset_id = 0;
            req.offset_peer = new TLRPC.TL_inputPeerEmpty();
        } else {
            req.offset_id = lastMessage.getId();
            req.offset_rate = lastMessage.messageOwner.date;
            int id;
            if (lastMessage.messageOwner.peer_id.channel_id != 0) {
                id = -lastMessage.messageOwner.peer_id.channel_id;
            } else if (lastMessage.messageOwner.peer_id.chat_id != 0) {
                id = -lastMessage.messageOwner.peer_id.chat_id;
            } else {
                id = lastMessage.messageOwner.peer_id.user_id;
            }
            req.offset_peer = MessagesController.getInstance(UserConfig.selectedAccount).getInputPeer(id);
        }
        req.flags |= 1;

        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                if (error == null) {
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    KKLoger.logFileData("message count:" + res.messages.size() + ", tag=" + fileType.getQueryString() + ", page=" + page);
                    if (res.messages.size() > 0) {
                        KKLoger.logFileData("first message date:" + res.messages.get(0).date);
                    }
                    int lastValidIndex = getLastInScopeMessageIndex(page, res.messages);
                    for (int i = 0; i <= lastValidIndex; i++) {
                        MessageObject messageObject = new MessageObject(UserConfig.selectedAccount, res.messages.get(i), false, true);
                        if (messageObject.getDocument() == null) {
                            continue;
                        }
                        messageObject.setQuery(fileType.getQueryString());
                        KKFileDownloadStatus status = KKFileDownloadStatusManager.getInstance().addWatch(messageObject);
                        KKFileDownloadStatusManager.getInstance().addFileDownloadListener(messageObject, downloadListener);
                        KKFileMessage fileMessage = new KKFileMessage(messageObject, status, KKFileTypes.parseFileType(fileType.getQueryString()));
                        result.add(fileMessage);
                    }
                    if (lastValidIndex >= 0) {
                        lastMessage = new MessageObject(UserConfig.selectedAccount, res.messages.get(lastValidIndex), false, false);
                    }
                    MessagesStorage.getInstance(UserConfig.selectedAccount).putUsersAndChats(res.users, res.chats, true, true);
                    MessagesController.getInstance(UserConfig.selectedAccount).putUsers(res.users, false);
                    MessagesController.getInstance(UserConfig.selectedAccount).putChats(res.chats, false);
                    if (lastValidIndex > 0 && lastValidIndex == res.messages.size() - 1) {
                        loadFileMessages(page, downloadListener, result, totalLatch);
                    } else {
                        totalLatch.countDown();
                    }
                } else {
                    totalLatch.countDown();
                }

            }
        });
    }
}
