package shanghai.enrique.com.falco;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by ZHANGFUSHENG on 2015/9/25.
 * kimura.floyd@gmail.com
 */
public class AsynchronousLink {
    private static final String TAG = "AsynchronousLink";
    private boolean D = true;
    private int operation = 0;
    private StringBuilder stringBuilder = new StringBuilder();      //保存临时从串口缓存读取到的（转换为string之后二进制数据）

    private Handler mHandler;
    private BluetoothSocket mSocket;
    private BluetoothAdapter mAdapter;


    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private ReadResponseThread mReadResponseThread;

    public AsynchronousLink(Handler handler) {
        this.mHandler = handler;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * 启动新线程来创建一个到远程设备的连接
     *
     * @param device    远程设备
     * @param operation 操作模式，0表示nmea-0813语句，1表示配置板卡指令
     */
    public synchronized void connect(BluetoothDevice device, int operation) {
        //Todo:执行连接device的线程，并通知UI做出相应的提示

        //若当前正在执行连接线程，即取消当前连接线程
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        //若当前正在执行接收线程，即取消当前接收线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mReadResponseThread != null) {
            mReadResponseThread.cancel();
            mReadResponseThread = null;
        }

        this.operation = operation;

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        setStatus(GlobalConstantValue.CONNECTING);
    }

    /**
     * 启动一个新线程来管理建立的连接
     *
     * @param socket 连接对象
     */
    public synchronized void connected(BluetoothSocket socket) {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            try {
                mConnectedThread.mmInstream.close();
                mConnectedThread.mmOutstream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mConnectedThread = null;
        }
        if (mReadResponseThread != null) {
            mReadResponseThread.cancel();
            try {
                mReadResponseThread.mmInstream.close();
                mReadResponseThread.mmOutstream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mReadResponseThread = null;
        }

        if (operation == 0) {
            mConnectedThread = new ConnectedThread(socket);
            mConnectedThread.start();
        } else {
            mReadResponseThread = new ReadResponseThread(socket);
            mReadResponseThread.start();
        }
    }

    /**
     * 通知UI 蓝牙的连接状态
     */
    private synchronized void setStatus(String title) {
        try {
            this.mHandler.obtainMessage(GlobalConstantValue.MESSAGE_FROM_ASYNCHRONOUSLINK_TO_MAINACTIVITY, title).sendToTarget();
        } catch (Exception ex) {
            //Todo:
            Log.d(TAG, "send link status changed notify failed", ex);
        }
    }

    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        //setStatus(GlobalConstantValue.CONNECT_DISCONNECT);
    }


    private void connectionFailed(Exception e) {
        Message msg = mHandler.obtainMessage(GlobalConstantValue.MEESAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GlobalConstantValue.TOAST, e.getMessage());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        Message msg = mHandler.obtainMessage(GlobalConstantValue.MEESAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(GlobalConstantValue.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private class ConnectThread extends Thread {
        private BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mDevice = device;
            Method method;
            try {
                if (Build.VERSION.SDK_INT >= 10) {
                    method = mDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    tmp = (BluetoothSocket) method.invoke(mDevice, 1);
                } else {
                    tmp = mDevice.createRfcommSocketToServiceRecord(UUID.fromString(GlobalConstantValue.UUID));
                }
            } catch (IOException e) {
                Log.d(TAG, "bluetooth socket create failed", e);
                //setStatus(GlobalConstantValue.CONNECTED_FAILED);
            } catch (Exception e1) {
                Log.d(TAG, "bluetooth socket create failed", e1);
                //setStatus(GlobalConstantValue.CONNECTED_FAILED);
            }

            mSocket = tmp;
        }

        @Override
        public void run() {
            //Todo:
            mAdapter.cancelDiscovery();
            try {
                mSocket.connect();
            } catch (IOException e) {
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Log.d(TAG, "Unable to close() socket during connection failure", e1);
                }
                connectionFailed(e);
                setStatus(GlobalConstantValue.CONNECTED_FAILED);
                return;
            }
            setStatus(GlobalConstantValue.CONNECTED);
            connected(mSocket);
            // we are done
            synchronized (AsynchronousLink.this) {
                mConnectThread = null;
            }
        }

        /**
         * 停止连接蓝牙设备线程
         */
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                //Todo:
                Log.d(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mmInstream;
        private final OutputStream mmOutstream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            MainActivity.mBluetoothObj.mBluetoothSocket = socket;//赋值给全局socket，为差分数据的写入提供接口
            mSocket = socket;

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                //Todo:
                Log.d(TAG, "tmp sockets not created", e);
            }

            mmInstream = tmpIn;
            mmOutstream = tmpOut;

        }

        @Override
        public void run() {
            int bytes;
            byte[] buffer = new byte[256];

            while (D) {
                try {
                    bytes = mmInstream.read(buffer, 0, buffer.length);
                    Log.d(TAG, "正常接收，数据长度:" + String.valueOf(bytes));
                    if (bytes >= 1) {
                        stringBuilder.append(new String(buffer, 0, bytes));

                        String tmpMsg = stringBuilder.toString();
                        int start = tmpMsg.indexOf("$");
                        int end = tmpMsg.lastIndexOf("\r\n");
                        //确保当前有完整语句可供解析
                        if (start >= 0 && start < end) {
                            stringBuilder.setLength(0);
                            stringBuilder.append(tmpMsg.substring(end + 2));

                            String tmp = tmpMsg.substring(start, end);
                            String[] msgSet = tmp.split("\r\n");
                            for (String singleMsg : msgSet) {
                                mHandler.obtainMessage(GlobalConstantValue.MESSAGE_READ, singleMsg).sendToTarget();
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.d(TAG, "disconnect", e);
                    connectionLost();
                    setStatus(GlobalConstantValue.CONNECT_DISCONNECT);
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                //Todo:
                Log.d(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ReadResponseThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mmInstream;
        private final OutputStream mmOutstream;

        public ReadResponseThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mSocket = socket;
            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                //Todo:
            }
            mmInstream = tmpIn;
            mmOutstream = tmpOut;
        }

        @Override
        public void run() {
            //Todo:
            write();

            int bytes;
            byte[] buffer = new byte[256];
            while (D) {
                try {
                    bytes = mmInstream.read(buffer, 0, buffer.length);

                    if (bytes >= 1) {
                        //Todo:
                        Log.d(TAG, "指令响应：" + buffer.toString());
                    }
                } catch (IOException e) {
                    //Todo:
                    Log.d(TAG, "disconnect", e);
                    connectionLost();
                    break;
                }
            }
        }


        /**
         * Write to the connected OutStream.
         */
        public void write() {

            byte[] mCommand;
            mCommand = GlobalConstantValue.cmdGetSerial;
            String headerCmd = GlobalConstantValue.CMD_HEADER
                    + String.valueOf(16 + mCommand.length) + ","
                    + mCommand.toString()
                    + GlobalConstantValue.CMD_END;

            try {
                mmOutstream.write(headerCmd.getBytes());
//                mmOutstream.write(mCommand);
//                mmOutstream.write(GlobalConstantValue.CMD_END.getBytes());

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                //Todo:
                Log.d(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
