package in.dualspace.cloner.components.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import in.dualspace.cloner.AppConstants;
import in.dualspace.cloner.R;
import in.dualspace.cloner.clone.CloneManager;
import in.dualspace.cloner.db.CloneModel;
import in.dualspace.cloner.db.DBManager;
import in.dualspace.cloner.utils.DisplayUtils;
import in.dualspace.cloner.utils.MLogs;
import in.dualspace.cloner.utils.PreferencesUtils;
import in.dualspace.cloner.widget.PackageSwitchListAdapter;
import in.dualspace.cloner.widget.RoundSwitch;

import java.util.List;


/**
 * Created by DualApp on 2017/1/1.
 */

public class LockSettingsActivity extends BaseActivity {

    public static final String EXTRA_KEY_FROM = "from";
    public static final int REQUEST_SET_PASSWORD = 0;
    private Context mContext;

    private RoundSwitch lockerEnableSwitch;
    private LinearLayout detailedSettingLayout;
    private List<CloneModel> mClonedModels;
    private PackageSwitchListAdapter mAppsAdapter;
    private ListView mCloneAppsListView;
    private boolean isSettingChanged = false;
    private String from;
    private Spinner lockIntervalSpinner;

    private final long ARR_INTERVAL[] = {5*1000, 15*1000, 30*1000, 60*1000, 15*60*1000, 30*60*1000, 60*60*1000};

    public static void start(Activity activity, String from) {
        Intent intent = new Intent(activity, LockSettingsActivity.class);
        intent.putExtra(LockSettingsActivity.EXTRA_KEY_FROM, from);
        activity.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_settings);
        setTitle(getResources().getString(R.string.lock_settings_title));
        mContext = this;
        initView();
        initData();
    }

    private int getIntervalIdx(long inverval) {
        int i = 0;
        for (long val: ARR_INTERVAL) {
            if (inverval == val) {
                return i;
            }
            i ++;
        }
        return  -1;
    }

    private void initData() {
        doInitLocker();
        from = getIntent().getStringExtra(LockSettingsActivity.EXTRA_KEY_FROM);
        mClonedModels = CloneManager.getInstance(this).getClonedApps();
        mAppsAdapter = new PackageSwitchListAdapter(LockSettingsActivity.this);
        mAppsAdapter.setModels(mClonedModels);
        mAppsAdapter.setOnCheckStatusChangedListener(new PackageSwitchListAdapter.OnCheckStatusChangedListener() {
            @Override
            public void onCheckStatusChangedListener(CloneModel model, boolean status) {
                //
                isSettingChanged = true;
                if(status) {
                    model.setLockerState(AppConstants.AppLockState.ENABLED_FOR_CLONE);
                } else {
                    model.setLockerState(AppConstants.AppLockState.DISABLED);
                }
                DBManager.updateCloneModel(mContext, model);
            }
        });
        mAppsAdapter.setIsCheckedCallback(new PackageSwitchListAdapter.IsCheckedCallback() {
            @Override
            public boolean isCheckedCallback(CloneModel model) {
                return model.getLockerState() != AppConstants.AppLockState.DISABLED;
            }
        });
        mCloneAppsListView.setAdapter(mAppsAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView(){
        detailedSettingLayout = (LinearLayout)findViewById(R.id.locker_detailed_settings);
        lockerEnableSwitch = (RoundSwitch)findViewById(R.id.enable_lock_switch);
        lockIntervalSpinner = (Spinner) findViewById(R.id.lock_interval_spinner);
        lockerEnableSwitch.setChecked(PreferencesUtils.getBoolean(mContext,AppConstants.PreferencesKey.LOCKER_FEATURE_ENABLED));
        lockerEnableSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSettingChanged = true;
                PreferencesUtils.setLockMainApp(lockerEnableSwitch.isChecked());
            }
        });
        lockerEnableSwitch.setChecked(PreferencesUtils.isMainAppLocked());
        mCloneAppsListView = (ListView)findViewById(R.id.switch_lock_apps);
        lockIntervalSpinner.setSelection(getIntervalIdx(PreferencesUtils.getLockInterval()), true);
        lockIntervalSpinner.setDropDownVerticalOffset(DisplayUtils.dip2px(this, 15));
        lockIntervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                PreferencesUtils.setLockInterval(ARR_INTERVAL[i]);
                isSettingChanged = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void doInitLocker() {
        if (TextUtils.isEmpty(PreferencesUtils.getEncodedPatternPassword(mContext))) {
            LockPasswordSettingActivity.start(this, true, null, REQUEST_SET_PASSWORD);
            Toast.makeText(this,getString(R.string.no_password_set), Toast.LENGTH_SHORT).show();
        }
    }
    private void onLockerEnabled(boolean enabled, boolean report) {
        if (enabled) {
            if (TextUtils.isEmpty(PreferencesUtils.getEncodedPatternPassword(mContext)) ) {
                LockPasswordSettingActivity.start(this, true, null, REQUEST_SET_PASSWORD);
                Toast.makeText(this,getString(R.string.no_password_set), Toast.LENGTH_SHORT).show();
            } else {
                detailedSettingLayout.setVisibility(View.VISIBLE);
                PreferencesUtils.setLockerEnabled(this, true);
            }
        }else{
            detailedSettingLayout.setVisibility(View.GONE);
            PreferencesUtils.setEncodedPatternPassword(this,"");
            PreferencesUtils.setLockerEnabled(this, false);
        }
    }

    private void passwordSetDone(boolean success) {
        if (success
                || (!TextUtils.isEmpty(PreferencesUtils.getEncodedPatternPassword(this)))) {
            detailedSettingLayout.setVisibility(View.VISIBLE);
            PreferencesUtils.setLockerEnabled(this, true);
        } else {
            finish();
        }
    }

    public void onPasswordSettingClick(View view) {
        //PreferencesUtils.setEncodedPatternPassword(mContext,"");
        isSettingChanged = true;
        LockPasswordSettingActivity.start(this, true, null, REQUEST_SET_PASSWORD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MLogs.d("onActivityResult:　" + requestCode + ":" + resultCode);
        switch (requestCode) {
            case REQUEST_SET_PASSWORD:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        passwordSetDone(true);
                        break;
                    case Activity.RESULT_CANCELED:
                        passwordSetDone(false);
                        break;
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
       // if (isSettingChanged) {
            CloneManager.reloadLockerSetting();
        //}
    }
}
