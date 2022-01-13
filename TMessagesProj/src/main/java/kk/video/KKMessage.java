package kk.video;

import org.telegram.tgnet.TLRPC;

/**
 * 封装TLRPC.Message
 */
public class KKMessage {
    private final TLRPC.Message message;
    private final long downloadTime;

    public KKMessage(TLRPC.Message message, long downloadTime) {
        this.message = message;
        this.downloadTime = downloadTime;
    }

    public TLRPC.Message getMessage() {
        return message;
    }

    public long getDownloadTime() {
        return downloadTime;
    }
}
