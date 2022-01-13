package kk.files;

import android.text.TextUtils;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import kk.video.KKFileDownloadStatus;

/**
 * 文件消息封装类
 */
public class KKFileMessage {

    private String dateObject;
    protected MessageObject messageObject;
    protected KKFileDownloadStatus downloadStatus;
    private long downloadTime;
    private KKFileTypes fileType;

    public KKFileMessage(String dateObject){
        this.dateObject = dateObject;
    }

    public KKFileMessage(MessageObject messageObject, KKFileDownloadStatus downloadStatus, KKFileTypes fileType) {
        this.messageObject = messageObject;
        this.downloadStatus = downloadStatus;
        this.fileType = fileType;
    }

    /***
     *
     * @return 是否是日期分组消息
     */
    public boolean isDateMessage() {
        return !TextUtils.isEmpty(this.dateObject);
    }

    /***
     *
     * @return 分组日期
     */
    public String getDateObject(){
        return this.dateObject;
    }

    /**
     *
     * @return 文件类型
     */
    public KKFileTypes getFileType() {
        return fileType;
    }

    /**
     *
     * @return 消息id
     */
    public int getId() {
        return this.messageObject.getId();
    }

    /**
     *
     * @return Document
     */
    public TLRPC.Document getDocument() {
        return this.messageObject.getDocument();
    }

    /**
     *
     * @return 文件大小
     */
    public int getSize() {
        if (this.messageObject.getDocument() == null) return 0;
        return this.messageObject.getDocument().size;
    }

    /**
     *
     * @return MessageObject
     */
    public MessageObject getMessageObject() {
        return messageObject;
    }

    /**
     *
     * @return 文件名称
     */
    public String getFileName() {
        return FileLoader.getDocumentFileName(messageObject.getDocument());
    }

    /**
     *
     * @return 文件下载本地文件名
     */
    public String getDownloadFileName() {
        return FileLoader.getAttachFileName(messageObject.getDocument());
    }

    /**
     *
     * @return 群名称。也可能是单聊的用户名等名称
     */
    public String getFromName() {
        String fromName = null;
        TLRPC.User user = messageObject.messageOwner.from_id.user_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getUser(messageObject.messageOwner.from_id.user_id) : null;
        TLRPC.Chat chatFrom = messageObject.messageOwner.from_id.chat_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getChat(messageObject.messageOwner.peer_id.chat_id) : null;
        if (chatFrom == null) {
            chatFrom = messageObject.messageOwner.from_id.channel_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getChat(messageObject.messageOwner.peer_id.channel_id) : null;
        }
        TLRPC.Chat chatTo = messageObject.messageOwner.peer_id.channel_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getChat(messageObject.messageOwner.peer_id.channel_id) : null;
        if (chatTo == null) {
            chatTo = messageObject.messageOwner.peer_id.chat_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getChat(messageObject.messageOwner.peer_id.chat_id) : null;
        }
        if (user != null && chatTo != null) {
            fromName = chatTo.title;
        } else if (user != null) {
            fromName = ContactsController.formatName(user.first_name, user.last_name);
        } else if (chatFrom != null) {
            fromName = chatFrom.title;
        }
        return fromName == null ? "" : fromName;
    }

    /**
     *
     * @return 发消息用户名称
     */
    public String getFromUserName() {
        TLRPC.User user = messageObject.messageOwner.from_id.user_id != 0 ? MessagesController.getInstance(UserConfig.selectedAccount).getUser(messageObject.messageOwner.from_id.user_id) : null;
        if (user != null) {
            return ContactsController.formatName(user.first_name, user.last_name);
        } else {
            return "";
        }
    }

    /**
     *
     * @return 文件下载状态
     */
    public KKFileDownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    /**
     *
     * @return DialogId
     */
    public long getDialogId() {
        return messageObject.getDialogId();
    }

    /**
     *
     * @return 用于区分列表显示样式
     */
    public boolean hasUserName() {
        return messageObject.isFromUser();
    }

    /**
     *
     * @return 获取文件描述
     */
    public String getMessage() {
        if(messageObject != null && messageObject.caption != null){
            return messageObject.caption.toString();
        }
        return "";
    }

    /**
     * 设置下载开始时间
     * @param downloadTime
     */
    public void setDownloadTime(long downloadTime) {
        this.downloadTime = downloadTime;
    }

    /**
     *
     * @return 下载开始时间
     */
    public long getDownloadTime() {
        return downloadTime;
    }

    /**
     *
     * @return 媒体文件长度，单位为秒
     */
    public int getMediaDuration() {
        return messageObject.getDuration();
    }

    @Override
    public String toString() {
        return "KKFileMessage{" +
                "message=" + getMessage() +
                ", fromUserName=" + getFromUserName() +
                ", getFileName=" + getFileName() +
                ", getFromName=" + getFromName() +
                ", getDocument=" + getDocument() +
                ", fileType=" + getFileType() +
                '}';
    }
}
