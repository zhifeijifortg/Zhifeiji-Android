package kk.files;

import org.telegram.tgnet.TLRPC;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import kk.KKLoger;

public class BaseTagSearch {

    private int getRequestMinTime(int page) {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        long delta = 1000 * 60 * 60 * 24;
        long result = (now.getTimeInMillis() - delta * page) / 1000;
        Date date = new Date(result * 1000);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        KKLoger.logFileData("page=" + page + ", date=" + dateFormat.format(date) + ", timestamp=" + result);
        return (int) result;
    }

    protected int getLastInScopeMessageIndex(int page, List<TLRPC.Message> messages) {
        int index = -1;
        int size = messages.size();
        int minDate = getRequestMinTime(page);
        for (int i = 0; i < size; i++) {
            if (messages.get(i).date >= minDate ) {
                index = i;
            } else {
                break;
            }
        }
        return index;
    }
}
