package mochat.multiple.parallel.whatsclone.component.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import mochat.multiple.parallel.whatsclone.R;
import mochat.multiple.parallel.whatsclone.component.BaseActivity;
import mochat.multiple.parallel.whatsclone.utils.DisplayUtils;
import mochat.multiple.parallel.whatsclone.utils.MD5Utils;
import mochat.multiple.parallel.whatsclone.utils.MLogs;
import mochat.multiple.parallel.whatsclone.utils.PreferencesUtils;
import mochat.multiple.parallel.whatsclone.utils.ResourcesUtil;
import mochat.multiple.parallel.whatsclone.utils.ToastUtils;
import mochat.multiple.parallel.whatsclone.widgets.MenuPopup;
import mochat.multiple.parallel.whatsclone.widgets.WheelView;

import java.util.ArrayList;

/**
 * Created by guojia on 2017/1/2.
 */

public class LockSecureQuestionActivity extends BaseActivity implements View.OnClickListener {

    public final static String EXTRA_IS_SETTING= "is_setting_question";

    private TextView mBtnFinish;

    private View mQuestionLayout;

    private LinearLayout mBtnShowQuestion;

    private EditText mEtQuestion;

    private EditText mEtAnswer;

    private View mDayPickerView;

    private WheelView mWheelMonth;
    private WheelView mWheelDay;

    private int mQuestionId = 0;

    // 是否执行重置逻辑
    private boolean isSettingQuestion = false;

    private boolean mIsBdayQuestion = true;


    private boolean mIsQuestionEditable = false;

    private int mDefaulYear = 2016;
    private int mDefaulMonth = 5;
    private int mDefaultDay = 14;

    private int mSelectedMonth = mDefaulMonth;
    private int mSelectedDay = mDefaultDay;

    private String[] mMonthArray;

    private String[] mQuestionArray;
    private String[] mQuestionByDayArray;

    private Context mContext;

    public static void start(Activity activity, int requestCode, boolean isSettingQuestion) {
        Intent intent = new Intent(activity, LockSecureQuestionActivity.class);
        intent.putExtra(LockSecureQuestionActivity.EXTRA_IS_SETTING, isSettingQuestion);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!isSettingQuestion) {
            if (item.getItemId() == android.R.id.home) {
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        } else {
            ToastUtils.ToastDefult(this, getString(R.string.need_set_secure_answer));
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (isSettingQuestion) {
            ToastUtils.ToastDefult(this, getString(R.string.need_set_secure_answer));
        } else {
            super.onBackPressed();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(R.layout.activity_secure_question);
        Intent intent = getIntent();
        if (intent != null) {
            isSettingQuestion = intent.getBooleanExtra(EXTRA_IS_SETTING, false);
        }
        MLogs.d("onCreate is setting? " + isSettingQuestion);
        mMonthArray = getResources().getStringArray(R.array.month);
        mQuestionArray = getResources().getStringArray(R.array.secure_questions);
        mQuestionByDayArray = getResources().getStringArray(R.array.secure_question_by_day_config);

        initView();
    }

    private void initView() {
        if (isSettingQuestion) {
            View navBtn = findViewById(R.id.navigation_bar);
            if (navBtn != null) {
                navBtn.setVisibility(View.INVISIBLE);
            }
        }
        mBtnFinish = (TextView) findViewById(R.id.btn_finish);
        mBtnShowQuestion = (LinearLayout) findViewById(R.id.btn_show_question);
        mBtnFinish.setOnClickListener(this);

        mQuestionLayout = findViewById(R.id.question_layout);

        mEtQuestion = (EditText) findViewById(R.id.et_question);
        mEtAnswer = (EditText) findViewById(R.id.et_answer);
        mEtQuestion.setFocusable(false);
        int id = PreferencesUtils.getSafeQuestionId(mContext);
        mIsBdayQuestion = mQuestionByDayArray.length > id ? mQuestionByDayArray[id].equals("1"): false;
        if (isSettingQuestion) {
            if ((id == mQuestionArray.length - 1 ) &&
                    (!TextUtils.isEmpty(PreferencesUtils.getCustomizedQuestion(this)))){
                mEtQuestion.setText(PreferencesUtils.getCustomizedQuestion(this));
            } else {
                mEtQuestion.setText(mQuestionArray[id]);
            }
            mEtAnswer.requestFocus();
            mBtnShowQuestion.setOnClickListener(this);
            setTitle(getString(R.string.app_lock_verifier_tile));
        }else {
            mEtQuestion.setFocusable(false);
            mEtQuestion.setFocusableInTouchMode(false);
            if ((id == mQuestionArray.length - 1 ) &&
                    (!TextUtils.isEmpty(PreferencesUtils.getCustomizedQuestion(this)))){
                mEtQuestion.setText(PreferencesUtils.getCustomizedQuestion(this));
            } else {
                mEtQuestion.setText(mQuestionArray[id]);
            }
            mEtAnswer.setHint(ResourcesUtil.getString(R.string.app_lock_safe_answer_reset_hint));
            mBtnShowQuestion.setVisibility(View.GONE);
            setTitle(getString(R.string.app_lock_reset_password));
        }

        if (mIsBdayQuestion) {
            ViewStub stub = (ViewStub) findViewById(R.id.month_day_picker_stub);
            stub.setLayoutResource(R.layout.widget_datetpicker);
            mDayPickerView = stub.inflate();
            mEtAnswer.setVisibility(View.INVISIBLE);
            String existingMonthDay = mEtAnswer.getText().toString();
            int month = -1;
            int day = -1;
            if (!TextUtils.isEmpty(existingMonthDay)) {
                String[] tokens = existingMonthDay.split("/");
                if (tokens.length == 2) {
                    try {
                        month = Integer.valueOf(tokens[0]);
                        day = Integer.valueOf(tokens[1]);
                    } catch (NumberFormatException e) {

                    }
                }
            }
            initDayPicker(month, day);
        }
    }

    private void showQuestionMenu(){
        ArrayList<MenuPopup.MenuPopItem> items = new ArrayList<>();
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = v.getId();
                mIsQuestionEditable = false;
                mIsBdayQuestion = false;
                mEtAnswer.setText("");

                String strQuestion = mQuestionArray[index];
                if (index == mQuestionArray.length - 1){
                    mIsQuestionEditable = true;
                    strQuestion = "";
                    mEtQuestion.setFocusableInTouchMode(true);
                    mEtQuestion.requestFocus();
                }
                mIsBdayQuestion = mQuestionByDayArray.length > index ? mQuestionByDayArray[index].equals("1") : false;

                mQuestionId = index;

                mEtQuestion.setFocusable(mIsQuestionEditable);

                mEtAnswer.setVisibility(mIsBdayQuestion ? View.INVISIBLE : View.VISIBLE);
                if (mDayPickerView != null) {
                    mDayPickerView.setVisibility(mIsBdayQuestion ? View.VISIBLE : View.INVISIBLE);
                }

                mEtQuestion.setText(strQuestion);
                mEtAnswer.requestFocus();
                if (TextUtils.isEmpty(strQuestion)) {
                    mEtAnswer.setText("");
                    mEtQuestion.requestFocus();
                }

                MenuPopup.dismiss();
            }
        };
        MenuPopup.MenuPopItem item = null;
        for (int i = 0; i < mQuestionArray.length;i++){
            item = new MenuPopup.MenuPopItem(i, mQuestionArray[i], clickListener);
            items.add(item);
        }

        if (isFinishing()) {
            return;
        }
        int x = - DisplayUtils.dip2px(this,145);
        MenuPopup.show(mBtnShowQuestion,x,items);
    }


    private void initDayPicker(int month, int day) {
        if (month != -1) {
            mDefaulMonth = month;
        }
        if (day != -1) {
            mDefaultDay = day;
        }
        mSelectedMonth = mDefaulMonth;
        mSelectedDay = mDefaultDay;

        mWheelMonth = (WheelView) findViewById(R.id.month);
        mWheelDay = (WheelView) findViewById(R.id.day);

        mWheelMonth.setData(createMonthString());
        mWheelDay.setData(createDayString(mDefaulYear,mDefaulMonth+1));

        mWheelMonth.setOnSelectListener(new WheelView.OnSelectListener() {
            @Override
            public void endSelect(int id, String text) {
                mWheelDay.setData(createDayString(mDefaulYear,id+1));
                mWheelDay.setDefault(mDefaultDay);
            }
            @Override
            public void selecting(int id, String text) {
            }
        });
        mWheelMonth.setDefault(mDefaulMonth);
        mWheelDay.setDefault(mDefaultDay);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MenuPopup.dismiss();
    }

    private ArrayList<String> createMonthString() {
        ArrayList<String> wheelString = new ArrayList<>();
        if (mMonthArray  == null){
            return wheelString;
        }
        for (String month : mMonthArray){
            wheelString.add(month);
        }
        return wheelString;
    }

    public ArrayList<String> createDayString(int year, int month) {
        ArrayList<String> wheelString = new ArrayList<>();
        int size;
        if (isLeapMonth(month)) {
            size = 31;
        } else if (month == 2) {
            if (isLeapYear(year)) {
                size = 29;
            } else {
                size = 28;
            }
        } else {
            size = 30;
        }

        for (int i = 1; i <= size; i++) {
            wheelString.add(String.format("%2d", i));
        }
        return wheelString;
    }

    /**
     * 计算闰月
     *
     * @param month
     * @return
     */
    private static boolean isLeapMonth(int month) {
        return month == 1 || month == 3 || month == 5 || month == 7
                || month == 8 || month == 10 || month == 12;
    }

    /**
     * 计算闰年
     *
     * @param year
     * @return
     */
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    @Override
    public void onClick(View v) {
        String strQuestion = "";
        String strAnswer;
        switch (v.getId()){
            case R.id.btn_show_question:
                showQuestionMenu();
                break;
            case R.id.btn_finish:
                if (mIsBdayQuestion) {
                    mSelectedMonth = mWheelMonth.getSelected()+1;
                    mSelectedDay = mWheelDay.getSelected()+1;
                    mEtAnswer.setText(mSelectedMonth + "/" + mSelectedDay);
                }

                strQuestion = mEtQuestion.getText().toString().trim();
                strAnswer = mEtAnswer.getText().toString().trim();
                if (TextUtils.isEmpty(strQuestion)) {
                    mEtQuestion.requestFocus();
                    Toast.makeText(LockSecureQuestionActivity.this,
                            ResourcesUtil.getString(R.string.app_lock_safe_question_toast_ask_Question),
                            Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(strAnswer)) {
                    mEtAnswer.requestFocus();
                    Toast.makeText(LockSecureQuestionActivity.this,
                            ResourcesUtil.getString(R.string.app_lock_safe_question_toast_ask_Answer),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // 判断是否进入验证过程
                String answerMd5 = MD5Utils.getStringMd5(strAnswer);
                if (!isSettingQuestion) {
                    if (answerMd5.equals(PreferencesUtils.getSafeAnswer(mContext))) {
                        setResult(Activity.RESULT_OK);
                        PreferencesUtils.setSafeQuestionId(mContext,mQuestionId);
                        PreferencesUtils.setCustomizedQuestion(mContext,strQuestion);
                        PreferencesUtils.setSafeAnswer(mContext, answerMd5);
                        LockPasswordSettingActivity.start(this, true, null, 0);
                        finish();
                    } else {
                        // 没有验证通过，则清掉密码
                        Toast.makeText(LockSecureQuestionActivity.this,
                                ResourcesUtil.getString(R.string.app_lock_safe_question_toast_error),
                                Toast.LENGTH_SHORT).show();
                        mEtAnswer.getText().clear();
                        return;
                    }
                } else {
                    PreferencesUtils.setSafeQuestionId(mContext,mQuestionId);
                    PreferencesUtils.setCustomizedQuestion(mContext,strQuestion);
                    PreferencesUtils.setSafeAnswer(mContext, answerMd5);

                    setResult(Activity.RESULT_OK);
                    finish();
                }
                break;
        }
    }
}
