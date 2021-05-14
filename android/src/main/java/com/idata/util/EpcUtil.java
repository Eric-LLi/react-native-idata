package com.idata.util;

import com.idata.IdataModule;
import com.uhf.api.cls.Reader;


/**
 * Author CYD
 * Date 2018/12/13
 */
public class EpcUtil {

    private EpcData epcData;

    private boolean ifPause = true;

    private EpcUtil() {
    }

    public static EpcUtil getInstance() {
        return MySingleton.instance;
    }

    static class MySingleton {
        static final EpcUtil instance = new EpcUtil();
    }

    private boolean isIfOpenQuickInventoryMode() {
        return IdataModule.ifOpenQuickInventoryMode;
    }

    public void setEpcData(EpcData epcData) {
        this.epcData = epcData;
    }

    public boolean inventoryStart() {
        ifPause = false;
        boolean flag = true;
        //     MLog.e(" inventoryStart isIfOpenQuickInventoryMode = " + isIfOpenQuickInventoryMode());
        if (isIfOpenQuickInventoryMode()) {
            MyLib.getInstance().setAdditionalData(1);
            flag = MyLib.getInstance().asyncStartReading();
        }
        ReadThread.getInstance().setInventory(true);
        return flag;
    }

    public boolean invenrotyStop() {
        ReadThread.getInstance().setInventory(false);
        return ifPause = isIfOpenQuickInventoryMode() ? MyLib.getInstance().asyncStopReading() : MyLib.getInstance().stopTagReading();
    }

    //Stop tag inventory
    public boolean isInventoryNoPause() {
        return !ifPause;
    }

    /**
     * recycle source and exit
     */
    public void exit() {
        invenrotyStop();
        ReadThread.getInstance().closeThrad();
        MyLib.getInstance().powerOff();
        System.exit(0);
    }

    void getTag() {
        int[] rcvData = new int[]{0};
        boolean flag = false;
        // MLog.e("nums isIfOpenQuickInventoryMode = " + isIfOpenQuickInventoryMode());
        if (isIfOpenQuickInventoryMode())
            flag = MyLib.getInstance().asyncGetTagCount(rcvData);
        else
            flag = MyLib.getInstance().tagInventory_Raw(rcvData);
        if (flag) {
            if (rcvData[0] > 0) {
                for (int i = 0; i < rcvData[0]; i++) {
                    Reader.TAGINFO temp = IdataModule.getInstance().getReader().new TAGINFO();
                    if (isIfOpenQuickInventoryMode())
                        flag = MyLib.getInstance().asyncGetNextTag(temp);
                    else
                        flag = MyLib.getInstance().getNextTag(temp);
                    if (flag) {
                        String[] tagData = new String[2];
                        tagData[0] = Reader.bytes_Hexstr(temp.EpcId); //EPC data
                        tagData[1] = Reader.bytes_Hexstr(temp.EmbededData);  //Additional data,default as TID
                        int rssi = temp.RSSI;
                        if (tagData[1].length() == 256) { //data filtering
                            tagData[1] = tagData[1].substring(0, 24);
                        }
                        MLog.e("epc1111 = " + tagData[0] + " tid = " + tagData[1] + " rssi = " + rssi);
                        epcData.getEpcData(tagData);
                    }
                }
            }

        } else {
            MLog.e("failed ");
        }
    }

}
