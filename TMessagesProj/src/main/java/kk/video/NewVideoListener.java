package kk.video;

/**
 * 有新的视频消息时回调
 */
public interface NewVideoListener {
    /**
     * 有新的视频消息
     * @param messageId 新视频消息id
     */
    public void onNewVideoMessage(int messageId);
}
