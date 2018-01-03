package im.socks.yysk.api;


import im.socks.yysk.App;
import im.socks.yysk.AppDZ;
import im.socks.yysk.util.XBean;

/**
 * Created by Android Studio.
 * ProjectName: Yysk
 * Author: haozi
 * Date: 2018/1/2
 * Time: 16:29
 */
public class YyskDZApi extends YyskApi {

    protected AppDZ app;

    public YyskDZApi(AppDZ app) {
        super(app);
    }

    @Override
    protected void initApi(App app) {
        super.initApi(app);
        if(app instanceof AppDZ){
            this.app = (AppDZ) app;
        }
    }

    /*--------------------------------------------------------------------------------------------*/
    public void loginDZ(String strPhoneNum, String strPassword, final ICallback<XBean> cb) {
        //如果仅仅执行登录，感觉没有做任何事情，什么都不返回，token也没有
        //invoke("10020", "20020", new XBean("PhoneNumber", strPhoneNum, "Password", strPassword),cb);
        final XBean loginParams = new XBean("PhoneNumber", strPhoneNum, "Password", strPassword);
        final XBean profileParams = new XBean("PhoneNumber", strPhoneNum);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                //先登录，不登录也不影响，实际上应该返回用户的基本信息，如：user_id+token(控制api的访问)
                XBean result = invoke("10040", "20040", loginParams);
                if (result != null && result.isEquals("retcode", "succ")) {
                    //获得uuid
                    XBean resultUUid = invoke("10024", "20024", profileParams);
                    if (resultUUid != null && resultUUid.isEquals("retcode", "succ")) {
                        //获得profile成功
                        result.putAll(resultUUid);
                    } else {
                        //
                    }
                }
                if (cb != null) {
                    final XBean result2 = result;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            cb.onResult(result2);
                        }
                    });
                }
            }
        });
    }

    public void bindDevice(String account, final ICallback<XBean> cb) {
        //如果仅仅执行登录，感觉没有做任何事情，什么都不返回，token也没有
        final XBean params = new XBean("account", account);
        //先登录，不登录也不影响，实际上应该返回用户的基本信息，如：user_id+token(控制api的访问)
        invoke("10040", "20040", params, cb);
    }
}
