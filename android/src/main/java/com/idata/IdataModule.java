// IdataModule.java

package com.idata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import com.idata.util.EmshConstant;
import com.idata.util.EpcData;
import com.idata.util.EpcUtil;
import com.idata.util.MConstant;
import com.idata.util.MUtil;
import com.idata.util.MyLib;
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
    private final String BARCODE = "BARCODE";

    private static ArrayList<String> cacheTags = new ArrayList<>();
    private static boolean isSingleRead = false;
    private static boolean isReadBarcode = false;

    //Barcode
    private static ScannerInterface scanner;
    private static IdataModule instance = null;
    private static IntentFilter intentFilter;
    private static BroadcastReceiver scanReceiver;
    private static final String RES_ACTION = "android.intent.action.SCANRESULT";

    //RFID
    private static EmshStatusBroadcastReceiver mEmshStatusReceiver;
    private static Reader mReader;
    private static JniModuleAPI jniModuleAPI;
    public static String currentDeviceName = "";
    public static boolean poweronStatus; // power on status
    private static int currentStatue = -1;
    private static boolean ifPoweron = false;
    public static boolean ifOpenQuickInventoryMode;//Whether to enable the quick inventory mode
    private static EpcUtil mUtil;

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

    public void onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 190 || keyCode == 188 || keyCode == 189) {
            if (event.getRepeatCount() == 0) {
                WritableMap map = Arguments.createMap();
                map.putBoolean("status", true);
                sendEvent(TRIGGER_STATUS, map);

                if (isReadBarcode) {
                    barcodeRead();
                } else {
//                    read();
                }
            }
        }
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 190 || keyCode == 188 || keyCode == 189) {
            if (event.getRepeatCount() == 0) {
                WritableMap map = Arguments.createMap();
                map.putBoolean("status", false);
                sendEvent(TRIGGER_STATUS, map);

                if (isReadBarcode) {
                    barcodeCancel();
                } else {
//                    cancel();
                }
            }
        }
    }

    @Override
    public void onHostResume() {
        //
    }

    @Override
    public void onHostPause() {
        //
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
        cacheTags = new ArrayList<>();
    }

    @ReactMethod
    public void setSingleRead(boolean enable) {
        isSingleRead = enable;
    }

    private void doConnect() {
        //RFID
        if (mReader == null) {
            mReader = new Reader();
            currentDeviceName = (String) MUtil.getInstance().getSystemProp(MConstant.DeviceCode);//to get the device SN.

            jniModuleAPI = new JniModuleAPI();
            mEmshStatusReceiver = new EmshStatusBroadcastReceiver();
            mUtil = EpcUtil.getInstance();
            poweronStatus = MyLib.getInstance().powerOn();
            Log.d(LOG, "Power ON：" + poweronStatus);

            MUtil.getInstance().changCode(true);

            if (!poweronStatus) {
                //Failed to power on.
            } else {
                //
            }

            if (currentDeviceName.equals(A5_Device) || currentDeviceName.equals(A5P_Device)
                    || currentDeviceName.equals(A5P_ComBaseLin_Device)) {
                Log.d(LOG, "come to emsh ");
                mEmshStatusReceiver = new EmshStatusBroadcastReceiver();
                IntentFilter intentFilter = new IntentFilter(EmshConstant.Action.INTENT_EMSH_BROADCAST);
                this.reactContext.registerReceiver(mEmshStatusReceiver, intentFilter);

                Intent intent = new Intent(EmshConstant.Action.INTENT_EMSH_REQUEST);
                intent.putExtra(EmshConstant.IntentExtra.EXTRA_COMMAND, EmshConstant.Command.CMD_REFRESH_EMSH_STATUS);
                sendBroadcast(intent);
            }
        }

        //Barcode
        if (scanner == null) {
            scanner = new ScannerInterface(this.reactContext);
//            scanner.lockScanKey();
            scanner.unlockScanKey();
            scanner.setOutputMode(1);

            scanner.enablePlayBeep(true);
            scanner.enableFailurePlayBeep(false);

            intentFilter = new IntentFilter(RES_ACTION);
            scanReceiver = new ScannerResultReceiver();
            this.reactContext.registerReceiver(scanReceiver, intentFilter);
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
        }

        //Barcode
        if (scanner != null) {
            this.reactContext.unregisterReceiver(scanReceiver);
            scanner = null;
            intentFilter = null;
            scanReceiver = null;
        }
    }

    private void read() {
        if (mReader != null) {
            mUtil.inventoryStart();
        }
    }

    private void cancel() {
        if (mReader != null) {
            mUtil.invenrotyStop();
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
    public void getEpcData(String[] data) {
        String epc = data[0];
        String tid = data[1];
        Log.d(LOG, epc);
    }

    public class EmshStatusBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (EmshConstant.Action.INTENT_EMSH_BROADCAST.equalsIgnoreCase(intent.getAction())) {

                int sessionStatus = intent.getIntExtra("SessionStatus", 0);
                int batteryPowerMode = intent.getIntExtra("BatteryPowerMode", -1);
                if ((sessionStatus & EmshConstant.EmshSessionStatus.EMSH_STATUS_POWER_STATUS) != 0) {
                    // current status of pistol trigger
                    if (batteryPowerMode == currentStatue) { //no processing if in the same state
                        Log.d(LOG, "....SAME STATUS  batteryPowerMode =  " + batteryPowerMode);
                        if (!ifPoweron) {
                            ifPoweron = MyLib.getInstance().powerOn();
                            if (ifPoweron) {
                                Log.d(LOG, "Power on success");
                            }
                        }
                        return;
                    }
                    currentStatue = batteryPowerMode;
                    switch (batteryPowerMode) {
                        case EMSH_PWR_MODE_STANDBY:
                            Log.d(LOG, "....STANDBY ");
//                            MLog.e("....STANDBY ");
                            ifPoweron = MyLib.getInstance().powerOn();
                            break;
                        case EMSH_PWR_MODE_DSG_UHF:
                            Log.d(LOG, "....DSG_UHF ");
                            if (!ifPoweron) {
                                ifPoweron = MyLib.getInstance().powerOn();
                                Log.d(LOG, "...power on again powenon = " + ifPoweron); //...power on again
                            } else {
                                Log.d(LOG, "Power on success");
                            }
                            break;
                    }
                } else {
                    Log.d(LOG, "....ERROR STATUS ");
                }
            }
        }
    }
}
