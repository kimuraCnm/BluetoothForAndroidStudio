package shanghai.enrique.com.falco;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * 描述与cors服务器交互的操作类
 * Created by ZHANGFUSHENG on 2015/9/30.
 * kimura.floyd@gmail.com
 */
public class NetworkService {

    private static final String TAG = "NetworkService";
    private boolean _run = true;//设置一个标识变量来控制线程的关闭

    private int oprationMode = -1;//定义资源请求模式：0为请求源列表，1为请求差分数据，-1表示无操作

    private Handler mHandler;
    private String mIP;
    private String mPwd;
    private String mUserID;
    private String mPort;
    private String mMountPoint;
    // private Socket mSocket;
    private OutputStream bluetoothOut;
    private ConnectThread mConnectThread;
    private AcceptThread mAcceptThread;
    private DownloadThread mDownloadThread;
    private ReportThread mReportThread;

    /**
     * 用给定的参数实例化cors网络服务对象
     *
     * @param handler 当前handler
     * @param ip      ip地址
     * @param port    端口号
     */
    public NetworkService(Handler handler, String ip, String port, int operation) {
        mHandler = handler;
        mIP = ip;
        mPort = port;
        oprationMode = operation;
    }

    public void setmMountPoint(String mMountPoint) {
        this.mMountPoint = mMountPoint;
    }

    public void setmUserID(String mUserID) {
        this.mUserID = mUserID;
    }

    public void setmPwd(String mPwd) {
        this.mPwd = mPwd;
    }


    public synchronized void connect2Caster() throws IOException {
        if (mConnectThread != null)
            mConnectThread = null;

        mConnectThread = new ConnectThread(mIP, mPort);
        mConnectThread.start();
    }

    public synchronized void downloadSourceTable(Socket socket) {
        if (mDownloadThread != null) mDownloadThread = null;

        mDownloadThread = new DownloadThread(socket);
        mDownloadThread.start();

    }

    public synchronized void acceptDiff(Socket socket) {
        if (mReportThread != null)
            mReportThread._rep = false;
        if (mAcceptThread != null) {
            _run = false;
            //释放相关资源
            try {
                mAcceptThread.mmIn.close();

                mAcceptThread.mmOut.flush();
                mAcceptThread.mmOut.close();

                mAcceptThread.mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mAcceptThread = null;
        }

        mAcceptThread = new AcceptThread(socket);
        mAcceptThread.start();
    }

    private class ConnectThread extends Thread {
        Socket tmpSocket = null;
        SocketAddress address = null;

        public ConnectThread(String ip, String port) {
            address = new InetSocketAddress(ip, Integer.valueOf(port));
            tmpSocket = new Socket();
        }

        @Override
        public void run() {
            try {
                tmpSocket.connect(address, 1000 * 5);
                //socket超时
                tmpSocket.setSoTimeout(1000 * 15);

                if (tmpSocket.isConnected()) {
                    mHandler.obtainMessage(ConfigActivity.MESSAGE_CHANGE_UI, true).sendToTarget();

                    //根据操作模式不同启动不同的线程
                    if (oprationMode == GlobalConstantValue.OPRATION_MODE_MOUNTPOINT) {
                        downloadSourceTable(tmpSocket);
                    } else if (oprationMode == GlobalConstantValue.OPRATION_MODE_DIFF) {
                        acceptDiff(tmpSocket);
                    }
                }
            } catch (IOException e) {
                //Todo:处理socket连接失败事宜               
                mHandler.obtainMessage(ConfigActivity.MESSAGE_SOCKETTIMEOUT_EXCEPTION, null).sendToTarget();
            } finally {
                //we're done
                synchronized (NetworkService.this) {
                    mConnectThread = null;
                }
            }
        }
    }

    /**
     * 获取差分源列表线程
     */
    private class DownloadThread extends Thread {
        private final InputStream mmIn;
        private final OutputStream mmOut;
        Socket mmSocket = null;

        public DownloadThread(Socket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmSocket = socket;
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                //Todo:
                e.printStackTrace();
            }

            mmIn = tmpIn;
            mmOut = tmpOut;
        }

        @Override
        public void run() {
            String request = Tools.protocol(mMountPoint, mUserID, mPwd);
            byte[] buffer = new byte[2048];     //该buffer的大小有异议，取决于
            try {
                mmOut.write(request.getBytes());

                int bytes = mmIn.read(buffer, 0, buffer.length);//此处阻塞
                while (bytes >= 1) {
                    byte[] tmpBuffer = new byte[bytes];
                    System.arraycopy(buffer, 0, tmpBuffer, 0, bytes);

                    String response = new String(tmpBuffer);
                    if (response.startsWith("SOURCETABLE 200 OK")) {
                        //Todo:解析源列表
                        mHandler.obtainMessage(ConfigActivity.MESSAGE_SOURCE_TABLE_OK, tmpBuffer).sendToTarget();
                    }

                    bytes = mmIn.read(buffer, 0, buffer.length);
                }
            } catch (IOException e) {
                //Todo:
                e.printStackTrace();
            }
        }
    }

    /**
     * 差分数据接收线程
     */
    private class AcceptThread extends Thread {
        private final InputStream mmIn;
        private final OutputStream mmOut;
        Socket mmSocket = null;


        public AcceptThread(Socket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmSocket = socket;
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                //Todo:
                e.printStackTrace();
            }

            mmIn = tmpIn;
            mmOut = tmpOut;
            _run = true;      //线程控制                    
        }

        @Override
        public void run() {
            String request = Tools.protocol(mMountPoint, mUserID, mPwd);
            byte[] buffer = new byte[256];

            try {
                mmOut.write(request.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (_run) {
                int bytes = 0;
                //此处阻塞，读取缓存
                try {
                    bytes = mmIn.read(buffer, 0, buffer.length);
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());

                    _run = false;
                    if (null != mReportThread) {
                        mReportThread._rep = false;
                    }
                }

                if (bytes >= 1) {
                    byte[] tmpBuffer = new byte[bytes];
                    System.arraycopy(buffer, 0, tmpBuffer, 0, bytes);
                    String response = new String(tmpBuffer);
                    //Log.d(TAG, "接收到的差分数据：" + response);

                    if (response.indexOf("401 Unauthorized") > 1) {
                        //Todo:给出提示信息
                        mHandler.obtainMessage(ConfigActivity.MESSAGE_UNAUTHORIZED, null).sendToTarget();
                    } else if (response.startsWith("SOURCETABLE 200 OK")) {
                        //Todo:解析源列表
                        mHandler.obtainMessage(ConfigActivity.MESSAGE_SOURCE_TABLE_OK, tmpBuffer).sendToTarget();
                    } else if (response.startsWith("ICY 200 OK")) {
                        //Todo:登录验证成功                                            
                        mHandler.obtainMessage(ConfigActivity.MESSAGE_DIFF_SUCCESS, null).sendToTarget();
                        //启动GGA上报线程
                        mReportThread = new ReportThread(mmOut);
                        mReportThread.start();
                    } else {
                        mHandler.obtainMessage(ConfigActivity.MESSAGE_CORS_TOTAL, bytes).sendToTarget();
                        String tmpHeader = GlobalConstantValue.CMD_HEADER + String.valueOf(bytes + 17) + ",";
                        try {
                            if (null == bluetoothOut) {
                                if (null != MainActivity.mBluetoothObj.mBluetoothSocket) {
                                    bluetoothOut = MainActivity.mBluetoothObj.mBluetoothSocket.getOutputStream();
                                    bluetoothOut.write(tmpHeader.getBytes());
                                    bluetoothOut.write(tmpBuffer);
                                    bluetoothOut.write(GlobalConstantValue.CMD_END.getBytes());
                                } else {
                                    continue;
                                }
                            } else {
                                bluetoothOut.write(tmpHeader.getBytes());
                                bluetoothOut.write(tmpBuffer);
                                bluetoothOut.write(GlobalConstantValue.CMD_END.getBytes());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * GGA上报线程
     */
    private class ReportThread extends Thread {
        public boolean _rep = true;
        OutputStream mOutStream = null;

        public ReportThread(OutputStream stream) {
            mOutStream = stream;
        }

        @Override
        public void run() {
            while (_rep) {
                //Log.d(TAG, "上报线程正在运行......");
                if (MainActivity.wholeGGAMsg == "") {
                    MainActivity.wholeGGAMsg = "$GPGGA,072151.00,3120.75826052,N,12137.38343927,E,1,27,0.5,20.477,M,8.346,M,,*6F";
                    MainActivity.wholeGGAMsg += "\r\n\r\n";
                }
                try {
                    mOutStream.write(MainActivity.wholeGGAMsg.getBytes());
                    //Log.d(TAG, MainActivity.wholeGGAMsg);

                    Thread.sleep(1000 * 10);
                } catch (IOException e) {
                    Log.d(TAG, "上报线程停止");
                    _rep = false;
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    Log.d(TAG, "上报线程停止");
                    _rep = false;
                    e.printStackTrace();
                }
            }
        }
    }
}
