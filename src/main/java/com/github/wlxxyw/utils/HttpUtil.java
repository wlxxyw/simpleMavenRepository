package com.github.wlxxyw.utils;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 远程仓库连接
 */
public class HttpUtil {

    public static byte[] doGet(String httpUrl) {
        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            // 创建远程url连接对象
            URL url = new URL(httpUrl);
            // 通过远程url连接对象打开一个连接，强转成httpURLConnection类
            connection = (HttpURLConnection) url.openConnection();
            // 设置连接方式：get
            connection.setRequestMethod("GET");
            // 设置连接主机服务器的超时时间：15000毫秒
            connection.setConnectTimeout(15000);
            // 设置读取远程返回的数据时间：60000毫秒
            connection.setReadTimeout(60000);
            // 设置请求头
            connection.setRequestProperty("Accept_encoding","gzip,deflate");
            connection.setRequestProperty("Connection","Keep-Alive");
            connection.setRequestProperty("Host",RegularUtil.getHttpHost(httpUrl,""));
            connection.setRequestProperty("User-agent","Apache-Maven/3.6.3 (Java 11.0.9; Windows 10 10.0)");
            connection.setRequestProperty("Cache-store","no-store");
            connection.setRequestProperty("Cache-control","no-cache");
            // 发送请求
            connection.connect();
            // 通过connection连接，获取输入流
            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                return IoUtil.readAsByteArray(is, connection.getHeaderFieldInt("Content-Length", 0));
            }
            Logger.info("doGet failed, responseCode = {}, url = {}", connection.getResponseCode(), httpUrl);
        }catch (UnknownHostException e){
            Logger.warn("Unknown Host : {}", RegularUtil.getHttpHost(httpUrl,""));
        } catch (IOException e) {
            Logger.error(e.getLocalizedMessage(),e);
            Logger.warn("doGet failed, url = {}", httpUrl);
        } finally {
            // 关闭资源
            IoUtil.close(is);
            if (null != connection) {
                connection.disconnect();// 关闭远程连接
            }
        }
        return null;
    }
    public static String serverIp(){
        try{
            byte[] address = InetAddress.getLocalHost().getAddress();
            List<String> _address = new ArrayList<>(4);
            for(byte b:address){_address.add(String.valueOf((b+256)%256));}
            return String.join(".",_address);
        } catch (UnknownHostException e) {
            return "localhost";
        }

    }
}