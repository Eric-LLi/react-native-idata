package com.idata.util;


import android.content.Intent;
import android.text.TextUtils;

import com.idata.IdataModule;
import com.idata.idcardreader.SimpleInterface;
import com.idata.jnitool.UHFAndIDCardControl;
import com.idata.serialport.SerialPort;
import com.uhf.api.cls.JniModuleAPI;
import com.uhf.api.cls.Reader;

import static com.idata.IdataModule.currentDeviceName;

/**
 * <p>Author CYD</p>
 * <p>Date 2020/5/28</p>
 * <p>verion 2.3.1</p>
 * <p>Notes:</p>
 * <p>1.The API and Demo are applicable to SLR1200 and SLR5100 modules ;</p>
 * <p>2.Please import libModuleAPIJni.so,libserial_port_idata.so, libSimpleJni.so and ModuleAPI_J.jar</p>
 * <p>3.Please create the specified package name:com.device.serialport and copy SerialPort class</p>
 * <p>4.Please create the specified package name:com.example.scarx.idcardreader and copy SimpleInterface class</p>
 */
public class MyLib {// device model

    public static String A5_Device = "A5VR2V100"; //50V2 series
    public static String V4G_95Device = "KBA2KV100";//95 series
    private String V5_95VDevice = "KB172V100";
    private String W4G_95Device = "95VR2V100";
    private String YF_70Device = "YFA7V100"; //70 series
    private String VR2_70Device = "70VR2V100";
    private String NX2_A5V1Device = "KB3A5V100";//50V1 & NX2
    public static final String A5P_Device = "50S-V01-R01";//50P & NX2(Android9.0)
    public static final String A5P_ComBaseLin_Device = "50V400R001";//A5P common baseline version

    private int cmd = 1;//The default command value used by jni
    private int ant[] = {1};//Antenna array, only one antenna by default
    private int option = 0;//The default as 0; if additional data is set, it is 32768
    private short maxPower = 3000; //Max power
    private Reader.READER_ERR operate_success = Reader.READER_ERR.MT_OK_ERR; //Status of successful operation
    private int defaultCmd = 1; //antenna1 by default
    private short defaultTime = 1000; //Timeout
    private Reader mReader;
    private JniModuleAPI jniModuleAPI;

    private MyLib() {
        mReader = IdataModule.getInstance().getReader();
        jniModuleAPI = IdataModule.getInstance().getJniModuleAPI();
    }

    /**
     * Get singleton object of current MyLib class
     *
     * @return MyLib object
     */
    public static MyLib getInstance() {
        // return new MyLib();
        return MySingleton.instance;
    }

    static class MySingleton {
        static final MyLib instance = new MyLib();
    }

    /**
     * Module power on
     *
     * @return true -> succeed, false-> failed
     */
    public boolean powerOn() {
        //   MLog.e(currentDeviceName);
        if (TextUtils.isEmpty(currentDeviceName))
            return false;
        String path = "";
        if (currentDeviceName.equals(A5_Device) || currentDeviceName.equals(NX2_A5V1Device)) { //50设备
            path = "/dev/ttyMT2";
            //Power on
            enableUartComm_UHF(true);
            setPowerState_UHF(true);
        } else if (currentDeviceName.equals(V5_95VDevice) || currentDeviceName.equals(V4G_95Device)) {
            //  Note:please excute getDevPath() to get the device path before power-on
            path = SerialPort.getDevPath(2); //device mount path
            int values = SerialPort.ioctlFromJNI(3);//Power on via IO port 
            if (values != 0) {
                return false;
            }
        } else if (currentDeviceName.equals(W4G_95Device)) {
            if (SimpleInterface.IOCTL_UHF_POWER_ON()) {
                path = "/dev/ttyMT2";
            }
        } else if (currentDeviceName.equals(YF_70Device) || currentDeviceName.equals(VR2_70Device)) {
            UHFAndIDCardControl.openMainPower();
            UHFAndIDCardControl.UHFPowOn();
            path = "/dev/ttyMT1";
        } else if (currentDeviceName.equals(A5P_Device) || currentDeviceName.equals(A5P_ComBaseLin_Device)) {
            path = "/dev/ttyS1";
            enableUartComm_UHF(true);
            setPowerState_UHF(true);
        }
        //In particular, after the device is initialized and mounted at this time, mReader.hReader[0] =1,and subsequent operation commands that use mReader will use this value
        return mReader.InitReader_Notype(path, 1) == operate_success; //the address of the mounted device
    }

    /**
     * Power-off
     *
     * @return true -> succeed, false-> failed
     */
    public boolean powerOff() {
        if (TextUtils.isEmpty(currentDeviceName))
            return false;
        //   mReader.CloseReader();
        if (currentDeviceName.equals(A5_Device) || currentDeviceName.equals(A5P_Device) || currentDeviceName.equals(A5P_ComBaseLin_Device)) { //50设备
            enableUartComm_UHF(false);
            setPowerState_UHF(false);
            return true;
        } else if (currentDeviceName.equals(V5_95VDevice)) {
            return SerialPort.ioctlFromJNI(4) == 0;
        } else if (currentDeviceName.equals(W4G_95Device)) {
            return SimpleInterface.IOCTL_UHF_POWER_OFF();
        } else if (currentDeviceName.equals(currentDeviceName.equals(YF_70Device) || currentDeviceName.equals(VR2_70Device))) {
            UHFAndIDCardControl.UHFPowOff();
            UHFAndIDCardControl.closeMainPower();
            return true;
        }
        return false;
    }

    private void enableUartComm_UHF(boolean bEnable) {
        Intent intent = new Intent(EmshConstant.Action.INTENT_EMSH_REQUEST);
        intent.putExtra(EmshConstant.IntentExtra.EXTRA_COMMAND, EmshConstant.Command.CMD_REQUEST_ENABLE_UHF_COMM);
        intent.putExtra(EmshConstant.IntentExtra.EXTRA_PARAM_1, (bEnable ? 1 : 0));
        IdataModule.getInstance().sendBroadcast(intent);
    }

    private void setPowerState_UHF(boolean bPowerOn) {
        Intent intent = new Intent(EmshConstant.Action.INTENT_EMSH_REQUEST);
        intent.putExtra(EmshConstant.IntentExtra.EXTRA_COMMAND, EmshConstant.Command.CMD_REQUEST_SET_POWER_MODE);
        intent.putExtra(EmshConstant.IntentExtra.EXTRA_PARAM_1, (bPowerOn ? EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_DSG_UHF : EmshConstant.EmshBatteryPowerMode.EMSH_PWR_MODE_STANDBY));
        IdataModule.getInstance().sendBroadcast(intent);
    }


    /**
     * (Only available for SLR1200 + quick inventory mode)
     * Enable quick inventory
     *
     * @return true -> succeed, false-> failed
     */
    public boolean asyncStartReading() {
        int val = jniModuleAPI.AsyncStartReading(cmd, ant, ant.length, option);
        return val == 0;
    }

    /**
     * (Only available for SLR1200 + quick inventory mode)
     * Get the number of tags obtained from quick inventory
     *
     * @param rcvData ->Pass in an array and assign a value, rcvData[0] is the number of tags obtained this time
     * @return true -> succeed, false-> failed
     */
    public boolean asyncGetTagCount(int[] rcvData) {
        return mReader.AsyncGetTagCount(rcvData) == operate_success;
    }

    /**
     * (Only available for SLR1200 + quick inventory mode)
     * Get the tag data of the quick inventory and assign it to the entity class
     *
     * @param temp-> for receiving specific data entity class
     * @return true -> succeed, false-> failed  <br/>
     * <pre id="codeUse">
     *     <b>Complete example of quick inventory :</b>
     *     private Handler myHandler = new Handler();
     *     MyLib.getInstance().powerOn(); //Power-on
     *     MyLib.getInstance().setAdditionalData(1);//Set additional data, 0：does not return additional data, 1：returns TID, 2：returns RSSI (optional operation)
     *     MyLib.getInstance().asyncStartReading();
     *     myHandler.postDelayed(runnable_MainActivitys, 0);
     *     // Tag inventory operation
     *     private Runnable runnable_MainActivitys = new Runnable() {
     *     <code>@Override</code>
     *     public void run(){
     *         int[] rcvData = new int[] { 0 };
     *         boolean flag = false;
     *         flag = MyLib.getInstance().asyncGetTagCount(rcvData);
     *         if (flag) {
     *            if (rcvData[0] > 0) {
     *               for (int i = 0; i < rcvData[0]; i++) {
     *                   Reader.TAGINFO temp = MyApp.getMyApp().getReader().new TAGINFO();
     *                   flag = MyLib.getInstance().asyncGetNextTag(temp);
     *                   if (flag) {
     *                      String epc = Reader.bytes_Hexstr(temp.EpcId); // hex string of EPC
     *                      String embededData=Reader.bytes_Hexstr(temp.EmbededData);   // additional data, default as TID
     *                      int rssi = temp.RSSI;//	RSSI data
     *                      if (embededData.length() == 256) { // data filtering
     *                         embededData = embededData.substring(0, 24);
     *                      }
     *                      MLog.e("epc1111 = " + epc + " tid = " + embededData + " rssi = " + rssi);//Output log  (for testing, not required)
     *                   }
     *               }
     *            }
     *         }
     *         myHandler.post(runnable_MainActivitys);
     *       }
     *     };
     * </pre>
     */
    public boolean asyncGetNextTag(Reader.TAGINFO temp) {
        return mReader.AsyncGetNextTag(temp) == operate_success;
    }

    /**
     * (Only available for SLR1200 + quick inventory mode)
     * Stop quick inventory
     *
     * @return true -> succeed, false-> failed
     */
    public boolean asyncStopReading() {
        return jniModuleAPI.AsyncStopReading(cmd) == 0;
    }

    /**
     * Ordinary tag inventory, get the number of tags for this inventory
     *
     * @param rcvData ->convey to array for receiving data
     * @return true -> succeed, false-> failed
     */
    public boolean tagInventory_Raw(int[] rcvData) {
        return mReader.TagInventory_Raw(ant, ant.length, (short) 50, rcvData) == operate_success;
    }

    /**
     * tag data of ordinary inventory and assign to entity class
     *
     * @param temp for receving specific data entity class
     * @return true -> succeed, false-> failed  <br/>
     * <pre id="codeUse">
     *     <b>Complete example of common inventory:</b>
     *     MyLib.getInstance().powerOn();//Power-on
     *     int[] rcvData = new int[] { 0 };
     *     if (MyLib.getInstance().tagInventory_Raw(rcvData)) {
     *          if (rcvData[0] > 0) {
     *              for (int i = 0; i < rcvData[0]; i++) {
     *                  Reader.TAGINFO temp = MyApp.getMyApp().getReader().new TAGINFO();
     *                  if (MyLib.getInstance().getNextTag(temp)) {
     *                      String epc = Reader.bytes_Hexstr(temp.EpcId); // hex string of EPC
     *                      String embededData = Reader.bytes_Hexstr(temp.EmbededData); // TID
     *                      int rssi = temp.RSSI;// RSSI data
     *                      if(embededData.length() == 256){// data filtering
     *                          embededData = embededData.substring(0, 24);
     *                      }
     *                      MLog.e("epc1111 = " + epc + " tid = " + embededData + " rssi = " + rssi);//output log(for testing, not required)
     *                  }
     *              }
     *          }
     *     }
     * </pre>
     */
    public boolean getNextTag(Reader.TAGINFO temp) {
        return mReader.GetNextTag(temp) == operate_success;
    }

    /**
     * Stop ordinary tag inventory
     *
     * @return true -> succeed, false-> failed
     */
    public boolean stopTagReading() {
        return mReader.StopReading() == operate_success;
    }


    /**
     * Set additional data return(Only available for SLR1200 + quick inventory mode)
     *
     * @param flag ->0:does not to return additional data, 1:return TID data, 2:return RSSI data
     * @return true -> succeed, false-> failed
     */
    public boolean setAdditionalData(int flag) {
        switch (flag) {
            case 0:
                option = 0;
                return true;
            case 1:
                option = 32768;
                Reader.EmbededData_ST edst = mReader.new EmbededData_ST();
                edst.accesspwd = null;
                edst.bank = 2;
                edst.startaddr = 0;
                edst.bytecnt = 12;
                Reader.READER_ERR er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst);
                return er == operate_success;
            case 2:
                int metaflag = 0;
                metaflag |= 0X0002;
                option = (metaflag << 8) | 0;
                return true;
        }
        return false;
    }

    /**
     * Get read/write power
     *
     * @return length of short[]Length=2;  0:read power, 1:write power
     */
    public short[] getPower() {
        Reader.AntPowerConf apcf = mReader.new AntPowerConf();
        Reader.READER_ERR er = mReader.ParamGet(Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);
        short st[] = null;
        if (er == operate_success) {
            st = new short[2];
            st[0] = apcf.Powers[0].readPower; //Get current read power
            st[1] = apcf.Powers[0].writePower; //Get current write power
        }
        return st;
    }

    /**
     * Set read/write power
     *
     * @param readPower
     * @param writePower
     * @return true -> succeed, false-> failed
     */
    public boolean setPower(short readPower, short writePower) {
        Reader.AntPowerConf apcf = mReader.new AntPowerConf();
        apcf.antcnt = 1;
        Reader.AntPower jaap = mReader.new AntPower();
        jaap.antid = 1;
        jaap.readPower = readPower;
        jaap.writePower = writePower;
        apcf.Powers[0] = jaap;
        Reader.READER_ERR er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);
        return er == operate_success;
    }

    /**
     * Set regional frequency
     *
     * @param frequency Area：0:CN,1:NA,2:Korea,3:EU,4:India,5:Canada
     * @return true -> succeed, false-> failed
     */
    public boolean setFrequency(int frequency) {
        Reader.Region_Conf rre = null;
        switch (frequency) {
            case 0:
                rre = Reader.Region_Conf.RG_PRC;
                break;
            case 1:
                rre = Reader.Region_Conf.RG_NA;
                break;
            case 2:
                rre = Reader.Region_Conf.RG_EU3;
                break;

        }
        Reader.READER_ERR er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rre);
        return er == operate_success;
    }

    /**
     * Get regional frequency
     *
     * @return Area：0:CN,1:NA,2:Korea,3:EU,4:India,5:Canada
     */
    public int getFrequency() {
        Reader.Region_Conf[] rcf2 = new Reader.Region_Conf[1];
        Reader.READER_ERR er = mReader.ParamGet(Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rcf2);
        int value = -1;
        if (rcf2[0].value() < 0) {
            return value;
        }
        if (er == Reader.READER_ERR.MT_OK_ERR) {
            switch (rcf2[0]) {
                case RG_PRC:
                    value = 0;
                    break;
                case RG_NA:
                    value = 1;
                    break;
                case RG_EU3:
                    value = 2;
                    break;
            }
        }
        return value;
    }

    /**
     * Set the specified frequency value (please set the area firstly)
     *
     * @param value ->frequency value
     * @return true -> succeed, false-> failed
     */
    public boolean setFrequencyChannel(int value) {
        //Set the specified frequency
        int[] vls = new int[]{value};
        Reader.HoptableData_ST hdst = mReader.new HoptableData_ST();
        hdst.lenhtb = vls.length;
        hdst.htb = vls;
        Reader.READER_ERR er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE, hdst);
        return er == operate_success;
    }

    /**
     * (Only SLR1200 modules support)
     * Quick inventory mode
     *
     * @param value 0:s0 mode with max power,1:s1 mode with max power
     */
    public boolean setFastMode(int value) {
        setPower(maxPower, maxPower);//to set max Read/write power 
        Reader.READER_ERR er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, new int[]{value});
        return er == operate_success;
    }

    /**
     * Filtering settings
     * the structure of Reader.TagFilter_ST it the same as com.szyd.util.TagFilter_ST
     *
     * @param tfst ->entity class for filtering
     * @return true-> succeed, false-> failed
     */
    public boolean setFilter(Reader.TagFilter_ST tfst) {
        Reader.READER_ERR er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, tfst);
        return er == operate_success;
    }

    /**
     * Get filters
     * the structure of Reader.TagFilter_ST it the same as com.szyd.util.TagFilter_ST
     *
     * @return return a TagFilter_ST entity object，null->failed
     */
    public Reader.TagFilter_ST getFilter() {
        Reader.TagFilter_ST tfst2 = mReader.new TagFilter_ST();
        Reader.READER_ERR er = mReader.ParamGet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, tfst2);
        return er == operate_success ? tfst2 : null;
    }

    /**
     * reading
     *
     * @param bank       ->Operating area,0:Reserved bank,1:EPC bank,2:TID bank,3:USER bank
     * @param startBlock ->Satrt block(word)
     * @param len        ->Required block length
     * @param rcvData    ->Byte data for reception
     * @param pwd        ->password
     * @return true-> succeed, false-> failed <br/>
     * <img src="image/epc.jpg" width="100%" height="40%" />
     * <pre id="codeUse">
     *  <b>Example of reading tag EPC bank:</b>
     *  char bank =1;
     *  int startBlocks = 2;
     *  int len =6;
     *  byte [] rcvData = new byte[len*2];
     *  byte [] pwd = new byte[4] ;
     *  boolean result = readTag(bank,startBlocks,len,rcvData,pwd);
     * </pre>
     */
    public boolean readTag(char bank, int startBlock, int len, byte[] rcvData, byte[] pwd) {
        Reader.READER_ERR er = mReader.GetTagData(defaultCmd, bank, startBlock, len, rcvData, pwd, defaultTime); //Get the specified area data
        return er == operate_success;
    }

    /**
     * write tag
     *
     * @param bank       ->Operating area,0:Reserved bank,1:EPC bank,2:TID bank,3:USER bank
     * @param startBlock ->Start block(word)
     * @param data       ->data for filtering
     * @param pwd        ->password
     * @return true-> succeed, false-> failed <br/>
     * <img src="image/epc.jpg" width="100%" height="40%" />
     * <pre id="codeUse">
     *  <b>example of writing EPC bank:</b>
     *  char bank =1;
     *  int  startBlocks = 2;
     *  byte [] filterData = {...}; //data to be written in(convert hex to byte[])
     *  byte [] pwd = new byte[4] ;
     *  boolean result = writeTag(bank,startBlocks,data,pwd);
     * </pre>
     */
    public boolean writeTag(char bank, int startBlock, byte[] data, byte[] pwd) {
        Reader.READER_ERR er = mReader.WriteTagData(defaultCmd, bank, startBlock, data, data.length, pwd, defaultTime); //向指定区域写数据
        return er == operate_success;
    }

    /**
     * lock/unlock tag
     *
     * @param bank       ->Operating area,0:access password,1:destroy password,2:EPC bank,3:TID bank,4:USER bank
     * @param opeateType ->Operation type,0:unlock,1:temporary lock,2:permanent lock
     * @param pwd        ->password
     * @return true-> succeed, false-> failed <br/>
     * <img src="image/reserved.jpg" width="100%" height="40%" />
     * <pre id="codeUse">
     * <b>example of locking tag:</b>
     * byte[] pwd = {0x66,0x66,0x66,0x66};
     * byte[] defalutPwd = new byte[4];
     * writeTag(0,2,pwd,defalutPwd); //When the tag is blank, write AccessPassword in the Reserved area to initialize
     * lockTag(2,1,pwd); //lock EPC bank temporarily
     * </pre>
     */
    public boolean lockTag(int bank, int opeateType, byte[] pwd) {
        Reader.Lock_Obj lobj = null;
        Reader.Lock_Type ltyp = null;
        if (bank == 0) {
            lobj = Reader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD;
            if (opeateType == 0)
                ltyp = Reader.Lock_Type.ACCESS_PASSWD_UNLOCK;
            else if (opeateType == 1)
                ltyp = Reader.Lock_Type.ACCESS_PASSWD_LOCK;
            else if (opeateType == 2)
                ltyp = Reader.Lock_Type.ACCESS_PASSWD_PERM_LOCK;

        } else if (bank == 1) {
            lobj = Reader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD;
            if (opeateType == 0)
                ltyp = Reader.Lock_Type.KILL_PASSWORD_UNLOCK;
            else if (opeateType == 1)
                ltyp = Reader.Lock_Type.KILL_PASSWORD_LOCK;
            else if (opeateType == 2)
                ltyp = Reader.Lock_Type.KILL_PASSWORD_PERM_LOCK;
        } else if (bank == 2) {
            lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK1;
            if (opeateType == 0)
                ltyp = Reader.Lock_Type.BANK1_UNLOCK;
            else if (opeateType == 1)
                ltyp = Reader.Lock_Type.BANK1_LOCK;
            else if (opeateType == 2)
                ltyp = Reader.Lock_Type.BANK1_PERM_LOCK;
        } else if (bank == 3) {
            lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK2;
            if (opeateType == 0)
                ltyp = Reader.Lock_Type.BANK2_UNLOCK;
            else if (opeateType == 1)
                ltyp = Reader.Lock_Type.BANK2_LOCK;
            else if (opeateType == 2)
                ltyp = Reader.Lock_Type.BANK2_PERM_LOCK;
        } else if (bank == 4) {
            lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK3;
            if (opeateType == 0)
                ltyp = Reader.Lock_Type.BANK3_UNLOCK;
            else if (opeateType == 1)
                ltyp = Reader.Lock_Type.BANK3_LOCK;
            else if (opeateType == 2)
                ltyp = Reader.Lock_Type.BANK3_PERM_LOCK;
        }
        Reader.READER_ERR er = mReader.LockTag(defaultCmd, (byte) lobj.value(), (short) ltyp.value(), pwd, defaultTime);
        return er == operate_success;
    }

    /**
     * Kill tag
     *
     * @param pwd -> password
     * @return true->succeed,false-> failed
     */
    public boolean killTag(byte[] pwd) {
        Reader.READER_ERR er = mReader.KillTag(defaultCmd, pwd, defaultTime);
        return er == operate_success;
    }
}
