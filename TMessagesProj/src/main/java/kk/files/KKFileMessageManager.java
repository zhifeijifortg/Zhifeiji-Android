package kk.files;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import kk.KKLoger;
import kk.video.KKFileDownloadStatus;
import kk.video.KKFileDownloadStatusManager;
import kk.video.KKFileMessageFilter;
import kk.video.KKVideoDownloadListener;
import kk.video.KKVideoMessage;

/**
 *  按照文件类型查询消息
 */
public class KKFileMessageManager {

    private static KKFileMessageManager instance;

    private final int PAGE_SIZE = 20;

    private final BlockingQueue<LoadRequest> tasks = new LinkedBlockingDeque<>();
    private final Set<KKFileMessageLoadListener> listeners = new HashSet<>();
    private final Object requestLock = new Object();
    private final Object peekObjectLock = new Object();
    private int requestId = 1;
    private Map<KKFileTypes, TagSearch> globalSearchMap = new HashMap<>();
    private Map<KKFileTypes, TagSearch> dialogSearchMap = new HashMap<>();
    private KKFileMessageFilter<KKFileMessage> fileMessageFilter = new KKFileMessageFilter<>();
    private int currentDialogId = -1;

    private KKFileMessageManager() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        LoadRequest loadRequest = tasks.take();
                        KKLoger.logFileData("take loadRequest.");
                        loadFileMessages(loadRequest);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static KKFileMessageManager getInstance() {
        if (instance == null) {
            synchronized (KKFileMessageManager.class) {
                if (instance == null) {
                    instance = new KKFileMessageManager();
                }
            }
        }
        return instance;
    }

    public void addListener(KKFileMessageLoadListener listener) {
        listeners.add(listener);
    }

    public void removeListener(KKFileMessageLoadListener listener) {
        listeners.remove(listener);
    }

    /**
     * 开始下载视频文件
     * @param fileMessage  KKVideoMessage实例
     */
    public void startDownloadVideo(KKFileMessage fileMessage) {
        KKLocalFileManager.getInstance().watchMessageObject(fileMessage.getMessageObject());
        KKFileDownloadStatusManager.getInstance().startDownload(fileMessage.getMessageObject());
        FileLoader.getInstance(UserConfig.selectedAccount).loadFile(fileMessage.getDocument(), fileMessage.getMessageObject(), 0, 0);
    }

    /**
     * 暂停下载视频文件
     * @param fileMessage  KKVideoMessage实例
     */
    public void pauseDownloadVideo(KKFileMessage fileMessage) {
        KKFileDownloadStatusManager.getInstance().pauseDownload(fileMessage.getMessageObject());
        FileLoader.getInstance(UserConfig.selectedAccount).cancelLoadFile(fileMessage.getDocument());
    }

    /**
     * 隐藏在线文件
     * @param fileMessage  文件消息
     */
    public void hideOnlineVideo(KKFileMessage fileMessage) {
        fileMessageFilter.hideVideoMessage(fileMessage);
    }

    /**
     * 获取文件消息列表
     * @param dialogId  会话id
     * @param type      文件类型
     * @param page      页数，0表示当天的，每页加1
     * @param downloadListener  文件下载状态回调
     * @return
     */
    public int loadFileMessages(long dialogId, KKFileTypes type, int page, KKVideoDownloadListener downloadListener) {
        synchronized (requestLock) {
            int requestId = this.requestId++;
            try {
                tasks.put(
                        new LoadRequest(
                                requestId,
                                dialogId,
                                page,
                                type,
                                downloadListener
                        )
                );
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return requestId;
        }
    }

    private int latestDate = 0;
    private KKFileMessage latestMessage = null;
    private TagSearch latestTagGlobalSearch = null;

    private Map<KKFileTypes, TagSearch> getTagSearchMap(int dialogId, KKFileTypes[] types) {
        Map<KKFileTypes, TagSearch> resultMap = null;
        if (dialogId == 0) {
            resultMap = globalSearchMap;
        } else {
            resultMap = dialogSearchMap;
            if (currentDialogId != dialogId) {
                currentDialogId = dialogId;
                dialogSearchMap.clear();
            }
        }
        for (KKFileTypes type : types) {
            if (!resultMap.containsKey(type)) {
                resultMap.put(type,
                        dialogId == 0 ?
                        TagGlobalSearchFactory.getTagGlobalSearch(type) :
                        TagDialogSearchFactory.getTagDialogSearch(type, dialogId));
            }
        }
        return resultMap;
    }

    private void loadFileMessages(final LoadRequest loadRequest) {
        KKLoger.logFileData("==========loadVideoMessages for page:" + loadRequest.getPage() + ", dialogId=" + loadRequest.getDialogId());
        KKFileTypes fileType = loadRequest.getFileType();
        KKFileTypes[] types = KKFileTypes.getQueries(fileType);
        Map<KKFileTypes, TagSearch> tagSearchMap = getTagSearchMap((int) loadRequest.getDialogId(), types);
        if (loadRequest.getPage() == 0) {
            for (KKFileTypes type : types) {
                tagSearchMap.get(type).resetData();
            }
        }

        List<KKFileMessage> result = new ArrayList<>();
        int queriesCount = types.length;
        while (result.size() < PAGE_SIZE) {
            latestMessage = null;
            latestDate = 0;
            latestTagGlobalSearch = null;
            final CountDownLatch countDownLatch = new CountDownLatch(queriesCount);
            for (KKFileTypes type : types) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TagSearch tagSearch = null;
                        synchronized (peekObjectLock) {
                            tagSearch = tagSearchMap.get(type);
                            if (tagSearch == null) {
                                tagSearch = TagGlobalSearchFactory.getTagGlobalSearch(type);
                                tagSearchMap.put(type, tagSearch);
                            }
                        }
                        KKFileMessage fileMessage = tagSearch.getNextMessage(loadRequest.downloadListener);
                        synchronized (peekObjectLock) {
                            if (fileMessage != null && fileMessage.getMessageObject().messageOwner.date > latestDate) {
                                latestMessage = fileMessage;
                                latestDate = fileMessage.getMessageObject().messageOwner.date;
                                latestTagGlobalSearch = tagSearch;
                            }
                            countDownLatch.countDown();
                        }
                    }
                }).start();
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (latestMessage == null) {
                break;
            } else {
                result.add(latestMessage);
                latestTagGlobalSearch.increaseIndex();
                if (result.size() >= PAGE_SIZE) {
                    result = fileMessageFilter.filterMessages(result);
                }
            }
        }
        for (KKFileMessageLoadListener listener : listeners) {
            listener.onMessagesLoad(requestId, result);
        }
    }

    private static class LoadRequest {
        private final int requestId;
        private final long dialogId;
        private final int page;
        private final KKFileTypes fileType;
        private final KKVideoDownloadListener downloadListener;

        public LoadRequest(int requestId, long dialogId, int page, KKFileTypes fileType, KKVideoDownloadListener downloadListener) {
            this.requestId = requestId;
            this.dialogId = dialogId;
            this.page = page;
            this.fileType = fileType;
            this.downloadListener = downloadListener;
        }

        public int getRequestId() {
            return requestId;
        }

        public long getDialogId() {
            return dialogId;
        }

        public int getPage() {
            return page;
        }

        public KKFileTypes getFileType() {
            return fileType;
        }

        public KKVideoDownloadListener getDownloadListener() {
            return downloadListener;
        }
    }
}
