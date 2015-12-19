package shanghai.enrique.com.falco;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends ActionBarActivity {

    private final static String PACKAGE_NAME = "shanghai.enrique.com.falco";
    private final static String TAG = "MainActivity";
    public static String wholeGGAMsg = "";      //保存实时GGA语句，请求差分数据时使用
    public static BluetoothObj mBluetoothObj;
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Todo:
            switch (msg.what) {
                case GlobalConstantValue.MESSAGE_READ:
                    String readMsg = msg.obj.toString();
                    send2UI(readMsg);
                    mArrayAdapter.add(readMsg);
                    break;
                case GlobalConstantValue.MESSAGE_FROM_ASYNCHRONOUSLINK_TO_MAINACTIVITY:
                    String status = (String) msg.obj;
                    if (status == GlobalConstantValue.CONNECTING) {
                        //todo:
                        actionBar.setTitle("正在创建与蓝牙设备的连接，请稍候...");
                    } else if (status == GlobalConstantValue.CONNECTED) {
                        //todo:
                        actionBar.setTitle("成功连接到：" + mBluetoothObj.mDeviceName);
                    } else if (status == GlobalConstantValue.CONNECT_DISCONNECT) {
                        //todo:
                        actionBar.setTitle("已与蓝牙设备断开连接");
                    } else if (status == GlobalConstantValue.CONNECTED_FAILED) {
                        //todo:
                        actionBar.setTitle("连接创建失败，请重试");
                    }
                    break;
                case GlobalConstantValue.MEESAGE_TOAST:
                    Bundle mBundle = msg.getData();
                    Toast.makeText(getApplicationContext(), mBundle.getString(GlobalConstantValue.TOAST), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    ActionBar actionBar;
    private ListView mListView;                           //NMEA全语句显示控件
    private ArrayAdapter<String> mArrayAdapter;           //NMEA语句对应的适配器
    private ListView mDevicesListView;                    //已匹配设备列表控件
    private ArrayAdapter<String> mDevicesAdapter;         //已匹配设备适配器
    private TextView mLatiLongi, mDatetime, mNumber, mAltitude, mQuality, mDgps, mHdop, mHorizontalError, mAltitudeError, mBaseStation;
    private Set<BluetoothDevice> mPairedDevices;
    private AsynchronousLink mAsyncLink;
    private long pressBackKeyTime = 0;      //保存【返回键】按下时间

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //禁止横屏
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actionBar = getSupportActionBar();

        if (null == mBluetoothObj) {
            mBluetoothObj = new BluetoothObj();
            mBluetoothObj.mAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        mListView = (ListView) findViewById(R.id.msg);
        mLatiLongi = (TextView) findViewById(R.id.latiLongi);
        mDatetime = (TextView) findViewById(R.id.dateTime);
        mBaseStation = (TextView) findViewById(R.id.baseStation);
        mHdop = (TextView) findViewById(R.id.hdop);
        mDgps = (TextView) findViewById(R.id.dgps);
        mQuality = (TextView) findViewById(R.id.quality);
        mHorizontalError = (TextView) findViewById(R.id.horizontalError);
        mAltitude = (TextView) findViewById(R.id.altitude);
        mAltitudeError = (TextView) findViewById(R.id.altitudeError);
        mNumber = (TextView) findViewById(R.id.number);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mBluetoothObj.mAdapter.isEnabled()) {
            Intent enableBluetoothAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothAdapter, GlobalConstantValue.ENABLE_BLUETOOTHADAPTER_REQUEST);
        } else {
            mArrayAdapter = new ArrayAdapter<>(this, R.layout.listview_item_msg);
            mListView.setAdapter(mArrayAdapter);

            mPairedDevices = mBluetoothObj.mAdapter.getBondedDevices();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *按钮点击事件 */
    public void onClickEvent(View view) {
        final int viewID = view.getId();
        try {
            switch (viewID) {
                case R.id.getSerial:
                    break;
                case R.id.showDevices:
                    if (mPairedDevices.size() == 0) {
                        //Todo:提示当前没有配对的设备
                        return;
                    } else {
                        mDevicesAdapter = new ArrayAdapter<>(this, R.layout.listview_item_msg);
                        for (BluetoothDevice device : mPairedDevices) {
                            mDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                            //mBluetoothObj.mDeviceName = device.getName();
                        }
                        //Todo:创建设备列表对话框
                        View mView = View.inflate(getApplicationContext(), R.layout.dalogview, null);       //创建View
                        final Dialog showedDialog = new AlertDialog.Builder(this).create();
                        showedDialog.show();
                        showedDialog.getWindow().setContentView(mView);

                        Point size = new Point();
                        this.getWindowManager().getDefaultDisplay().getSize(size);
                        WindowManager.LayoutParams params = showedDialog.getWindow().getAttributes();
                        params.width = size.x;
                        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        params.gravity = Gravity.BOTTOM;
                        showedDialog.getWindow().setAttributes(params);

                        mDevicesListView = (ListView) mView.findViewById(R.id.pairedDevicesListView);       //用来显示内容的View控件

                        //绑定数据与事件
                        mDevicesListView.setAdapter(mDevicesAdapter);
                        mDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                String text = ((TextView) view).getText().toString();
                                BluetoothDevice tmpDevice = mBluetoothObj.mAdapter.getRemoteDevice(text.substring(text.length() - 17));
                                mBluetoothObj.mDeviceName = text.substring(0, 17);
                                //Todo:开始连接远程设备
                                if (mAsyncLink != null) {
                                    mAsyncLink.stop();
                                    mAsyncLink = null;
                                }

                                mAsyncLink = new AsynchronousLink(mHandler);
                                if (viewID == R.id.getSerial) {
                                    mAsyncLink.connect(tmpDevice, 1);
                                } else {
                                    mAsyncLink.connect(tmpDevice, 0);
                                }

                                showedDialog.dismiss();
                            }
                        });
                    }
                    break;
                case R.id.internetConfig:
                    //Todo:
                    Intent configIntent = new Intent(getApplicationContext(), ConfigActivity.class);
                    startActivityForResult(configIntent, 0);
                    break;

                default:
                    //Todo:
                    break;
            }
        } catch (Exception ex) {
            //Todo:
            Log.d("MainActivity", ex.getMessage());
        }
    }

//    private final void setStatus(CharSequence title) {
//        try {
//            final ActionBar actionBar = getSupportActionBar();
//            actionBar.setTitle(title);
//        } catch (Exception ex) {
//            ex.getStackTrace();
//        }
//    }

    private final void setStatus(String title) {
        try {
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(title);
        } catch (Exception ex) {
            Log.d(TAG, "set ActionBar Title failed", ex);
        }
    }

    @Override
    public void onBackPressed() {
        //Todo:exit application
        if (System.currentTimeMillis() - pressBackKeyTime > 2000) {
            Toast.makeText(getApplicationContext(), GlobalConstantValue.EXIT_TOAST, Toast.LENGTH_SHORT).show();
            pressBackKeyTime = System.currentTimeMillis();
        } else {
            if (mAsyncLink != null) {
                mAsyncLink.stop();
                mAsyncLink = null;
            }
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.killBackgroundProcesses(PACKAGE_NAME);

            System.exit(0);
        }
    }

    /**
     * 更新UI界面显示
     *
     * @param msg 送至界面显示的message语句
     */
    private void send2UI(String msg) {
        String[] item;
        try {
            String msgHead = msg.substring(3, 6);
            switch (msgHead) {
                case "GGA":
                    item = ParseNMEA0813.getGGA(msg);
                    if (item == null) return;
                    //经纬度信息
                    String info = Tools.getLatitude(item[2]);
                    info += Tools.direction(item[3]);
                    info += "   " + Tools.getLongitude(item[4]);
                    info += Tools.direction(item[5]);
                    mLatiLongi.setText(info);

                    //时间信息
                    mDatetime.setText("本地时间：" + Tools.getDate(item[1]));
                    //卫星数量
                    mNumber.setText("卫星数量：" + Tools.getNumber(item[7]));
                    //解状态
                    mQuality.setText("解状态：" + Tools.getQuality(item[6]));
                    //HDOP
                    mHdop.setText("水平精度因子：" + item[8]);
                    //高程
                    mAltitude.setText("高程信息（天线高+海拔高）:" + Tools.getAltitude(item[9],
                            item[11]) + " m");
                    //差分龄期
                    if (null == item[13] || item[13] == "") {
                        mDgps.setText("差分龄期：NA");
                    } else {
                        mDgps.setText("差分龄期：" + item[13]);
                    }

                    mArrayAdapter.add(msg);
                    wholeGGAMsg = msg + "\r\n";
                    break;
                case "GST":
                    item = ParseNMEA0813.getGST(msg);
                    if (item == null) return;
                    double latiErr = 0.0;
                    double longiErr = 0.0;
                    if (null != item[6] && !item[6].equals("")) {
                        latiErr = Double.valueOf(item[6]);
                    }
                    if (null != item[7] && !item[7].equals("")) {
                        longiErr = Double.valueOf(item[7]);
                    }
                    mHorizontalError.setText("水平误差：" +
                            String.valueOf(ParseNMEA0813.getHorizontalError(latiErr, longiErr)) + " m");
                    mAltitudeError.setText("高程误差：" + item[8] + " m");

                    mArrayAdapter.add(msg);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            //todo:
        }
    }
}
