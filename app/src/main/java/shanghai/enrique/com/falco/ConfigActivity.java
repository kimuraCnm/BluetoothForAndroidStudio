package shanghai.enrique.com.falco;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class ConfigActivity extends ActionBarActivity {

    public static final int MESSAGE_SOURCE_TABLE_OK = 0;
    public static final int MESSAGE_DIFF_SUCCESS = 1;
    public static final int MESSAGE_UNAUTHORIZED = 2;
    public static final int MESSAGE_SOCKETTIMEOUT_EXCEPTION = 3;
    public static final int MESSAGE_CHANGE_UI = 4;
    public static final int MESSAGE_NETWORK_UNAVAILABLE = 5;
    public static final int MESSAGE_CORS_TOTAL = 6;
    private static String TAG = "ConfigActivity";
    private static int RESULT_CANCELED = 0;
    private int total = 0;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Todo:
            switch (msg.what) {
                //网络不可用
                case MESSAGE_NETWORK_UNAVAILABLE:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                //源列表解析
                case MESSAGE_SOURCE_TABLE_OK:
                    byte[] buffer = (byte[]) msg.obj;
                    String response = new String(buffer);
                    parseResponseFromCaster(response);
                    Toast.makeText(getApplicationContext(), "源列表获取成功", Toast.LENGTH_SHORT).show();
                    break;
                //用户登录通过
                case MESSAGE_DIFF_SUCCESS:
                    Toast.makeText(getApplicationContext(), "登录验证通过，即将接收差分数据...", Toast.LENGTH_SHORT).show();
                    break;
                //socket异常信息抛出
                case MESSAGE_SOCKETTIMEOUT_EXCEPTION:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                //后台线程更新UI
                case MESSAGE_CHANGE_UI:
                    mUpdate.setEnabled((boolean) msg.obj);
                    if (!mConnect.isEnabled()) mConnect.setEnabled(true);
                    break;
                case MESSAGE_CORS_TOTAL:
                    int len = (int) msg.obj;
                    total += len;
                    mTextView.setText("Total:" + String.valueOf(total));
                default:
                    break;
            }
        }
    };
    private EditText mIP, mPort, mUserID, mUserPwd;
    private Button mUpdate, mConnect;
    private Spinner mSpinner;
    private TextView mTextView;
    //private ArrayList<String> mSourceList;
    private ArrayAdapter<String> mAdapter;

    private NetworkService mService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);

        mIP = (EditText) findViewById(R.id.ip);
        mPort = (EditText) findViewById(R.id.port);
        mUserID = (EditText) findViewById(R.id.userID);
        mUserPwd = (EditText) findViewById(R.id.userPwd);
        mUpdate = (Button) findViewById(R.id.btnUpdate);
        mConnect = (Button) findViewById(R.id.connect);
        mSpinner = (Spinner) findViewById(R.id.mSpinner);
        mTextView = (TextView) findViewById(R.id.corsInfo);


        mAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.listview_item_sourcetable);
        mSpinner.setAdapter(mAdapter);

        setResult(RESULT_CANCELED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_config, menu);
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

    public void onClickEvent(View view) {
        if (!Tools.isNetworkAvailable(this)) {
            //Todo:提示网络不可用
            mHandler.obtainMessage(MESSAGE_NETWORK_UNAVAILABLE, "当前网络不可用，请先连接网络！").sendToTarget();
            return;
        }

        int viewID = view.getId();
        String port = mPort.getText().toString().trim();
        String ip = mIP.getText().toString().trim();
        if ("".equals(port) || "".equals(ip)) {
            //Todo:提示参数为空
            return;
        }
        if (!Tools.isIpAddressLegal(ip)) {
            //Todo:提示IP不合法
            return;
        }
        try {
            if (mService != null) {
                mService = null;
            }
            switch (viewID) {
                case R.id.btnUpdate:
                    mUpdate.setEnabled(false);
                    mService = new NetworkService(mHandler, ip, port, GlobalConstantValue.OPRATION_MODE_MOUNTPOINT);

                    break;
                case R.id.connect:
                    mConnect.setEnabled(false);

                    //连接之前要做一系列的判断：是否有网络，IP和端口等参数是否填写、是否合法等
                    String mountPoint = mSpinner.getSelectedItem().toString();
                    String user = mUserID.getText().toString().trim();
                    String pwd = mUserPwd.getText().toString().trim();

                    mService = new NetworkService(mHandler, ip, port, GlobalConstantValue.OPRATION_MODE_DIFF);
                    mService.setmMountPoint(mountPoint);
                    mService.setmUserID(user);
                    mService.setmPwd(pwd);

                    break;
                default:
                    break;
            }
            mService.connect2Caster();
        } catch (Exception ex) {
            //Todo:
            Log.d(TAG, ex.getMessage());
        }
    }

    /**
     * 源列表解析方法
     *
     * @param response 返回的源列表资源，String类型
     */
    private void parseResponseFromCaster(String response) {
        mAdapter.clear();

        String[] items = response.split("\r\n");
        for (String item : items) {
            if (item.startsWith("STR")) {
                String[] subItems = item.split(";");
                mAdapter.add(subItems[1].trim());
            }
        }
    }
}
