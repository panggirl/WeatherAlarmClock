/*
 * Copyright (c) 2016. Kaku咖枯 Inc. All rights reserved.
 */
package com.kaku.weac.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.kaku.weac.R;
import com.kaku.weac.adapter.LocalAlbumDetailAdapter;
import com.kaku.weac.bean.Event.FinishLocalAlbumActivityEvent;
import com.kaku.weac.bean.Event.QRcodeLogoEvent;
import com.kaku.weac.bean.Event.WallpaperEvent;
import com.kaku.weac.bean.ImageItem;
import com.kaku.weac.common.WeacConstants;
import com.kaku.weac.util.MyUtil;
import com.kaku.weac.util.OttoAppConfig;

import java.io.File;
import java.util.List;

/**
 * 本地相册详细图片Activity
 *
 * @author 咖枯
 * @version 1.0 2016/1/14
 */
public class LocalAlbumDetailActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_IMAGE_CROP_THEME = 1;
    private static final int REQUEST_IMAGE_CROP_QRCODE_LOGO = 2;
    private LocalAlbumDetailAdapter mLocalAlbumDetailAdapter;
    private int mRequestType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_album_detail);
        ViewGroup backGround = (ViewGroup) findViewById(R.id.background);
        MyUtil.setBackgroundBlur(backGround, this);
        mRequestType = getIntent().getIntExtra(WeacConstants.REQUEST_LOCAL_ALBUM_TYPE, 0);
        assignViews();
    }

    private void assignViews() {
        ImageView actionBack = (ImageView) findViewById(R.id.action_back);
        actionBack.setOnClickListener(this);

        TextView actionTitle = (TextView) findViewById(R.id.action_title);
        String title = getIntent().getStringExtra(LocalAlbumActivity.ALBUM_NAME);
        if (!TextUtils.isEmpty(title)) {
            actionTitle.setText(title);
        }

        List<ImageItem> localAlbumDetailList = getIntent().getParcelableArrayListExtra
                (LocalAlbumActivity.ALBUM_PATH);
        mLocalAlbumDetailAdapter = new LocalAlbumDetailAdapter(this, localAlbumDetailList);
        GridView albumPictureDetailGv = (GridView) findViewById(R.id.album_picture_detail_gv);
        albumPictureDetailGv.setAdapter(mLocalAlbumDetailAdapter);
        albumPictureDetailGv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String imagePath = mLocalAlbumDetailAdapter.getItem(position).getImagePath();
                switch (mRequestType) {
                    // 主题
                    case 0:
                        cropImage(0, REQUEST_IMAGE_CROP_THEME, imagePath, WeacConstants.DIY_WALLPAPER_PATH);
                        break;
                    // 扫码
                    case 1:
                        Intent intent = new Intent();
                        intent.putExtra(WeacConstants.IMAGE_URL, imagePath);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                        overridePendingTransition(0, R.anim.zoomout);
                        break;
                    // 造码
                    case 2:
                        cropImage(1, REQUEST_IMAGE_CROP_QRCODE_LOGO, imagePath, WeacConstants.DIY_QRCODE_LOGO_PATH);
                        break;
                }
            }
        });
    }

    private void cropImage(int type, int requestType, String sourcePath, String savePath) {
        Uri imageUri = Uri.fromFile(new File(sourcePath));
        Intent intent = MyUtil.getCropImageOptions(this, imageUri, savePath, type);
        startActivityForResult(intent, requestType);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            overridePendingTransition(0, R.anim.zoomout);
            return;
        }
        switch (requestCode) {
            // 截图(选取主题)
            case REQUEST_IMAGE_CROP_THEME:
                String filePath = MyUtil.getFilePath(this, WeacConstants.DIY_WALLPAPER_PATH);
                // 保存壁纸信息
                MyUtil.saveWallpaper(this, WeacConstants.WALLPAPER_PATH, filePath);
                // 发送壁纸更新事件
                OttoAppConfig.getInstance().post(new WallpaperEvent());
                // 发送关闭【本地相册activity】事件
                OttoAppConfig.getInstance().post(new FinishLocalAlbumActivityEvent());
                finish();
                overridePendingTransition(0, R.anim.zoomout);
                break;
            // 截图(选取二维码图片)
            case REQUEST_IMAGE_CROP_QRCODE_LOGO:
                String logoPath = MyUtil.getFilePath(this, WeacConstants.DIY_QRCODE_LOGO_PATH);
                // 保存自定义二维码logo地址
                MyUtil.saveQRcodeLogoPath(this, logoPath);
                // 发送自定义二维码logo截取地址事件
                OttoAppConfig.getInstance().post(new QRcodeLogoEvent(logoPath));
                // 发送关闭【本地相册activity】事件
                OttoAppConfig.getInstance().post(new FinishLocalAlbumActivityEvent());

                finish();
                overridePendingTransition(0, R.anim.move_out_bottom);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        finish();
    }
}