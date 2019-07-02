package cn.rjgc.otg_pm25;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;

import cn.rjgc.otg_pm25.utils.AppConstant;
import cn.rjgc.otg_pm25.utils.CrashHandler;
import cn.rjgc.otg_pm25.utils.MyApplication;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_DEVICE_PERMISSION = "com.linc.USB_PERMISSION";
    private static final String TAG = "MainActivity";
    private static final int USB_CLASS_HID = 3;
    private static final int PM25_VENDOR_ID = 1155;
    private static final int PM25_PRODUCT_ID = 22352;
    private static final int HCHO_VENDOR_ID = 1155;
    private static final int HCHO_PRODUCT_ID = 22368;

    //USB管理器：负责管理USB设备的类
    private UsbManager mUsbManager;
    //找到的USB设备
    private UsbDevice mUsbDevice;
    //代表USB设备的一个接口
    private UsbInterface mInterface;
    private HashMap<String, UsbDevice> deviceList;

    private UsbDeviceConnection myDeviceConnection;
    public TextView tvPM25;
    public TextView tvPollue;
    public TextView tvUnits;

    private UsbEndpoint epOut;
    private UsbEndpoint epIn;

    private int mVendorID = 0;    //这里要改成自己硬件的厂商ID
    private int mProductID = 0;

    private PendingIntent mPermissionIntent;
//    private byte[] buffer = new byte[6];
    private byte[] buffer = new byte[64];
    private boolean mIsLoop = true;

    private CustomLine customLine;//折线
    private Long curTime;
    private String time;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    OrientationEventListener mScreenOrientationEventListener;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null) {
                Bundle bundle = msg.getData();
                String type = bundle.getString("type");
                String data = bundle.getString("data");
                String refresh = bundle.getString("refresh");
                if (!"".equals(data) && data != null) {
                    //                tvPM25.setText(bundle.getString("pm25") + new Date().toString());
                    try {
                        int p25 = Integer.parseInt(data);
                        if (type.equals("pm25")) {
                            tvPM25.setText(data);
                            if (p25 < 35) {
                                tvPollue.setText("优");
                            } else if (p25 < 70) {
                                tvPollue.setText("良");
                            } else if (p25 < 100) {
                                tvPollue.setText("轻度污染");
                            } else if (p25 < 150) {
                                tvPollue.setText("中度污染");
                            } else if (p25 < 200) {
                                tvPollue.setText("重度污染");
                            } else {
                                tvPollue.setText("严重污染");
                            }
                        } else {
                            double num = 30 / 25 * (p25 / 100.0);
//                            Toast.makeText(MainActivity.this, num + "", Toast.LENGTH_SHORT).show();
                            DecimalFormat df = new DecimalFormat("#.000");
                            double result=Double.parseDouble(df.format(num));
                            tvPM25.setText(result+"");
                            if (result <= 0.03) {
                                tvPollue.setText("优");
                            } else if (result > 0.03 && result <= 0.08) {
                                tvPollue.setText("良");
                            } else if (result > 0.08 && result <= 0.3) {
                                tvPollue.setText("轻度污染");
                            } else if (result > 0.3 && result <= 0.8) {
                                tvPollue.setText("重度污染");
                            } else {
                                tvPollue.setText("极度污染");
                            }
                        }

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                }
                if ("refresh".equals(refresh)) {
                    customLine.invalidate();//刷新view
                }

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check storage permissions       6.0以上版本必须如此申请权限
        MyApplication.verifyStoragePermissions(MainActivity.this);

        customLine = (CustomLine) findViewById(R.id.custom_line);
        /*if (mVendorID == 0) {
            readXml();//读取xml数据
        }*/

        checkScreenChange();
        tvPM25 = (TextView) findViewById(R.id.tv_pm25);
        tvPollue = (TextView) findViewById(R.id.tv_pollue);
        tvUnits = (TextView) findViewById(R.id.data_units);


        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        //注册广播
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_DEVICE_PERMISSION), 0);
        IntentFilter permissionFilter = new IntentFilter();
        permissionFilter.addAction(ACTION_DEVICE_PERMISSION);
        permissionFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        permissionFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver,permissionFilter);
        enumerateDevice();
        drawLine();


        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);  //传入参数必须为Activity，否则AlertDialog将不显示。
        // 创建错误
//        throw new NullPointerException();
    }
    private void checkScreenChange(){
        this.mScreenOrientationEventListener = new OrientationEventListener(MainActivity.this) {
            @Override
            public void onOrientationChanged(int i) {
                // i的范围是0～359
                // 屏幕左边在顶部的时候 i = 90;
                // 屏幕顶部在底部的时候 i = 180;
                // 屏幕右边在底部的时候 i = 270;
                // 正常情况默认i = 0;

                if (i == 180) {
//
                    Toast.makeText(MainActivity.this, "反了"+getRequestedOrientation(), Toast.LENGTH_SHORT).show();
                    if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                    {
                        Toast.makeText(MainActivity.this, "正", Toast.LENGTH_SHORT).show();
//                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    } else if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                        Toast.makeText(MainActivity.this, "反方向", Toast.LENGTH_SHORT).show();
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } /*else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }*/

                }
                /*if(45 <= i && i < 135) {
                    mScreenExifOrientation = ExifInterface.ORIENTATION_ROTATE_180;
                } else if(135 <= i && i < 225) {
                    mScreenExifOrientation = ExifInterface.ORIENTATION_ROTATE_270;
                } else if(225 <= i && i < 315) {
                    mScreenExifOrientation = ExifInterface.ORIENTATION_NORMAL;
                } else {
                    mScreenExifOrientation = ExifInterface.ORIENTATION_ROTATE_90;
                }*/
            }
        };
    }

    //读取自定义xml
    private void readXml(){
        XmlResourceParser xmlp= getResources().getXml(R.xml.device_filter);
        try {
            while (xmlp.getEventType()!=XmlResourceParser.END_DOCUMENT) {
                if(xmlp.getEventType()==XmlResourceParser.START_TAG){
                    if(xmlp.getName().equals("usb-device")){
                        String vid = xmlp.getAttributeValue(null, "vendor-id");
                        if (vid != null) {
                            mVendorID = (Integer.parseInt(vid));
                        }
                        String pid=xmlp.getAttributeValue(null, "product-id");
                        if (pid != null) {
                            mProductID = (Integer.parseInt(pid));
                        }
                    }
                }
                xmlp.next();
            }

        } catch (IOException e) {
            Log.e("TAG",e.toString());
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            Log.e("TAG1",e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 分配端点，IN | OUT，即输入输出；此处我直接用1为OUT端点，0为IN，当然你也可以通过判断
     */

    //USB_ENDPOINT_XFER_BULK 
     /*         	 
     #define USB_ENDPOINT_XFER_CONTROL 0   --控制传输
     #define USB_ENDPOINT_XFER_ISOC  1     --等时传输
     #define USB_ENDPOINT_XFER_BULK  2     --块传输
     #define USB_ENDPOINT_XFER_INT   3     --中断传输   
     * */
    private void assignEndpoint() {

        if (mInterface != null) { //这一句不加的话 很容易报错  导致很多人在各大论坛问:为什么报错呀 
            //这里的代码替换了一下 按自己硬件属性判断吧
            for (int i = 0; i < mInterface.getEndpointCount(); i++) {
                UsbEndpoint ep = mInterface.getEndpoint(i);
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                    if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                        epOut = ep;
                    } else {
                        epIn = ep;
                    }
                    final int inMax = epIn.getMaxPacketSize();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            while (mIsLoop) {
                                    try {
                                        int ncount = myDeviceConnection.bulkTransfer(epIn, buffer, inMax,
                                                100);
                                        if (ncount != -1) {
//                                            String s= bytesToHexString(buffer);//转16进制
                                            Message msg = new Message();
                                            Bundle bundle = new Bundle();
                                            int datas;
                                            Log.e(TAG, "run: "+mProductID);
                                            if (mProductID == PM25_PRODUCT_ID) {
                                                datas = (buffer[18]&0xff) * 256 + (buffer[19]&0xff);//byte转int?
                                                bundle.putString("type","pm25");
                                            }else {
                                                datas = (buffer[36] & 0xff) * 256 + (buffer[37] & 0xff);//byte转int?
                                                bundle.putString("type","hcho");
                                            }
                                            Log.e(TAG, "run: "+datas );

                                            bundle.putString("data",datas+"");
                                            msg.setData(bundle);
                                            mHandler.sendMessage(msg);
                                        }else {
                                            Log.e(TAG, ncount + "");
                                        }
                                        Thread.sleep(1000);//睡眠1s
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Log.e("TAG", e.getMessage()+"-------");
                                    }
//                                }
                            }
                        }
                    }).start();
                }else {
                    Toast.makeText(this, "无法获取数据", Toast.LENGTH_SHORT).show();
                }
            }


        }
    }


    /**
     * 更新折线图
     */
    private void drawLine(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {//在线程中不断往集合中添加数据
                    try {
                        Thread.sleep(5000);//睡眠5s
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (customLine.mData.size() > customLine.mMaxDataSize) {//判断集合长度是否大于最大绘制长度
                        customLine.mData.remove(0);//删除头数据
                        customLine.mXLabel.remove(0);//删除头数据
                    }

//                    customLine.mData.add(new Random().nextInt(5) + 1);//生成1-6的随机数
                    String s = tvPM25.getText().toString();
                    if (!"".equals(s) && s != null) {
                        try {
                            customLine.mData.add(Integer.parseInt(s));
                        } catch (NumberFormatException e) {
                            customLine.mData.add(0);
                        }
                    }
                    //获取当前时间
                    curTime = System.currentTimeMillis();
                    time = sdf.format(curTime);
                    customLine.mXLabel.add(time);
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString("refresh","refresh");
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);//发送空消息通知刷新
                }
            }
        }).start();
    }

    /**
     * byte数组转16进制
     * @param src
     * @return
     */
    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv+" ");
        }
        return stringBuilder.toString();
    }
    /**
     * 打开设备
     */
    private void openDevice() {
        if (mInterface != null) {
            UsbDeviceConnection conn = null;
            // 在open前判断是否有连接权限；对于连接权限可以静态分配，也可以动态分配权限，可以查阅相关资料
            if (mUsbManager.hasPermission(mUsbDevice)) {
                conn = mUsbManager.openDevice(mUsbDevice);
            }else {
                mUsbManager.requestPermission(mUsbDevice,mPermissionIntent);
                Toast.makeText(this, "无连接权限", Toast.LENGTH_SHORT).show();
            }

            if (conn == null) {
                return;
            }

            if (conn.claimInterface(mInterface, true)) {
                myDeviceConnection = conn; // 到此你的android设备已经连上HID设备
//                Toast.makeText(this, "打开设备成功", Toast.LENGTH_SHORT).show();
                Log.e("TAG", "打开设备成功");
                assignEndpoint();
            } else {
                conn.close();
            }
        }else {
            if(BuildConfig.DEBUG){
                Toast.makeText(this, "mInterface为null", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 找设备接口
     */
    private void findInterface() {
        if (mUsbDevice != null) {
            Toast.makeText(this, "interfaceCounts : " + mUsbDevice.getInterfaceCount(), Toast.LENGTH_SHORT).show();
            for (int i = 0; i < mUsbDevice.getInterfaceCount(); i++) {
                UsbInterface intf = mUsbDevice.getInterface(i);
                // 根据手上的设备做一些判断，其实这些信息都可以在枚举到设备时打印出来
                if (intf.getInterfaceClass() == USB_CLASS_HID
                        && intf.getInterfaceSubclass() == 0
                        && intf.getInterfaceProtocol() == 0) {
                    mInterface = intf;
                    if(BuildConfig.DEBUG){
                        Toast.makeText(this, "找到我的设备接口", Toast.LENGTH_SHORT).show();
                        Log.e("TAG", "找到我的设备接口");
                    }
                    openDevice();
                }
                break;
            }
        }
        else {
            Toast.makeText(this, "没有找到设备", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 枚举设备
     */
    private void enumerateDevice() {
        if (mUsbManager == null) {
            Toast.makeText(this, "mUsbManager为空", Toast.LENGTH_SHORT).show();
            return;
        }
        deviceList = mUsbManager.getDeviceList();
        if (!deviceList.isEmpty()) { // deviceList不为空
//            deviceInfo();//设备信息
            Toast.makeText(this, "枚举设备中", Toast.LENGTH_SHORT).show();
            for (UsbDevice device : deviceList.values()) {
                // 枚举到设备
                if (device.getVendorId() == PM25_VENDOR_ID &&
                        device.getProductId() == PM25_PRODUCT_ID) {

                } else if (device.getVendorId() == HCHO_VENDOR_ID &&
                        device.getProductId() == HCHO_PRODUCT_ID) {
                    tvUnits.setText("mg/m³");
                } else {
                    Toast.makeText(this, "枚举失败"+device.getVendorId()+"---"+device.getProductId(), Toast.LENGTH_SHORT).show();
                    return;
                }

                mVendorID = device.getVendorId();
                mProductID = device.getProductId();

                mUsbDevice = device;
                if(BuildConfig.DEBUG){
                    Toast.makeText(this, "枚举设备成功", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "枚举设备成功");
                }
                findInterface();
            }
        }else {
            Toast.makeText(this, "请插入设备", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设备信息
     */
    private void deviceInfo() {
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();


        StringBuilder sb = new StringBuilder();
        while (deviceIterator.hasNext()){
            UsbDevice usbDevice = deviceIterator.next();
            sb.append("DeviceName="+usbDevice.getDeviceName()+"\n");
            sb.append("DeviceId="+usbDevice.getDeviceId()+"\n");
            sb.append("VendorId="+usbDevice.getVendorId()+"\n");
            sb.append("ProductId="+usbDevice.getProductId()+"\n");
            sb.append("DeviceClass="+usbDevice.getDeviceClass()+"\n");
            int deviceClass = usbDevice.getDeviceClass();
            if(deviceClass==0) {
                UsbInterface anInterface = usbDevice.getInterface(0);
                int interfaceClass = anInterface.getInterfaceClass();

                sb.append("device Class 为0-------------\n");
                sb.append("Interface.describeContents()="+anInterface.describeContents()+"\n");
                sb.append("Interface.getEndpointCount()="+anInterface.getEndpointCount()+"\n");
                sb.append("Interface.getId()="+anInterface.getId()+"\n");
                //http://blog.csdn.net/u013686019/article/details/50409421
                //http://www.usb.org/developers/defined_class/#BaseClassFFh
                //http://ms.csdn.net/geek/85372
                //通过下面的InterfaceClass 来判断到底是哪一种的，例如7就是打印机，8就是usb的U盘
                sb.append("Interface.getInterfaceClass()="+anInterface.getInterfaceClass()+"\n");
                if(anInterface.getInterfaceClass()==7){
                    sb.append("此设备是打印机\n");
                }else if(anInterface.getInterfaceClass()==8){
                    sb.append("此设备是U盘\n");
                }
                sb.append("anInterface.getInterfaceProtocol()="+anInterface.getInterfaceProtocol()+"\n");
                sb.append("anInterface.getInterfaceSubclass()="+anInterface.getInterfaceSubclass()+"\n");
                sb.append("device Class 为0------end-------\n");
            }


            sb.append("DeviceProtocol="+usbDevice.getDeviceProtocol()+"\n");
            sb.append("DeviceSubclass="+usbDevice.getDeviceSubclass()+"\n");
            sb.append("+++++++++++++++++++++++++++\n");
            sb.append("                           \n");
        }
        TextView textView = (TextView) findViewById(R.id.tv_pm25);
        textView.setText(sb);
    }


    //广播接收器
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BuildConfig.DEBUG){
                Log.e(TAG, "BroadcastReceiver in");
            }
            if (ACTION_DEVICE_PERMISSION.equals(action)) {
//                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            openDevice();
                            if(BuildConfig.DEBUG){
                                Log.e(TAG, "usb EXTRA_PERMISSION_GRANTED");
                            }
                        }
                    } else {
                        if(BuildConfig.DEBUG){
                            Log.e(TAG, "usb EXTRA_PERMISSION_GRANTED null!!!");
                        }
                    }
//                }
            }if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
                if(BuildConfig.DEBUG){
                    Toast.makeText(MainActivity.this, "attached", Toast.LENGTH_SHORT).show();
                }
                mIsLoop = true;
                enumerateDevice();
            }if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
                if(BuildConfig.DEBUG){
                    Toast.makeText(MainActivity.this, "detached", Toast.LENGTH_SHORT).show();
                }
                mIsLoop = false;
            }
            else {
                /*if(BuildConfig.DEBUG){
                    Toast.makeText(MainActivity.this, "没有匹配的广播："+action, Toast.LENGTH_SHORT).show();
                }*/
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mIsLoop = true;
        assignEndpoint();
        if (BuildConfig.DEBUG) {
            Log.e("TAG", "onResume");
            //        Toast.makeText(this, "onResume", Toast.LENGTH_SHORT).show();

        }
        mScreenOrientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsLoop = false;
        if(BuildConfig.DEBUG){
            Log.e("TAG", "onPause");
        }
        mScreenOrientationEventListener.disable();
    }


    public void startCamera(View view) {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        intent.putExtra("pm25", tvPM25.getText().toString());
        intent.putExtra("pmLevel", tvPollue.getText().toString());
        startActivityForResult(intent, AppConstant.REQUEST_CODE.CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != AppConstant.RESULT_CODE.RESULT_OK) {
            return;
        }
        if (requestCode == AppConstant.REQUEST_CODE.CAMERA) {
            String img_path = data.getStringExtra(AppConstant.KEY.IMG_PATH);
            Intent intent = new Intent(MainActivity.this, ShowPicActivity.class);
            intent.putExtra(AppConstant.KEY.IMG_PATH, img_path);
            startActivity(intent);
        }
    }


    public void testENUM(View view) {
        enumerateDevice();
    }
}
