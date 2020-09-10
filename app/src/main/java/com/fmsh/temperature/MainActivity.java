package com.fmsh.temperature;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.fmsh.temperature.fragment.RecordFragment;
import com.fmsh.temperature.fragment.SettingFragment;
import com.fmsh.temperature.fragment.TemperatureMeasuremenFragment;
import com.fmsh.temperature.listener.OnUpdateFragmentListener;
import com.fmsh.temperature.util.LogUtil;
import com.fmsh.temperature.util.NFCUtils;
import com.fmsh.temperature.util.ToastUtil;
import com.fmsh.temperature.util.UIUtils;
import com.fmsh.temperature.util.Util;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.tablayout)
    TabLayout tablayout;
    private String[] title = {UIUtils.getString(R.string.text_main1), UIUtils.getString(R.string.text_main2), UIUtils.getString(R.string.text_main3)};
    private TemperatureMeasuremenFragment mTemperatureMeasuremenFragment;
    private ArrayList<Fragment> mFragments;
    private NfcAdapter mDefaultAdapter;
    private PendingIntent mPendingIntent;
    public String mStrId;
    private OnUpdateFragmentListener mOnUpdateFragmentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initView();
        mDefaultAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mDefaultAdapter == null)
            ToastUtil.sToastUtil.shortDuration("该设备不支持nfc");
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

    }

    private void initView() {
        tablayout.setupWithViewPager(viewPager);
        mTemperatureMeasuremenFragment = new TemperatureMeasuremenFragment();
        RecordFragment recordFragment = new RecordFragment();
        SettingFragment settingFragment = new SettingFragment();
        mFragments = new ArrayList<>();
        mFragments.add(mTemperatureMeasuremenFragment);
        mFragments.add(recordFragment);
        mFragments.add(settingFragment);
        MyPagreFragment myPagreFragment = new MyPagreFragment(getSupportFragmentManager());
        viewPager.setAdapter(myPagreFragment);


    }

    private class MyPagreFragment extends FragmentPagerAdapter {
        public MyPagreFragment(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return title[position];
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDefaultAdapter != null)
            mDefaultAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String[] techList = tag.getTechList();
        for (String tech : techList) {
            LogUtil.d(tech);

        }
        if (tag != null) {
            byte[] id = tag.getId();
            mStrId = Util.bytesToHexString(id, ':');
//           mStrId= NFCUtils.bytesToHexString(id,id.length);
        }
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] duration = {15, 30, 60, 90};
        vibrator.vibrate(duration, -1);
        if (mOnUpdateFragmentListener != null) {
            mOnUpdateFragmentListener.shouwId(mStrId);
            mOnUpdateFragmentListener.tagData(tag);
        }



    }

    @Override
    protected void onPause() {
        if (mDefaultAdapter != null)
            mDefaultAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void setOnUpdateFragmentListener(OnUpdateFragmentListener onUpdateFragmentListener) {
        mOnUpdateFragmentListener = onUpdateFragmentListener;
    }
}
