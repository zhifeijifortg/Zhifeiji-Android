package kk.video;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import kk.files.KKLocalFileManager;

public class KKVideoDataManager implements NotificationCenter.NotificationCenterDelegate {

    private static KKVideoDataManager instance;

    private final Map<Long, Integer> nextRateIdMap = new HashMap<>();
    private final Map<Long, MessageObject> lastMessageObjectMap = new HashMap<>();
    private final BlockingQueue<LoadRequest> tasks = new LinkedBlockingDeque<>();
    private final Object requestLock = new Object();
    private final Object taskLock = new Object();
    private int requestId = 1;
    private final int downloadControllerTag = DownloadController.getInstance(UserConfig.selectedAccount).generateObserverTag();
    private final Set<KKVideoLoadListener> listeners = new HashSet<>();
    private final KKFileMessageFilter<KKVideoMessage> messageHideFilter = new KKFileMessageFilter<>();
    private final Set<NewVideoListener> newVideoListeners = new HashSet<>();

    private BlockingQueue<Object> checkLastVideoQueue = new ArrayBlockingQueue<>(1);
    private final Object requestLastVideoLock = new Object();

    private KKVideoDataManager() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (taskLock) {
                    while (true) {
                        try {
                            LoadRequest loadRequest = tasks.take();
                            loadVideoMessages(loadRequest);
                            taskLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        checkLastVideoQueue.take();
                        checkLastVideo();
                        synchronized (requestLastVideoLock) {
                            requestLastVideoLock.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.didReceiveNewMessages);
    }

    private void checkLastVideo() {
        final TLRPC.TL_messages_searchGlobal req = new TLRPC.TL_messages_searchGlobal();
        req.limit = 1;
        req.q = "";
        req.filter = new TLRPC.TL_inputMessagesFilterVideo();
        req.offset_rate = 0;
        req.offset_id = 0;
        req.offset_peer = new TLRPC.TL_inputPeerEmpty();
        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                List<KKVideoMessage> messageObjects = new ArrayList<>();
                if (error == null) {
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    int n = res.messages.size();
                    if (n > 0) {
                        MessageObject messageObject = new MessageObject(UserConfig.selectedAccount, res.messages.get(0), false, true);
                        SharedPreferences sp = ApplicationLoader.applicationContext.getSharedPreferences("lastVideo", Context.MODE_PRIVATE);
                        if (sp.getInt("lastVideoId", 0) != messageObject.getId()) {
                            sp.edit().putInt("lastVideoId", messageObject.getId()).apply();
                            for (NewVideoListener newVideoListener : newVideoListeners) {
                                newVideoListener.onNewVideoMessage(messageObject.getId());
                            }
                        }
                    }
                    synchronized (requestLastVideoLock) {
                        requestLastVideoLock.notifyAll();
                    }
                }
            }
        });
    }

    private void requestCheckLastVideo() {
        try {
            checkLastVideoQueue.add(new Object());
        } catch (IllegalStateException e) {

        }
    }

    public static KKVideoDataManager getInstance() {
        if (instance == null) {
            synchronized (KKVideoDataManager.class) {
                if (instance == null) {
                    instance = new KKVideoDataManager();
                }
            }
        }
        return instance;
    }

    /**
     *
     * @param newVideoListener 增加新视频消息提醒监听
     */
    public void addNewVideoListener(NewVideoListener newVideoListener) {
        newVideoListeners.add(newVideoListener);
    }

    /**
     *
     * @param newVideoListener 移除新视频消息提醒监听
     */
    public void removeNewVideoListener(NewVideoListener newVideoListener) {
        newVideoListeners.remove(newVideoListener);
    }

    /**
     * 添加视频列表加载listener
     * @param listener
     */
    public void addListener(KKVideoLoadListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除视频列表加载listener
     * @param listener
     */
    public void removeListener(KKVideoLoadListener listener) {
        listeners.remove(listener);
    }

    /**
     * 刷新视频列表数据，从最新数据开始加载
     * @param dialogId 会话id，查询全部视频传0
     * @param downloadStatusListener 下载状态回调接口
     * @return  loadRequestId，用于标记数据加载回调结果
     */
    public int refreshVideoMessages(long dialogId, KKVideoDownloadListener downloadStatusListener) {
        synchronized (requestLock) {
            int requestId = this.requestId++;
            try {
                tasks.put(new LoadRequest(requestId, dialogId, true, downloadStatusListener));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return requestId;
        }
    }

    /**
     * 分页加载更多视频列表数据，会根据dialogId接上一页结果继续加载
     * @param dialogId  会话id，查询全部视频传0
     * @param downloadStatusListener 下载状态回调接口
     * @return  loadRequestId，用于标记数据加载回调结果
     */
    public int loadMoreVideoMessages(long dialogId, KKVideoDownloadListener downloadStatusListener) {
        synchronized (requestLock) {
            int requestId = this.requestId++;
            try {
                tasks.put(new LoadRequest(requestId, dialogId, false, downloadStatusListener));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return requestId;
        }
    }

    /**
     * 开始下载视频文件
     * @param videoMessage  KKVideoMessage实例
     */
    public void startDownloadVideo(KKVideoMessage videoMessage) {
        KKLocalFileManager.getInstance().watchMessageObject(videoMessage.getMessageObject());
        KKFileDownloadStatusManager.getInstance().startDownload(videoMessage.getMessageObject());
        FileLoader.getInstance(UserConfig.selectedAccount).loadFile(videoMessage.getDocument(), videoMessage.getMessageObject(), 0, 0);
    }

    /**
     * 暂停下载视频文件
     * @param videoMessage  KKVideoMessage实例
     */
    public void pauseDownloadVideo(KKVideoMessage videoMessage) {
        KKFileDownloadStatusManager.getInstance().pauseDownload(videoMessage.getMessageObject());
        FileLoader.getInstance(UserConfig.selectedAccount).cancelLoadFile(videoMessage.getDocument());
    }

    /**
     *
     * @param dialogId  dialogId，跟加载视频列表方法的参数一样
     * @return  Chat实例。群名称用chat.title获取；显示群头像参考ChatAvatarContainer第578行
     */
    public TLRPC.Chat getChat(long dialogId) {
        return MessagesController.getInstance(UserConfig.selectedAccount).getChat(parseChatId(dialogId));
    }

    /**
     * 隐藏在线视频
     * @param videoMessage  视频消息
     */
    public void hideOnlineVideo(KKVideoMessage videoMessage) {
        messageHideFilter.hideVideoMessage(videoMessage);
    }

    /**
     * 移除本地文件时调用，删除本地下载状态记录
     * @param localFileName 本地文件文件名，例如：5_6276088241942692236.mp4
     */
    public void removeLocalFile(String localFileName) {
        KKLocalVideoFileManager.getInstance().removeLocalRecord(localFileName);
    }

    /**
     *
     * @return 用于标记加载数据请求的id
     */
    private void loadVideoMessages(final LoadRequest loadRequest) {
        TLObject request;
        long dialogId = loadRequest.getDialogId();
        if (dialogId != 0) {
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
            req.peer = AccountInstance.getInstance(UserConfig.selectedAccount).getMessagesController().getInputPeer((int) loadRequest.getDialogId());
            if (req.peer == null) {
                return;
            }
            req.limit = 20;
            req.q = "";
            req.filter = new TLRPC.TL_inputMessagesFilterVideo();
            if (!loadRequest.isRefresh() && lastMessageObjectMap.containsKey(dialogId)) {
                req.offset_id = lastMessageObjectMap.get(dialogId).getId();
            } else {
                req.offset_id = 0;
            }
            request = req;
        } else {
            final TLRPC.TL_messages_searchGlobal req = new TLRPC.TL_messages_searchGlobal();
            req.limit = 20;
            req.q = "";
            req.filter = new TLRPC.TL_inputMessagesFilterVideo();
            if (loadRequest.isRefresh()) {
                req.offset_rate = 0;
                req.offset_id = 0;
                req.offset_peer = new TLRPC.TL_inputPeerEmpty();
            } else {
                MessageObject lastMessage = lastMessageObjectMap.get(dialogId);
                if (lastMessage == null) {
                    throw new IllegalStateException("Call refreshVideoMessages(" + loadRequest.dialogId + ") " +
                            "first before call loadVideoMessages(" + loadRequest.dialogId + ").");
                }
                req.offset_id = lastMessage.getId();
                req.offset_rate = nextRateIdMap.get(dialogId);
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
            request = req;
        }
        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(request, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error) {
                List<KKVideoMessage> messageObjects = new ArrayList<>();
                if (error == null) {
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    nextRateIdMap.put(loadRequest.dialogId, res.next_rate);
                    int n = res.messages.size();
                    for (int i = 0; i < n; i++) {
                        MessageObject messageObject = new MessageObject(UserConfig.selectedAccount, res.messages.get(i), false, true);
                        messageObject.setQuery("");
                        KKFileDownloadStatus status = KKFileDownloadStatusManager.getInstance().addWatch(messageObject);
                        KKFileDownloadStatusManager.getInstance().addFileDownloadListener(messageObject, loadRequest.downloadListener);
                        KKVideoMessage videoMessage = new KKVideoMessage(messageObject, status);
                        messageObjects.add(videoMessage);
                        if (i == n - 1) {
                            lastMessageObjectMap.put(loadRequest.getDialogId(), messageObject);
                        }
                    }
                    MessagesStorage.getInstance(UserConfig.selectedAccount).putUsersAndChats(res.users, res.chats, true, true);
                    MessagesController.getInstance(UserConfig.selectedAccount).putUsers(res.users, false);
                    MessagesController.getInstance(UserConfig.selectedAccount).putChats(res.chats, false);
                    messageObjects = messageHideFilter.filterMessages(messageObjects);
                    for (KKVideoLoadListener listener:listeners) {
                        listener.onMessagesLoad(loadRequest.getRequestId(), messageObjects);
                    }
                    synchronized (taskLock) {
                        taskLock.notifyAll();
                    }
                } else {
                    for (KKVideoLoadListener listener:listeners) {
                        listener.onError(loadRequest.getRequestId(), error.code, error.text);
                    }
                    synchronized (taskLock) {
                        taskLock.notifyAll();
                    }
                }

            }
        });
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

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            requestCheckLastVideo();
        }
    }

    private static class LoadRequest {
        private final int requestId;
        private final long dialogId;
        private final boolean refresh;
        private final KKVideoDownloadListener downloadListener;

        public LoadRequest(int requestId, long dialogId, boolean refresh, KKVideoDownloadListener downloadListener) {
            this.requestId = requestId;
            this.dialogId = dialogId;
            this.refresh = refresh;
            this.downloadListener = downloadListener;
        }

        public int getRequestId() {
            return requestId;
        }

        public long getDialogId() {
            return dialogId;
        }

        public boolean isRefresh() {
            return refresh;
        }

        public KKVideoDownloadListener getDownloadListener() {
            return downloadListener;
        }
    }
}
