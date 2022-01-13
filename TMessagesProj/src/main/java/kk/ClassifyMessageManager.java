package kk;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static org.telegram.ui.ChatActivity.MODE_PINNED;
import static org.telegram.ui.ChatActivity.MODE_SCHEDULED;

public class ClassifyMessageManager implements NotificationCenter.NotificationCenterDelegate {

    private static final ClassifyMessageManager[] classifyMessageManagers = new ClassifyMessageManager[UserConfig.MAX_ACCOUNT_COUNT];

    static {
        Arrays.fill(classifyMessageManagers, null);
    }

    public static ClassifyMessageManager getInstance(int account) {
        if (classifyMessageManagers[account] == null) {
            synchronized (ClassifyMessageManager.class) {
                if (classifyMessageManagers[account] == null) {
                    classifyMessageManagers[account] = new ClassifyMessageManager(account);
                }
            }
        }
        return classifyMessageManagers[account];
    }

    private final int account;

    private ClassifyMessageManager(int account) {
        this.account = account;
        NotificationCenter notificationCenter = AccountInstance.getInstance(account).getNotificationCenter();
        notificationCenter.addObserver(this, NotificationCenter.dialogsNeedReload);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... objects) {
        if (id == NotificationCenter.dialogsNeedReload) {
            MessagesController messagesController = AccountInstance.getInstance(account).getMessagesController();
            ArrayList<TLRPC.Dialog> dialogs = messagesController.getDialogs(0);
            int totalUnreadCount = 0;
            for (TLRPC.Dialog dialog : dialogs) {
                long dialogId = dialog.id;
                int lower_part = (int) dialogId;
                int high_id = (int) (dialogId >> 32);
                int message_id = 0; //应该只有搜索的时候会不为0
                Bundle args = new Bundle();
                if (lower_part != 0) {
                    if (lower_part > 0) {
                        args.putInt("user_id", lower_part);
                    } else if (lower_part < 0) {
                        args.putInt("chat_id", -lower_part);
                    }
                } else {
                    args.putInt("enc_id", high_id);
                }


                int unreadMentionsCount = dialog.unread_count;
                totalUnreadCount += unreadMentionsCount;
                KKLoger.logClassifyMessageManager("dialog id:" + dialog.id + ", dialog unreadMentionsCount:" + dialog.unread_count);
            }
            KKLoger.logClassifyMessageManager("totalMentionsCount:" + totalUnreadCount);
        }
    }

    private MessagesController getMessagesController() {
        return AccountInstance.getInstance(account).getMessagesController();
    }

    private MessagesStorage getMessagesStorage() {
        return AccountInstance.getInstance(account).getMessagesStorage();
    }

    private class MessagesLoader {

        private final Bundle arguments;
        private TLRPC.Chat currentChat;
        private long dialog_id;
        private final int classGuid;
        private boolean showScrollToMessageError;
        private int lastLoadIndex = 1;
        private boolean wasManualScroll;
        private boolean needSelectFromMessageId;
        private boolean loadingFromOldPosition;
        private int startLoadFromMessageOffset = Integer.MAX_VALUE;
        private ArrayList<Integer> pinnedMessageIds = new ArrayList<>();
        private HashMap<Integer, MessageObject> pinnedMessageObjects = new HashMap<>();

        private MessagesLoader(Bundle arguments) {
            this.arguments = arguments;
            synchronized (MessagesLoader.class) {
                this.classGuid = ConnectionsManager.generateClassGuid();
            }
        }

        public void loadMessages(Bundle arguments) {
            final int chatId = arguments.getInt("chat_id", 0);
            final int userId = arguments.getInt("user_id", 0);
            final int encId = arguments.getInt("enc_id", 0);
            int chatMode = arguments.getInt("chatMode", 0);
            long inlineReturn = arguments.getLong("inline_return", 0);
            String inlineQuery = arguments.getString("inline_query");
            int startLoadFromMessageId = arguments.getInt("message_id", 0);
            boolean historyPreloaded = arguments.getBoolean("historyPreloaded", false);
            int migrated_to = arguments.getInt("migrated_to", 0);
            boolean scrollToTopOnResume = arguments.getBoolean("scrollToTopOnResume", false);
            boolean needRemovePreviousSameChatActivity = arguments.getBoolean("need_remove_previous_same_chat_activity", true);
            if (chatId != 0) {
                currentChat = getMessagesController().getChat(chatId);
                if (currentChat == null) {
                    final CountDownLatch countDownLatch = new CountDownLatch(1);
                    final MessagesStorage messagesStorage = getMessagesStorage();
                    messagesStorage.getStorageQueue().postRunnable(() -> {
                        currentChat = messagesStorage.getChat(chatId);
                        countDownLatch.countDown();
                    });
                    try {
                        countDownLatch.await();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    if (currentChat != null) {
                        getMessagesController().putChat(currentChat, true);
                    } else {
                        return;
                    }
                }
                dialog_id = -chatId;
                if (ChatObject.isChannel(currentChat)) {
                    getMessagesController().startShortPoll(currentChat, classGuid, false);
                }
            } else if (userId != 0) {
                //忽略
            } else if (encId != 0) {
                //忽略
            } else {
                return;
            }
            if (chatMode == MODE_PINNED) {
//                ArrayList<MessageObject> messageObjects = new ArrayList<>();
//                for (int a = 0, N = pinnedMessageIds.size(); a < N; a++) {
//                    Integer id = pinnedMessageIds.get(a);
//                    MessageObject object = pinnedMessageObjects.get(id);
//                    if (object != null) {
//                        MessageObject o = new MessageObject(object.currentAccount, object.messageOwner, true, false);
//                        o.replyMessageObject = object.replyMessageObject;
//                        o.mediaExists = object.mediaExists;
//                        o.attachPathExists = object.attachPathExists;
//                        messageObjects.add(o);
//                    }
//                }
//                int loadIndex = lastLoadIndex++;
//                waitingForLoad.add(loadIndex);
//                getNotificationCenter().postNotificationName(NotificationCenter.messagesDidLoad, dialog_id, messageObjects.size(), messageObjects, false, 0, last_message_id, 0, 0, 2, true, classGuid, loadIndex, pinnedMessageIds.get(0), 0, MODE_PINNED);
            } else {
//                loading = true;   忽略
            }

            if (chatMode == 0) {
                getMessagesController().loadPeerSettings(null, currentChat);

                if (startLoadFromMessageId == 0) {
                    SharedPreferences sharedPreferences = MessagesController.getNotificationsSettings(account);
                    int messageId = sharedPreferences.getInt("diditem" + dialog_id, 0);
                    if (messageId != 0) {
                        wasManualScroll = true;
                        loadingFromOldPosition = true;
                        startLoadFromMessageOffset = sharedPreferences.getInt("diditemo" + dialog_id, 0);
                        startLoadFromMessageId = messageId;
                    }
                } else {
                    showScrollToMessageError = true;
                    needSelectFromMessageId = true;
                }
            }

            boolean loadInfo = false;
//            if (currentChat != null) {
//                chatInfo = getMessagesController().getChatFull(currentChat.id);
//                groupCall = getMessagesController().getGroupCall(currentChat.id, true);
//                if (currentChat.megagroup && !getMessagesController().isChannelAdminsLoaded(currentChat.id)) {
//                    getMessagesController().loadChannelAdmins(currentChat.id, true);
//                }
//                fillInviterId(false);
//                if (chatMode != MODE_PINNED) {
//                    getMessagesStorage().loadChatInfo(currentChat.id, ChatObject.isChannel(currentChat), null, true, false, startLoadFromMessageId);
//                }
//                if (chatMode == 0 && chatInfo != null && ChatObject.isChannel(currentChat) && chatInfo.migrated_from_chat_id != 0 && !isThreadChat()) {
//                    mergeDialogId = -chatInfo.migrated_from_chat_id;
//                    maxMessageId[1] = chatInfo.migrated_from_max_id;
//                }
//                loadInfo = chatInfo == null;
//            } else if (currentUser != null) {
//                if (chatMode != MODE_PINNED) {
//                    getMessagesController().loadUserInfo(currentUser, true, classGuid, startLoadFromMessageId);
//                }
//                loadInfo = userInfo == null;
//            }
    //
    //        if (chatMode != MODE_PINNED) {
    //            waitingForLoad.add(lastLoadIndex);
    //            if (startLoadFromMessageId != 0 && (!isThreadChat() || startLoadFromMessageId == highlightMessageId)) {
    //                startLoadFromMessageIdSaved = startLoadFromMessageId;
    //                if (migrated_to != 0) {
    //                    mergeDialogId = migrated_to;
    //                    getMessagesController().loadMessages(mergeDialogId, 0, loadInfo, loadingFromOldPosition ? 50 : (AndroidUtilities.isTablet() || isThreadChat() ? 30 : 20), startLoadFromMessageId, 0, true, 0, classGuid, 3, 0, ChatObject.isChannel(currentChat), chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++);
    //                } else {
    //                    getMessagesController().loadMessages(dialog_id, mergeDialogId, loadInfo, loadingFromOldPosition ? 50 : (AndroidUtilities.isTablet() || isThreadChat() ? 30 : 20), startLoadFromMessageId, 0, true, 0, classGuid, 3, 0, ChatObject.isChannel(currentChat), chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++);
    //                }
    //            } else {
    //                if (historyPreloaded) {
    //                    lastLoadIndex++;
    //                } else {
//                        getMessagesController().loadMessages(dialog_id, mergeDialogId, loadInfo, AndroidUtilities.isTablet() || isThreadChat() ? 30 : 20, startLoadFromMessageId, 0, true, 0, classGuid, 2, 0, ChatObject.isChannel(currentChat), chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++);
    //                }
    //            }
    //        }
    //        if (chatMode == 0 && !isThreadChat()) {
    //            waitingForLoad.add(lastLoadIndex);
    //            getMessagesController().loadMessages(dialog_id, mergeDialogId, false, 1, 0, 0, true, 0, classGuid, 2, 0, ChatObject.isChannel(currentChat), MODE_SCHEDULED, threadMessageId, replyMaxReadId, lastLoadIndex++);
    //        }
    //        getMessagesController().loadMessages(dialog_id, mergeDialogId, loadInfo, AndroidUtilities.isTablet() || isThreadChat() ? 30 : 20, startLoadFromMessageId, 0, true, 0, classGuid, 2, 0, ChatObject.isChannel(currentChat), chatMode, threadMessageId, replyMaxReadId, lastLoadIndex++);
        }
    }
}
