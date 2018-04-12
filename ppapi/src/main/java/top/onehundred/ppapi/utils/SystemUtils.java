package top.onehundred.ppapi.utils;

/**
 * System utils
 * Created by LiuPeng on 2018/1/18.
 */
public class SystemUtils {

    /**
     * get phone system version
     *
     * @return phone system version
     */
    public static String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * get phone model
     *
     * @return phone model
     */
    public static String getSystemModel() {
        return android.os.Build.MODEL;
    }

    /**
     * get phone brand
     *
     * @return phone brand
     */
    public static String getDeviceBrand() {
        return android.os.Build.BRAND;
    }
}
