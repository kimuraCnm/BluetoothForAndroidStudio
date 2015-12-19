package shanghai.enrique.com.falco;

/**
 * Created by ZHANGFUSHENG on 2015/9/24.
 * kimura.floyd@gmail.com
 */
public class GlobalConstantValue {
    public static final int MESSAGE_FROM_ASYNCHRONOUSLINK_TO_MAINACTIVITY = 1;
   
    //message types sends from AsynchronousLink
    public static final int MEESAGE_TOAST = 5;
    public static final int MESSAGE_READ = 0;

    public static final int OPRATION_MODE_MOUNTPOINT = 0;
    public static final int OPRATION_MODE_DIFF = 1;

    //消息类别
    public static int ENABLE_BLUETOOTHADAPTER_REQUEST = 0;
    //状态栏通知消息
    public static String CONNECT_DISCONNECT = "Disconnected";
    public static String CONNECTING = "Connecting";
    public static String CONNECTED = "Connected";
    public static String CONNECTED_FAILED="Connecting failed";
    //public static int DISCONNECT_STATUS_NOTIFICATION = 2;


    public static String UUID = "00001101-0000-1000-8000-00805F9B34FB";
    public static String TOAST = "toast";
    public static String EXIT_TOAST = "再按一次返回退出当前程序";

    //与设备交互的协议字头与字尾
    //协议为：字头，长度，内容，字尾
    public static String CMD_HEADER = "$FCMDB,";
    public static String CMD_END = ",*FF\r\n";

    public static byte[] cmdGetSerial = new byte[]{0x02, 0x08, 0x06, 0x00, 0x0E, 0x03};
}
