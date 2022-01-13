package kk.utils.sort;

import java.util.Comparator;

import kk.model.LocalVideoEntity;

/**
 * Created by LSD on 2021/3/26.
 * Desc
 */
public class LocalVideoSort implements Comparator<LocalVideoEntity> {
    @Override
    public int compare(LocalVideoEntity t1, LocalVideoEntity t2) {
        final long result = t2.time - t1.time;
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        } else {
            return 0;
        }
    }

}
