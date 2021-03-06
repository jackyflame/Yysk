package im.socks.yysk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;

import im.socks.yysk.api.YyskApi;
import im.socks.yysk.data.Proxy;
import im.socks.yysk.util.XBean;

/**
 * Created by cole on 2017/10/24.
 */

public class ProxyListFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProxyAdapter adapter;
    private TextView errorView;
    private TextView loginView;

    private SmartRefreshLayout refreshLayout;

    private final App app = Yysk.app;

    /**
     * 表示选择proxy后，为start还是reload vpn
     */
    private boolean isReloadVpn = false;



    private EventBus.IListener eventListener = new EventBus.IListener() {
        @Override
        public void onEvent(String name, Object data) throws Exception {
            if (Yysk.EVENT_LOGIN.equals(name) || Yysk.EVENT_LOGOUT.equals(name)) {
                doRefresh();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            isReloadVpn = args.getBoolean("is_reload_vpn", false);
        } else {
            isReloadVpn = false;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_proxy_list, container, false);

        loginView = view.findViewById(R.id.loginView);
        errorView = view.findViewById(R.id.errorView);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        adapter = new ProxyAdapter(getActivity());
        recyclerView.setAdapter(adapter);

        loginView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);

        loginView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentStack().show(LoginFragment.newInstance(null), "login", false);
            }
        });
        errorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doRefresh();
            }
        });

        initRefresyLayout(view);
        doRefresh();

        app.getEventBus().on(Yysk.EVENT_ALL, eventListener);

        return view;
    }

    @Override
    public void onDestroyView() {
        if(adapter!=null){
            adapter.destroy();
            adapter=null;
        }
        app.getEventBus().un(Yysk.EVENT_ALL, eventListener);
        super.onDestroyView();
    }

    private FragmentStack getFragmentStack() {
        return ((MainActivity) getActivity()).getFragmentStack();
    }

    private void initRefresyLayout(View view) {
        refreshLayout = view.findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                doRefresh();
            }
        });
    }

    private void doRefresh() {
        if (app.getSessionManager().getSession().isLogin()) {
            String phoneNumber = app.getSessionManager().getSession().user.phoneNumber;
            app.getApi().getProxyList(phoneNumber, new YyskApi.ICallback<List<XBean>>() {
                @Override
                public void onResult(List<XBean> result) {
                    MyLog.d("getProxyList=%s", result);
                    if (result != null && adapter != null) {
                        adapter.setItems(result);
                        refreshLayout.finishRefresh(true);

                        errorView.setVisibility(View.GONE);
                        loginView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);


                        Toast.makeText(getContext(),"为了获得准确的ping时间，建议先断开vpn连接",Toast.LENGTH_LONG).show();

                    } else {
                        //adapter.setItems();
                        refreshLayout.finishRefresh(true);

                        errorView.setText("获得代理列表失败");
                        errorView.setVisibility(View.VISIBLE);
                        loginView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.GONE);
                    }

                }
            });
        } else {
            recyclerView.setVisibility(View.GONE);
            errorView.setVisibility(View.GONE);
            loginView.setVisibility(View.VISIBLE);

        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!app.getVpn().onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static ProxyListFragment newInstance(boolean isReloadVpn) {
        ProxyListFragment fragment = new ProxyListFragment();
        Bundle args = new Bundle();
        args.putBoolean("is_reload_vpn", isReloadVpn);
        fragment.setArguments(args);
        return fragment;
    }


    private class ProxyAdapter extends RecyclerView.Adapter<ProxyHolder> {
        private List<XBean> items = new ArrayList<>();
        private Context context;
        private Ping ping=null;

        public ProxyAdapter(Context context) {
            this.context = context;
        }

        @Override
        public ProxyHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_proxy_list_proxy, viewGroup, false);
            return new ProxyHolder(view);
        }

        @Override
        public void onBindViewHolder(ProxyHolder proxyHolder, int i) {
            XBean item = items.get(i);
            proxyHolder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void setItems(List<XBean> items) {
            stopPing();
            this.items.clear();
            this.items.addAll(items);
            notifyDataSetChanged();
            startPing();
        }

        public void destroy(){
            stopPing();
        }

        private void stopPing(){
            if(ping!=null){
                ping.close();
                ping=null;
            }
        }

        private void startPing(){
            stopPing();
            if(items.isEmpty()){
                return;
            }
            List<String> hosts = new ArrayList<>();
            for(XBean item:items){
                String host = item.getString("host");
                hosts.add(host);
            }
            ping = new Ping();
            ping.setCount(5);
            ping.setTimeout(30);
            ping.ping(hosts, new Ping.IPingListener() {
                @Override
                public void onTime(String host, String time) {
                    updatePingTime(host,time);
                }
            });
        }

        private void updatePingTime(String host,String time){
            for(int i=0;i<items.size();i++){
                XBean item = items.get(i);
                if(item.isEquals("host",host)){
                    item.put("ping_time",time+"ms");
                    notifyItemChanged(i);
                    break;
                }
            }
        }

    }

    private class ProxyHolder extends RecyclerView.ViewHolder {
        private XBean data;

        private TextView nameView;
        private TextView pingTimeView;

        public ProxyHolder(View itemView) {
            super(itemView);
            init();

        }

        private void init() {
            nameView = itemView.findViewById(R.id.nameView);
            pingTimeView = itemView.findViewById(R.id.pingTimeView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSelect();
                }
            });
        }

        public void bind(XBean data) {
            this.data = data;
            nameView.setText(data.getString("name"));
            pingTimeView.setText(data.getString("ping_time","正在测试"));

        }

        private void onSelect() {
            //发出一个事件，然后HomeFragment就可以监听到了
            //Yysk.getProxyManager().select(data);


            Proxy proxy = new Proxy();
            //proxy.id="";
            proxy.name = data.getString("name");
            proxy.data = data;
            proxy.isCustom = false;

            //如果没有登录，返回的是deviceId
            //如果登录了，返回的是phoneNumber
            //proxy.phoneNumber = data.getString("user");

            //所在的activity需要实现onActivityResult => app.getVpn().onActivityResult()

            Activity activity = getActivity();
            getFragmentStack().back();

            app.getSessionManager().setProxy(activity, proxy, isReloadVpn);

        }
    }

}
