package kk.utils;

import android.os.Handler;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kk.files.KKFileTypes;
import kk.model.LocalVideoEntity;
import kk.utils.sort.LastModifiedFileComparator;

/**
 * Created by LSD on 2020/10/31.
 * Desc
 */
public class FileScanManager {
    static Handler handler = new Handler();

    public interface FileListener {
        void onFind(List<LocalVideoEntity> list);

        void onFinish(List<LocalVideoEntity> list);
    }

    public static void scanFile(String[] dirPathArr, FileListener listener) {
        new Thread(() -> {
            List<LocalVideoEntity> findList = new ArrayList<>();
            for (String path : dirPathArr) {
                List<LocalVideoEntity> list = getAllFiles(path, null);
                if (list != null) {
                    findList.addAll(0, list);
                    handler.post(() -> {
                        listener.onFind(list);
                    });
                }
            }
            handler.post(() -> {
                listener.onFinish(findList);
            });
        }).start();
    }

    private static List<LocalVideoEntity> getAllFiles(String dirPath, List<LocalVideoEntity> entities) {
        File f = new File(dirPath);
        if (!f.exists()) return null;
        File[] files = f.listFiles();
        if (files == null) return null;
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);//排序
        if (entities == null) entities = new ArrayList<>();
        for (File file : files) {//遍历目录
            String extensionName = SystemUtil.getExtensionName(file.getName());
            extensionName = extensionName.toLowerCase();
            String name = file.getName();
            String path = file.getAbsolutePath();
            long size = file.length();
            long time = file.lastModified();
            if (file.isFile()) {
                KKFileTypes fileType = KKFileTypes.parseFileType("." + extensionName);
                String localVideo = LocaleController.getString("tools_local_video", R.string.tools_local_video);
                String localFile = LocaleController.getString("tools_local_file", R.string.tools_local_file);
                if (fileType != KKFileTypes.ALL && fileType != KKFileTypes.UNKNOWN && fileType != KKFileTypes.IGNORE) {
                    LocalVideoEntity entity = new LocalVideoEntity(name, path, time, size);
                    entity.type = 1;
                    entity.fileType = fileType;
                    if ("mp4".equals(extensionName) || "mkv".equals(extensionName) || "mov".equals(extensionName)) {
                        entity.fromText = localVideo;
                        if (size > (3 * 1024 * 1024)) {
                            entities.add(entity);
                        }
                    } else {
                        entity.fromText = localFile;
                        entities.add(entity);
                    }
                }
            } else if (file.isDirectory()) {//查询子目录
                getAllFiles(file.getAbsolutePath(), entities);
            } else {
            }
        }
        return entities;
    }
}
