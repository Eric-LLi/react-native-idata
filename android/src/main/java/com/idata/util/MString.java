package com.idata.util;

/**
 * Author CYD
 * Date 2018/12/25
 */
public class MString {

    /**
     * Convert byte array to hex
     *
     * @param bytes
     * @return
     */
    public static String byteToHex(byte[] bytes) {
        String strHex = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < bytes.length; n++) {
            strHex = Integer.toHexString(bytes[n] & 0xFF);
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex); // Each byte is represented by two characters;if the number of digits is not enough,filled with 0 before the high digit
        }
        return sb.toString().trim();
    }

    /**
     * Convert hex to byte array
     *
     * @param hex
     * @return
     */
    public static byte[] hexToByte(String hex) {
        int m = 0, n = 0;
        int byteLen = hex.length() / 2; // Every two characters to describe a byte
        byte[] ret = new byte[byteLen];
        for (int i = 0; i < byteLen; i++) {
            m = i * 2 + 1;
            n = m + 1;
            int intVal = Integer.decode("0x" + hex.substring(i * 2, m) + hex.substring(m, n));
            ret[i] = Byte.valueOf((byte) intVal);
        }
        return ret;
    }


    public static boolean ifHexString(String str) {
        if (str.length() == 0) {
//            MToast.show(R.string.hexdata_not_null);
            return false;
        }
        boolean flag = str.length() % 2 == 0;
        if (!flag) return false;
//            MToast.show(R.string.hexdata_must_even);
        return flag;
    }

    public static boolean ifBits(String str) {
        if (str.length() == 0) {
//            MToast.show(R.string.bitdata_not_null);
            return false;
        }

        boolean flag = Integer.parseInt(str) % 4 == 0;
        if (!flag) return false;
//            MToast.show(R.string.bitdata_must_multiple_of_4);
        return flag;
    }
}
