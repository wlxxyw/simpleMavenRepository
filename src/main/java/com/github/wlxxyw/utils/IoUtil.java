package com.github.wlxxyw.utils;

import lombok.NonNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * io工具
 */
public class IoUtil {
    public static void close(Closeable closeable){
        if(null==closeable)return;
        try {
            closeable.close();
            Logger.debug("IO close");
        }catch (IOException e){
            Logger.debug("error when closing IO",e);
        }
    }
    public static byte[] readAsByteArray(@NonNull String filepath){
        InputStream is;
            is = IoUtil.class.getResourceAsStream(filepath);
            if(null==is){
               is =  IoUtil.class.getClassLoader().getResourceAsStream(filepath);
            }
            if(null==is){
                File file = new File(filepath);
                if(file.isFile()) {
                    try {
                        is = new FileInputStream(file);
                    } catch (FileNotFoundException ignored) {}
                }
            }
            if(null==is){
                Logger.error("file not exist, file = {}",filepath);
                return new byte[0];
            }
        return readAsByteArray(is);
    }
    public static byte[] readAsByteArray(@NonNull File file) {
        try {
            if(file.exists()){
                return readAsByteArray(Files.newInputStream(file.toPath()));
            }
        }catch (IOException ignored){}
        return new byte[0];
    }
    public static byte[] readAsByteArray(@NonNull InputStream is) {
        try {
            return readAsByteArray(is,is.available());
        }catch (IOException e){
            return readAsByteArray(is,0);
        }
    }
    public static byte[] readAsByteArray(@NonNull InputStream is,int size) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(size < 1){size = Integer.MAX_VALUE;}
        byte[] buffer = new byte[1024];
        int length;
        while(true){
            length = Math.min(size,buffer.length);
            try {
                length = is.read(buffer,0,length);
                if(length < 1 )break;
                baos.write(buffer,0,length);
            } catch (IOException e) {
                Logger.error("error when reading IO");
                break;
            }
            size -= length;
        }
        Logger.debug("read from inputStream, size: {}",baos.size());
        return baos.toByteArray();
    }
    public static void writeFile(@NonNull String path, @NonNull byte[] bs) {
        writeFile(new File(path),bs);
    }
    public static void writeFile(@NonNull File file, @NonNull byte[] bs) {
        if(!file.getParentFile().exists()&&!file.getParentFile().mkdirs()){
            throw  new RuntimeException("fail to mkdir"+file.getParent());
        }
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            out.write(bs);
            out.flush();
        } catch (Exception e) {
            Logger.error("writeFile failed, path = {}, msg = {}", file.getAbsolutePath(), e.getLocalizedMessage(), e);
        }
    }
    public static InputStream getInputStream(String path) {
        InputStream is = null;
        try {
            is = Files.newInputStream(Paths.get(path));
        }catch (IOException ignored){}
        if(null==is){
            is = IoUtil.class.getResourceAsStream(path);
        }
        if(null==is){
            is = IoUtil.class.getClassLoader().getResourceAsStream(path);
        }
        return is;
    }
    public static <T> T[] join(final T[] array1,final T...array2){
        T[] all = Arrays.copyOf(array1,array1.length+ array2.length);
        System.arraycopy(array2,0,all,array1.length,array2.length);
        return all;
    }
}
