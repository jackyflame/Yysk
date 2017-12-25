package im.socks.yysk.data;

import im.socks.yysk.util.Json;
import im.socks.yysk.util.XBean;

/**
 * Created by cole on 2017/10/27.
 */

public class Session implements Json.IJsonable<XBean> {

    public User user = new User();
    //public Proxy proxy = null;

    private boolean isLogin = false;
    //正常的实现应该是存储一个token，然后到后台检查的，但是现在后台没有返回一个token

    public Session() {
    }

    public boolean isLogin() {
        return isLogin;
    }

    public void setLogin(boolean b) {
        this.isLogin = b;
    }

    @Override
    public XBean toJson() {
        //应该存储token，而不是is_login的
        return new XBean("user", user.toJson(), "is_login", isLogin);
    }

    @Override
    public boolean fill(XBean bean) {
        if (bean.getXBean("user") != null) {
            user.fill(bean.getXBean("user"));
        } else {

        }
        //实际上应该获得的是token
        isLogin = bean.getBoolean("is_login", false);

        return true;
    }


}
