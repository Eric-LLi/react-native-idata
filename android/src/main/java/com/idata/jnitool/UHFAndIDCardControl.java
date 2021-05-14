package com.idata.jnitool;

/**
 * Author CYD
 * Date 2018/12/20
 */
//UHF module power control (70 series)
public class UHFAndIDCardControl {

    static {
        try {
            System.loadLibrary("UHFIDCardJni");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Turn on the main power
     *
     * @return
     */
    public static native boolean openMainPower();

    /**
     * Turn off the main power
     *
     * @return
     */
    public static native boolean closeMainPower();

    /**
     * Power on the UHF module (please power on the main power first, otherwise it will be invalid), (70 series)
     *
     * @return
     */
    public static native boolean UHFPowOn();  //power on UHF module

    /**
     * power off UHF module,(70 series)
     *
     * @return
     */
    public static native boolean UHFPowOff(); //power off UHF module

    /**
     * Power on ID card module (please power on first, otherwise it will be invalid)
     *
     * @return
     */
    public static native boolean IDCardOn();  //power on IDCARD

    /**
     * Power off ID card module
     *
     * @return
     */
    public static native boolean IDCardOff(); //power off IDCARD

}
