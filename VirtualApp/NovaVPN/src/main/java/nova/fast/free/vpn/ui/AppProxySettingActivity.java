package nova.fast.free.vpn.ui;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.futuremind.recyclerviewfastscroll.FastScroller;
import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;
import nova.fast.free.vpn.R;
import nova.fast.free.vpn.core.AppInfo;
import nova.fast.free.vpn.core.AppProxyManager;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.android.schedulers.AndroidSchedulers;
import nova.fast.free.vpn.ui.widget.RoundSwitch;
import nova.fast.free.vpn.utils.CommonUtils;
import nova.fast.free.vpn.utils.PreferenceUtils;

/**
 * Created by so898 on 2017/5/3.
 */

public class AppProxySettingActivity extends BaseActivity{
    private View loadingView;
    private RecyclerView appListView;
    private FastScroller fastScroller;
    private AppManagerAdapter adapter;
    private RoundSwitch globalSwitch;
    private View applist;

    public static void start(Activity activity) {
        Intent inent  = new Intent();
        inent.setClass(activity, AppProxySettingActivity.class);
        activity.startActivity(inent);
    }

    @Override
    protected boolean useCustomTitleBar() {
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.layout_apps);
        setTitle(getString(R.string.select_app));
        applist = findViewById(R.id.app_list);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        loadingView = findViewById(R.id.loading);
        appListView = (RecyclerView)findViewById(R.id.list);
        appListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        appListView.setItemAnimator(new DefaultItemAnimator());
        fastScroller = (FastScroller)findViewById(R.id.fastscroller);
        globalSwitch = findViewById(R.id.switch_proxy_allapp);
        globalSwitch.setChecked(PreferenceUtils.isGlobalVPN());
        globalSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PreferenceUtils.setGlobalVPN(b);
                updateAppList();
            }
        });
        updateAppList();

        Observable<List<AppInfo>> observable = Observable.create(new ObservableOnSubscribe<List<AppInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<AppInfo>> appInfo) throws Exception {
                queryAppInfo();
                adapter = new AppManagerAdapter();
                appInfo.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        Observer<List<AppInfo>> observer = new Observer<List<AppInfo>>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onNext(List<AppInfo> aLong) {}

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onComplete() {
                appListView.setAdapter(adapter);
                fastScroller.setRecyclerView(appListView);
                long shortAnimTime = 1;
                appListView.setAlpha(0);
                appListView.setVisibility(View.VISIBLE);
                appListView.animate().alpha(1).setDuration(shortAnimTime);
                loadingView.animate().alpha(0).setDuration(shortAnimTime).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {}

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        loadingView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {}

                    @Override
                    public void onAnimationRepeat(Animator animator) {}
                });
            }
        };
        observable.subscribe(observer);
    }

    private void updateAppList() {
        if(PreferenceUtils.isGlobalVPN()) {
            applist.setVisibility(View.INVISIBLE);
        }else{
            applist.setVisibility(View.VISIBLE);
        }
    }

    public void queryAppInfo() {
        PackageManager pm = this.getPackageManager(); // 获得PackageManager对象
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(pm));
        if (AppProxyManager.Instance.mlistAppInfo != null) {
            AppProxyManager.Instance.mlistAppInfo.clear();
            for (ResolveInfo reInfo : resolveInfos) {
                String pkgName = reInfo.activityInfo.packageName; // 获得应用程序的包名
                if (!CommonUtils.getBlockedApps().contains(pkgName)) {
                    String appLabel = (String) reInfo.loadLabel(pm); // 获得应用程序的Label
                    Drawable icon = reInfo.loadIcon(pm); // 获得应用程序图标
                    AppInfo appInfo = new AppInfo();
                    appInfo.setAppLabel(appLabel);
                    appInfo.setPkgName(pkgName);
                    appInfo.setAppIcon(icon);
                    if (!appInfo.getPkgName().equals("nova.fast.free.vpn"))//App本身会强制加入代理列表
                        AppProxyManager.Instance.mlistAppInfo.add(appInfo);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private ImageView icon = (ImageView)itemView.findViewById(R.id.itemicon);
    private TextView label = (TextView) itemView.findViewById(R.id.app_label);
    private RoundSwitch check = (RoundSwitch)itemView.findViewById(R.id.itemcheck);
    private AppInfo item;
    private Boolean proxied = false;

    AppViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
    }

    void bind(AppInfo app) {
        this.item = app;
        proxied = AppProxyManager.Instance.isAppProxy(app.getPkgName());
        icon.setImageDrawable(app.getAppIcon());
        label.setText(app.getAppLabel());
        check.setChecked(proxied);
    }

    @Override
    public void onClick(View view) {
        if (proxied) {
            AppProxyManager.Instance.removeProxyApp(item.getPkgName());
            check.setChecked(false);
        } else {
            AppProxyManager.Instance.addProxyApp(item.getPkgName());
            check.setChecked(true);
        }
        proxied = !proxied;
    }
}

class AppManagerAdapter extends RecyclerView.Adapter<AppViewHolder> implements SectionTitleProvider {


    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AppViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_apps_item, parent, false));
    }

    @Override
    public void onBindViewHolder(AppViewHolder holder, int position) {
        AppInfo appInfo = AppProxyManager.Instance.mlistAppInfo.get(position);
        holder.bind(appInfo);
    }

    @Override
    public int getItemCount() {
        return AppProxyManager.Instance.mlistAppInfo.size();
    }

    @Override
    public String getSectionTitle(int position) {
        AppInfo appInfo = AppProxyManager.Instance.mlistAppInfo.get(position);
        return appInfo.getAppLabel();
    }
}
