package kk.video;

import androidx.annotation.Nullable;

import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.lang.ref.WeakReference;

import kk.KKLoger;

/**
 * 文件下载状态信息
 */
public class KKFileDownloadStatus {
    private WeakReference<KKVideoDownloadListener> downloadListenerWeakReference;
    private long totalSize;
    private long downloadedSize;
    private File downloadedFile;
    private Status status = Status.NOT_START;

    public KKFileDownloadStatus() {}

    /**
     * @return 已下载文件大小
     */
    public long getDownloadedSize() {
        return downloadedSize;
    }

    /**
     *
     * @param downloadedSize 已下载文件大小
     */
    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }

    /**
     * @return 文件总大小
     */
    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    /**
     * @return 文件下载状态
     */
    public Status getStatus() {
        return status;
    }

    /**
     *
     * @param status 新下载状态
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * @return 已下载的视频文件
     */
    @Nullable
    public File getVideoFile() {
        return this.downloadedFile;
    }

    public void setDownloadedFile(File downloadedFile) {
        this.downloadedFile = downloadedFile;
    }

    @Override
    public String toString() {
        return "KKFileDownloadStatus{" +
                "totalSize=" + totalSize +
                ", downloadedSize=" + downloadedSize +
                ", status=" + status +
                ", videoFile=" + getVideoFile() +
                '}';
    }

    /**
     * 下载状态
     */
    public enum Status {
        NOT_START,  //未开始下载
        DOWNLOADING,    //下载中
        PAUSE,          //暂停下载
        DOWNLOADED,     //下载完成
        FAILED          //下载失败
    }
}
