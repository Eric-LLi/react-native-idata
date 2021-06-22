// IdataModule.java

package com.idata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.Objects;

import com.idata.util.EmshConstant;
import com.idata.util.EpcData;
import com.idata.util.EpcUtil;
import com.idata.util.MConstant;
import com.idata.util.MLog;
import com.idata.util.MString;
import com.idata.util.MUtil;
import com.idata.util.MyLib;
import com.idata.util.ReadThread;
import com.uhf.api.cls.JniModuleAPI;
import com.uhf.api.cls.Reader;

import android.util.Log;

import static com.idata.util.EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_DSG_UHF;
import static com.idata.util.EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_STANDBY;
import static com.idata.util.MyLib.A5P_ComBaseLin_Device;
import static com.idata.util.MyLib.A5P_Device;
import static com.idata.util.MyLib.A5_Device;

public class IdataModule extends ReactContextBaseJavaModule implements LifecycleEventListener, EpcData {

    private final ReactApplicationContext reactContext;

    private final String LOG = "[IDATA]";
    private final String READER_STATUS = "READER_STATUS";
    private final String TRIGGER_STATUS = "TRIGGER_STATUS";
    private final String WRITE_TAG_STATUS = "WRITE_TAG_STATUS";
    private final String TAG = "TAG";
    private final String TAGS = "TAGS";
    private final String BARCODE = "BARCODE";

    private static final ArrayList<String> cacheTags = new ArrayList<>();
    private static boolean isSingleRead = false;
    private static boolean isReadBarcode = false;
    private static boolean isReading = false;

    private static IdataModule instance = null;

    //Barcode
    private static ScannerInterface scanner;
    private static BroadcastReceiver scanReceiver;
    private static final String RES_ACTION = "android.intent.action.SCANRESULT";

    //RFID
    private static EmshStatusBroadcastReceiver mEmshStatusReceiver;
    private static Reader mReader;
    private static JniModuleAPI jniModuleAPI;
    public static String currentDeviceName = "";
    public static boolean ifPoweron; // power on status
    private static int currentStatue = -1;
    public static boolean ifOpenQuickInventoryMode = false;//Whether to enable the quick inventory mode
//    private static EpcUtil mUtil;

    public IdataModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addLifecycleEventListener(this);
        instance = this;
    }

    @Override
    public String getName() {
        return "Idata";
    }

    public static IdataModule getInstance() {
        return instance;
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private void sendEvent(String eventName, String msg) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, msg);
    }

    private void sendEvent(String eventName, WritableArray array) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, array);
    }

    public void onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 138) {
            if (event.getRepeatCount() == 0) {
                isReading = true;

                if (isReadBarcode) {
                    barcodeRead();
                } else {
                    read();
                }

                WritableMap map = Arguments.createMap();
                map.putBoolean("status", true);
                sendEvent(TRIGGER_STATUS, map);
            }
        }
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 138) {
            if (event.getRepeatCount() == 0) {
                isReading = false;

                if (isReadBarcode) {
                    barcodeCancel();
                } else {
                    cancel();
                }

                WritableMap map = Arguments.createMap();
                map.putBoolean("status", false);
                sendEvent(TRIGGER_STATUS, map);
            }
        }
    }

    @Override
    public void onHostResume() {
        Log.d(LOG, "onHostResume");
//        doConnect();
    }

    @Override
    public void onHostPause() {
        Log.d(LOG, "onHostPause");
//        if (ifPoweron)
//            doDisconnect();
    }

    @Override
    public void onHostDestroy() {
        doDisconnect();
    }

    @ReactMethod
    public void isConnected(Promise promise) {
        promise.resolve(ifPoweron);
    }

    @ReactMethod
    public void connect(Promise promise) {
        try {
            doConnect();
            promise.resolve(true);
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void reconnect() {
        doConnect();
    }

    @ReactMethod
    public void disconnect(Promise promise) {
        try {
            doDisconnect();

            promise.resolve(true);
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void clear() {
        cacheTags.clear();
    }

    @ReactMethod
    public void setSingleRead(boolean enable) {
        isSingleRead = enable;
    }

    @ReactMethod
    public void getDeviceDetails(Promise promise) {
        try {
            if (ifPoweron) {
                short[] st = MyLib.getInstance().getPower();

                WritableMap map = Arguments.createMap();
                map.putString("name", "IData");
                map.putString("mac", "");

                if (st != null) {
                    int rPostion = st[0] / 100;
                    int wPostion = st[1] / 100;

                    map.putInt("antennaLevel", rPostion);

                    promise.resolve(map);
                } else {
                    throw new Exception("Failed to get reader's setting");
                }
            } else {
                throw new Exception("Reader is not connected");
            }
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void setAntennaLevel(int antennaLevel, Promise promise) {
        try {
            if (ifPoweron) {
                short power = (short) (antennaLevel * 100);

                boolean result = MyLib.getInstance().setPower(power, power);

                if (result)
                    promise.resolve(true);
                else
                    throw new Exception("Failed to change antenna power");
            } else {
                throw new Exception("Reader is not connected");
            }
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void softReadCancel(boolean enable, Promise promise) {
        try {
            if (enable)
                read();
            else
                cancel();

            promise.resolve(true);
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void programTag(String oldTag, String newTag, Promise promise) {
        try {
            if (ifPoweron) {
                if (MString.ifHexString(oldTag) && MString.ifHexString(newTag)) {
                    setFilter(oldTag);

                    byte[] postdata = MString.hexToByte(newTag); //data to be written in
                    byte[] pwd = MString.hexToByte("00000000");
                    Thread.sleep(500);
                    boolean isSucceed = MyLib.getInstance().writeTag((char) 1, 2, postdata, pwd);

                    promise.resolve(true);

                    WritableMap map = Arguments.createMap();
                    map.putBoolean("status", isSucceed);
                    map.putString("error", !isSucceed ? "Failed to program tag" : "");
                    sendEvent(WRITE_TAG_STATUS, map);
                } else {
                    throw new Exception("Invalid tags");
                }
            } else {
                throw new Exception("Reader is not connected");
            }
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void setEnabled(boolean enable, Promise promise) {
        try {
            isReadBarcode = !enable;

            promise.resolve(true);
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    private void doConnect() {
        try {
            Intent intent = new Intent(EmshConstant.Action.INTENT_EMSH_REQUEST);
            intent.putExtra(EmshConstant.IntentExtra.EXTRA_COMMAND, EmshConstant.Command.CMD_REQUEST_ENABLE_EMSH_SVC);
            intent.putExtra(EmshConstant.IntentExtra.EXTRA_PARAM_1, 1);
            sendBroadcast(intent);

            if (ifPoweron) {
                doDisconnect();
            }

            //RFID
            if (!ifPoweron) {
                currentDeviceName = (String) MUtil.getInstance().getSystemProp(MConstant.DeviceCode);//to get the device SN.
                mReader = new Reader();
                jniModuleAPI = new JniModuleAPI();

                mEmshStatusReceiver = new EmshStatusBroadcastReceiver();

                Thread.sleep(1000);
                MLog.e("Power ON：" + (ifPoweron = MyLib.getInstance().powerOn()));

                if (!ifPoweron) {
                    //Failed to power on.
                    throw new Exception("Failed to connect reader");
                } else {
                    EpcUtil.getInstance().setEpcData(this);
                }

                ReadThread.init();
                ReadThread.getInstance().start();

                MUtil.getInstance().changCode(true);

                if (currentDeviceName.equals(A5_Device) || currentDeviceName.equals(A5P_Device)
                        || currentDeviceName.equals(A5P_ComBaseLin_Device)) {
                    Log.d(LOG, "come to emsh ");
                    mEmshStatusReceiver = new EmshStatusBroadcastReceiver();
                    IntentFilter intentFilter = new IntentFilter(EmshConstant.Action.INTENT_EMSH_BROADCAST);
                    this.reactContext.registerReceiver(mEmshStatusReceiver, intentFilter);

                    Intent intent2 = new Intent(EmshConstant.Action.INTENT_EMSH_REQUEST);
                    intent.putExtra(EmshConstant.IntentExtra.EXTRA_COMMAND, EmshConstant.Command.CMD_REFRESH_EMSH_STATUS);
                    sendBroadcast(intent2);
                }

                //RFID Default Setting
                setFastMode(1);

                setAreaFrequency(0);

                //Barcode
//            if (scanner == null) {
                scanner = new ScannerInterface(this.reactContext);
//                scanner.open();
//                scanner.lockScanKey();
                scanner.unlockScanKey();
                scanner.setOutputMode(1);

                scanner.enablePlayBeep(false);
                scanner.enableFailurePlayBeep(false);

                IntentFilter intentFilter = new IntentFilter(RES_ACTION);
                scanReceiver = new ScannerResultReceiver();
                this.reactContext.registerReceiver(scanReceiver, intentFilter);
//            }
            }

            WritableMap map = Arguments.createMap();
            map.putBoolean("status", ifPoweron);
            map.putString("error", null);

            sendEvent(READER_STATUS, map);
        } catch (Exception err) {
            WritableMap map = Arguments.createMap();
            map.putString("error", err.getMessage());
            map.putBoolean("status", false);

            sendEvent(READER_STATUS, map);
        }
    }

    private void doDisconnect() {
        //RFID
        if (mReader != null) {
            MUtil.getInstance().changCode(false);

            if (mEmshStatusReceiver != null) {
                this.reactContext.unregisterReceiver(mEmshStatusReceiver);
                mEmshStatusReceiver = null;
            }

            EpcUtil.getInstance().exit();

            mReader = null;
            jniModuleAPI = null;
            currentDeviceName = "";
            ifPoweron = false;
            currentStatue = -1;
//            ifOpenQuickInventoryMode = false;
        }

        //Barcode
        if (scanner != null) {
            this.reactContext.unregisterReceiver(scanReceiver);
            scanner = null;
            scanReceiver = null;
        }

        WritableMap map = Arguments.createMap();
        map.putBoolean("status", false);
        map.putString("error", null);

        sendEvent(READER_STATUS, map);
    }

    private void read() {
        if (ifPoweron) {
            EpcUtil.getInstance().inventoryStart();
        }
    }

    private void cancel() {
        if (ifPoweron) {
            EpcUtil.getInstance().invenrotyStop();
        }
    }

    private void barcodeRead() {
        if (scanner != null) {
            scanner.scan_start();
        }
    }

    private void barcodeCancel() {
        if (scanner != null) {
            scanner.scan_stop();
        }
    }

    public Reader getReader() {
        return mReader;
    }

    public JniModuleAPI getJniModuleAPI() {
        return jniModuleAPI;
    }

    public void sendBroadcast(Intent intent) {
        this.reactContext.sendBroadcast(intent);
    }

    private class ScannerResultReceiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), RES_ACTION)) {
                final String scanResult = intent.getStringExtra("value");
                int barocodelen = intent.getIntExtra("length", 0);
                String type = intent.getStringExtra("type");

                WritableMap map = Arguments.createMap();
                sendEvent(BARCODE, scanResult);
            }
        }
    }

    @Override
    public void getEpcData(ArrayList<String[]> tags) {
        if (isSingleRead) {
            String temp_tag = "";
            int temp_rssi = -1000;

            for (String[] tag : tags) {
                String epc = tag[0];
                String tid = tag[1];
                int rssi = Integer.parseInt(tag[2]);

                if (rssi >= temp_rssi) {
                    temp_tag = epc;
                    temp_rssi = rssi;
                }
            }

            if (addTagToList(temp_tag) && cacheTags.size() == 1) {
                cancel();

                sendEvent(TAG, temp_tag);
            }
        } else {
            if (isReading) {
                ArrayList<String> temp_tags = new ArrayList<>();

                for (String[] tag : tags) {
                    String epc = tag[0];
                    String tid = tag[1];
                    int rssi = Integer.parseInt(tag[2]);

                    if (addTagToList(epc)) {
                        temp_tags.add(epc);
                    }
                }

                if (temp_tags.size() > 0) {
                    sendEvent(TAGS, Arguments.fromList(temp_tags));
                }
            }
        }

//            ArrayList<String> temp_tags = new ArrayList<>();
//
//            for (String[] tag : tags) {
//                String epc = tag[0];
//                String tid = tag[1];
//                int rssi = Integer.parseInt(tag[2]);
//
//                if (addTagToList(epc)) {
//                    temp_tags.add(epc);
//                }
//            }
//
//            if (temp_tags.size() > 0) {
//                sendEvent(TAGS, Arguments.fromList(temp_tags));
//            }

    }

    // Set the session mode
    private void setFastMode(int value) throws Exception {
        boolean isSucceed = MyLib.getInstance().setFastMode(value);

        if (!isSucceed) {
            throw new Exception("Failed to set session");
        }
    }

    //Set frequency
    private void setAreaFrequency(int frequency) throws Exception {
        boolean isSucceed = MyLib.getInstance().setFrequency(frequency);

        if (!isSucceed) {
            throw new Exception("Failed to set frequency");
        }
    }

    private void setFilter(String tag) throws Exception {
        if (!MString.ifHexString(tag)) {
            throw new Exception("Invalid tag");
        }

        //EPC
        int selectBlank = 1;
        int startBits = 32;
        int fneedDataLen = tag.length();

        Reader.TagFilter_ST g2tf = mReader.new TagFilter_ST();
        g2tf.fdata = MString.hexToByte(tag);
        g2tf.flen = fneedDataLen; // Required hex length (equal to the length of the filtering hex string * 4)
        g2tf.isInvert = 0;
        g2tf.bank = selectBlank;
        g2tf.startaddr = startBits;

        boolean isSucceed = MyLib.getInstance().setFilter(g2tf);

        if (!isSucceed) {
            throw new Exception("Failed to program tags");
        }
    }

    private boolean addTagToList(String strEPC) {
        if (strEPC != null) {
            if (!cacheTags.contains(strEPC)) {
                cacheTags.add(strEPC);
                return true;
            }
        }
        return false;
    }

    public class EmshStatusBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (EmshConstant.Action.INTENT_EMSH_BROADCAST.equalsIgnoreCase(intent.getAction())) {

                int sessionStatus = intent.getIntExtra("SessionStatus", 0);
                int batteryPowerMode = intent.getIntExtra("BatteryPowerMode", -1);
                if ((sessionStatus & EmshConstant.EmshSessionStatus.EMSH_STATUS_POWER_STATUS) != 0) {
                    if (batteryPowerMode == currentStatue) { //no processing if in the same state
//                        MLog.e("....SAME STATUS  batteryPowerMode =  " + batteryPowerMode);
                        if (!ifPoweron) {
                            ifPoweron = MyLib.getInstance().powerOn();
                            if (ifPoweron) {
                                //
                            } else {
                                WritableMap map = Arguments.createMap();
                                map.putBoolean("status", false);
                                map.putString("error", "Failed to connect reader");
                                sendEvent(READER_STATUS, map);
                            }
                        }
                        return;
                    }
                    currentStatue = batteryPowerMode;
                    switch (batteryPowerMode) {
                        case EMSH_PWR_MODE_STANDBY:
                            MLog.e("....STANDBY ");
                            ifPoweron = MyLib.getInstance().powerOn();
                            break;
                        case EMSH_PWR_MODE_DSG_UHF:
                            MLog.e("....DSG_UHF ");
                            if (!ifPoweron) {
                                ifPoweron = MyLib.getInstance().powerOn();
                                MLog.e("...power on again powenon = " + ifPoweron); //...power on again

                                if (!ifPoweron) {
                                    WritableMap map = Arguments.createMap();
                                    map.putBoolean("status", false);
                                    map.putString("error", "Failed to connect reader");
                                    sendEvent(READER_STATUS, map);
                                }
                            } else {
//                                MToast.show(R.string.poweron_success);
                            }
                            break;
                    }
                } else {
                    MLog.e("....ERROR STATUS ");
//                    MUtil.getInstance().warningDialog(MainActivity.this);
                }
            }
        }
    }
}
