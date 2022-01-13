package kk.video;

import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import kk.KKLoger;

/**
 * 统一管理下载状态、进度
 */
public class KKFileDownloadStatusManager implements NotificationCenter.NotificationCenterDelegate {

    private final Map<String, KKFileDownloadStatus> fileDownloadStatus = new ConcurrentHashMap<>();
    private final Map<String, Set<KKVideoDownloadListener>> listeners = new ConcurrentHashMap<>();

    private KKFileDownloadStatusManager() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileDidFailToLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileDidLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.FileLoadProgressChanged);
    }

    private static volatile KKFileDownloadStatusManager instance;

    public static KKFileDownloadStatusManager getInstance() {
        if (instance == null) {
            synchronized (KKFileDownloadStatusManager.class) {
                if (instance == null) {
                    instance = new KKFileDownloadStatusManager();
                }
            }
        }
        return instance;
    }

    public KKFileDownloadStatus addWatch(MessageObject messageObject) {
        TLRPC.Document document = messageObject.getDocument();
        if (document != null) {
            String fileName = FileLoader.getAttachFileName(document);
            KKLoger.logVideoDataManager("addWatch for " + fileName);
            if (!fileDownloadStatus.containsKey(fileName)) {
                KKFileDownloadStatus status = new KKFileDownloadStatus();
                status.setTotalSize(document.size);

                File f = null;
                if (messageObject.messageOwner.attachPath != null && messageObject.messageOwner.attachPath.length() != 0) {
                    f = new File(messageObject.messageOwner.attachPath);
                }
                if (f == null || f != null && !f.exists()) {
                    f = FileLoader.getPathToMessage(messageObject.messageOwner);
                }
                status.setDownloadedFile(f);

                if (messageObject.attachPathExists || messageObject.mediaExists) {
                    status.setStatus(KKFileDownloadStatus.Status.DOWNLOADED);
                    status.setDownloadedSize(document.size);
                } else {
                    fileName = FileLoader.getAttachFileName(messageObject.getDocument());
                    String[] nameSplit = fileName.split("\\.");
                    if (nameSplit.length > 0) {
                        File tmpFilesDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE);
                        String tmpFileName = nameSplit[0] + ".temp";
                        File tmpFile = new File(tmpFilesDir, tmpFileName);
                        if (tmpFile.exists()) {
                            status.setStatus(KKFileDownloadStatus.Status.PAUSE);
                            status.setDownloadedSize(tmpFile.length());
                        }
                    }
                }
                fileDownloadStatus.put(fileName, status);
                return status;
            } else {
                return fileDownloadStatus.get(fileName);
            }
        } else {
            return null;
        }
    }

    public void startDownload(MessageObject messageObject) {
        addWatch(messageObject);
        TLRPC.Document document = messageObject.getDocument();
        if (document != null) {
            String fileName = FileLoader.getAttachFileName(document);
            if (fileDownloadStatus.containsKey(fileName)) {
                KKFileDownloadStatus status = fileDownloadStatus.get(fileName);
                synchronized (status) {
                    if (status.getStatus() != KKFileDownloadStatus.Status.DOWNLOADING) {
                        status.setStatus(KKFileDownloadStatus.Status.DOWNLOADING);
                        notify(fileName, status);
                    }
                }
            } else {
                throw new IllegalStateException("Call addWatch() method first.");
            }
        }
    }

    public void pauseDownload(MessageObject messageObject) {
        addWatch(messageObject);
        TLRPC.Document document = messageObject.getDocument();
        if (document != null) {
            String fileName = FileLoader.getAttachFileName(document);
            if (fileDownloadStatus.containsKey(fileName)) {
                KKFileDownloadStatus status = fileDownloadStatus.get(fileName);
                synchronized (status) {
                    if (status.getStatus() != KKFileDownloadStatus.Status.PAUSE) {
                        status.setStatus(KKFileDownloadStatus.Status.PAUSE);
                        notify(fileName, status);
                    }
                }
            } else {
                throw new IllegalStateException("Call addWatch() method first.");
            }
        }
    }

    public void addFileDownloadListener(MessageObject messageObject, KKVideoDownloadListener listener) {
        addWatch(messageObject);
        Set<KKVideoDownloadListener> downloadListeners = null;
        TLRPC.Document document = messageObject.getDocument();
        if (document != null) {
            String fileName = FileLoader.getAttachFileName(document);
            if (listeners.containsKey(fileName)) {
                downloadListeners = listeners.get(fileName);
            } else {
                downloadListeners = new HashSet<>();
                listeners.put(fileName, downloadListeners);
            }
            synchronized (downloadListeners) {
                if (!downloadListeners.contains(listener)) {
                    downloadListeners.add(listener);
                    if (fileDownloadStatus.containsKey(fileName)) {
                        KKFileDownloadStatus status = fileDownloadStatus.get(fileName);
                        listener.updateVideoDownloadStatus(fileName, status);
                    } else {
                        throw new IllegalStateException("Call addWatch() method first.");
                    }
                }
            }
        }
    }

    private void notify(String fileName, KKFileDownloadStatus status) {
        if (listeners.containsKey(fileName)) {
            Set<KKVideoDownloadListener> set = listeners.get(fileName);
            synchronized (set) {
                for (KKVideoDownloadListener downloadListener : set) {
                    downloadListener.updateVideoDownloadStatus(fileName, status);
                }
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileDidFailToLoad || id == NotificationCenter.httpFileDidFailedLoad) {
            String fileName = (String) args[0];
            Integer canceled = (Integer) args[1];
            onFailedDownload(fileName, (canceled == 1));
        } else if (id == NotificationCenter.fileDidLoad || id == NotificationCenter.httpFileDidLoad) {
            String fileName = (String) args[0];
            onSuccessDownload(fileName);
        } else if (id == NotificationCenter.FileLoadProgressChanged) {
            String fileName = (String) args[0];
            Long loadedSize = (Long) args[1];
            Long totalSize = (Long) args[2];
            onProgressDownload(fileName, loadedSize, totalSize);
        }
    }

    public void onFailedDownload(String fileName, boolean isCanceled) {
        KKLoger.logVideoDataManager("onFailedDownload, fileName:" + fileName);
        if (fileDownloadStatus.containsKey(fileName)) {
            KKFileDownloadStatus status = fileDownloadStatus.get(fileName);
            synchronized (status) {
                if (status != null
                        && status.getStatus() != KKFileDownloadStatus.Status.FAILED
                        && status.getStatus() != KKFileDownloadStatus.Status.DOWNLOADED) {
                    if (isCanceled) {
                        status.setStatus(KKFileDownloadStatus.Status.PAUSE);
                    } else {
                        status.setStatus(KKFileDownloadStatus.Status.FAILED);
                    }
                    notify(fileName, status);
                }
            }
        }
    }

    public void onSuccessDownload(String fileName) {
        KKLoger.logVideoDataManager("onSuccessDownload, fileName:" + fileName);
        if (fileDownloadStatus.containsKey(fileName)) {
            KKFileDownloadStatus status = fileDownloadStatus.get(fileName);
            synchronized (status) {
                if (status != null
                        && status.getStatus() != KKFileDownloadStatus.Status.DOWNLOADED
                        && status.getStatus() != KKFileDownloadStatus.Status.FAILED) {
                    status.setStatus(KKFileDownloadStatus.Status.DOWNLOADED);
                    notify(fileName, status);
                }
            }
        }
    }

    public void onProgressDownload(String fileName, long downloadSize, long totalSize) {
        KKLoger.logVideoDataManager("onProgressDownload:" + fileName + ", downloadSize=" + downloadSize + ", totalSize=" + totalSize);
        synchronized (fileDownloadStatus) {
            if (fileDownloadStatus.containsKey(fileName)) {
                KKFileDownloadStatus status = fileDownloadStatus.get(fileName);
                if (status != null
                        && status.getStatus() == KKFileDownloadStatus.Status.DOWNLOADING) {
                    status.setDownloadedSize(downloadSize);
                    notify(fileName, status);
                }
            }
        }
    }
}
