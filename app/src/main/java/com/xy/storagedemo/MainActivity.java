package com.xy.storagedemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.xy.storagedemo.databinding.ActivityMainBinding;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;

    private StorageManager storageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setClick(new ClickProxy());
        storageManager = StorageManager.getInstance();

        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_MEDIA_LOCATION}, 0);
    }


    public class ClickProxy {

        public void saveImage() {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pic_emptypage_wuwangluo_white);
            if (bitmap != null) {
                storageManager.saveImage("storage", "wangluo.jpg", bitmap);
            }
        }

        public void queryImage() {
            Bitmap bitmap = storageManager.queryBitmapByName(Environment.DIRECTORY_PICTURES, "storage", "wangluo.jpg");
            if (bitmap != null) {
                mBinding.ivImage.setImageBitmap(bitmap);
            }
        }

        public void savePublicImage() {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pic_emptypage_wuwangluo_white);
            if (bitmap != null) {
                storageManager.savePublicImage("storage", "wangluo", bitmap);
            }
        }

        public void queryPublicImage() {
            List<Bitmap> bitmapList = storageManager.queryPublicBitmap("storage", "wangluo.jpg");
            if (bitmapList != null && bitmapList.size() > 0) {
                mBinding.ivImage.setImageBitmap(bitmapList.get(0));
            }
        }

        public void deleteImage() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                storageManager.deletePublicImage("storage", "wangluo.jpg", new StorageManager.OnDeleteListener() {
                    @Override
                    public void deletePicture(IntentSender intentSender) {
                        try {
                            startIntentSenderForResult(intentSender, 10010, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        public void toSaf() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, 10012);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case 10010:
                new ClickProxy().deleteImage();
                break;
            case 10012:
                if (data != null) {
                    Uri uri = data.getData();
                    if(uri!=null){
                        storageManager.copyUriToExternalFilesDir(uri,"test.jpg");
                    }
                    /*Log.d("---",uri.toString());
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        mBinding.ivImage.setImageBitmap(BitmapFactory.decodeStream(inputStream));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }*/
                }
                break;
        }

    }
}