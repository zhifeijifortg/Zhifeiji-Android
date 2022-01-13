package kk.files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KKFileMessageMerge implements IKKFileMessageMerge {

    private Map<Integer, Integer> indexes = new HashMap<>();

    private int findMinTimeListIndex(List<List<KKFileMessage>> messageLists) {
        int index = -1;
        int minDate = Integer.MAX_VALUE;
        int listCount = messageLists.size();
        for (int i = 0; i < listCount; i++) {
            int listSize = messageLists.get(i).size();
            if (!indexes.containsKey(i)) {
                indexes.put(i, 0);
            }
            int listIndex = indexes.get(i);
            if (listIndex < listSize) {
                KKFileMessage kkFileMessage = messageLists.get(i).get(listIndex);
                int date = kkFileMessage.getMessageObject().messageOwner.date;
                if (date < minDate) {
                    minDate = date;
                    index = i;
                }
            }
        }
        return index;
    }

    @Override
    public List<KKFileMessage> mergeLists(List<List<KKFileMessage>> messageLists) {
        List<KKFileMessage> mergedList = new ArrayList<>();
        int listIndex = -1;
        while ((listIndex = findMinTimeListIndex(messageLists)) != -1) {
            mergedList.add(messageLists.get(listIndex).get(indexes.get(listIndex)));
            indexes.put(listIndex, indexes.get(listIndex) + 1);
        }
        return mergedList;
    }
}
