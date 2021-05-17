package com.idata.util;

/**
 * Tag inventory thread
 */
public class ReadThread extends Thread {

    private static ReadThread instance;

    public static ReadThread getInstance() {
        return instance;
//        return MySingleton.singleton;
    }

    private static class MySingleton {
//        final static ReadThread singleton = new ReadThread();
    }

    public static void init() {
        instance = new ReadThread();
    }

    private ReadThread() {
    }

    private boolean ifAlive = true;
    private boolean ifInventory = false;

    public void setInventory(boolean flag) {
        ifInventory = flag;
    }

    public void closeThrad() {
        ifAlive = false;
    }

    @Override
    public void run() {
        super.run();
        while (ifAlive) {
            if (ifInventory) {
                EpcUtil.getInstance().getTag();
            }
        }
    }
}

