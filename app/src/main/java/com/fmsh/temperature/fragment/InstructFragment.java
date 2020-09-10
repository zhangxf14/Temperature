package com.fmsh.temperature.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.fmsh.temperature.R;
import com.fmsh.temperature.util.LogUtil;
import com.fmsh.temperature.util.MyConstant;
import com.fmsh.temperature.util.NFCUtils;
import com.fmsh.temperature.util.ToastUtil;
import com.fmsh.temperature.util.UIUtils;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by wyj on 2018/7/9.
 */
public class InstructFragment extends BaseFragment {
    @BindView(R.id.btn_tp)
    Button btnTp;
    @BindView(R.id.btn_pressure)
    Button btnPressure;
    @BindView(R.id.btn_sensor)
    Button btnSensor;
    @BindView(R.id.btn_cell)
    Button btnCell;
    @BindView(R.id.btn_field)
    Button btnField;
    @BindView(R.id.btn_start)
    Button btnStart;
    @BindView(R.id.btn_end)
    Button btnEnd;
    @BindView(R.id.tvContent)
    TextView tvContent;
    Unbinder unbinder;
    @BindView(R.id.btn_read)
    Button btnRead;
    @BindView(R.id.btn_wake)
    Button btnWake;
    Unbinder unbinder1;
    @BindView(R.id.btn_status1)
    Button btnStatus1;
    @BindView(R.id.btn_status2)
    Button btnStatus2;
    Unbinder unbinder2;
    @BindView(R.id.switchRealTime)
    Switch switchRealTime;
    @BindView(R.id.tvID)
    TextView tvID;
    @BindView(R.id.tvType)
    TextView tvType;
    private int type = 0; //指令类型
    private MyReceiver mMyReceiver;

    @Override
    protected int setView() {
        return R.layout.fragment_instruct;
    }

    @Override
    protected void init(View view) {

        MyConstant.ISREALTIME = switchRealTime.isChecked();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("record");
        mMyReceiver = new MyReceiver();
        mContext.registerReceiver(mMyReceiver, intentFilter);

    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        switchRealTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MyConstant.ISREALTIME = isChecked;
            }
        });

    }


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String result = (String) msg.obj;
            //            tvContent.setText(result);
            switch (msg.what) {
                case 7:
                    if (result.contains("0000"))
                        ToastUtil.sToastUtil.shortDuration("开启RTC测温成功");
                    else
                        ToastUtil.sToastUtil.shortDuration("开启RTC测温失败");
                    break;
                case 8:
                    if (result.contains("0100"))
                        ToastUtil.sToastUtil.shortDuration("结束RTC测温成功");
                    else
                        ToastUtil.sToastUtil.shortDuration("结束RTC测温失败");
                    break;
                case 9:
                    String s = NFCUtils.hexStrToBinaryStr(
                            result.substring(
                                    result.length() - 2, result.length())
                                    + result.substring(result.length() - 4, result.length()-2));
                    if (s.length() > 12 && s.substring(12, 13).equals("1"))
                        ToastUtil.sToastUtil.shortDuration("当前处于RTC测温流程");
                    else
                        ToastUtil.sToastUtil.shortDuration("当前处于非测温流程");
                    break;
            }
        }
    };

    private Tag mTag;

    @OnClick({R.id.btn_tp, R.id.btn_pressure, R.id.btn_sensor, R.id.btn_cell, R.id.btn_field, R.id.btn_open, R.id.btn_start, R.id.btn_end,
            R.id.btn_check})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_tp:
                mContext.enableReaderMode();
                MyConstant.MEASURETYPE = 0;
                tvType.setText(UIUtils.getString(R.string.text_main1));
                break;
            case R.id.btn_pressure:
                ToastUtil.sToastUtil.shortDuration("暂未实现");
                break;
            case R.id.btn_sensor:
                ToastUtil.sToastUtil.shortDuration("暂未实现");
                break;
            case R.id.btn_cell:
                ToastUtil.sToastUtil.shortDuration("暂未实现");
                break;
            case R.id.btn_field:
                ToastUtil.sToastUtil.shortDuration("暂未实现");
                break;
            case R.id.btn_open:
                mContext.startReaderModeA();
                MyConstant.MEASURETYPE = 5;
                tvType.setText(UIUtils.getString(R.string.text_open));

                break;
            case R.id.btn_start:
                MyConstant.MEASURETYPE = 0;
                type = 7;
                break;
            case R.id.btn_end:
                ToastUtil.sToastUtil.shortDuration("暂未实现");
                break;
            case R.id.btn_check:
                MyConstant.MEASURETYPE = 0;
                type = 9;
                break;

        }
        UIUtils.setHandler(mHandler);
        if (mContext.mTag != null) {
            String[] techList = mContext.mTag.getTechList();
            for (String tech : techList) {
                LogUtil.d(tech);
                if (tech.contains("NfcV")) {
                    NFCUtils.startV(mContext.mTag, type);
                }
                if (tech.contains("NfcA")) {
                    NFCUtils.startA(mContext.mTag, type);
                }
            }
        }

    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] id = mContext.mTag.getId();
//            if( MyConstant.MEASURETYPE == 0){
//                for (int i = 0; i <id.length / 2 ; i++) {
//                    byte temp = id[i];
//                    id[i] = id[id.length-1 -i];
//                    id[id.length -1 -i] = temp;
//                }
//            }
            tvID.setText(NFCUtils.bytesToHexString(id, ':'));
            Message message = new Message();
            message.obj = mContext.mTag;
            mContext.mCommThread.getWorkerThreadHan().sendMessage(message);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMyReceiver != null) {
            mContext.unregisterReceiver(mMyReceiver);
        }
    }
}
