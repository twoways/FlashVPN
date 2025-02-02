package mochat.multiple.parallel.whatsclone.component.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.polestar.clone.client.core.VirtualCore;
import com.polestar.clone.remote.InstalledAppInfo;
import mochat.multiple.parallel.whatsclone.BuildConfig;
import mochat.multiple.parallel.whatsclone.R;
import mochat.multiple.parallel.whatsclone.component.BaseActivity;
import mochat.multiple.parallel.whatsclone.utils.MLogs;
import mochat.multiple.parallel.whatsclone.utils.PreferencesUtils;
import mochat.multiple.parallel.whatsclone.utils.ToastUtils;

public class FeedbackActivity extends BaseActivity {

    private Context mContext;
    private EditText mEtFeedback;
    private Button mBtSubmit;
    private TextView mGoFAQ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        mContext = this;
        initView();
    }

    private void initView() {
        setTitle(getString(R.string.feedback));

        mEtFeedback = (EditText) findViewById(R.id.et_feedback);
        mBtSubmit = (Button) findViewById(R.id.bt_submit);
        mGoFAQ = (TextView) findViewById(R.id.tv_go_faq);
        mBtSubmit.setEnabled(false);

        SpannableString spanText=new SpannableString(getString(R.string.feedback_go_faq));
        spanText.setSpan(new ClickableSpan() {

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE);       //设置文件颜色
                ds.setUnderlineText(true);      //设置下划线
            }

            @Override
            public void onClick(View view) {
                Intent intentToFAQ = new Intent(FeedbackActivity.this, FaqActivity.class);
                startActivity(intentToFAQ);
            }
        }, spanText.length() - 4, spanText.length()-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mGoFAQ.setText(spanText);
        mGoFAQ.setHighlightColor(Color.TRANSPARENT);
        mGoFAQ.setMovementMethod(LinkMovementMethod.getInstance());

        mEtFeedback.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 0) {
                    mBtSubmit.setEnabled(true);
                } else {
                    mBtSubmit.setEnabled(false);
                }
            }
        });

        mBtSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = mEtFeedback.getText().toString();
                if (content == null) {
                    ToastUtils.ToastDefult(mContext, getResources().getString(R.string.feedback_no_description));
                    return;
                }

                Intent data=new Intent(Intent.ACTION_SENDTO);
                data.setData(Uri.parse("mailto:doriscoco.studio@gmail.com"));
                data.putExtra(Intent.EXTRA_SUBJECT, "Feedback about WhatsClone");
                String fullContent = content + "\n\n\n\n"  + "Additional Info: \n" + "WhatsClone version: " + BuildConfig.VERSION_NAME
                        + "\n" + "Model info: " + Build.FINGERPRINT + "\nGMS state: " + PreferencesUtils.isGMSEnable() + "\n";
                for (InstalledAppInfo appInfo: VirtualCore.get().getInstalledApps(0)) {
                    String pkgInfo = "\n Package: " + appInfo.packageName + " path: " + appInfo.apkPath;
                    fullContent += pkgInfo;
                }

                data.putExtra(Intent.EXTRA_TEXT, fullContent);
                try {
                    startActivity(data);
                }catch (Exception e) {
                    MLogs.e("Start email activity fail!");
                    ToastUtils.ToastDefult(FeedbackActivity.this, getResources().getString(R.string.submit_success));
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
