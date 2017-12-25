package im.socks.yysk.data;

import im.socks.yysk.util.Json;
import im.socks.yysk.util.XBean;

/**
 * Created by cole on 2017/10/27.
 */

public class User implements Json.IJsonable<XBean> {
    public String id;
    public String phoneNumber;

    public User() {
    }

    public boolean isGuest2() {
        //return id==null||id.isEmpty();
        return phoneNumber == null || phoneNumber.isEmpty();
    }


    @Override
    public XBean toJson() {
        return new XBean("id", id, "phone_number", phoneNumber);
    }

    @Override
    public boolean fill(XBean bean) {
        id = bean.getString("id", null);
        phoneNumber = bean.getString("phone_number", null);
        return true;
    }
}
