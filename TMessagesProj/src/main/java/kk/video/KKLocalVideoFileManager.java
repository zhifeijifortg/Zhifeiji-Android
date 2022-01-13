package kk.video;

import org.telegram.messenger.MessageObject;

/**
 * 本地视频文件下载状态管理
 */
public class KKLocalVideoFileManager extends BaseLocalFileManager<KKVideoMessage> {

    private static KKLocalVideoFileManager instance = null;

    private KKLocalVideoFileManager() {}

    public static KKLocalVideoFileManager getInstance() {
        if (instance == null) {
            synchronized (KKLocalVideoFileManager.class) {
                if (instance == null) {
                    instance = new KKLocalVideoFileManager();
                }
            }
        }
        return instance;
    }

    @Override
    protected KKVideoMessage createKKMessage(KKMessage message, MessageObject messageObject, KKFileDownloadStatus status) {
        KKVideoMessage kkVideoMessage = new KKVideoMessage(messageObject, status);
        kkVideoMessage.setDownloadTime(message.getDownloadTime());
        return kkVideoMessage;
    }
}
