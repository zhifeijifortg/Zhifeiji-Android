package kk.files;

import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import kk.KKLoger;
import kk.video.BaseLocalFileManager;
import kk.video.KKFileDownloadStatus;
import kk.video.KKMessage;

public class KKLocalFileManager extends BaseLocalFileManager<KKFileMessage> {

    private static KKLocalFileManager instance = null;

    private KKLocalFileManager() {
    }

    public static KKLocalFileManager getInstance() {
        if (instance == null) {
            synchronized (KKLocalFileManager.class) {
                if (instance == null) {
                    instance = new KKLocalFileManager();
                }
            }
        }
        return instance;
    }

    @Override
    protected KKFileMessage createKKMessage(KKMessage message, MessageObject messageObject, KKFileDownloadStatus status) {
        KKFileTypes type = KKFileTypes.UNKNOWN;
        TLRPC.Document document = messageObject.getDocument();
        if (document != null) {
            String suffix = FileLoader.getDocumentSuffix(document);
            type = KKFileTypes.parseFileType(suffix);
        }
        KKFileMessage kkFileMessage = new KKFileMessage(messageObject, status, type);
        kkFileMessage.setDownloadTime(message.getDownloadTime());
        return kkFileMessage;
    }
}
