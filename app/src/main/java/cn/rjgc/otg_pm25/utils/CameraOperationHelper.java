package cn.rjgc.otg_pm25.utils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

/**
 * Created by Don on 2016/12/28.
 */

public class CameraOperationHelper {

    private static CameraOperationHelper cameraOperationHelper;

    private Camera mCamera;
    private int mCameraPosition = 0;//0代表前置摄像头,1代表后置摄像头,默认打开前置摄像头
    private int mCameraCount = 0;//获得相机的摄像头数量

    private CameraOperationHelper(){
        mCameraCount = Camera.getNumberOfCameras();

    }
    public static CameraOperationHelper getCameraOperationHelper(){
        if (cameraOperationHelper == null) {
            cameraOperationHelper = new CameraOperationHelper();
        }
        return cameraOperationHelper;
    }
}
