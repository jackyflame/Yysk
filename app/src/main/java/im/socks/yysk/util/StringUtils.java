package im.socks.yysk.util;

import android.text.TextUtils;

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

}
