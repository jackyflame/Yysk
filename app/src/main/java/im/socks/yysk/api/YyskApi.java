package im.socks.yysk.api;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.util.Base64;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import im.socks.yysk.App;
import im.socks.yysk.EventBus;
import im.socks.yysk.MyLog;
import im.socks.yysk.Yysk;
import im.socks.yysk.util.IOUtil;
import im.socks.yysk.util.Json;
import im.socks.yysk.util.XBean;

public class YyskApi {

    protected String deviceId;
    /**
     * 默认的api url
     */
    protected String defaultApiUrl = null;

    /**
     * 当前有效的url
     */
    protected AtomicReference<String> apiUrl = new AtomicReference<>(null);
    /**
     * 用户设置的api url
     */
    protected AtomicReference<String> customApiUrl = new AtomicReference<>(null);

    protected ExecutorService executor;
    protected Handler handler = null;
    protected Context context = null;

    protected Http http = null;

    protected App app;

    protected EventBus.IListener eventListener = new EventBus.IListener() {
        @Override
        public void onEvent(String name, Object data) throws Exception {
            if (Yysk.EVENT_SETTINGS.equals(name)) {
                //{name:'',value:''}
                XBean bean = (XBean) data;
                String key = bean.getString("name");
                if ("api_server".equals(key)) {
                    XBean value = bean.getXBean("value");
                    updateCustomApiUrl(value);
                }
            }
        }
    };

    public YyskApi(App app) {
        initApi(app);
    }

    protected void initApi(App app){
        this.app = app;
        this.context = app.getContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newCachedThreadPool();//

        this.deviceId = buildDeviceId();

        //这是默认的
        this.defaultApiUrl = "http://api.tissotlab.com:8080/ApiServer/SsrHandle?Msg=";


        this.http = new Http(app);

        updateCustomApiUrl(app.getSettings().getData().getXBean("api_server"));

        app.getEventBus().on(Yysk.EVENT_SETTINGS, eventListener);
    }


    protected void updateCustomApiUrl(XBean value) {
        if (value == null) {
            setCustomApiUrl(null);
        } else {
            setCustomApiUrl(value.getString("host"), value.getInteger("port", 8080));
        }

    }

    public void destroy() {
        app.getEventBus().un(Yysk.EVENT_SETTINGS, eventListener);
        if (http != null) {
            http.destroy();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }


    protected String buildDeviceId() {
        //参考：http://www.jianshu.com/p/b6f4b0aca6b0
        //TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //String tmDevice;
        //String tmSerial;
        //String tmPhone;

        //tmDevice = "this is a test";
        //tmSerial = "1231231";

        //需要权限READ_PHONE_STATE,Android 6.0 以上需要用户手动赋予该权限
        //可能为null
        //tmDevice = tm.getDeviceId();
        //tmSerial = Build.getSerial();

        //表示是否使用androidId
        boolean useAndroid = true;
        String androidId = Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if ("9774d56d682e549c".equals(androidId)) {
            //以前的老android2.2(api8)可能会返回同一个，新的不会了
        }

        MyLog.d("androidId=%s", androidId);

        if (useAndroid && androidId != null && androidId.length() > 0) {
            try {
                String s = androidId + Build.SERIAL;
                return UUID.nameUUIDFromBytes(s.getBytes("utf-8")).toString();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("", e);
            }
        } else {
            File file = app.getDataFile("device.json");
            XBean data = IOUtil.load(file, XBean.class);
            String deviceId = null;
            if (data != null) {
                deviceId = data.getString("device_id", null);
                //检查是否有效
                if (deviceId != null) {
                    try {
                        UUID.fromString(deviceId);
                    } catch (IllegalArgumentException e) {
                        deviceId = null;
                    }
                }
            }

            if (deviceId == null) {
                deviceId = UUID.randomUUID().toString();
                data = new XBean();
                data.put("device_id", deviceId);
                try {
                    IOUtil.save(Json.stringify(data), file);
                } catch (IOException e) {
                    MyLog.e(e);
                }
            }
            MyLog.d("deviceId=%s", deviceId);
            return deviceId;
        }
    }

    /* 获取权限控制
    * userType: 用户类型 1 普通用户 测试环境不支持，先不使用该接口
    * 返回: 成功返回直接结果{"show_shop":"1", "show_pay":"1", "show_socksim":"1", "show_notice":"1"}，失败返回null
    *
    * */
    public void getPowerCtrl(int userType, ICallback<XBean> cb) {
        invoke("10007", "20007", new XBean("UserType", "" + userType), cb);
    }

    /* 获取支付接口
     * int chanel 支付通道，wx表示微信，alipay表示支付宝，apple表示苹果内购，
     * amount: 支付金额 单位分
     * strPhoneNum: 支付手机号
     * 返回值： 成功返回执行结果{"charge":"微信或支付宝支付订单,苹果为空"},否则返回null
      *  */
    public void createOrder(String strPhoneNum, String channel, int amount, ICallback<XBean> cb) {
        //FIXME 注意：参数的名字为amout，而不是amount，应该是后台拼写错误了
        invoke("10005", "20005", new XBean("PhoneNumber", strPhoneNum, "channel", channel, "amout", "" + amount), cb);

    }

    /* 查询订单状态
    * strOrderNo 订单号
    * 返回值：成功返回{"retcode":"succ/fail", "error":"错误描述"}，否则返回null
    * */
    public void getOrder(String strOrderNo, ICallback<XBean> cb) {
        invoke("10006", "20006", new XBean("order_no", strOrderNo), cb);

    }


    /* 查询用户节点信息
    * strPhoneNum: 登录手机号
    * 返回值: 成功返回[{"authscheme":"","host":"","name":"","password":"","port":"","price":,"ssrObfs":"n","ssrProtocol":"","user":""}],失败返回null
    * */
    public void getProxyList(String strPhoneNum, ICallback<List<XBean>> cb) {
        invoke("10014", "20014", new XBean("PhoneNumber", strPhoneNum), cb);
    }

    /* 用户登录
    * strPhoneNum: 登录手机号
    * String strPassword: 登录用户名
    *  返回值: 成功返回{"retcode":"succ",  ##succ || fail, "error":"" },否则返回null
    * */
    public void login2(String strPhoneNum, String strPassword, final ICallback<XBean> cb) {
        invoke("10020", "20020", new XBean("PhoneNumber", strPhoneNum, "Password", strPassword), cb);
    }

    public void login(String strPhoneNum, String strPassword, final ICallback<XBean> cb) {
        //如果仅仅执行登录，感觉没有做任何事情，什么都不返回，token也没有
        //invoke("10020", "20020", new XBean("PhoneNumber", strPhoneNum, "Password", strPassword),cb);
        final XBean loginParams = new XBean("PhoneNumber", strPhoneNum, "Password", strPassword);
        final XBean profileParams = new XBean("PhoneNumber", strPhoneNum);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                //先登录，不登录也不影响，实际上应该返回用户的基本信息，如：user_id+token(控制api的访问)
                XBean result = invoke("10020", "20020", loginParams);
                if (result != null && result.isEquals("retcode", "succ")) {
                    //获得uuid
                    result = invoke("10024", "20024", profileParams);
                    if (result != null && result.isEquals("retcode", "succ")) {
                        //获得profile成功
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

    /* 用户注册
    * strPhoneNum: 注册手机号
    * strPassword: 登录用户名
    * strVeryCode 验证码
    *  返回值: 成功返回{"retcode":"succ",  ##succ || fail, "error":"" },否则返回null
    * */
    public void register(String strPhoneNum, String strPassword, String strVeryCode, ICallback<XBean> cb) {
        invoke("10022", "20022", new XBean("PhoneNumber", strPhoneNum, "Password", strPassword, "VeryCode", strVeryCode), cb);
    }

    /* 用户修改密码
    * strPhoneNum: 手机号
    * strPassword: 密码
    * strVeryCode: 验证码
    * */
    public void changePassword(String strPhoneNum, String strPassword, String strVeryCode, ICallback<XBean> cb) {
        invoke("10023", "20023", new XBean("PhoneNumber", strPhoneNum, "Password", strPassword, "VeryCode", strVeryCode), cb);
    }


    /* 获取用户配置信息
    * strPhoneNum: 手机号
    * */
    public void getUserProfile(String strPhoneNum, ICallback<XBean> cb) {
        invoke("10024", "20024", new XBean("PhoneNumber", strPhoneNum), cb);

    }


    /**
     * @param strPhoneNum
     * @param strClickType video(点击视频),advert(点击广告),sign(积分商城签到)
     * @param cb
     */
    public void rewardIntegral(String strPhoneNum, String strClickType, ICallback<XBean> cb) {
        invoke("10025", "20025", new XBean("PhoneNumber", strPhoneNum, "clicktype", strClickType), cb);

    }


    /*  获取积分商城免登陆url
    * strPhoneNum:  手机号
    * */
    public void getIntegralShopUrl(String phoneNumber, ICallback<XBean> cb) {
        invoke("10027", "20027", new XBean("PhoneNumber", phoneNumber), cb);
    }

    /*  获取公告信息
    *  phoneNumber:  手机号
    * */
    public void getNoticeInfo(String phoneNumber, ICallback<XBean> cb) {
        invoke("10029", "20029", new XBean("PhoneNumber", phoneNumber), cb);
    }

    /**
     * 获得验证码
     *
     * @param phoneNumber
     * @param cb
     */
    public void getVerifyCode(String phoneNumber, ICallback<XBean> cb) {
        invoke("10021", "20021", new XBean("PhoneNumber", phoneNumber), cb);
    }

    /**
     * 获得后台系统的信息
     *
     * @param phoneNumber
     * @param cb
     */
    public void getSystemInfo(String phoneNumber, ICallback<List<XBean>> cb) {
        invoke("10035", "20035", new XBean("PhoneNumber", phoneNumber), cb);
    }

    /**
     * 获得官网的地址 {url:''}
     *
     * @param phoneNumber
     * @param cb
     */
    public void getSiteUrl(String phoneNumber, ICallback<XBean> cb) {
        invoke("10034", "20034", new XBean("PhoneNumber", phoneNumber), cb);
    }

    /**
     * 获得admob的配置信息
     *
     * @param cb
     */
    public void getAdmob(ICallback<List<XBean>> cb) {
        invoke("10031", "20031", new XBean(), cb);
    }

    /**
     * 获得邀请码{}
     *
     * @param phoneNumber
     * @param cb
     */
    public void getInviteInfo(String phoneNumber, ICallback<XBean> cb) {
        invoke("10032", "20032", new XBean("PhoneNumber", phoneNumber), cb);
    }

    /**
     * 输入邀请码，获得积分
     *
     * @param phoneNumber
     * @param code
     * @param cb
     */
    public void submitInviteCode(String phoneNumber, String code, ICallback<XBean> cb) {
        invoke("10033", "20033", new XBean("PhoneNumber", phoneNumber, "UserSk", code), cb);
    }

    public void getAppVersion(String version,ICallback<XBean> cb){
        invoke("10037","20037",new XBean("Os","android","VersionId",version),cb);
    }

    /**
     *
     * 获得推荐的url
     * @param phoneNumber
     * @param password
     * @param cb {errorcode:200,error:'',spreadurl:''}  200表示成功
     */
    public void getSpreadInfo(String phoneNumber,String password,ICallback<XBean> cb){
        invoke("10038","20083",new XBean("PhoneNumber",phoneNumber,"Password",password));
    }

    /**
     * 获得系统消息的url
     * @param cb
     */
    public void getAnnoUrl(final ICallback<String> cb){
        //先调用一个最简单api，获得有效的url，然后转换
        getSiteUrl("", new ICallback<XBean>() {
            @Override
            public void onResult(XBean result) {
               //不管成功还是失败
                String url = apiUrl.get();
                if(result!=null && url!=null){
                    //http://api.tissotfit.com:8080/ApiServer/SsrHandle?Msg=
                    url = url.replace("SsrHandle?Msg=","AnnoServlet?cmd=list");
                }else{
                    url = "http://api.tissotfit.com:8080/ApiServer/AnnoServlet?cmd=list";
                }
                cb.onResult(url);

            }
        });

    }

    protected <T> void invoke(final String msgId, final String ackMsgId, final XBean params, final ICallback<T> cb) {
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

    protected <T> T invoke(String msgId, String acKMsgId, XBean params) {
        params.put("DeviceId", deviceId);
        params.put("deviceid", deviceId);
        //先使用上一次成功的url
        String oldUrl = apiUrl.get();
        String url = null;
        XBean result = null;
        List<String> urls = new ArrayList<>();
        urls.add(oldUrl);
        urls.add(customApiUrl.get());
        urls.add(defaultApiUrl);
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

    protected XBean invokeApi(String url, String msgId, XBean params) {
        // 补充消息头
        XBean msg = new XBean();
        msg.put("msgid", msgId);
        msg.put("msgbody", params);

        String strJson = Json.stringify(msg);

        String strMsg = encode(strJson);
        // http://host:port/Msg=xxxxxxxx
        String body = http.doGet(url + strMsg);


        if (body != null) {
            String s = decode(body);
            MyLog.d("invokeApi,url=%s,request=%s,response=%s", url, strJson, s);
            XBean rst = Json.parse(s, XBean.class);
            return rst;
        } else {
            //有错误
            MyLog.d("invokeApi,url=%s,request=%s,response=%s", url, strJson, null);
            return null;
        }

    }

    /**
     * 设置指定的apiurl，会尝试使用这个url
     *
     * @param url
     */
    public void setCustomApiUrl(String url) {
        customApiUrl.set(url);
        //如果设置了，就先清除获得的url，让自定义的先使用一次
        apiUrl.set(null);
    }

    public void setCustomApiUrl(String host, int port) {
        //http://api.tissotlab.com:8080/ApiServer/SsrHandle?Msg=
        String url = "http://" + host + ":" + port + "/ApiServer/SsrHandle?Msg=";
        setCustomApiUrl(url);
    }

    /**
     * 从博客中获得url
     *
     * @return
     */
    protected String getApiUrlFromBlog() {
        //http://yuyansuoke.lofter.com/post/1f159cbf_112fea9c
        //String url = decode("aHR0cDovL3l1eWFuc3Vva2UubG9mdGVyLmNvbS9wb3N0LzFmMTU5Y2JmXzExMmZlYTlj");
        String url = "http://yuyansuoke.lofter.com/post/1f159cbf_112fea9c";
        String body = http.doGet(url);
        if (null == body) {
            return null;
        }
        //嘿嘿http://host:port/Msg=哈哈
        String startTag = "嘿嘿";
        String endTag = "哈哈";
        int index1 = body.indexOf(startTag);
        int index2 = body.indexOf(endTag);
        if (index1 < 0 || index2 < 0) {
            return null;
        }
        return body.substring(index1 + startTag.length(), index2);

    }

    protected String encode(String s) {
        try {
            return Base64.encodeToString(s.getBytes("utf-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("", e);
        }
    }

    protected String decode(String s) {
        try {
            byte[] bytes = Base64.decode(s.getBytes("US-ASCII"), Base64.NO_WRAP);
            return new String(bytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("", e);
        }
    }


    public interface ICallback<T> {
        /**
         * 在UI线程执行
         *
         * @param result
         */
        void onResult(T result);
    }


}