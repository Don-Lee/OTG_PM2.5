package cn.rjgc.otg_pm25;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.rjgc.otg_pm25.utils.SystemUtils;

/**
 * Created by Don on 2016/12/21.
 */

public class CustomLine extends View {

    private Context mContext;
    MainActivity mainActivity = new MainActivity();
    //坐标轴原点位置
    private int mXPoint = SystemUtils.dp2px(getContext(),30);
    private int mYPoint = SystemUtils.dp2px(getContext(),275);
    //刻度长度
    private int mXScale = SystemUtils.dp2px(getContext(),30);//30个单位构成一个刻度
    private int mYScale = SystemUtils.dp2px(getContext(),25);
    private int mYBaseNumber = 20;//Y轴刻度扩大倍数
    //x与y坐标轴长度
//    private int mXLength = 850;
    private int mXLength = SystemUtils.dp2px(getContext(),290);
    private int mYLength = SystemUtils.dp2px(getContext(),275);
    public int mMaxDataSize = mXLength / mXScale;//横坐标，最多可绘制的点
    public List<Integer> mData = new ArrayList<>();//存放纵坐标所描绘的点
//    public List<Double> mData = new ArrayList<>();//存放纵坐标所描绘的点
    public int mYMaxNum = mYLength / mYScale;
    private String[] mYLabel = new String[mYMaxNum];//y轴刻度上显示的文字集合
    public List<String> mXLabel = new ArrayList<>();//轴X刻度上显示的文字集合

    //设置view的宽度和高度
    int mWidth = mXLength + 50;
    int mHeight = mYLength + 50;

    private Long curTime;
    private String time;
    private SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                CustomLine.this.invalidate();//刷新View
            }
        }
    };

    public CustomLine(Context context) {
        super(context);
        mContext = context;
    }

    public CustomLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        for (int i = 0; i < mYLabel.length; i++) {
           /* if (i < 5) {
                mYLabel[i] = " "+(i * mYBaseNumber);
            } else {*/
                mYLabel[i] = (i * mYBaseNumber) + "";
//            }

        }

       /* new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {//在线程中不断往集合中添加数据
                    try {
                        Thread.sleep(5000);//睡眠5s
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mData.size() > mMaxDataSize) {//判断集合长度是否大于最大绘制长度
                        mData.remove(0);//删除头数据
                        mXLabel.remove(0);//删除头数据
                    }

                    mData.add(new Random().nextInt(5) + 1);//生成1-6的随机数

                   *//* String s = mainActivity.tvInfo.getText().toString();
                    if (!"".equals(s) && s != null) {
//                        mData.add(Integer.parseInt(s));
                        mData.add(new Random().nextInt(5) + 1);
                    }*//*

                    //获取当前时间
                    curTime = System.currentTimeMillis();
                    time = sdf.format(curTime);
                    mXLabel.add(time);
                    mHandler.sendEmptyMessage(0);//发送空消息通知刷新
                }
            }
        }).start();*/
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);//获得view宽度的specMode
        int widthSpceSize = MeasureSpec.getSize(widthMeasureSpec);//获得view的宽度
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);//获得view高度的specMode
        int heightSpceSize = MeasureSpec.getSize(heightMeasureSpec);//获得view的高度

        //手动处理View的宽或高为wrap_content的情况；
        // 不理解的话可以查看http://blog.csdn.net/lfdfhl/article/details/51347818
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mWidth, mHeight);
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mWidth, heightSpceSize);
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpceSize, mHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint mDataPaint = new Paint();
        mDataPaint.setStyle(Paint.Style.STROKE);
        mDataPaint.setAntiAlias(true);
        mDataPaint.setColor(Color.WHITE);
        mDataPaint.setStrokeWidth(3);

        Paint mTimePaint = new Paint();
        mTimePaint.setStyle(Paint.Style.STROKE);
        mTimePaint.setAntiAlias(true);
        mTimePaint.setColor(Color.WHITE);
        mTimePaint.setTextSize(20);

        Paint mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStrokeWidth(3);
        mLinePaint.setColor(Color.WHITE);
        mLinePaint.setTextSize(40);
        //绘制Y轴
        canvas.drawLine(mXPoint, mYPoint - mYLength, mXPoint, mYPoint, mLinePaint);
        //绘制Y轴左右两边的箭头
        canvas.drawLine(mXPoint, mYPoint - mYLength, mXPoint - 5, mYPoint - mYLength + 10, mLinePaint);
        canvas.drawLine(mXPoint, mYPoint - mYLength, mXPoint + 5, mYPoint - mYLength + 10, mLinePaint);

        //Y轴上的刻度与文字
        for (int i = 1; i * mYScale < mYLength; i++) {
            //刻度
            canvas.drawLine(mXPoint, mYPoint - i * mYScale, mXPoint + 5, mYPoint - i * mYScale, mLinePaint);
            //文字
//            canvas.drawText(mYLabel[i], mXPoint - 90, mYPoint - i * mYScale, mLinePaint);
            canvas.drawText(mYLabel[i], mXPoint - 65, mYPoint - i * mYScale, mLinePaint);
        }

        //X轴
        canvas.drawLine(mXPoint, mYPoint, mXPoint + mXLength, mYPoint, mLinePaint);
        //绘制X轴左右两边的箭头
        canvas.drawLine(mXPoint + mXLength, mYPoint, mXPoint + mXLength - 10, mYPoint - 5, mLinePaint);
        canvas.drawLine(mXPoint + mXLength, mYPoint, mXPoint + mXLength - 10, mYPoint + 5, mLinePaint);
        //X轴上的刻度
        for (int i = 0; i * mXScale < mXLength; i++) {
            //刻度
            canvas.drawLine(mXPoint + i * mXScale, mYPoint, mXPoint + i * mXScale, mYPoint - 5, mLinePaint);
        }
        //X轴上的时间
        for (int i = 0; i < mXLabel.size(); i++) {
            //时间
            canvas.drawText(mXLabel.get(i), mXPoint + i * mXScale - 40, mYPoint + 30, mTimePaint);
        }

        drawDatas(canvas, mDataPaint);
//        drawDatasAndColor(canvas, mDataPaint);
//        drawDatas_Color_Stroke(canvas, mDataPaint);
    }

    //仅仅绘制数据
    private void drawDatas(Canvas canvas, Paint mPaint) {
        //如果集合中有数据
        if (mData.size() > 1) {
            //方式一drawLine
            /*for (int i = 1; i < mData.size(); i++) {
                //依次去除数据进行绘制
                canvas.drawLine(mXPoint+(i-1)*mXScale,mYPoint-mData.get(i-1)*mYScale,mXPoint+i*mXScale,mYPoint-mData.get(i)*mYScale,mPaint);
            }*/
            //方式二drawPath
            Path path = new Path();
            canvas.drawCircle(mXPoint, mYPoint - (mData.get(0) * mYScale)/mYBaseNumber,1,mPaint);
            path.moveTo(mXPoint, mYPoint - (mData.get(0) * mYScale)/mYBaseNumber);//起点
//            Log.e("TEST0", (mData.get(0) * mYScale)/mYBaseNumber+ "mData0"+mData.get(0));
            for (int i = 1; i < mData.size(); i++) {
                canvas.drawCircle(mXPoint + i * mXScale, mYPoint - (mData.get(i) * mYScale)/mYBaseNumber,1,mPaint);
                path.lineTo(mXPoint + i * mXScale, mYPoint - (mData.get(i) * mYScale)/mYBaseNumber);
//                Log.e("TEST"+i, (mData.get(i) * mYScale)/mYBaseNumber+ "mData0"+mData.get(i));
            }
            canvas.drawPath(path, mPaint);
        }
    }

    //绘制数据并填充颜色
    private void drawDatasAndColor(Canvas canvas, Paint mPaint) {
        //填充效果
        mPaint.setStyle(Paint.Style.FILL);
        if (mData.size() > 1) {
            Path path = new Path();
            path.moveTo(mXPoint, mYPoint);
            for (int i = 0; i < mData.size(); i++) {
                path.lineTo(mXPoint + i * mXScale, mYPoint - mData.get(i) * mYScale/mYBaseNumber);
            }
            path.lineTo(mXPoint + (mData.size() - 1) * mXScale, mYPoint);
            canvas.drawPath(path, mPaint);
        }
    }

    //绘制数据、填充颜色、并描边
    private void drawDatas_Color_Stroke(Canvas canvas, Paint mPaint) {
        mPaint.setStrokeWidth(5);
        Paint mPaint2 = new Paint();
        mPaint2.setColor(Color.BLUE);
        mPaint2.setStyle(Paint.Style.FILL);
        if (mData.size() > 1) {
            Path path = new Path();
            Path path2 = new Path();
            path.moveTo(mXPoint, mYPoint - mData.get(0) * mYScale);
            path2.moveTo(mXPoint, mYPoint);
            for (int i = 0; i < mData.size(); i++) {
                path.lineTo(mXPoint + i * mXScale, mYPoint - mData.get(i) * mYScale);
                path2.lineTo(mXPoint + i * mXScale, mYPoint - mData.get(i) * mYScale);
            }
            path2.lineTo(mXPoint + (mData.size() - 1) * mXScale, mYPoint);
            canvas.drawPath(path, mPaint);
            canvas.drawPath(path2, mPaint2);
        }
    }
}
