package shanghai.enrique.com.falco;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Base64;
import android.util.Log;

import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 进行一些字符串格式转换、经纬度有效位获取等操作的描述类
 * Created by ZHANGFUSHENG on 2015/9/29.
 * kimura.floyd@gmail.com
 */
public class Tools {
    private static String TAG = "Tools";

    /**
     * 将度分分格式转换为度分秒
     *
     * @param latitude 度分分格式的纬度坐标
     * @return 度分秒格式纬度坐标
     */
    public static String getLatitude(String latitude) {

        String tmp = "0°0＇0＂";
        try {
            if (latitude.equals(null) || latitude.equals("")) return tmp;
            tmp = latitude;
            String deg, min, sec;
            deg = tmp.substring(0, 2);//获取度
            min = tmp.substring(2, 4);//获取分

            sec = "0." + tmp.substring(tmp.indexOf(".") + 1);//获取小数位分,需特殊处理转换为秒
            double tmpSec = Double.parseDouble(sec) * 60;
            sec = new Formatter().format(Locale.CHINA, "%.7f", tmpSec).toString();

            tmp = deg + "°" + min + "＇" + sec + "＂";
        } catch (Exception e) {
            //todo:
            Log.d(TAG, "getLatitude:", e);
        } finally {
            return tmp;
        }
    }

    /**
     * 将度分分格式转换为度分秒
     *
     * @param longitude 度分分格式的经度坐标
     * @return 度分秒格式经度坐标
     */

    public static String getLongitude(String longitude) {

        String tmp = "0°0＇0＂";
        try {
            if (longitude.equals(null) || longitude.equals("")) return tmp;
            tmp = longitude;
            String deg, min, sec;
            deg = tmp.substring(0, 3);//获取度
            min = tmp.substring(3, 5);//获取分

            sec = "0." + tmp.substring(tmp.indexOf(".") + 1);//获取小数位分,需特殊处理转换为秒
            double tmpSec = Double.parseDouble(sec) * 60;
            sec = new Formatter().format(Locale.CHINA, "%.7f", tmpSec).toString();

            tmp = deg + "°" + min + "＇" + sec + "＂";
        } catch (Exception e) {
            //todo:
            Log.d(TAG, "getLongitude:", e);
        } finally {
            return tmp;
        }
    }

    /**
     * 星历时间转换
     *
     * @param dateTime 星历时间
     * @return 00:00:00格式时间
     */
    public static String getDate(String dateTime) {
        String tmp = "00:00:00";
        try {
            if (dateTime.equals(null) || dateTime.equals("")) return tmp;
            tmp = dateTime;
            String hour = tmp.substring(0, 2);
            int intHour = Integer.parseInt(hour) + 8;
            if (intHour >= 24) intHour -= 24;

            tmp = String.valueOf(intHour) + ":" + tmp.substring(2, 4) + ":" + tmp.substring(4, 6);
        } catch (Exception e) {
            //todo:
            Log.d(TAG, "getDate:", e);
        } finally {
            return tmp;
        }
    }

    public static String getNumber(String number) {
        String tmp = "0";
        try {
            if (number.equals(null) || number.equals("")) return tmp;
            tmp = Integer.valueOf(number).toString();
        } catch (Exception e) {//todo:
            Log.d(TAG, "getNumber:", e);
        } finally {
            return tmp;
        }
    }

    public static String getQuality(String quality) {
        String tmp = "未定位";
        try {
            if (quality.equals(null) || quality.equals("")) return tmp;
            switch (quality) {
                case "1":
                    tmp = "单点";
                    break;
                case "2":
                    tmp = "伪距";
                    break;
                case "4":
                    tmp = "固定";
                    break;
                case "5":
                    tmp = "浮点";
                    break;
            }
        } catch (Exception e) {
            //todo:
        } finally {
            return tmp;
        }
    }

    public static String getAltitude(String arg1, String arg2) {
        if (null == arg1 || arg1.equals("")) {
            arg1 = "0";
        }
        if (null == arg2 || arg2.equals("")) {
            arg2 = "0";
        }

        double l = Double.valueOf(arg1) + Double.valueOf(arg2);
        return new Formatter().format(Locale.CHINA, "%.2f", l).toString();
    }

    public static String direction(String dir) {
        try {
            if (null == dir || dir.equals("")) return "NaN";
            else return "——" + dir;
        } catch (Exception e) {
            //todo:
            return "NaN";
        }
    }

    /**
     * 简单判断网络是否可用
     *
     * @return Ture表示可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager comManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return comManager.getActiveNetworkInfo().isAvailable();
    }

    /**
     * 利用正则表达式判断ip是否合法
     *
     * @param addr
     * @return True
     */
    public static boolean isIpAddressLegal(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

        Pattern pat = Pattern.compile(rexp);

        Matcher mat = pat.matcher(addr);

        boolean ipAddress = mat.find();

        return ipAddress;
    }

    /**
     * 获取HTTP请求头
     *
     * @param mountPoint 挂载点
     * @param userID     用户账号
     * @param pwd        用户密码
     * @return HTTP请求头字符串
     */
    public static String protocol(String mountPoint, String userID, String pwd) {
        String httpRequest;
        if (mountPoint == null || mountPoint.equals("")) {
            httpRequest = "GET / HTTP/1.0\r\n";
        } else {
            httpRequest = "GET /" + mountPoint + " HTTP/1.0\r\n";
        }
        httpRequest += "User-Agent:NTRIP GNSSInternetRadio/1.4 .11\r\n";
        httpRequest += "Accept:*/*\r\n";
        httpRequest += "Connection: close\r\n";
        if (userID != null) {
            String tmp = userID + ":" + pwd;
            tmp = Base64.encodeToString(tmp.getBytes(), Base64.NO_WRAP);
            httpRequest += "Authorization: Basic " + tmp;
        }
        httpRequest += "\r\n\r\n\r\n";

        return httpRequest;
    }
}
