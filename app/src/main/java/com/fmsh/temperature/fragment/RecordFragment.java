package com.fmsh.temperature.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.fmsh.temperature.R;
import com.fmsh.temperature.activity.ChartActivity;
import com.fmsh.temperature.adapter.RecordAdapter;
import com.fmsh.temperature.listener.OnItemClickListener;
import com.fmsh.temperature.util.DialogUtils;
import com.fmsh.temperature.util.LogUtil;
import com.fmsh.temperature.util.MyConstant;
import com.fmsh.temperature.util.NFCUtils;
import com.fmsh.temperature.util.SpUtils;
import com.fmsh.temperature.util.TimeUitls;
import com.fmsh.temperature.util.ToastUtil;
import com.fmsh.temperature.util.UIUtils;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import butterknife.BindView;
import butterknife.Unbinder;

/**
 * Created by wyj on 2018/7/2.
 */
public class RecordFragment extends BaseFragment {


    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.graphView)
    GraphView graphView;
    @BindView(R.id.realGraphView)
    GraphView realGraphView;
    private RecordAdapter mRecordAdapter;
    private LineGraphSeries<DataPointInterface> mSeries;
    private Runnable mTimer;
    private double graphLastXValue = 0d;
    private MyReceiver mMyReceiver;
    private double mTp;
    private  Handler mHandler =new MyHandler(this);
    private LineGraphSeries<DataPoint> mSeries1;
    private int type = -1;
    private final Calendar calendar = Calendar.getInstance();
    private static String timeHex;
    private AlertDialog.Builder mBuilder;
    private AlertDialog mAlertDialog;

    @Override
    protected int setView() {
        return R.layout.fragment_record;
    }

    @Override
    protected void init(View view) {

        recyclerView.setVisibility(View.GONE);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecordAdapter = new RecordAdapter(mContext);
        recyclerView.setAdapter(mRecordAdapter);
        mRecordAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void itemClickListener(int position) {
                startActivity(new Intent(mContext, ChartActivity.class));
            }
        });
        mMyReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("record");
        mContext.registerReceiver(mMyReceiver,intentFilter);

        initGraph(graphView,true);
        initGraph(realGraphView,false);

    }
    private void initGraph(GraphView graphView ,boolean show){

       Viewport mViewport = graphView.getViewport();
//        viewport.setScalable(true);
//        viewport.setScrollable(true);
        mViewport.setYAxisBoundsManual(true);
        mViewport.setMinY(-40);
        mViewport.setMaxY(80);
        mViewport.setXAxisBoundsManual(true);
        if(!show){
            mViewport.setMinX(0);
            mViewport.setMaxX(4);
        }


        GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
        gridLabelRenderer.setLabelVerticalWidth(80);
        gridLabelRenderer.setGridColor(R.color.nxp_blue);
        gridLabelRenderer.setHighlightZeroLines(false);
        gridLabelRenderer.setHorizontalLabelsColor(R.color.nxp_blue);
        gridLabelRenderer.setVerticalLabelsColor(R.color.nxp_blue);
        gridLabelRenderer.setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        if(show)
        gridLabelRenderer.setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity(), new SimpleDateFormat("HH:mm:ss")));
        gridLabelRenderer.setNumVerticalLabels(25);
        if(show)
        gridLabelRenderer.setNumHorizontalLabels(5);
        gridLabelRenderer.setHorizontalLabelsAngle(30);
        gridLabelRenderer.setHumanRounding(true,false);
        if(show)
        gridLabelRenderer.setHorizontalLabelsVisible(false);
        gridLabelRenderer.reloadStyles();


        mTimer = new Runnable() {
            @Override
            public void run() {
                try{
                    graphLastXValue += 1d;
                    mSeries.appendData(new DataPoint(graphLastXValue, mTp), true, 100*1024);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        };
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser){
            type = 2;
            if(MyConstant.ISREALTIME){
                graphView.setVisibility(View.GONE);
                realGraphView.setVisibility(View.VISIBLE);
            }else {
                graphView.setVisibility(View.VISIBLE);
                realGraphView.setVisibility(View.GONE);
            }
        }else {
            type = -1;
        }
    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }
    public void onResume() {
        super.onResume();

    }

    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mTimer);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mMyReceiver !=  null)
            mContext.unregisterReceiver(mMyReceiver);
    }

    private class MyReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            graphLastXValue = 0;
            realGraphView.removeAllSeries();
            mSeries = new LineGraphSeries<>();
            mSeries.setDrawDataPoints(true);
            mSeries.setDrawBackground(false);
            mSeries.setColor(0xFFFF0000);
            mSeries.setDataPointsRadius(20f);
            realGraphView.addSeries(mSeries);
            mSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    double y = dataPoint.getY();
                    ToastUtil.sToastUtil.shortDuration(y+"℃");
                }
            });
            Message message = new Message();
            message.obj = mContext.mTag;
            message.what = type;
            UIUtils.setHandler(mHandler);
            mContext.mCommThread.getWorkerThreadHan().sendMessage(message);
            if(!MyConstant.ISREALTIME && type == 2 && MyConstant.MEASURETYPE == 0) {
                DialogUtils.createLoadingDialog(mContext, "温度读取中...");
            }

        }
    }
    private Map<Date,Double> mMap = new HashMap();
    private List<Date> mDateList = new ArrayList<>();
    private List<Double> mTpList = new ArrayList<>();
    private  static class MyHandler extends Handler{

        private final WeakReference<RecordFragment> mRecordFragmentWeakReference;

        public MyHandler(RecordFragment recordFragment){
            mRecordFragmentWeakReference = new WeakReference<>(recordFragment);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            RecordFragment recordFragment = mRecordFragmentWeakReference.get();
            if(recordFragment != null){
                switch (msg.what){
                    case -1:
                        recordFragment.mHandler.removeCallbacks(recordFragment.mTimer);
                        break;
                    case 1:
                        recordFragment.mTp = (Double) msg.obj;
                        recordFragment.mHandler.postDelayed(recordFragment.mTimer, 0);
                        break;
                    case 2:
                        DialogUtils.closeDialog();
                        if( recordFragment.mAlertDialog != null)
                        recordFragment.mAlertDialog.dismiss();
                        recordFragment.mTpList.clear();
                        recordFragment.mDateList.clear();
                        StringBuffer sb = (StringBuffer) msg.obj;
                        String value = sb.toString();
                        //
                        StringBuilder ssb=new StringBuilder();
                        if(value .contains(",")){
                            String[] split = value.split(",");
                            int length = split[0].length();
                            for (int i = 0; i <length ; i+=8) {
                                if (i+8>length) break;
                                double tp=NFCUtils.strFromat(split[0].substring(i, i + 4));
                                recordFragment.calendar.add(Calendar.SECOND,1);
                                recordFragment.mMap.put(recordFragment.calendar.getTime(),tp);
                                recordFragment.mDateList.add(recordFragment.calendar.getTime());
                                recordFragment.mTpList.add(tp);
                                //
                                ssb.append(split[0].substring(i, i + 8)+" ");
                            }
                            LogUtil.d(split[0]);
                            LogUtil.d(ssb.toString());
                            if(recordFragment.mTpList.size() > 0)
                                recordFragment.addSeriesData(split);
                        }
                        break;
                    case 3:
                        DialogUtils.closeDialog();
                        break;
                    case 15:
                        timeHex = (String) msg.obj;
                        LogUtil.d("timeHex"+ timeHex);
                        break;
                }

            }

        }
    }

    private void addSeriesData(String [] split){
//        DataPoint[] dataPoint = new DataPoint[mMap.size()];
//        int size = 0;
//        for (Map.Entry<Date,Double> map:mMap.entrySet()) {
//           dataPoint[size++] = new DataPoint(map.getKey(),map.getValue());
//
//        }
        DataPoint[] dataPoints = new DataPoint[mTpList.size()];
        for (int i = 0; i <mTpList.size() ; i++) {
            dataPoints[i] = new DataPoint(mDateList.get(i),mTpList.get(i));

        }
        graphView.removeAllSeries();
        LineGraphSeries lineGraphSeries = new LineGraphSeries(dataPoints);
        lineGraphSeries.setDrawDataPoints(true);
        lineGraphSeries.setDataPointsRadius(3.5f);
//        lineGraphSeries.setBackgroundColor(0x77000000 | 0x7bb1db);
        lineGraphSeries.setDrawBackground(false);
        lineGraphSeries.setColor(0xFFFF0000);
        lineGraphSeries.setAnimated(true);
        graphView.addSeries(lineGraphSeries);
        graphView.getViewport().setMinX(mDateList.get(0).getTime());
        graphView.getViewport().setMaxX(mDateList.get(mDateList.size()-1).getTime());
        lineGraphSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                ToastUtil.sToastUtil.shortDuration(dataPoint.getY()+"℃");
            }
        });
        int count = Integer.parseInt(split[1]);
        int max = Integer.parseInt(split[2],16);
        int min = Integer.parseInt(split[3],16) > 128 ? -(256 -Integer.parseInt(split[3],16)):Integer.parseInt(split[3],16);
        int tpTime = Integer.parseInt(split[4]);
        long l = Long.parseLong(split[5], 16) * 1000;
        String startTime = TimeUitls.formatDateTime(l);
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(String.format("当前测温次数%d次\n",count));
        stringBuffer.append(String.format("当前设置温度最大值%d℃\n",max));
        stringBuffer.append(String.format("当前设置温度最小值%d℃\n",min));
        stringBuffer.append(String.format("当前测温度间隔时间%ds\n",tpTime));
        stringBuffer.append(String.format("当前测温开始时间%s\n",startTime));

        if(Integer.parseInt(split[1]) > 0)
        showDailog(stringBuffer.toString());

    }

    private void showDailog(String data){
        if(mAlertDialog != null){
            mBuilder = null;
            mAlertDialog = null;
        }
        mBuilder = new AlertDialog.Builder(mContext, R.style.dialog);
        mAlertDialog = mBuilder.create();
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.dialog_hint, null);
        TextView tvContent = inflate.findViewById(R.id.tvContent);
        tvContent.setText(data);
        TextView tvCancel=inflate.findViewById(R.id.tvCancel);
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog.dismiss();
            }
        });
        mAlertDialog.show();
        Window window = mAlertDialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setContentView(inflate);
        window.setWindowAnimations(R.style.dialogAnimation);
        WindowManager windowManager = mContext.getWindowManager();
        Display defaultDisplay = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.width = defaultDisplay.getWidth();
        attributes.height = (int) (defaultDisplay.getHeight() *0.5);
        window.setAttributes(attributes);

    }


}
