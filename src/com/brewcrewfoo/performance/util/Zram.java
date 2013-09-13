package com.brewcrewfoo.performance.util;

/**
 * Created by h0rn3t on 09.09.2013.
 */
public class Zram implements Constants {

    private int diskSize = 0;
    private int compressedDataSize = 0;
    private int originalDataSize = 0;
    private int memUsedTotal = 0;

    public int getDiskSize() throws Exception {
        if (diskSize == 0) {
            diskSize = _getDiskSize();
        }
        return diskSize;
    }

    public void setDiskSize(int v) throws Exception {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
            sb.append("busybox echo ")
                    .append(String.valueOf(v * 1024 * 1024))
                    .append(" > ")
                    .append(ZRAM_SIZE_PATH.replace("zram0", "zram" + i));
        }
        Helpers.shExec(sb);
    }

    private int _getDiskSize() throws Exception {
        final String sizeInString = Helpers.readOneLine(ZRAM_SIZE_PATH);
        return Integer.parseInt(sizeInString);
    }

    public int getCompressedDataSize() throws Exception {
        if (compressedDataSize == 0) {
            compressedDataSize = _getCompressedDataSize();
        }
        return compressedDataSize;
    }

    private int _getCompressedDataSize() throws Exception {
        final String sizeInString = Helpers.readOneLine(ZRAM_COMPR_PATH);
        return Integer.parseInt(sizeInString);
    }

    public int getOriginalDataSize() throws Exception {
        if (originalDataSize == 0) {
            originalDataSize = _getOriginalDataSize();
        }
        return originalDataSize;
    }

    private int _getOriginalDataSize() throws Exception {
        final String sizeInString = Helpers.readOneLine(ZRAM_ORIG_PATH);
        return Integer.parseInt(sizeInString);
    }

    public int getMemUsedTotal() throws Exception {
        if (memUsedTotal == 0) {
            memUsedTotal = _getMemUsedTotal();
        }
        return memUsedTotal;
    }

    private int _getMemUsedTotal() throws Exception {
        final String sizeInString = Helpers.readOneLine(ZRAM_MEMTOT_PATH);
        return Integer.parseInt(sizeInString);
    }

    public float getCompressionRatio() throws Exception {
        return (float) getCompressedDataSize() / (float) getOriginalDataSize();
    }

    public float getUsedRatio() throws Exception {
        return (float) getOriginalDataSize() / (float) getDiskSize();
    }
}
