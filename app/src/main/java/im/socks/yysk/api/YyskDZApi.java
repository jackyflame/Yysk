package im.socks.yysk.api;


import java.util.ArrayList;
import java.util.List;

import im.socks.yysk.App;
import im.socks.yysk.AppDZ;
import im.socks.yysk.MyLog;
import im.socks.yysk.util.Json;
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
    /**默认的api url*/
    protected String defaultApiDZUrl = null;

    public YyskDZApi(AppDZ app) {
        super(app);
    }

    @Override
    protected void initApi(App app) {
        super.initApi(app);
        if(app instanceof AppDZ){
            this.app = (AppDZ) app;
        }
        this.defaultApiDZUrl = "http://api.sljjxs.com:8080/ApiServer/SsrHandle?Msg=";
    }

    protected <T> void invokeDZ(final String msgId, final String ackMsgId, final XBean params, final ICallback<T> cb) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final T result = invoke(msgId, ackMsgId, params);
                if (cb != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            cb.onResult(result);
                        }
                    });
                }
            }
        });
    }

    protected <T> T invokeDZ(String msgId, String acKMsgId, XBean params) {
        params.put("DeviceId", deviceId);
        //先使用上一次成功的url
        String oldUrl = apiUrl.get();
        String url = null;
        XBean result = null;
        List<String> urls = new ArrayList<>();
        urls.add(oldUrl);
        urls.add(customApiUrl.get());
        urls.add(defaultApiDZUrl);
        List<String> tryUrls = new ArrayList<>();

        for (String u : urls) {
            if (u != null && !tryUrls.contains(u)) {
                tryUrls.add(u);
                result = invokeApi(u, msgId, params);
                if (result != null) {
                    url = u;
                    break;
                }
            }
        }
        //如果尝试了上面的，还是不能够获得，从微博获得一个
        if (result == null) {
            url = getApiUrlFromBlog();
            if (url != null) {
                result = invokeApi(url, msgId, params);
            }
        }

        if (result != null) {
            //如果获得了结果，表示该url有效，保存下次使用
            apiUrl.set(url);
            //joRet.has("msgid") && joRet.has("msgbody") && ((String)joRet.get("msgid")).equals("20007")
            if (result.isEquals("msgid", acKMsgId)) {
                //XBean or List
                return (T) result.get("msgbody");
            } else {
                return null;
            }
        } else {
            //如果不能够获得，url可能无效了，设置为null
            apiUrl.compareAndSet(oldUrl, null);
            return null;
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
                XBean result = invokeDZ("10040", "20040", loginParams);
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
        invokeDZ("10040", "20040", params, cb);
    }
}
