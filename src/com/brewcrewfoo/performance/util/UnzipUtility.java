package com.brewcrewfoo.performance.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by h0rn3t on 27.08.2013.
 */
public class UnzipUtility implements Constants {
    private static final int BUFFER_SIZE = 4096;

    public void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {destDir.mkdir();}

        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            if (!entry.isDirectory()) {
                File dir=new File(destDirectory,entry.getName());
                if(!dir.getParentFile().exists())
                    dir.getParentFile().mkdirs();
                extractFile(zipIn, destDirectory+"/"+entry.getName());
            }

            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    public Boolean testZip(String zipFilePath,String tip) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        String f="recovery.img";
        if(tip.equalsIgnoreCase("kernel")){ f="boot.img";}
        boolean gasit=false;
        while (entry != null) {
            if (!entry.isDirectory()) {
                if(entry.getName().contains(f)){gasit=true;break;}
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
        return gasit;
    }
}
