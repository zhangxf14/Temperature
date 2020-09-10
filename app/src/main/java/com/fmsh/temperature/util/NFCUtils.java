package com.fmsh.temperature.util;

import android.app.Dialog;
import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcV;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.util.Size;

import com.fmsh.temperature.R;
import com.fmsh.temperature.listener.OnItemClickListener;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created by wyj on 2018/7/6.
 */
public class NFCUtils {

    private static CommomDialog commomDialog;

    public static String getRfid(Tag tag) {
        byte[] id = null;
        if (tag != null) {
            id = tag.getId();
        }
        return bytesToHexString(id, ':');

    }


    /**
     * 读取芯片15693芯片数据
     *
     * @param tag
     * @return
     */
    public static void readNfcVData(final Tag tag, int type) {
        final NfcV nfcV = NfcV.get(tag);
        final byte[] id = tag.getId();
        StringBuffer sb = new StringBuffer();
        try {
            if (nfcV != null) {
                if (nfcV.isConnected())
                    nfcV.close();
                nfcV.connect();
                if (MyConstant.ISREALTIME && type == 2) {
                    final Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                nfcV.connect();
                                byte[] bytes = instructV(11, id);
                                byte[] transceive = nfcV.transceive(bytes);
                                String hexString = getHexString(transceive, transceive.length);
                                LogUtil.d(hexString);
                                SystemClock.sleep(400);
                                if (hexString != null && hexString.contains("FAFF")) {
                                    byte[] transceive1 = nfcV.transceive(instructV(12, id));
                                    double v = strFromat(getHexString(transceive1, transceive1.length));
                                    // TODO: 2018/7/13 温度结果处理
                                    if (UIUtils.getHandler() != null) {
                                        Message message = new Message();
                                        message.what = 1;
                                        message.obj = v;
                                        UIUtils.getHandler().sendMessage(message);
                                    }
                                    handler.postDelayed(this, 1000);
                                } else {
                                    ToastUtil.sToastUtil.shortDuration("测温错误");
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Message message = new Message();
                                message.what = -1;
                                UIUtils.getHandler().sendMessage(message);

                            } finally {
                                try {
                                    nfcV.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    handler.postDelayed(runnable, 1000);

                } else {
                    if (type == 2 && UIUtils.getHandler() != null) {

                        byte[] res = new byte[0];
                        byte[] bytes2 = new byte[]{0x22, (byte) 0xb1, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xb0, 0x54, 0x00, (byte) 0x03};
                        System.arraycopy(id, 0, bytes2, 3, id.length);
                        res = nfcV.transceive(bytes2);
                        if (res != null) {
                            String hexString = getHexString(res, res.length);
                            String substring = hexString.substring(hexString.length() - 2, hexString.length());
                            int size = Integer.parseInt(substring, 16) * 1024; //存储的数据区域大小

                            byte[] transceive = nfcV.transceive(instructV(14, id));
                            String hexString1 = getHexString(transceive, transceive.length).substring(2);
                            //测温次数
                            int tpCount = Integer.parseInt(hexString1.substring(2, 4) + hexString1.substring(0, 2), 16);

                            //测温间隔 ,测温时间
                            byte[] transceive1 = nfcV.transceive(instructV(21, id));
                            String hexString2 = getHexString(transceive1, transceive1.length).substring(2);
                            String startTime = hexString2.substring(4, 12);
                            int tpTime = Integer.parseInt(hexString2.substring(hexString2.length() - 4), 16);

                            String[] split = bytesToHexString(nfcV.transceive(instructV(18, id)), ':').substring(3).split(":");
//                            tpCount =4864;测试用
                            if (tpCount * 4 > size)
                                addDataV(size, id, nfcV, sb);
                            else if (tpCount > 0)
                                addDataV(tpCount * 4, id, nfcV, sb);

                            sb.append(",").append(tpCount).append(",").append(split[0]).append(",")
                                    .append(split[2]).append(",").append(tpTime).append(",").append(startTime);
                            if (UIUtils.getHandler() != null && type == 2) {
                                Message message = new Message();
                                message.what = 2;
                                message.obj = sb;
                                UIUtils.getHandler().sendMessage(message);

                            }
                        }
                    } else if (type == 3) {
                        byte[] bytes = instructV(1, id);
                        nfcV.transceive(bytes);
                        byte[] transceive = nfcV.transceive(instructV(2, id));
                        String string = getHexString(transceive, transceive.length);
                        LogUtil.d(string);
                        if (string.contains("5555")) {
                            nfcV.transceive(instructV(3, id));
                            nfcV.transceive(instructV(5, id));
                            nfcV.transceive(instructV(13, id));
                            nfcV.transceive(instructV(16, id));
                            nfcV.transceive(instructV(17, id));
                            nfcV.transceive(instructV(19, id));
                            nfcV.transceive(instructV(20, id));
                            nfcV.transceive(instructV(22, id));

                            byte[] transceive1 = nfcV.transceive(instructV(7, id));
                            String hexString = getHexString(transceive1, transceive1.length);
                            if (hexString.contains("0000"))
                                ToastUtil.sToastUtil.shortDuration("开启测温成功");
                            else
                                ToastUtil.sToastUtil.shortDuration("开启测温失败");
                        } else {
                            ToastUtil.sToastUtil.shortDuration("配置失败");
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (type == 3) {
                ToastUtil.sToastUtil.shortDuration("配置失败");
            }
            if (type == 2) {
                Message message = new Message();
                message.what = 2;
                message.obj = sb;
                UIUtils.getHandler().sendMessage(message);
                ToastUtil.sToastUtil.shortDuration("读取失败,请重试!");
            }
        } finally {
            try {
                nfcV.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static void addDataV(int size, byte[] id, NfcV nfcV, StringBuffer sb) throws IOException {
        // x 高位 y 地位 ,n 读取大小,count 次数,p 余数
        int x = 0x10, y = 0x00, n = 0xf8, count = 1, p = 0;
        p = size % 248;
        count = size / 248;
        if (p != 0) {
            count = count + 1;
        }
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                y = 0x00;
            } else if (i == 1) {
                y = y + 0xfc;
            } else {
                x = x + 0x01;
                y = y - 0x04;
            }
            if (p != 0 && i == count - 1) {
                n = p - count * 4;
            }

            int firstAddress=(int)((x & 0xff)<<8)+y;
            int numOfByte=n<0? (byte)(n&0xff):n;

            if (firstAddress+numOfByte>0x5c00){//该配置下，最大有效地址为5c00H.
                numOfByte=(0x5c00-firstAddress-4);
                n=numOfByte;
            }
            if (firstAddress>=0x5c00) {
                break;
            }
            if (i==76){
                System.out.println(""+i);
            }
            int hAddr=(firstAddress & 0xFF00)>>8;
            int lAddr =firstAddress & 0x00ff;
//            byte[] bytes1 = new byte[]{0x22, (byte) 0xb1, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) x, (byte) y, 0x00, (byte) n};
            byte[] bytes1 = new byte[]{0x22, (byte) 0xb1, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) hAddr, (byte) lAddr, 0x00, (byte) n};
            System.arraycopy(id, 0, bytes1, 3, id.length);
            byte[] transceive = nfcV.transceive(bytes1);
            String hexString1 = getHexString(transceive, transceive.length);
            if (hexString1.equals("000300")) {
                break;
            }
            sb.append(hexString1.substring(2, hexString1.length()));
            LogUtil.d(hexString1);

        }
    }

    /**
     * 指令类型处理
     *
     * @param type
     * @param id
     * @return
     */
    public static byte[] instructV(int type, byte[] id) {
        byte[] bytes = null;
        switch (type) {
            case 1: //唤醒
                bytes = new byte[]{0x22, (byte) 0xc4, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x00};
                break;
            case 2://查看唤醒
                bytes = new byte[]{0x22, (byte) 0xc4, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x80};
                break;
            case 3: //延时
                bytes = new byte[]{0x22, (byte) 0xc5, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xc0, (byte) 0x84, (byte) 0x00, (byte) SpUtils.getIntValue(MyConstant.delayTime, 0)};
                break;
            case 4: //查看延时
                bytes = new byte[]{0x22, (byte) 0xc6, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xc0, (byte) 0x84};
                break;
            case 5: //测温间隔
                bytes = new byte[]{0x22, (byte) 0xc5, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xc0, (byte) 0x85, (byte) 0x00, (byte) SpUtils.getIntValue(MyConstant.intervalTime, 1)};
                break;
            case 6: //查看测温间隔
                bytes = new byte[]{0x22, (byte) 0xc6, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xc0, (byte) 0x85};
                break;
            case 7: //开始rtc
                bytes = new byte[]{0x22, (byte) 0xc2, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x00, 0, 0, 0, 0};
                break;
            case 8: //结束rtc测温
                bytes = new byte[]{0x22, (byte) 0xc2, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x80, 0, 0, 0, 0};
                break;
            case 9://查看电池,等状态
                bytes = new byte[]{0x22, (byte) 0xcf, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x00, (byte) 0x00, 0x00};
                break;
            case 10://读取数据
                bytes = new byte[]{0x22, (byte) 0xb1, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x10, (byte) 0x00, 0x00, (byte) 0xfa};
                break;
            case 11://开始实时测温
                bytes = new byte[]{0x22, (byte) 0xc0, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x06, (byte) 0x00};
                break;
            case 12://获取实时测温结果
                bytes = new byte[]{0x22, (byte) 0xc0, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x84, (byte) 0x00};
                break;
            case 13: //设置测温次数
                bytes = new byte[]{0x22, (byte) 0xb3, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xb0, (byte) 0x94, 0x01, (byte) getCount(SpUtils.getIntValue(MyConstant.tpCount, 10))[1], (byte) getCount(SpUtils.getIntValue(MyConstant.tpCount, 10))[0]};
                break;
            case 14: //查看测温次数
                bytes = new byte[]{0x22, (byte) 0xb1, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xb0, (byte) 0x94, 0x00, (byte) 0x3};
                break;
            case 15:
                bytes = new byte[]{0x22, (byte) 0xb1, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0x00, 0x26, 0x00, 0x07};
                break;
            case 16://设置最小温度值
                bytes = new byte[]{0x22, (byte) 0xb3, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xb0, (byte) 0x8e, 0x01, (byte) SpUtils.getIntValue(MyConstant.tpMin, 0), 0x00};
                break;
            case 17: // 设置最大温度值
                bytes = new byte[]{0x22, (byte) 0xb3, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xb0, (byte) 0x8c, 0x01, (byte) SpUtils.getIntValue(MyConstant.tpMax, 40), 0x00};
                break;
            case 18://获取温度值
                bytes = new byte[]{0x22, (byte) 0xb1, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xb0, (byte) 0x8c, 0x00, 0x03};
                break;
            case 19: //写温度大小值
                bytes = new byte[]{0x22, (byte) 0xb3, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x00, (byte) 0x1e, 0x03, 0x00, (byte) SpUtils.getIntValue(MyConstant.tpMin, 0), 0x00, (byte) SpUtils.getIntValue(MyConstant.tpMax, 40)};
                break;
            case 20: //写测温间隔
                bytes = new byte[]{0x22, (byte) 0xb3, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x00, (byte) 0x22, 0x01, 0x00, (byte) SpUtils.getIntValue(MyConstant.intervalTime, 1)};
                break;
            case 21: // 。
                bytes = new byte[]{0x22, (byte) 0xb1, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x00, (byte) 0x18, 0x00, 0x0b};
                break;
            case 22://写时间
                bytes = new byte[]{0x22, (byte) 0xb3, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, 0x00, 0x1a, 0x03, (byte) getTimeHex()[0], (byte) getTimeHex()[1], (byte) getTimeHex()[2], (byte) getTimeHex()[3]};
                break;
            case 23: //读取时间
                bytes = new byte[]{0x22, (byte) 0xb1, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, 0x00, (byte) 0x18, 0x00, 0x07};
                break;
            case 24: //休眠
                bytes = new byte[]{0x22, (byte) 0xc3, 0x1d, 0, 0, 0, 0, 0, 0, 0, 0, 0x01};
                break;

        }
        System.arraycopy(id, 0, bytes, 3, id.length);
        return bytes;
    }

    private static int[] getTimeHex() {
        int[] ints = new int[4];
        long aLong = System.currentTimeMillis() / 1000;
        String string = Long.toHexString(aLong);
        int a = Integer.parseInt(string.substring(0, 2), 16),
                b = Integer.parseInt(string.substring(2, 4), 16),
                c = Integer.parseInt(string.substring(4, 6), 16),
                d = Integer.parseInt(string.substring(6, 8), 16);
        LogUtil.d(string);
        ints[0] = a;
        ints[1] = b;
        ints[2] = c;
        ints[3] = d;
        return ints;
    }

    /**
     * 读取14443芯片数据
     *
     * @param tag
     * @return
     */
    public static void readNfcAData(final Tag tag, int type) {
        final NfcA nfcA = NfcA.get(tag);
        StringBuffer sb = new StringBuffer();
        try {
            if (nfcA != null) {
                if (nfcA.isConnected())
                    nfcA.close();
                nfcA.connect();
                if (MyConstant.ISREALTIME && type == 2) {
                    final Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                nfcA.connect();
                                byte[] bytes = instructA(11);
                                byte[] transceive = nfcA.transceive(bytes);
                                String hexString = getHexString(transceive, transceive.length);
                                LogUtil.d(hexString);
                                SystemClock.sleep(400);
                                if (hexString != null && hexString.contains("FAFF")) {
                                    byte[] transceive1 = nfcA.transceive(instructA(12));
                                    double v = strFromat(getHexString(transceive1, transceive1.length));
                                    // TODO: 2018/7/13 温度结果处理
                                    if (UIUtils.getHandler() != null) {
                                        Message message = new Message();
                                        message.what = 1;
                                        message.obj = v;
                                        UIUtils.getHandler().sendMessage(message);
                                    }
                                    handler.postDelayed(this, 1000);
                                } else {
                                    ToastUtil.sToastUtil.shortDuration("测温错误");
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Message message = new Message();
                                message.what = -1;
                                UIUtils.getHandler().sendMessage(message);

                            } finally {
                                try {
                                    nfcA.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    handler.postDelayed(runnable, 1000);

                } else {
                    if (type == 2 && UIUtils.getHandler() != null) {

                        byte[] res = new byte[0];
                        byte[] bytes = new byte[]{0x40, (byte) 0xb1, (byte) 0xb0, 0x54, 0x00, (byte) 0x03, 0x00};
                        res = nfcA.transceive(bytes);
                        if (res != null) {
                            String hexString = getHexString(res, res.length);
                            String substring = hexString.substring(hexString.length() - 2, hexString.length());
                            int size = Integer.parseInt(substring, 16) * 1024; //存储的数据区域大小

                            byte[] transceive = nfcA.transceive(instructA(14));
                            String hexString1 = getHexString(transceive, transceive.length);
                            //测温次数
                            int tpCount = Integer.parseInt(hexString1.substring(2, 4) + hexString1.substring(0, 2), 16);

                            byte[] transceive1 = nfcA.transceive(instructA(21));
                            String hexString2 = getHexString(transceive1, transceive1.length);
                            String startTime = hexString2.substring(0, 8);
                            int tpTime = Integer.parseInt(hexString2.substring(hexString2.length() - 8, hexString2.length() - 4), 16);

                            String[] split = bytesToHexString(nfcA.transceive(instructA(18)), ':').split(":");

                            if (tpCount * 4 > size)
                                getTpDataA(nfcA, size, sb);
                            else if (tpCount > 0)
                                getTpDataA(nfcA, tpCount * 4, sb);
                            sb.append(",").append(tpCount).append(",").append(split[0]).append(",")
                                    .append(split[2]).append(",").append(tpTime).append(",").append(startTime);
                            if (UIUtils.getHandler() != null && type == 2) {
                                Message message = new Message();
                                message.what = 2;
                                message.obj = sb;
                                UIUtils.getHandler().sendMessage(message);

                            }
                        }
                    } else if (type == 3) {
                        nfcA.transceive(instructA(1));
                        byte[] transceive = nfcA.transceive(instructA(2));
                        String string = getHexString(transceive, transceive.length);
                        LogUtil.d(string);
                        if (string.contains("5555")) {
                            nfcA.transceive(instructA(3));
                            nfcA.transceive(instructA(5));
                            nfcA.transceive(instructA(13));
                            nfcA.transceive(instructA(16));
                            nfcA.transceive(instructA(17));
                            nfcA.transceive(instructA(19));
                            nfcA.transceive(instructA(20));
                            nfcA.transceive(instructA(22));
                            byte[] transceive1 = nfcA.transceive(instructA(7));
                            String string1 = getHexString(transceive1, transceive.length);
                            if (string1.contains("0000"))
                                ToastUtil.sToastUtil.shortDuration("开启测温成功");
                            else
                                ToastUtil.sToastUtil.shortDuration("开启测温失败");

                        } else {
                            ToastUtil.sToastUtil.shortDuration("配置失败");
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (type == 3) {
                ToastUtil.sToastUtil.shortDuration("配置失败");
            }
            if (type == 2) {
                Message message = new Message();
                message.what = 2;
                message.obj = sb;
                UIUtils.getHandler().sendMessage(message);
                ToastUtil.sToastUtil.shortDuration("读取失败,请重试!");
            }
        } finally {

            try {
                nfcA.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void getTpDataA(NfcA nfcA, int size, StringBuffer sb) throws IOException {
        // x 高位 y 地位 ,n 读取大小,count 次数,p 余数
        int x = 0x10, y = 0x00, n = 0xf8, count = 1, p = 0;
        p = size % 248;
        count = size / 248;
        if (p != 0) {
            count = count + 1;
        }
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                y = 0x00;
            } else if (i == 1) {
                y = y + 0xfc;
            } else {
                x = x + 0x01;
                y = y - 0x04;
            }
            if (p != 0 && i == count - 1) {
                n = p - count * 4;
            }
            byte[] bytes = new byte[]{0x40, (byte) 0xb1, (byte) x, (byte) y, (byte) 0x00, (byte) n, 0x00};
            byte[] transceive = nfcA.transceive(bytes);
            if (transceive != null) {
                String hexString1 = getHexString(transceive, transceive.length);
                LogUtil.d(hexString1);
                sb.append(hexString1);
            }

        }


    }

    private static void addDataA(int count, int address, int p, NfcA nfcA, StringBuffer sb) throws IOException {
        int x = address;
        int y = 0x00;
        int n = 0xf8;
        for (int i = 0; i < count; i++) {
            byte[] bytes = new byte[0];
            if (i == 0) {
                y = 0x00;
            } else if (i == 1) {
                y = y + 0xfc;
            } else {
                x = x + 0x01;
                y = y - 0x04;
            }
            if (p != 0 && i == count - 1) {
                n = p - 4;
            }
            bytes = new byte[]{0x40, (byte) 0xb1, (byte) x, (byte) y, (byte) 0x00, (byte) n, 0x00};
            byte[] transceive = nfcA.transceive(bytes);
            if (transceive != null) {
                String hexString1 = getHexString(transceive, transceive.length);
                LogUtil.d(hexString1);
                sb.append(hexString1);


            }


        }
    }

    public static void startV(Tag tag, int type) {
        NfcV nfcV = NfcV.get(tag);
        byte[] id = tag.getId();
        try {
            nfcV.connect();
            byte[] bytes = instructV(type, id);
            byte[] transceive = nfcV.transceive(bytes);
            String hexString = getHexString(transceive, transceive.length);
            LogUtil.d(hexString);
            Message message = new Message();
            message.what = type;
            message.obj = hexString.substring(2, hexString.length());
            if (UIUtils.getHandler() != null)
                UIUtils.getHandler().sendMessage(message);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                nfcV.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void startA(Tag tag, int type) {
        NfcA nfcA = NfcA.get(tag);
        try {
            nfcA.connect();
            byte[] bytes = instructA(type);
            byte[] transceive = nfcA.transceive(bytes);
            String hexString = getHexString(transceive, transceive.length);
            LogUtil.d(hexString);
            Message message = new Message();
            message.obj = hexString;
            message.what = type;
            if (UIUtils.getHandler() != null)
                UIUtils.getHandler().sendMessage(message);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                nfcA.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] instructA(int type) {
        byte[] bytes = null;
        switch (type) {
            case 1://唤醒指令
                bytes = new byte[]{0x40, (byte) 0xc4, 0x00, 0x00, 0x00, 0x00, 0x00};
                break;
            case 2://查看唤醒状态
                bytes = new byte[]{0x40, (byte) 0xc4, (byte) 0x80, 0x00, 0x00, 0x00, 0x00};
                break;
            case 3://配置延时测温时间 m
                bytes = new byte[]{0x40, (byte) 0xc5, (byte) 0xc0, (byte) 0x84, 0x00, (byte) SpUtils.getIntValue(MyConstant.delayTime, 0), 0x00};
                break;
            case 4://查看延时测温时间
                bytes = new byte[]{0x40, (byte) 0xc6, (byte) 0xc0, (byte) 0x84, 0x00, 0x00, 0x00};
                //                System.arraycopy(id,0,bytes,2,id.length);
                break;
            case 5://配置测温间隔时间 s
                bytes = new byte[]{0x40, (byte) 0xc5, (byte) 0xc0, (byte) 0x85, 0x00, (byte) SpUtils.getIntValue(MyConstant.intervalTime, 1), 0x00};
                break;
            case 6://查看测温间隔时间
                bytes = new byte[]{0x40, (byte) 0xc6, (byte) 0xc0, (byte) 0x85, 0x00, 0x00, 0x00};
                break;
            case 7: //开启rtc测温
                bytes = new byte[]{0x40, (byte) 0xc2, (byte) 0x00, (byte) 0x00, 0x00, 0x00, 0x00};
                break;
            case 8:// 结束rtc测温
                bytes = new byte[]{0x40, (byte) 0xc2, (byte) 0x80, (byte) 0x00, 0x00, 0x00, 0x00};
                break;
            case 9://查看状态结果
                bytes = new byte[]{0x40, (byte) 0xcf, (byte) 0x00, (byte) 0x00, 0x00, 0x00, 0x00};
                break;
            case 10: //读取rtc测温数据
                bytes = new byte[]{0x40, (byte) 0xb1, (byte) 0x10, 0x00, 0x00, (byte) 0xfa, 0x00};
                break;
            case 11: //启动实时测温
                bytes = new byte[]{0x40, (byte) 0xc0, 0x06, 0x00, 0x00, 0x00, 0x00};
                break;
            case 12: // 获取实时测温数据
                bytes = new byte[]{0x40, (byte) 0xc0, (byte) 0x84, 0x00, 0x00, 0x00, 0x00};
                break;
            case 13://设置测温次数
                bytes = new byte[]{0x40, (byte) 0xb3, (byte) 0xb0, (byte) 0x94, 0x01, 0x00, 0x00, (byte) getCount(SpUtils.getIntValue(MyConstant.tpCount, 10))[1], (byte) getCount(SpUtils.getIntValue(MyConstant.tpCount, 10))[0]};
                break;
            case 14: //查看测温次数
                bytes = new byte[]{0x40, (byte) 0xb1, (byte) 0xb0, (byte) 0x94, 0x00, (byte) 0x3, 0x00};
                break;
            case 15: //获取测温时间
                bytes = new byte[]{0x40, (byte) 0xb1, 0x00, 0x26, 0x00, 0x07, 0x00};
                break;
            case 16: // 设置最小温度值
                bytes = new byte[]{0x40, (byte) 0xb3, (byte) 0xb0, (byte) 0x8e, 0x01, 0x00, 0x00, (byte) SpUtils.getIntValue(MyConstant.tpMin, 0), 0x00};
                break;
            case 17: // 设置最大温度值
                bytes = new byte[]{0x40, (byte) 0xb3, (byte) 0xb0, (byte) 0x8c, 0x01, 0x00, 0x00, (byte) SpUtils.getIntValue(MyConstant.tpMax, 40), 0x00};
                break;
            case 18: //获取最大最小温度值
                bytes = new byte[]{0x40, (byte) 0xb1, (byte) 0xb0, (byte) 0x8c, 0x00, 0x03, 0x00};
                break;
            case 19: //写到block9温度大小值
                bytes = new byte[]{0x40, (byte) 0xb3, (byte) 0x00, (byte) 0x24, 0x03, 0x00, 0x00, 0x00, (byte) SpUtils.getIntValue(MyConstant.tpMin, 0), 0x00, (byte) SpUtils.getIntValue(MyConstant.tpMax, 40)};
                break;
            case 20: //写测温block10间隔
                bytes = new byte[]{0x40, (byte) 0xb3, (byte) 0x00, (byte) 0x28, 0x03, 0x00, 0x00, 0x00, (byte) SpUtils.getIntValue(MyConstant.intervalTime, 1), 0x12, 03};
                break;
            case 21: //读取测温间隔
                bytes = new byte[]{0x40, (byte) 0xb1, 0x00, 0x20, 0x00, 0x0b, 0x00};
                break;
            case 22: //写时间
                bytes = new byte[]{0x40, (byte) 0xb3, (byte) 0x00, (byte) 0x20, 0x03, 0x00, 0x00, (byte) getTimeHex()[0], (byte) getTimeHex()[1], (byte) getTimeHex()[2], (byte) getTimeHex()[3]};
                break;
            case 23: //读测温时间
                bytes = new byte[]{0x40, (byte) 0xb1, 0x00, 0x20, 0x00, 0x03, 0x00};
                break;
            case 24: //休眠
                bytes = new byte[]{0x40, (byte) 0xc3, 0x01, 0, 0, 0, 0};
                break;


        }
        return bytes;
    }

    private static int[] getCount(int count) {
        int[] ints = {0, 0};
        if (count <= 255) {
            ints[1] = count;
        } else {
            ints[0] = count / 256;
            ints[1] = count % 256;
        }
        return ints;
    }

    public static void writeData(Tag tag, int count) {
        NfcA nfcA = NfcA.get(tag);
        try {
            nfcA.connect();
            int address = 3;
            for (int i = 0; i < count; i++) {
                address = i * 4;
                byte[] bytes = new byte[]{0x40, (byte) 0xb3, 0x10, (byte) address, (byte) 0x03, 0, 0, 0, 0, 0, 0};
                byte[] transceive = nfcA.transceive(bytes);
                String hexString = getHexString(transceive, transceive.length);
                LogUtil.d(hexString);
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                nfcA.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public static float strFromat(String str) {
        float result = 0;
        LogUtil.d(str);
        String substring = str.substring(str.length() - 4, str.length() - 2);
        String substring1 = str.substring(str.length() - 2, str.length());
        String newstr = substring1 + substring;
        char[] chars = substring1.toCharArray();
        char nu = '2';
        if (chars[1] >= nu) {

            int i = -((0xffff - Integer.parseInt(newstr, 16)) & 0x03ff) - 1;
            result = (float) (i / 4.00);
        } else {
            int a = Integer.parseInt(substring, 16);
            result = (float) (a / 4.00);
        }
      LogUtil.d(result+"");
        return result;
    }

    public static String getTpData(String valueTp) {


        return null;
    }


    /**
     * byte数组转十六进制字符串
     *
     * @param bytes     数组
     * @param separator 分隔符
     * @return
     */
    public static String bytesToHexString(byte[] bytes, char separator) {
        String s = "0";
        StringBuilder hexString = new StringBuilder();
        if ((bytes != null) && (bytes.length > 0)) {
            for (byte b : bytes) {
                int n = b & 0xff;
                if (n < 0x10) {
                    hexString.append("0");
                }
                hexString.append(Integer.toHexString(n));
                if (separator != 0) {
                    hexString.append(separator);
                }
            }
            s = hexString.substring(0, hexString.length() - 1);
        }
        return s;
    }

    // Hex help
    private static final byte[] HEX_CHAR_TABLE = {(byte) '0', (byte) '1',
            (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6',
            (byte) '7', (byte) '8', (byte) '9', (byte) 'A', (byte) 'B',
            (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F'};

    public static String getHexString(byte[] raw, int len) {
        byte[] hex = new byte[2 * len];
        int index = 0;
        int pos = 0;

        for (byte b : raw) {
            if (pos >= len)
                break;

            pos++;
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }

        return new String(hex);
    }

    public static double getRTC(String tp) {
        double result = 0;
        if (tp != null && tp.length() == 4) {


        }
        return result;

    }

    /**
     * 将十六进制的字符串转换成二进制的字符串
     *
     * @param hexString
     * @return
     */
    public static String hexStrToBinaryStr(String hexString) {

        if (hexString == null || hexString.equals("")) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        // 将每一个十六进制字符分别转换成一个四位的二进制字符
        for (int i = 0; i < hexString.length(); i++) {
            String indexStr = hexString.substring(i, i + 1);
            String binaryStr = Integer.toBinaryString(Integer.parseInt(indexStr, 16));
            while (binaryStr.length() < 4) {
                binaryStr = "0" + binaryStr;
            }
            sb.append(binaryStr);
        }

        return sb.toString();
    }


    public static void readNdefData(Tag tag, Context context) {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                NdefRecord[] records = ndefMessage.getRecords();
                StringBuffer stringBuffer = new StringBuffer();
                for (int i = 0; i < records.length; i++) {
                    String s = ReadOrWriteNFCUtil.parseTextRecord(records[i]);
                    stringBuffer.append(s);
                }
                String[] string = stringBuffer.toString().split(":");
                String uid = string[1].substring(0, 14);
                int count = Integer.parseInt(string[1].substring(15), 16);
                String state = "";
                if (string[1].contains("x"))
                    state = "On";
                else
                    state = "Off";
                String value = String.format("UID:%s\n\nCount:%d\n\nState:%s\n\nContext:%s", uid, count, state, string[0]);

                //                if (stringBuffer.toString().contains("x")) {
                //                    value = UIUtils.getString(R.string.text_connet);
                //                } else {
                //                    value = UIUtils.getString(R.string.text_disconnet);
                //                }
                if (commomDialog != null) {
                    commomDialog.dismiss();
                    commomDialog = null;
                }
                commomDialog = new CommomDialog(context, R.style.dialog, R.layout.dialog_prompt, "");

                commomDialog.setOnCloseListener(new CommomDialog.OnCloseListener() {
                    @Override
                    public void onClick(Dialog dialog, boolean confirm) {
                        dialog.dismiss();
                    }
                });

                commomDialog.show();
                commomDialog.contentTxt.setText(value);


            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
