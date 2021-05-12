// IdataModule.java

package com.idata;

import androidx.annotation.Nullable;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;

public class IdataModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;

    private final String LOG = "[IDATA]";
    private final String READER_STATUS = "READER_STATUS";
    private final String TRIGGER_STATUS = "TRIGGER_STATUS";
    private final String WRITE_TAG_STATUS = "WRITE_TAG_STATUS";
    private final String TAG = "TAG";
    private final String BARCODE = "BARCODE";

    private static IdataModule instance = null;

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
        // doDisconnect();
    }
}
