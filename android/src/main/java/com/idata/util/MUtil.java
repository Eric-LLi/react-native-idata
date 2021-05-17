package com.idata.util;

import android.content.Intent;
import android.text.TextUtils;

import com.idata.IdataModule;

import java.lang.reflect.Method;

import static com.idata.IdataModule.currentDeviceName;
import static com.idata.util.MyLib.A5P_ComBaseLin_Device;
import static com.idata.util.MyLib.A5P_Device;

public class MUtil {

    public static MUtil getInstance() {
        return MySingleton.singleton;
    }

    private static class MySingleton {
        final static MUtil singleton = new MUtil();
    }

    private MUtil() {
    }

    //Modify the parameters of the pistol trigger
    private static final String ACTION_KEYBD_REMAP = "android.intent.extend.KEYBD_REMAP";
    private static final String INTENT_EXTRA_COMMAND = "cmd";
    private static final String INTENT_EXTRA_PARAM_1 = "arg1";
    private static final String INTENT_EXTRA_PARAM_2 = "arg2";

    /**
     * modify the key value (disable it,the previous modification will be invalid)
     *
     * @param bEnable: true->enable ，false->disable
     */
    private void kpd_enableKeybd(boolean bEnable) {
        if (!currentDeviceName.equals(A5P_Device) && !currentDeviceName.equals(A5P_ComBaseLin_Device)) {
            return;
        }
        Intent intent = new Intent(ACTION_KEYBD_REMAP);
        intent.putExtra(INTENT_EXTRA_COMMAND, "enable");
        intent.putExtra(INTENT_EXTRA_PARAM_1, (bEnable ? 1 : 0)); // 0: disable / 1: enable
        IdataModule.getInstance().sendBroadcast(intent);
    }

    public void changCode(boolean status) {
        if (!currentDeviceName.equals(A5P_Device) && !currentDeviceName.equals(A5P_ComBaseLin_Device)) {
            return;
        }
        switchIScan(status);
        kpd_enableKeybd(status);
        if (status) {
            boolean flag = currentDeviceName.endsWith(A5P_Device);
            if (flag)
                kpd_rebindKeybd(260, 473);
            else
                kpd_rebindKeybd(62, 66);
        }
    }

    /**
     * Replace the specified keycode with another keycode
     *
     * @param origKeyCode     ->current keycode
     * @param rebindKeyCode-> new keycode
     */
    private void kpd_rebindKeybd(int origKeyCode, int rebindKeyCode) {
        Intent intent = new Intent();
        intent.setAction(ACTION_KEYBD_REMAP);
        intent.putExtra(INTENT_EXTRA_COMMAND, "remap");
        //**** Reference: /kernel-4.9/include/uapi/linux/input-event-codes.h ****
        intent.putExtra(INTENT_EXTRA_PARAM_1, origKeyCode); // original key scancode
        intent.putExtra(INTENT_EXTRA_PARAM_2, rebindKeyCode); // remap key scancode
        IdataModule.getInstance().sendBroadcast(intent);
    }

    private Object camType = null;
    private boolean isHardwareDecode = false;

    /**
     * Disable iScan to avoid serial port conflict
     * (Disabled by default, unless no serial port conflict is detected)
     *
     * @param flag true-> enable，false-> disable
     */
    private void switchIScan(boolean flag) {
        if (!currentDeviceName.equals(A5P_Device) && !currentDeviceName.equals(A5P_ComBaseLin_Device)) {
            return;
        }
        if (camType == null) {
            camType = getSystemProp(MConstant.CameraType);//
            //If camera scanning is unavailable, it means there is a scan engine which will lead to serial port conflict
            isHardwareDecode = TextUtils.isEmpty((String) camType) ||
                    Integer.parseInt((String) camType) < 1 || Integer.parseInt((String) camType) > 9;
        }
        if (isHardwareDecode) {
            String KEY_BARCODE_ENABLESCANNER_ACTION = "android.intent.action.BARCODESCAN";
            Intent it = new Intent(KEY_BARCODE_ENABLESCANNER_ACTION);
            it.setPackage("com.android.auto.iscan");
            it.putExtra(KEY_BARCODE_ENABLESCANNER_ACTION, flag);
            IdataModule.getInstance().sendBroadcast(it);
        }
    }

    /**
     * to get system property values
     *
     * @param prop
     * @return a corresponding returned value
     */
    public Object getSystemProp(String prop) {
        Object value = null;
        try {
            Class<?> cl = Class.forName("android.os.SystemProperties");
            Method md = cl.getMethod("get", String.class);
            value = md.invoke(cl, prop);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

}
