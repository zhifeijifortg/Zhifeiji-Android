package kk.video;

import org.telegram.messenger.MessageObject;

/**
 * 下载状态监听接口
 */
public interface KKVideoDownloadListener {

    /**
     *
     * @param fileName        下载文件文件名
     * @param fileDownloadStatus    文件下载状态
     */
    void updateVideoDownloadStatus(String fileName, KKFileDownloadStatus fileDownloadStatus);
}
