package im.socks.yysk;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import im.socks.yysk.api.YyskApi;
import im.socks.yysk.api.YyskDZApi;
import im.socks.yysk.util.StringUtils;
import im.socks.yysk.util.XBean;

/**
 * Created by Android Studio.
 * ProjectName: Yysk
 * Author: haozi
 * Date: 2018/1/2
 * Time: 17:24
 */

public class LoginFragmentDZ extends Fragment {

    private EditText phoneNumberText;
    private EditText passwordText;


    private final AppDZ app = Yysk.app;

    private String nextAction;

    private boolean isLoading = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            nextAction = args.getString("next_action", null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_dz, container, false);
        View registerButton = view.findViewById(R.id.registerButton);
        View forgetPasswordButton = view.findViewById(R.id.forgetPasswordButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentStack().show(RegisterFragment.newInstance(), null, false);
            }
        });
        forgetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentStack().show(ForgetPasswordFragment.newInstance(), null, false);
            }
        });


        View loginButton = view.findViewById(R.id.loginButton);
        phoneNumberText = view.findViewById(R.id.phoneNumberView);
        passwordText = view.findViewById(R.id.passwordView);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });


        //for test
        //phoneNumberText.setText("18011353062");
        //passwordText.setText("12345678a");

        return view;
    }

    private FragmentStack getFragmentStack() {
        return ((MainActivity) getActivity()).getFragmentStack();
    }

    private void doLogin() {

        final String phoneNumber = phoneNumberText.getText().toString();
        final String password = passwordText.getText().toString();

        YyskDZApi api = app.getApi();

        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setCancelable(false);
        dialog.setMessage("正在登录...");
        dialog.show();

        api.loginDZ(phoneNumber, password, new YyskApi.ICallback<XBean>() {
            @Override
            public void onResult(XBean result) {
                MyLog.d("login=%s",result);
                dialog.dismiss();
                if (result != null) {
                    //登录成功后什么都不返回，正常的实现应该返回一个token，然后其他操作都通过这个token来调用api
                    //
                    if (result.isEquals("retcode", "succ") ||
                            result.isEquals("retcode", "binded")) {
                        //
                        app.getSessionManager().onLogin(result.getString("uuid"), phoneNumber,
                                result.getString("terminal_num"),result.getString("binded_terminal_num"),
                                result.getString("entername"),password);
                        //判断是否绑定(未绑定提示是否绑定)
                        if(result.isEquals("retcode", "succ")){
                            showBindDialog(phoneNumber,result.getString("terminal_num"),
                                    result.getString("binded_terminal_num"));
                        }else{
                            //当前的fragment不需要保留在stack了，所以为替代
                            if ("show_money".equals(nextAction)) {
                                getFragmentStack().show(MoneyFragment.newInstance(), null, true);
                            } else {
                                getFragmentStack().back();
                            }
                        }
                    } else {
                        //登录失败，显示错误信息
                        showError("登录失败：" + result.getString("error"));
                    }
                } else {
                    //登录失败，主要是api调用失败
                    showError("登录失败，请检查你的本地网络是否通畅，或是登录服务器故障需要恢复后重新尝试登录");
                }

            }
        });

    }

    private void showError(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * @param nextAction 表示登录成功后，执行什么操作，如果为null，表示不执行，现在仅仅支持null，show_money
     * @return
     */
    public static LoginFragmentDZ newInstance(String nextAction) {
        LoginFragmentDZ fragment = new LoginFragmentDZ();
        Bundle args = new Bundle();
        args.putString("next_action", nextAction);
        fragment.setArguments(args);
        return fragment;
    }

    private void showBindDialog(final String account, String terminal_num, String binded_terminal_num){
        if(StringUtils.isInteger(terminal_num) || StringUtils.isInteger(binded_terminal_num)){
            new AlertDialog.Builder(getContext())
                    .setTitle("提醒")
                    .setMessage("获取绑定设备数据失败，请稍后再试")
                    .setPositiveButton("确定",null)
                    .show();
        }else{
            int terminalNum = Integer.valueOf(terminal_num);
            int bindedNum = Integer.valueOf(binded_terminal_num);
            if(terminalNum <= bindedNum){
                new AlertDialog.Builder(getContext())
                        .setTitle("提醒")
                        .setMessage("企业绑定设备数已用完，请通知管理员进行处理！")
                        .setPositiveButton("确定",new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                getFragmentStack().back();
                            }
                        })
                        .show();
            }else{
                new AlertDialog.Builder(getContext())
                        .setTitle("提醒")
                        .setMessage("是否绑定新的终端设备？")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                doBindDevice(account);
                            }
                        })
                        .setNegativeButton("取消",new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                getFragmentStack().back();
                            }
                        })
                        .show();
            }
        }
    }

    private void doBindDevice(String account){

        YyskDZApi api = app.getApi();

        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setCancelable(false);
        dialog.setMessage("正在绑定...");
        dialog.show();

        api.bindDevice(account, new YyskApi.ICallback<XBean>() {
            @Override
            public void onResult(XBean result) {
                MyLog.d("bindDevice=%s",result);
                dialog.dismiss();
                if (result != null) {
                    if (result.isEquals("retcode", "succ")) {
                        Toast.makeText(getContext(),"绑定成功",Toast.LENGTH_SHORT).show();
                        getFragmentStack().back();
                    }else{
                        showError("绑定失败：" + result.getString("error"));
                    }
                } else {
                    showError("绑定失败，请检查你的本地网络是否通畅，或是服务器故障需要恢复后重新尝试");
                }
            }
        });
    }
}