package com.github.wlxxyw.utils;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VerifyUtil {

    /**
     * MD5
     * @param data 数据
     * @return md5值
     */
    public static String md5Digest(byte[] data) {
        if (null == data) {
            return null;
        }
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            return bytesToHex(messagedigest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MessageDigest MD5 not exists", e);
        }
    }

    /**
     * SHA1
     * @param data 数据
     * @return sha1值
     */
    public static String sha1Digest(byte[] data) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("SHA1");
            return bytesToHex(messagedigest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MessageDigest SHA1 not exists", e);
        }
    }

    /**
     * byte[] to hex string
     * @param bytes byte数组
     * @return 16进制数据
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length*2);
        for(byte b:bytes){
            int num = b&0xFF;
            if(num<0x10){sb.append(0);}
            sb.append(Integer.toHexString(num));
        }
        return sb.toString();
    }


    /**
     * 循环目录MD5/sha1签名
     * @param file 开始路径
     */
    public static void signFile(File file) {
        if (null == file || !file.exists()) {
            return;
        }
        if (file.isFile()) {
            String name = file.getName().toLowerCase();
            //仅签名 xml/jar/pom文件
            if (!name.endsWith(".xml") && !name.endsWith(".jar") && !name.endsWith(".pom")) {
                return;
            }
            File sha1 = new File(file.getAbsolutePath() + ".sha1");
            File md5 = new File(file.getAbsolutePath() + ".md5");
            if (sha1.exists() && md5.exists()) {
                return;
            }
            byte[] bytes = IoUtil.readAsByteArray(file);
            if(bytes.length == 0)return;
            if (!sha1.exists()) {
                Logger.info("create sha1, file = {}", sha1.getAbsolutePath());
                IoUtil.writeFile(sha1.getAbsolutePath(), sha1Digest(bytes).getBytes());
            }
            if (!md5.exists()) {
                Logger.info("create md5, file = {}", md5.getAbsolutePath());
                IoUtil.writeFile(md5.getAbsolutePath(), md5Digest(bytes).getBytes());
            }
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (null != files) {
                for (File sub : files) {
                    signFile(sub);
                }
            }
        }
    }
}
