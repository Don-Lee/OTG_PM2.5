package cn.rjgc.otg_pm25;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.rjgc.otg_pm25.utils.AppConstant;
import cn.rjgc.otg_pm25.utils.BitmapUtils;
import cn.rjgc.otg_pm25.utils.CameraPreview;
import cn.rjgc.otg_pm25.utils.SystemUtils;

public class CameraActivity extends AppCompatActivity {

    private RelativeLayout mRelativeLayout;
//    private RelativeLayout mRL;
    private Camera mCamera;
    private CameraPreview mPreview;
    private int mCameraId = 0;//摄像头ID,前置为1，后置为0

    private TextView tvPMTip;
    private TextView tvPM25;
    private TextView tvCurrTime;
    private String pm25="42";
    private String pm25Level="Good";
    private String mCurrTime="2017.1.1 12:00";

    //屏幕宽高
    private int screenWidth;
    private int screenHeight;
    private FrameLayout flPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Intent intent = getIntent();
        pm25 = intent.getStringExtra("pm25");
        pm25Level = (intent.getStringExtra("pm25Level") == null ? "  优"
                : intent.getStringExtra("pm25Level"));
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        initView();
        mCamera = getCamera(mCameraId);
        mPreview = new CameraPreview(this, mCamera);
        flPreview = (FrameLayout) findViewById(R.id.camera_preview);
        flPreview.addView(mPreview);
    }

    private void initView() {
        tvPMTip = (TextView) findViewById(R.id.pm_tip);
        tvPM25 = (TextView) findViewById(R.id.tv_pm25);
        tvCurrTime = (TextView) findViewById(R.id.tv_curr_time);
        tvPMTip.setText("PM2.5" + pm25Level);
        tvPM25.setText(pm25);
        mCurrTime=getTime();
        tvCurrTime.setText(mCurrTime);
        mRelativeLayout = (RelativeLayout) findViewById(R.id.rl_custom);
        mRelativeLayout.getBackground().setAlpha(150);
    }

    public void captrue(View view) {
        mCamera.takePicture(null,null,new Camera.PictureCallback(){

            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                //将data 转换为位图 或者你也可以直接保存为文件使用 FileOutputStream
                //这里我相信大部分都有其他用处把 比如加个水印 后续再讲解
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                Bitmap saveBitmap = setTakePicktrueOrientation(mCameraId, bitmap);
//                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
//                    Toast.makeText(CameraActivity.this, "竖屏", Toast.LENGTH_SHORT).show();

//                }

//                saveBitmap = Bitmap.createScaledBitmap(saveBitmap, screenWidth, saveBitmap.getHeight(), true);
                /*Bitmap bitmap1 = getScreenPhoto(mRelativeLayout);
                Bitmap bitmap2 = createWatermark(saveBitmap, bitmap1, 100);*/

                Bitmap bitmap2 = createWatermark(saveBitmap, null, 100);
                //File.separator表示分隔符"\"
                String img_path = getExternalFilesDir(Environment.DIRECTORY_DCIM).getPath() +
                        File.separator + System.currentTimeMillis() + ".jpeg";
                BitmapUtils.saveJPGE_After(CameraActivity.this, bitmap2, img_path, 100);
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }

                if (!saveBitmap.isRecycled()) {
                    saveBitmap.recycle();
                }
                /*if (!bitmap1.isRecycled()) {
                    bitmap1.recycle();
                }*/
                if (!bitmap2.isRecycled()) {
                    bitmap2.recycle();
                }
//                Toast.makeText(CameraActivity.this, "ok", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra(AppConstant.KEY.IMG_PATH, img_path);
                setResult(AppConstant.RESULT_CODE.RESULT_OK, intent);
                finish();

            }
        });
    }

    /**
     * 截屏，这里就是截屏的地方了，我这里是截屏RelativeLayout，
     * 只要你将需要的信息放到这个RelativeLayout里面去就可以截取下来了
     *
     * @param waterPhoto waterPhoto
     * @return Bitmap
     */
    public Bitmap getScreenPhoto(RelativeLayout waterPhoto) {
        View view = waterPhoto;
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        int width = view.getWidth();
        int height = view.getHeight();
        Log.e("TEST", width + "--" + height + "--" + view.getWidth());

        Bitmap bitmap1 = null;
        try {
            bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), height);
            view.destroyDrawingCache();
        } catch (Exception e) {
            Log.e("TEST", e.getMessage());
        }
        bitmap = null;
        return bitmap1;
    }
    /**
     * 添加水印
     *
     * @param src       原图
     * @param watermark 水印图片
     * @param alpha 透明度
     * @return bitmap      打了水印的图
     */
    public Bitmap createWatermark(Bitmap src, Bitmap watermark, int alpha) {
        if (src == null) {
            return null;
        }

        // 获取图片的宽高
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        //创建一个和图片一样大小的背景图
//        Bitmap newb = Bitmap.createBitmap(screenWidth, srcHeight, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(src);
        //画背景图
        cv.drawBitmap(src, 0, 0, null);

        //创建背景画笔
        Paint mBGPaint = new Paint();
        mBGPaint.setAntiAlias(true);
        mBGPaint.setColor(Color.argb(160,103,102,99));
        //绘制矩形
        cv.drawRect(0, srcHeight - SystemUtils.dp2px(this,400), srcWidth,
                srcHeight - SystemUtils.dp2px(this,80), mBGPaint);

        //创建文字画笔
        Paint mTextPaint = new Paint();
        // 获取屏幕的密度，用于设置文本大小
//        float scale =  getResources().getDisplayMetrics().scaledDensity;
//        Toast.makeText(this, "scale:" + scale, Toast.LENGTH_SHORT).show();

        int pmTipX;
        int pmTipY;
        int pmLevelX;
        int pmLevelY;
        int addressX;
        int addressY;
        int pm25X;
        int pm25Y;
        int timeX;
        int timeY;
        int dataFromX;
        int dataFromY;
        // 水印的字体大小
        if (mCameraId == 1) {
            pmTipX = SystemUtils.dp2px(this,40);
            pmTipY = srcHeight - SystemUtils.dp2px(this,320);

            pmLevelX = SystemUtils.dp2px(this, 160);
            pmLevelY = srcHeight - SystemUtils.dp2px(this, 320);

            addressX = srcWidth - SystemUtils.dp2px(this,300);
            addressY = srcHeight - SystemUtils.dp2px(this,320);

            pm25X = SystemUtils.dp2px(this,70);
            pm25Y = srcHeight-SystemUtils.dp2px(this,200);

            timeX = srcWidth - SystemUtils.dp2px(this,310);
            timeY = srcHeight - SystemUtils.dp2px(this,220);

            dataFromX = srcWidth - SystemUtils.dp2px(this,360);
            dataFromY = srcHeight - SystemUtils.dp2px(this,130);
            mTextPaint.setTextSize(SystemUtils.sp2px(this,35));
        }else {
            pmTipX = SystemUtils.dp2px(this,60);
            pmTipY = srcHeight - SystemUtils.dp2px(this,320);

            pmLevelX = SystemUtils.dp2px(this,200);
            pmLevelY = srcHeight - SystemUtils.dp2px(this,320);

            addressX = srcWidth - SystemUtils.dp2px(this,350);
            addressY = srcHeight - SystemUtils.dp2px(this,320);

            pm25X = SystemUtils.dp2px(this,90);
            pm25Y = srcHeight-SystemUtils.dp2px(this,200);

            timeX = srcWidth - SystemUtils.dp2px(this,360);
            timeY = srcHeight - SystemUtils.dp2px(this,220);

            dataFromX = srcWidth - SystemUtils.dp2px(this,420);
            dataFromY = srcHeight - SystemUtils.dp2px(this,130);
            mTextPaint.setTextSize(SystemUtils.sp2px(this,45));
        }

//        mTextPaint.setTextSize(150);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        //文字坐标


        cv.drawText("PM2.5", pmTipX, pmTipY, mTextPaint);
        cv.drawText(pm25Level, pmLevelX, pmLevelY, mTextPaint);
        cv.drawText("上海市 闵行区", addressX, addressY, mTextPaint);
        cv.drawText(pm25, pm25X, pm25Y, mTextPaint);
        cv.drawText(mCurrTime, timeX, timeY, mTextPaint);
        cv.drawText("数据来源：EAWADA", dataFromX, dataFromY, mTextPaint);

        /*int ww = watermark.getWidth();
        int wh = watermark.getHeight();
        Paint paint=new Paint();
        paint.setAlpha(alpha);
        paint.setAntiAlias(true);
        cv.drawBitmap(watermark, 0, srcHeight/2, paint);
        */
//        cv.save(Canvas.ALL_SAVE_FLAG);
        cv.save();
        cv.restore();
        return src;
    }

    public Bitmap setTakePicktrueOrientation(int id, Bitmap bitmap) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        bitmap = rotaingImageView(id, info.orientation, bitmap);
        return bitmap;
    }
    /**
     * 把相机拍照返回照片转正
     *
     * @param angle 旋转角度
     * @return bitmap 图片
     */
    public  Bitmap rotaingImageView(int id, int angle, Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        //加入翻转 把相机拍照返回照片转正
        if (id == 1) {
            matrix.postScale(-1, 1);
        }

        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }

    private String getTime() {
        SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd HH:mm ");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        return formatter.format(curDate);
    }
    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

    public void switchCamera(View view) {
        try {

//            Camera.getCameraInfo(mCameraId, cameraInfo);// 得到每一个摄像头的信息
//            if (mCameraId == 1) {
//
//            }
            releaseCamera();
            mCameraId = (mCameraId + 1) % mCamera.getNumberOfCameras();
            mCamera = getCamera(mCameraId);
            mPreview = new CameraPreview(this, mCamera);
            flPreview.removeAllViews();
            flPreview.addView(mPreview);
        } catch (Exception e) {
            if(BuildConfig.DEBUG){
                Log.e("TAG", e.getMessage());
            }
        }
    }
    /**
     * 获取Camera实例
     *
     * @return
     */
    private Camera getCamera(int id) {
        Camera camera = null;
        try {
            camera = Camera.open(id);
        } catch (Exception e) {

        }
        return camera;
    }
    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mPreview = null;

        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        /*if (mCamera != null) {
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
        }*/
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }
}
