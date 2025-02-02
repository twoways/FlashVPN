package mochat.multiple.parallel.whatsclone.component.activity;

import java.util.List;

import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.polestar.ad.AdViewBinder;
import com.polestar.ad.adapters.FuseAdLoader;
import com.polestar.ad.adapters.IAdAdapter;
import com.polestar.ad.adapters.IAdLoadListener;
import mochat.multiple.parallel.whatsclone.R;
import mochat.multiple.parallel.whatsclone.utils.DisplayUtils;
import mochat.multiple.parallel.whatsclone.utils.MLogs;
import mochat.multiple.parallel.whatsclone.utils.RemoteConfig;
import mochat.multiple.parallel.whatsclone.utils.ToastUtils;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;


/**
 * Native display interstitial
 *
 * @author
 *
 */
public class NativeInterstitialActivity extends Activity {

    private static final String TAG = NativeInterstitialActivity.class.getName();
    private static final String UNIT_ID = "8998";
    public int BIG_IMG_REQUEST_AD_NUM = 1;
    private RelativeLayout mRlClose;
    private ProgressBar mProgressBar;
    private FuseAdLoader mFuseLoader;
    private NativeExpressAdView mAdmobExpressView;
    private LinearLayout mAdContainer;

    public static final String CONFIG_SLOT_HOME_LUCKY = "slot_home_lucky_new";
    //private static final String CONFIG_SLOT_HOME_LUCKY = "slot_test";

    private static final int MSG_TIMEOUT = 1;
    private static final int DEFAULT_TIMEOUT_DELAY = 30*1000;

    private boolean canceled = false;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case  MSG_TIMEOUT:
                    canceled = true;
                    ToastUtils.ToastDefult(NativeInterstitialActivity.this, getString(R.string.toast_no_lucky));
                    hideLoadding();
                    finish();
                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        canceled = true;
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.native_interstitial);
        initView();
        showLoadding();
        long timeout = RemoteConfig.getLong("config_lucky_timeout");
        MLogs.d("lucky timeout: " + timeout);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_TIMEOUT), timeout == 0? DEFAULT_TIMEOUT_DELAY: timeout);
        setlistener();
        //mvLoadNative();
        mFuseLoader = FuseAdLoader.get(CONFIG_SLOT_HOME_LUCKY, this);
        mFuseLoader.setBannerAdSize(getBannerSize());
        //mFuseLoader.addAdConfig(new AdConfig(AdConstants.AdType.AD_SOURCE_FACEBOOK, "1713507248906238_1787756514814644", -1));
        //mNativeAdLoader.addAdConfig(new AdConfig(AdConstants.AdType.AD_SOURCE_MOPUB, "ea31e844abf44e3690e934daad125451", -1));
        fuseLoadNative();
    }

    private void showLoadding() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoadding() {
        mProgressBar.setVisibility(View.GONE);
    }

    private void setlistener() {
        mRlClose.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                canceled = true;
                finish();
            }
        });
    }

    private void initView() {
        mRlClose = (RelativeLayout) findViewById(R.id.mobvista_interstitial_rl_close);
        mProgressBar = (ProgressBar) findViewById(R.id.mobvista_interstitial_progress);
        mAdContainer = (LinearLayout) findViewById(R.id.ad_container);
    }

    private AdSize getBannerSize(){
        int dpWidth = DisplayUtils.px2dip(this, DisplayUtils.getScreenWidth(this));
        dpWidth = Math.max(280, dpWidth*9/10);
        return new AdSize(dpWidth, 320);
    }

    public void fuseLoadNative() {
        mFuseLoader.loadAd(this, 1, new IAdLoadListener() {
            @Override
            public void onRewarded(IAdAdapter ad) {

            }

            @Override
            public void onAdLoaded(IAdAdapter ad) {
                hideLoadding();
                fillInterstitialLayout(ad);
                mFuseLoader.preloadAd(NativeInterstitialActivity.this);
                mHandler.removeMessages(MSG_TIMEOUT);
//                loadAdmobNativeExpress();
            }

            @Override
            public void onAdClicked(IAdAdapter ad) {

            }

            @Override
            public void onAdClosed(IAdAdapter ad) {

            }

            @Override
            public void onAdListLoaded(List<IAdAdapter> ads) {

            }

            @Override
            public void onError(String error) {
                MLogs.e("Lucky load native error " + error);
            }
        });
    }

    protected void fillInterstitialLayout(IAdAdapter ad) {
        if (ad.isInterstitialAd()) {
            if (!canceled) {
                ad.show();
            }
        } else {
            final AdViewBinder viewBinder = new AdViewBinder.Builder(R.layout.native_interstitial_layout)
                    .titleId(R.id.ad_title)
                    .textId(R.id.ad_subtitle_text)
                    .mainMediaId(R.id.ad_cover_image)
                    .fbMediaId(R.id.ad_fb_mediaview)
                    .admMediaId(R.id.ad_adm_mediaview)
                    .iconImageId(R.id.ad_icon_image)
                    .callToActionId(R.id.ad_cta_text)
                    .privacyInformationId(R.id.ad_choices_image)
                    .starLevelLayoutId(R.id.star_level_layout)
                    .build();
            View adView = ad.getAdView(this, viewBinder);
            mAdContainer.addView(adView);
            mAdContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}