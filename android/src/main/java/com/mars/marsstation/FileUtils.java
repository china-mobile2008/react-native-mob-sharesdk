package com.mars.marsstation;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateFormat;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class FileUtils {

    public static final int GLOBAL_BUFFER_SIZE = 512 * 1024;

    public static void makesureParentExist(File file_) {
        File parent = file_.getParentFile();
        if ((parent != null) && (!parent.exists()))
            mkdirs(parent);
    }

    public static void mkdirs(File dir_) {

        if ((!dir_.exists()) && (!dir_.mkdirs()) && (BuildConfig.DEBUG))
            throw new RuntimeException("fail to make " + dir_.getAbsolutePath());
    }

    public static void createNewFile(File file_) {
        if ((!__createNewFile(file_)) && (BuildConfig.DEBUG))
            throw new RuntimeException(file_.getAbsolutePath()
                    + " doesn't be created!");
    }

    private static boolean __createNewFile(File file_) {
        makesureParentExist(file_);
        if (file_.exists())
            delete(file_);
        try {
            return file_.createNewFile();
        } catch (IOException e) {

        }

        return false;
    }

    public static void delete(File f) {
        if ((f != null) && (f.exists()) && (!f.delete()) && (BuildConfig.DEBUG))
            throw new RuntimeException(f.getAbsolutePath()
                    + " doesn't be deleted!");
    }

    public static FileInputStream getFileInputStream(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {

        }

        return fis;
    }

    public static boolean closeStream(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
                return true;
            }
        } catch (IOException e) {
        }

        return false;
    }

    public static InputStream makeInputBuffered(InputStream input_) {
        if ((input_ instanceof BufferedInputStream)) {
            return input_;
        }
        return new BufferedInputStream(input_, GLOBAL_BUFFER_SIZE);
    }

    public static OutputStream makeOutputBuffered(OutputStream output_) {
        if ((output_ instanceof BufferedOutputStream)) {
            return output_;
        }

        return new BufferedOutputStream(output_, GLOBAL_BUFFER_SIZE);
    }

    public static void copy(File in_, File out_) throws IOException {
        makesureParentExist(out_);
        copy(new FileInputStream(in_), new FileOutputStream(out_));
    }

    public static void copy(InputStream input_, OutputStream output_)
            throws IOException {
        try {
            byte[] buffer = new byte[GLOBAL_BUFFER_SIZE];
            int temp = -1;
            input_ = makeInputBuffered(input_);
            output_ = makeOutputBuffered(output_);
            while ((temp = input_.read(buffer)) != -1) {
                output_.write(buffer, 0, temp);
                output_.flush();
            }
        } catch (IOException e) {
            throw e;
        } finally {
            FileUtils.closeStream(input_);
            FileUtils.closeStream(output_);
        }
    }

    /**
     * 根据Uri获取图片绝对路径，解决Android4.4以上版本Uri转换
     *
     * @param context
     * @param imageUri
     * @author yaoxing
     * @date 2014-10-12
     */
    @TargetApi(19)
    public static String getImageAbsolutePath(Context context, Uri imageUri) {
        if (context == null || imageUri == null)
            return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, imageUri)) {
            if (isExternalStorageDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(imageUri)) {
                String id = DocumentsContract.getDocumentId(imageUri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(imageUri))
                return imageUri.getLastPathSegment();
            return getDataColumn(context, imageUri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            return imageUri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = MediaStore.Images.Media.DATA;
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
