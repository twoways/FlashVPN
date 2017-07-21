package com.polestar.domultiple.components.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.lody.virtual.client.core.VirtualCore;
import com.polestar.domultiple.AppConstants;
import com.polestar.domultiple.BuildConfig;
import com.polestar.domultiple.R;
import com.polestar.domultiple.billing.BillingConstants;
import com.polestar.domultiple.billing.BillingProvider;
import com.polestar.domultiple.utils.PreferencesUtils;
import com.polestar.domultiple.utils.RemoteConfig;
import com.polestar.domultiple.widget.BlueSwitch;
import com.polestar.domultiple.widget.UpDownDialog;

/**
 * Created by guojia on 2017/7/18.
 */

public class SettingsActivity extends BaseActivity {
    private BlueSwitch shortCutSwich;
    private BlueSwitch gmsSwitch;
    private BlueSwitch adFreeSwitch;
    private TextView versionTv;
    private TextView followTv;
    private String fbUrl;

    private boolean requestAdFree;

    private final static int REQUEST_UNLOCK_SETTINGS = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity_layout);
        initView();
    }

    private void initView() {
        setTitle(getString(R.string.settings));
        versionTv = (TextView)findViewById(R.id.version_info);
        followTv = (TextView)findViewById(R.id.follow_us_txt);
        versionTv.setText(getString(R.string.settings_right) + "\n" + "Version: " + BuildConfig.VERSION_NAME);
        fbUrl = RemoteConfig.getString("fb_follow_page");
        if (fbUrl == null || fbUrl.equals("off")) {
            followTv.setVisibility(View.INVISIBLE);
        }
        shortCutSwich = (BlueSwitch) findViewById(R.id.shortcut_swichbtn);
        shortCutSwich.setChecked(PreferencesUtils.getBoolean(this, AppConstants.KEY_AUTO_CREATE_SHORTCUT,false));
        shortCutSwich.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreferencesUtils.putBoolean(SettingsActivity.this, AppConstants.KEY_AUTO_CREATE_SHORTCUT,shortCutSwich.isChecked());
            }
        });
        gmsSwitch = (BlueSwitch) findViewById(R.id.gms_switch_btn);
        gmsSwitch.setChecked(PreferencesUtils.isGMSEnable());
        DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                boolean orig = PreferencesUtils.isGMSEnable();
                switch (i) {
                    case UpDownDialog.NEGATIVE_BUTTON:
                        break;
                    case UpDownDialog.POSITIVE_BUTTON:
                        PreferencesUtils.setGMSEnable(!orig);
                        VirtualCore.get().restart();
                        boolean newStatus = PreferencesUtils.isGMSEnable();
                        if (newStatus) {
                            Toast.makeText(SettingsActivity.this, getString(R.string.settings_gms_enable_toast), Toast.LENGTH_SHORT);
                        } else {
                            Toast.makeText(SettingsActivity.this, getString(R.string.settings_gms_disable_toast), Toast.LENGTH_SHORT);
                        }
                        break;
                }
                gmsSwitch.setChecked(PreferencesUtils.isGMSEnable());
            }
        };
        gmsSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(PreferencesUtils.isGMSEnable()) {
                    UpDownDialog.show(SettingsActivity.this, getString(R.string.delete_dialog_title), getString(R.string.settings_gms_disable_notice),
                            getString(R.string.no_thanks), getString(R.string.yes), -1,
                            R.layout.dialog_up_down, dialogListener).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            gmsSwitch.setChecked(PreferencesUtils.isGMSEnable());
                        }
                    });
                } else {
                    UpDownDialog.show(SettingsActivity.this, getString(R.string.delete_dialog_title), getString(R.string.settings_gms_enable_notice),
                            getString(R.string.no_thanks), getString(R.string.yes), -1,
                            R.layout.dialog_up_down, dialogListener).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            gmsSwitch.setChecked(PreferencesUtils.isGMSEnable());
                        }
                    });
                }
            }
        });

        adFreeSwitch = (BlueSwitch) findViewById(R.id.adfree_switch);
        adFreeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BillingProvider.get().isAdFreeVIP()) {
                    PreferencesUtils.setAdFree(adFreeSwitch.isChecked());
                    updateBillingStatus();
                } else {
                    PreferencesUtils.updateLastAdFreeDialogTime();
                    UpDownDialog.show(SettingsActivity.this, getString(R.string.adfree_dialog_title), getString(R.string.adfree_dialog_content),
                            getString(R.string.no_thanks), getString(R.string.yes), -1, R.layout.dialog_up_down, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    switch (i) {
                                        case UpDownDialog.POSITIVE_BUTTON:
                                            BillingProvider.get().getBillingManager()
                                                    .initiatePurchaseFlow(SettingsActivity.this, BillingConstants.SKU_AD_FREE, BillingClient.SkuType.INAPP);
                                            requestAdFree = true;
                                            PreferencesUtils.updateAdFreeClickStatus(true);
                                            break;
                                        case UpDownDialog.NEGATIVE_BUTTON:
                                            PreferencesUtils.updateAdFreeClickStatus(false);
                                            break;
                                    }
                                    adFreeSwitch.setChecked(PreferencesUtils.isAdFree());
                                }
                            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            PreferencesUtils.updateAdFreeClickStatus(false);
                            adFreeSwitch.setChecked(PreferencesUtils.isAdFree());
                        }
                    });
                }
            }
        });
        adFreeSwitch.setChecked(PreferencesUtils.isAdFree());
    }

    public void onNotificationSettingClick(View view) {
        Intent notification = new Intent(this, NotificationActivity.class);
        startActivity(notification);
    }

    public void onPrivacyLockerClick(View view) {
        if (PreferencesUtils.isLockerEnabled(this) ) {
            LockPasswordSettingActivity.start(this, false, getString(R.string.lock_settings_title), REQUEST_UNLOCK_SETTINGS);
        } else {
            LockSettingsActivity.start(this,"setting");
        }
    }

    public void onPrivacyPolicyClick(View view) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_TITLE, getString(R.string.privacy_policy));
        intent.putExtra(WebViewActivity.EXTRA_URL, "file:///android_asset/privacy_policy.html");
        startActivity(intent);
    }

    public void onTermsClick(View view) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_TITLE, getString(R.string.terms_of_service));
        intent.putExtra(WebViewActivity.EXTRA_URL, "file:///android_asset/term_of_service.html");
        startActivity(intent);
    }

    public void onJoinUsClick(View view) {
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("https://plus.google.com/communities/104830818454731991305"));
//                intent.putExtra("START_OUTTER_APP_FLAG",true);
            startActivity(intent);
        } catch (Exception localException1) {
            localException1.printStackTrace();
        }
    }

    public void onFollowUsClick(View view) {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo("com.facebook.katana", 0);
            if (packageInfo != null && packageInfo.versionCode >= 3002850) {
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("fb://facewebmodal/f?href=" + fbUrl));
//                intent.putExtra("START_OUTTER_APP_FLAG",true);
                startActivity(intent);
            }else{
                Intent intent = new Intent("android.intent.action.VIEW",Uri.parse(fbUrl));
//                intent.putExtra("START_OUTTER_APP_FLAG",true);
                startActivity(intent);
            }
        } catch (Exception localException1) {
            try {
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(fbUrl));
//                intent.putExtra("START_OUTTER_APP_FLAG",true);
                startActivity(intent);
            } catch (Exception localException2) {
            }
        }
    }

    private void updateBillingStatus() {
        BillingProvider.get().updateStatus(new BillingProvider.OnStatusUpdatedListener() {
            @Override
            public void onStatusUpdated() {
                if (requestAdFree) {
                    if (BillingProvider.get().isAdFreeVIP()) {
                        PreferencesUtils.setAdFree(true);
                    }
                    requestAdFree = false;
                }
                adFreeSwitch.setChecked(PreferencesUtils.isAdFree());
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateBillingStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_UNLOCK_SETTINGS) {
            switch (resultCode) {
                case RESULT_OK:
                    LockSettingsActivity.start(this, "setting");
                    break;
                case RESULT_CANCELED:
                    break;
            }
        }
    }
}