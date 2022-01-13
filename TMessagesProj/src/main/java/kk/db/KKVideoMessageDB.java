package kk.db;

import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.annotation.UiThread;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.support.SparseLongArray;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import kk.KKLoger;
import kk.video.KKMessage;

public class KKVideoMessageDB extends BaseController {

    private DispatchQueue storageQueue = new DispatchQueue("KKVideoMessageQueue");
    private SQLiteDatabase database;
    private File cacheFile;
    private File walCacheFile;
    private File shmCacheFile;
    private AtomicLong lastTaskId = new AtomicLong(System.currentTimeMillis());
    private SparseArray<ArrayList<Runnable>> tasks = new SparseArray<>();

    private int lastDateValue = 0;
    private int lastPtsValue = 0;
    private int lastQtsValue = 0;
    private int lastSeqValue = 0;
    private int lastSecretVersion = 0;
    private byte[] secretPBytes = null;
    private int secretG = 0;

    private int lastSavedSeq = 0;
    private int lastSavedPts = 0;
    private int lastSavedDate = 0;
    private int lastSavedQts = 0;

    private ArrayList<MessagesController.DialogFilter> dialogFilters = new ArrayList<>();
    private SparseArray<MessagesController.DialogFilter> dialogFiltersMap = new SparseArray<>();
    private LongSparseArray<Boolean> unknownDialogsIds = new LongSparseArray<>();
    private int mainUnreadCount;
    private int archiveUnreadCount;
    private volatile int pendingMainUnreadCount;
    private volatile int pendingArchiveUnreadCount;

    private CountDownLatch openSync = new CountDownLatch(1);

    private static volatile KKVideoMessageDB[] Instance = new KKVideoMessageDB[UserConfig.MAX_ACCOUNT_COUNT];
    private final static int LAST_DB_VERSION = 1;

    public static KKVideoMessageDB getInstance(int num) {
        KKVideoMessageDB localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (KKVideoMessageDB.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new KKVideoMessageDB(num);
                }
            }
        }
        return localInstance;
    }

    private void ensureOpened() {
        try {
            openSync.await();
        } catch (Throwable ignore) {

        }
    }

    public int getLastDateValue() {
        ensureOpened();
        return lastDateValue;
    }

    public void setLastDateValue(int value) {
        ensureOpened();
        lastDateValue = value;
    }

    public int getLastPtsValue() {
        ensureOpened();
        return lastPtsValue;
    }

    public int getMainUnreadCount() {
        return mainUnreadCount;
    }

    public int getArchiveUnreadCount() {
        return archiveUnreadCount;
    }

    public void setLastPtsValue(int value) {
        ensureOpened();
        lastPtsValue = value;
    }

    public int getLastQtsValue() {
        ensureOpened();
        return lastQtsValue;
    }

    public void setLastQtsValue(int value) {
        ensureOpened();
        lastQtsValue = value;
    }

    public int getLastSeqValue() {
        ensureOpened();
        return lastSeqValue;
    }

    public void setLastSeqValue(int value) {
        ensureOpened();
        lastSeqValue = value;
    }

    public int getLastSecretVersion() {
        ensureOpened();
        return lastSecretVersion;
    }

    public void setLastSecretVersion(int value) {
        ensureOpened();
        lastSecretVersion = value;
    }

    public byte[] getSecretPBytes() {
        ensureOpened();
        return secretPBytes;
    }

    public void setSecretPBytes(byte[] value) {
        ensureOpened();
        secretPBytes = value;
    }

    public int getSecretG() {
        ensureOpened();
        return secretG;
    }

    public void setSecretG(int value) {
        ensureOpened();
        secretG = value;
    }

    public KKVideoMessageDB(int account) {
        super(account);
        //storageQueue.setPriority(Thread.MAX_PRIORITY);
        storageQueue.postRunnable(() -> openDatabase(1));
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public DispatchQueue getStorageQueue() {
        return storageQueue;
    }

    @UiThread
    public void bindTaskToGuid(Runnable task, int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            tasks.put(guid, arrayList);
        }
        arrayList.add(task);
    }

    @UiThread
    public void cancelTasksForGuid(int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            return;
        }
        for (int a = 0, N = arrayList.size(); a < N; a++) {
            storageQueue.cancelRunnable(arrayList.get(a));
        }
        tasks.remove(guid);
    }

    @UiThread
    public void completeTaskForGuid(Runnable runnable, int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            return;
        }
        arrayList.remove(runnable);
        if (arrayList.isEmpty()) {
            tasks.remove(guid);
        }
    }

    public long getDatabaseSize() {
        long size = 0;
        if (cacheFile != null) {
            size += cacheFile.length();
        }
        if (shmCacheFile != null) {
            size += shmCacheFile.length();
        }
        /*if (walCacheFile != null) {
            size += walCacheFile.length();
        }*/
        return size;
    }

    public void openDatabase(int openTries) {
        File filesDir = ApplicationLoader.getFilesDirFixed();
        if (currentAccount != 0) {
            filesDir = new File(filesDir, "account" + currentAccount + "/");
            filesDir.mkdirs();
        }
        cacheFile = new File(filesDir, "kkVideoMessage.db");
        walCacheFile = new File(filesDir, "kkVideoMessage.db-wal");
        shmCacheFile = new File(filesDir, "kkVideoMessage.db-shm");

        boolean createTable = false;
        //cacheFile.delete();
        if (!cacheFile.exists()) {
            createTable = true;
        }
        try {
            database = new SQLiteDatabase(cacheFile.getPath());
            database.executeFast("PRAGMA secure_delete = ON").stepThis().dispose();
            database.executeFast("PRAGMA temp_store = MEMORY").stepThis().dispose();
            database.executeFast("PRAGMA journal_mode = WAL").stepThis().dispose();
            database.executeFast("PRAGMA journal_size_limit = 10485760").stepThis().dispose();

            if (createTable) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("create new database");
                }

                database.executeFast("CREATE TABLE messages(mid INTEGER PRIMARY KEY, uid INTEGER, read_state INTEGER, send_state INTEGER, date INTEGER, data BLOB, out INTEGER, ttl INTEGER, media INTEGER, replydata BLOB, imp INTEGER, mention INTEGER, forwards INTEGER, replies_data BLOB, thread_reply_id INTEGER, download_time INTEGER, dialog_id INTEGER, local_attach_name TEXT)").stepThis().dispose();
                database.executeFast("CREATE TABLE message_filter(mid INTEGER PRIMARY KEY, date INTEGER)").stepThis().dispose();

                //version
                database.executeFast("PRAGMA user_version = " + LAST_DB_VERSION).stepThis().dispose();
            } else {
                int version = database.executeInt("PRAGMA user_version");
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("current db version = " + version);
                }
                if (version == 0) {
                    throw new Exception("malformed");
                }
                try {
                    SQLiteCursor cursor = database.queryFinalized("SELECT seq, pts, date, qts, lsv, sg, pbytes FROM params WHERE id = 1");
                    if (cursor.next()) {
                        lastSeqValue = cursor.intValue(0);
                        lastPtsValue = cursor.intValue(1);
                        lastDateValue = cursor.intValue(2);
                        lastQtsValue = cursor.intValue(3);
                        lastSecretVersion = cursor.intValue(4);
                        secretG = cursor.intValue(5);
                        if (cursor.isNull(6)) {
                            secretPBytes = null;
                        } else {
                            secretPBytes = cursor.byteArrayValue(6);
                            if (secretPBytes != null && secretPBytes.length == 1) {
                                secretPBytes = null;
                            }
                        }
                    }
                    cursor.dispose();
                } catch (Exception e) {
                    FileLog.e(e);
                    try {
                        database.executeFast("CREATE TABLE IF NOT EXISTS params(id INTEGER PRIMARY KEY, seq INTEGER, pts INTEGER, date INTEGER, qts INTEGER, lsv INTEGER, sg INTEGER, pbytes BLOB)").stepThis().dispose();
                        database.executeFast("INSERT INTO params VALUES(1, 0, 0, 0, 0, 0, 0, NULL)").stepThis().dispose();
                    } catch (Exception e2) {
                        FileLog.e(e2);
                    }
                }
                if (version < LAST_DB_VERSION) {
                    updateDbToLastVersion(version);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);

            if (openTries < 3 && e.getMessage().contains("malformed")) {
                if (openTries == 2) {
                    cleanupInternal(true);
                    for (int a = 0; a < 2; a++) {
                        getUserConfig().setDialogsLoadOffset(a, 0, 0, 0, 0, 0, 0);
                        getUserConfig().setTotalDialogsCount(a, 0);
                    }
                    getUserConfig().saveConfig(false);
                } else {
                    cleanupInternal(false);
                }
                openDatabase(openTries == 1 ? 2 : 3);
            }
        }
        try {
            openSync.countDown();
        } catch (Throwable ignore) {

        }
    }

    private void updateDbToLastVersion(final int currentVersion) {
        storageQueue.postRunnable(() -> {
            try {
                int version = currentVersion;
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void executeNoException(String query) {
        try {
            database.executeFast(query).stepThis().dispose();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void cleanupInternal(boolean deleteFiles) {
        lastDateValue = 0;
        lastSeqValue = 0;
        lastPtsValue = 0;
        lastQtsValue = 0;
        lastSecretVersion = 0;
        mainUnreadCount = 0;
        archiveUnreadCount = 0;
        pendingMainUnreadCount = 0;
        pendingArchiveUnreadCount = 0;
        dialogFilters.clear();
        dialogFiltersMap.clear();
        unknownDialogsIds.clear();

        lastSavedSeq = 0;
        lastSavedPts = 0;
        lastSavedDate = 0;
        lastSavedQts = 0;

        secretPBytes = null;
        secretG = 0;
        if (database != null) {
            database.close();
            database = null;
        }
        if (deleteFiles) {
            if (cacheFile != null) {
                cacheFile.delete();
                cacheFile = null;
            }
            if (walCacheFile != null) {
                walCacheFile.delete();
                walCacheFile = null;
            }
            if (shmCacheFile != null) {
                shmCacheFile.delete();
                shmCacheFile = null;
            }
        }
    }

    public void cleanup(final boolean isLogin) {
        storageQueue.postRunnable(() -> {
            cleanupInternal(true);
            openDatabase(1);
            if (isLogin) {
                Utilities.stageQueue.postRunnable(() -> getMessagesController().getDifference());
            }
        });
    }

    private static boolean isEmpty(SparseArray<?> array) {
        return array == null || array.size() == 0;
    }

    private static boolean isEmpty(SparseLongArray array) {
        return array == null || array.size() == 0;
    }

    private static boolean isEmpty(List<?> array) {
        return array == null || array.isEmpty();
    }

    private static boolean isEmpty(SparseIntArray array) {
        return array == null || array.size() == 0;
    }

    private static boolean isEmpty(LongSparseArray<?> array) {
        return array == null || array.size() == 0;
    }

    private void fixUnsupportedMedia(TLRPC.Message message) {
        if (message == null) {
            return;
        }
        boolean ok = false;
        if (message.media instanceof TLRPC.TL_messageMediaUnsupported_old) {
            if (message.media.bytes.length == 0) {
                message.media.bytes = new byte[1];
                message.media.bytes[0] = TLRPC.LAYER;
            }
        } else if (message.media instanceof TLRPC.TL_messageMediaUnsupported) {
            message.media = new TLRPC.TL_messageMediaUnsupported_old();
            message.media.bytes = new byte[1];
            message.media.bytes[0] = TLRPC.LAYER;
            message.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA;
        }
    }

    public Set<Integer> loadHideMessageIds(List<Integer> ids, int from, int to) {
        ensureOpened();
        Set<Integer> result = new HashSet<>();
        if (ids == null || ids.size() == 0) {
            return result;
        }
        StringBuilder inStr = new StringBuilder("(");
        for (Integer id : ids) {
            inStr.append(id);
            inStr.append(",");
        }
        inStr.deleteCharAt(inStr.length() - 1);
        inStr.append(")");
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT mid, date " +
                    " FROM message_filter " +
                    " WHERE date >= " + from + " " +
                    " AND date <=" + to +
                    " AND mid in " + inStr));
            while (cursor.next()) {
                result.add(cursor.intValue(0));
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    public void addHideMessageId(int mid, int date) {
        ensureOpened();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM message_filter WHERE mid=" + mid);
            if (cursor.next()) {
                KKLoger.logLocalVideoManager("has message_filter record for mid:" + mid);
            } else {
                KKLoger.logLocalVideoManager("insert message_filter for mid:" + mid);
                SQLitePreparedStatement state_messages = database.executeFast("INSERT INTO message_filter VALUES(" +
                        "?, " +     //mid
                        "? " +     //date
                        ")");
                state_messages.requery();
                state_messages.bindInteger(1, mid);
                state_messages.bindInteger(2, date);
                state_messages.step();
                state_messages.dispose();
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    public List<KKMessage> loadMessages() {
        ensureOpened();
        List<KKMessage> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid, date, dialog_id, download_time FROM messages ORDER BY download_time DESC"));
            while (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data != null) {
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    message.readAttachPath(data, getUserConfig().clientUserId);
                    data.reuse();
                    message.id = cursor.intValue(1);
                    message.date = cursor.intValue(2);
                    message.dialog_id = cursor.longValue(3);
                    long downloadTime = cursor.longValue(4);
                    result.add(new KKMessage(message, downloadTime));
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    public void removeMessageByAttachFileName(String attachFileName) {
        ensureOpened();
        try {
            SQLitePreparedStatement statement = database.executeFast("DELETE FROM messages WHERE local_attach_name = ?");
            statement.requery();
            statement.bindString(1, attachFileName);
            statement.step();
            statement.dispose();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public void putMessage(TLRPC.Message message, long downloadTime, String localFileName) {
        ensureOpened();
        int mid = message.id;
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM messages WHERE mid=" + mid);
            if (cursor.next()) {
                KKLoger.logLocalVideoManager("has record for mid:" + mid);
            } else {
                KKLoger.logLocalVideoManager("insert message for mid:" + mid);
                NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
                message.serializeToStream(data);
                SQLitePreparedStatement state_messages = database.executeFast("INSERT INTO messages VALUES(" +
                        "?, " +     //mid
                        "?, " +     //uid
                        "?, " +     //read_state
                        "?, " +     //send_state
                        "?, " +     //date
                        "?, " +     //data
                        "?, " +     //out
                        "?, " +     //ttl
                        "?, " +     //media
                        "NULL, " +  //replydata
                        "?, " +     //imp
                        "?, " +     //mention
                        "?, " +     //forwards
                        "?, " +     //replies_data
                        "?, " +     //thread_reply_id
                        "?, " +     //download_time
                        "?, " +     //dialog_id
                        "? "  +     //local_attach_name
                        ")");
                state_messages.requery();
                state_messages.bindLong(1, message.id);
                state_messages.bindLong(2, message.dialog_id);
                state_messages.bindInteger(3, MessageObject.getUnreadFlags(message));
                state_messages.bindInteger(4, message.send_state);
                state_messages.bindInteger(5, message.date);
                state_messages.bindByteBuffer(6, data);
                state_messages.bindInteger(7, (MessageObject.isOut(message) || message.from_scheduled ? 1 : 0));
                state_messages.bindInteger(8, message.ttl);
                if ((message.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0) {
                    state_messages.bindInteger(9, message.views);
                } else {
                    state_messages.bindInteger(9, getMessageMediaType(message));
                }
                int flags = 0;
                if (message.stickerVerified == 0) {
                    flags |= 1;
                } else if (message.stickerVerified == 2) {
                    flags |= 2;
                }
                state_messages.bindInteger(10, flags);
                state_messages.bindInteger(11, message.mentioned ? 1 : 0);
                state_messages.bindInteger(12, message.forwards);
                NativeByteBuffer repliesData = null;
                if (message.replies != null) {
                    repliesData = new NativeByteBuffer(message.replies.getObjectSize());
                    message.replies.serializeToStream(repliesData);
                    state_messages.bindByteBuffer(13, repliesData);
                } else {
                    state_messages.bindNull(13);
                }
                if (message.reply_to != null) {
                    state_messages.bindInteger(14, message.reply_to.reply_to_top_id != 0 ? message.reply_to.reply_to_top_id : message.reply_to.reply_to_msg_id);
                } else {
                    state_messages.bindInteger(14, 0);
                }
                state_messages.bindLong(15, downloadTime);
                state_messages.bindLong(16, message.dialog_id);
                state_messages.bindString(17, localFileName);
                state_messages.step();
                state_messages.dispose();
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    private int getMessageMediaType(TLRPC.Message message) {
        if (message instanceof TLRPC.TL_message_secret) {
            if ((message.media instanceof TLRPC.TL_messageMediaPhoto || MessageObject.isGifMessage(message)) && message.ttl > 0 && message.ttl <= 60 ||
                    MessageObject.isVoiceMessage(message) ||
                    MessageObject.isVideoMessage(message) ||
                    MessageObject.isRoundVideoMessage(message)) {
                return 1;
            } else if (message.media instanceof TLRPC.TL_messageMediaPhoto || MessageObject.isVideoMessage(message)) {
                return 0;
            }
        } else if (message instanceof TLRPC.TL_message && (message.media instanceof TLRPC.TL_messageMediaPhoto || message.media instanceof TLRPC.TL_messageMediaDocument) && message.media.ttl_seconds != 0) {
            return 1;
        } else if (message.media instanceof TLRPC.TL_messageMediaPhoto || MessageObject.isVideoMessage(message)) {
            return 0;
        }
        return -1;
    }
}
