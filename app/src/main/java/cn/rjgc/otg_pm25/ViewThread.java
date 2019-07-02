package cn.rjgc.otg_pm25;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

import cn.rjgc.otg_pm25.utils.CameraPreview;

public class ViewThread extends Thread {
    private CameraPreview mPanel;
    private SurfaceHolder mHolder;
    private boolean mRun = false;
    private long mStartTime;
    private long mElapsed;
    
    public ViewThread(CameraPreview panel) {
        mPanel = panel;
        mHolder = mPanel.getHolder();
    }
    
    public void setRunning(boolean run) {
        mRun = run;
    }
    
    @Override
    public void run() {
        Canvas canvas = null;
        mStartTime = System.currentTimeMillis();
        while (mRun) {
            canvas = mHolder.lockCanvas();
            if (canvas != null) {
                mPanel.doDraw(mElapsed, canvas);
                mElapsed = System.currentTimeMillis() - mStartTime;
                mHolder.unlockCanvasAndPost(canvas);
            }
            mStartTime = System.currentTimeMillis();
        }
    }
}