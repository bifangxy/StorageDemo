package com.xy.storagedemo;

import android.app.RecoverableSecurityException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe:
 * Created by xieying on 2020/9/1
 */
public class StorageManager {
    private static final String TAG = StorageManager.class.getSimpleName();

    private static String DEFAULT_IMAGE_PATH = "DCIM";


    private Context mContext;

    private static class SingleHolder {
        private static StorageManager INSTANCE = new StorageManager();
    }

    public static StorageManager getInstance() {
        return SingleHolder.INSTANCE;
    }

    public StorageManager() {
        mContext = MyApplication.getInstance().getContext();
    }

    /**
     * 保存图片到应用私有目录
     *
     * @param path     保存文件路径,指相对路径
     * @param fileName 文件名 单指文件名，不带路径
     * @param bitmap   bitmap
     */
    public void saveImage(String path, String fileName, Bitmap bitmap) {
        if (bitmap == null || TextUtils.isEmpty(path)) {
            return;
        }
        try {
            File pictures = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFileDirectory = new File(pictures + File.separator + path);
            if (!imageFileDirectory.exists()) {
                imageFileDirectory.mkdirs();
            }
            File imageFile = new File(pictures + File.separator + path + File.separator + fileName);
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            Log.d(TAG, "----save success");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 查询应用私有目录下的图片文件
     *
     * @param environmentType 文件类型
     * @param path            路径
     * @param fileName        文件名
     * @return bitmap
     */
    public Bitmap queryBitmapByName(String environmentType, String path, String fileName) {
        if (environmentType == null || fileName == null) {
            return null;
        }
        Bitmap bitmap = null;
        File pictures = mContext.getExternalFilesDir(environmentType);
        File imageFileDirectory = new File(pictures + File.separator + path);
        if (imageFileDirectory.exists() && imageFileDirectory.isDirectory()) {
            File[] files = imageFileDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    String signFileName = file.getName();
                    if (file.isFile() && fileName.equals(signFileName)) {
                        bitmap = BitmapFactory.decodeFile(file.getPath());
                        return bitmap;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 保存图片到公共文件夹
     *
     * @param path     路径 相当路径
     * @param fileName 文件名 单纯文件名，不带后缀
     * @param bitmap   bitmap
     */
    public void savePublicImage(String path, String fileName, Bitmap bitmap) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            //Android10 不再使用MediaStore.Images.Media.DATA，而使用MediaStore.Images.Media.RELATIVE_PATH 相对路径
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //DCIM是系统文件夹，关于系统文件夹可以到系统自带的文件管理器中查看，不可以写没存在的名字
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + path);
            } else {
                String realPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + File.separator + path;
                File file = new File(realPath);
                if(!file.exists()){
                    file.mkdirs();
                }
                contentValues.put(MediaStore.Images.Media.DATA, realPath + File.separator + fileName + ".jpg");
            }
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/JPEG");
            Uri uri = mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                OutputStream outputStream = mContext.getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    Log.d(TAG, "----save success");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取公共文件夹下的图片文件
     *
     * @param pathName 路径名称
     * @param fileName 文件名 如果需要根据文件名查询，需要带后缀
     * @return bitmap
     */
    public List<Bitmap> queryPublicBitmap(String pathName, String fileName) {
        List<Bitmap> bitmapList = new ArrayList<>();
        try {
            String queryPathKey = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                    MediaStore.Images.Media.RELATIVE_PATH :
                    MediaStore.Images.Media.DATA;

            pathName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                    DEFAULT_IMAGE_PATH + File.separator + pathName + File.separator :
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath()
                            + File.separator + pathName + File.separator + fileName;

            //根据路径和文件名查询图片
            String selection = queryPathKey + "=? ";
            String[] args = new String[]{pathName};
            //根据类型查询图片
//            String selection = MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=? ";
//            String[] args = new String[]{"image/jpeg", "image/png"};

            Cursor cursor = mContext.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.Media._ID, queryPathKey, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.DISPLAY_NAME},
                    selection,
                    args,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                    String path = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                            cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)) :
                            cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    String type = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));//图片类型
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));//图片名字
//            Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                        uri = MediaStore.setRequireOriginal(uri);
                    }
                    InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        bitmapList.add(bitmap);
                        InputStream is = mContext.getContentResolver().openInputStream(uri);
                        ExifInterface exifInterface = new ExifInterface(is);
                        float[] returnedLatLong = new float[2];
                        boolean success = exifInterface.getLatLong(returnedLatLong);
                        if (success) {
                            Log.d(TAG, "lat = " + returnedLatLong[0]);
                            Log.d(TAG, "lng = " + returnedLatLong[1]);
                        }
                    }
                } while (cursor.moveToNext());
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmapList;
    }

    /**
     * 删除文件
     *
     * @param path     路径
     * @param fileName 文件名
     * @param listener 删除权限回调
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void deletePublicImage(String path, String fileName, OnDeleteListener listener) {
        Uri uri = getPublicImageUri(path, fileName);
        deleteUri(uri, listener);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void deleteUri(Uri uri, OnDeleteListener listener) {
        if (uri != null) {
            int raw = 0;
            try {
                raw = mContext.getContentResolver().delete(uri, null, null);
            } catch (RecoverableSecurityException e) {
                e.printStackTrace();
                if (listener != null) {
                    listener.deletePicture(e.getUserAction().getActionIntent().getIntentSender());
                }
            }
            if (raw > 0) {
                Log.d(TAG, "----delete success---");
            }
        }
    }


    private Uri getPublicImageUri(String pathName, String fileName) {
        String queryPathKey = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                MediaStore.Images.Media.RELATIVE_PATH :
                MediaStore.Images.Media.DATA;
        pathName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                DEFAULT_IMAGE_PATH + File.separator + pathName + File.separator :
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + File.separator + pathName;
        String selection = queryPathKey + "=? and " + MediaStore.Images.Media.DISPLAY_NAME + "=? ";
        String[] args = new String[]{pathName, fileName};
        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID, queryPathKey, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.DISPLAY_NAME},
                selection,
                args,
                null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                //以下两种方法都可以获取到Uri
//              Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                //官方方法
                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                return uri;
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    /**
     * @param uri 将uri转换成为私有目录下的文件，这样可提供给其他第三方不支持Uri的SDK使用
     * @param fileName 文件名
     */
    public void copyUriToExternalFilesDir(Uri uri, String fileName) {
        try {
            InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
            String filePath = mContext.getExternalCacheDir() + File.separator + "temp";
            if (inputStream != null) {
                File pathFile = new File(filePath);
                if (!pathFile.exists()) {
                    pathFile.mkdirs();
                }
                File file = new File(filePath + File.separator + fileName);
                OutputStream outputStream = new FileOutputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                byte[] bytes = new byte[1024];
                int length = bufferedInputStream.read(bytes);
                while (length > 0) {
                    bufferedOutputStream.write(bytes, 0, length);
                    bufferedOutputStream.flush();
                    length = bufferedInputStream.read(bytes);
                }
                bufferedOutputStream.close();
                outputStream.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public interface OnDeleteListener {
        void deletePicture(IntentSender intentSender);
    }

}
