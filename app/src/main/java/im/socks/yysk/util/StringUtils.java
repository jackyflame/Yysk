package im.socks.yysk.util;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Android Studio.
 * ProjectName: Yysk
 * Author: Haozi
 * Date: 2018/1/2
 * Time: 22:03
 */

public class StringUtils {

    public static boolean isInteger(String str){
        if(TextUtils.isEmpty(str)){
            return false;
        }
        return TextUtils.isDigitsOnly(str);
    }

    /**
     * 判断字符串是否为正浮点型（Float or Double）数据
     * x.xx或+x.xx都算作正浮点数
     * 0.0不算做正浮点数
     * @param str
     * @return true=>是;false=>不是
     */
    public static boolean strIsFloat(String str) {
        if (TextUtils.isEmpty(str)){
            return false;
        }
        if(isInteger(str)){
            return true;
        }
        Pattern p = Pattern.compile("//d+(//.//d+)?");
        Matcher m = p.matcher(str);
        if (m.matches()){
            return true;
        }
        return false;
    }

    public static String getNowTimeStr(){
        long time=System.currentTimeMillis();//long now = android.os.SystemClock.uptimeMillis();
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date(time));
    }

    public static String getTimeStr(long time){
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date(time));
    }

}
