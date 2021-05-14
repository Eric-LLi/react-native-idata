package com.idata.util;

/**
 * Only for generating the SDK documents
 */
@Deprecated
public class TagFilter_ST {
    /**
     * Filter bank, 0:EPC,1:TID,2:USER
     */
    public int bank;
    /**
     * Start bit
     */
    public int startaddr;
    /**
     * Data length(bit)
     */
    public int flen;
    /**
     * Filtering data(hex)
     */
    public byte[] fdata = new byte[255];
    /**
     * Whether it matches the filter,0:match, 1:mismatch
     */
    public int isInvert;

    public TagFilter_ST() {
    }
}