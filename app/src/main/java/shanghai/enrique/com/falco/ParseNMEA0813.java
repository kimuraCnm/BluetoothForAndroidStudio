package shanghai.enrique.com.falco;

import android.util.Log;

import java.util.Formatter;
import java.util.Locale;

/**
 * 描述对NMEA0813语句的解析操作类
 * Created by ZHANGFUSHENG on 2015/9/29.
 * kimura.floyd@gmail.com
 */
public class ParseNMEA0813 {
    private static final String TAG = "ParseNMEA0813";

    private static String[] byteMsg;

    /**
     * 获取GGA详细项信息
     * An example of the GBS message string is:
     * $GPGGA,172814.0,3723.46587704,N,12202.26957864,W,2,6,1.2,18.893,M,-25.669,M,2.0,0031*4F
     *
     * @param ggaMsg GGA语句
     */
    public static String[] getGGA(String ggaMsg) {
        try {          
            byteMsg = ggaMsg.split(",");

            if (byteMsg.length != 15) {
                Log.e(TAG, "error length of gga message:" + ggaMsg);
                byteMsg = null;
            }
        } catch (Exception e) {
            //todo:
            Log.e(TAG, "Unable to get GGA massage", e);
        }
        return byteMsg;
    }

    /**
     * 获取GST详细项信息，An example of the GST message string is:
     * $GPGST,172814.0,0.006,0.023,0.020,273.6,0.023,0.020,0.031*6A
     *
     * @param gstMsg GST语句
     */
    public static String[] getGST(String gstMsg) {
        try {
            gstMsg = gstMsg.substring(0, gstMsg.indexOf("*"));
            byteMsg = gstMsg.split(",");

            if (byteMsg.length != 9) {
                Log.e(TAG, "error length of gst message:" + gstMsg);
                byteMsg = null;
            }
        } catch (Exception e) {
            //todo:
            Log.e(TAG, "Unable to get GST massage", e);
        }
        return byteMsg;
    }

    /**
     * 根据经纬度误差计算水平精度误差
     *
     * @param latiError  纬度误差，单位：米
     * @param longiError 经度误差，单位：米
     * @return 水平误差
     */
    public static double getHorizontalError(double latiError, double longiError) {
        String result = "0.0";
        try {
            double tmp = Math.sqrt(Math.pow(latiError, 2) + Math.pow(longiError, 2));
            result = new Formatter().format(Locale.CHINA, "%.2f", tmp).toString();
        } catch (Exception e) {
            //Todo:
        } finally {
            return Double.valueOf(result);
        }
    }
}
