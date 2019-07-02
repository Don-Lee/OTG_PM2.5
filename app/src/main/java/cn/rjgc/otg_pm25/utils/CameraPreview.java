package cn.rjgc.otg_pm25.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import cn.rjgc.otg_pm25.BuildConfig;

/**
 * Created by Don on 2017/1/3.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "SurfaceView";
    private Activity mCameraActivity;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;

    private int mElementNumber = 0;

    private Paint mPaint = new Paint();
    private String mScreenshotPath = Environment.getExternalStorageDirectory() + "/droidnova";

    public CameraPreview(Activity activity, Camera camera) {
        super(activity);
        mCameraActivity = activity;
        mCamera = camera;
        mSurfaceHolder = getHolder();//获得句柄
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//surfaceview不维护自己的缓冲区，等待屏幕渲染引擎将内容推送到用户面前
        mSurfaceHolder.setKeepScreenOn(true);// 屏幕常亮
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.setDisplayOrientation(getPreviewDegree(mCameraActivity));
            mCamera.startPreview();
        } catch (IOException e) {
            if(BuildConfig.DEBUG){
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mSurfaceHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        //在改变surface前要先停止预览。
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
            //停止一个不存在的预览。
        }
        // set preview size and make any resize, rotate or
        // reformatting changes here设置预览大小和作任何调整大小，旋转或重新设置格式修改
        // start preview with new settings
        //开始预览新设置。
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception e){
            if(BuildConfig.DEBUG){
                Log.e(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }


    /**
     * 用于根据手机方向获得相机预览画面旋转的角度
     *
     * @param activity
     * @return
     */
    public int getPreviewDegree(Activity activity) {
        // 获得手机的方向
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degree = 0;
        // 根据手机的方向计算相机预览画面应该选择的角度
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 90;
                break;
            case Surface.ROTATION_90:
                degree = 0;
                break;
            case Surface.ROTATION_180:
                degree = 270;
                break;
            case Surface.ROTATION_270:
                degree = 180;
                break;
        }
        return degree;
    }


    /**
     * If called, creates a screenshot and saves it as a JPG in the folder "droidnova" on the sdcard.
     */
    public void saveScreenshot() {
        if (ensureSDCardAccess()) {
            Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            doDraw(1, canvas);
            File file = new File(mScreenshotPath + "/" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.e("Panel", "FileNotFoundException", e);
            } catch (IOException e) {
                Log.e("Panel", "IOEception", e);
            }
        }
    }

    public void doDraw(long elapsed, Canvas canvas) {
        canvas.drawColor(Color.BLACK);

        canvas.drawText("FPS: " + Math.round(1000f / elapsed) + " Elements: " + mElementNumber, 10,
                10, mPaint);
    }

    /**
     * Helper method to ensure that the given path exists.
     * TODO: check external storage state
     */
    private boolean ensureSDCardAccess() {
        File file = new File(mScreenshotPath);
        if (file.exists()) {
            return true;
        } else if (file.mkdirs()) {
            return true;
        }
        return false;
    }
}
