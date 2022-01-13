package kk.video;

import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kk.db.KKVideoMessageDB;
import kk.files.KKFileMessage;

public class KKFileMessageFilter<T extends KKFileMessage> {

    private final KKVideoMessageDB messageDB = KKVideoMessageDB.getInstance(UserConfig.selectedAccount);

    public void hideVideoMessage(KKFileMessage fileMessageMessage) {
        messageDB.addHideMessageId(fileMessageMessage.getId(), fileMessageMessage.getMessageObject().messageOwner.date);
    }

    public List<T> filterMessages(List<T> fileMessages) {
        List<T> result = new ArrayList<>();
        if (fileMessages == null || fileMessages.size() == 0) return result;
        int startDate = fileMessages.get(0).getMessageObject().messageOwner.date;
        int endDate = fileMessages.get(fileMessages.size() - 1).getMessageObject().messageOwner.date;
        List<Integer> ids = new ArrayList<>();
        for (T videoMessage : fileMessages) {
            ids.add(videoMessage.getId());
        }
        Set<Integer> hideIds = messageDB.loadHideMessageIds(ids, Math.min(startDate, endDate), Math.max(startDate, endDate));
        for (T videoMessage : fileMessages) {
            if (!hideIds.contains(videoMessage.getId())) {
                result.add(videoMessage);
            }
        }
        return result;
    }
}
