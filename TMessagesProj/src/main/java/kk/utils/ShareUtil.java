package kk.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import org.telegram.messenger.BuildConfig;

import java.io.File;

import androidx.core.content.FileProvider;

public class ShareUtil {
    public static boolean shareVideo(Context context, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return false;
        Intent intent = new Intent(Intent.ACTION_SEND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".file_provider", file);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        }
        intent.setType("video/*");
        Intent chooser = Intent.createChooser(intent, "Share：");
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(chooser);
        }
        return true;
    }

    public static boolean shareVideo2(Context context, String filePath) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putExtra(Intent.EXTRA_STREAM, getContentUri(context, filePath));
        shareIntent.setType("video/*");
        context.startActivity(Intent.createChooser(shareIntent, "Share："));
        return true;
    }

    public static Uri getContentUri(Context context, String filePath) {
        try {
            Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Video.Media._ID}, MediaStore.Video.Media.DATA + "=? ", new String[]{filePath}, null);
            Uri uri = null;
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    Uri baseUri = Uri.parse("content://media/external/video/media");
                    uri = Uri.withAppendedPath(baseUri, "" + id);
                }
                cursor.close();
            }
            if (uri == null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DATA, filePath);
                uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            }
            return uri;
        } catch (Exception e) {
        }
        return null;
    }
}
