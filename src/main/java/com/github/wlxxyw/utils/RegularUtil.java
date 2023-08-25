package com.github.wlxxyw.utils;

import lombok.NonNull;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 验证工具
 */
public class RegularUtil {
    private static final Pattern IPV4 = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");
    private static final Pattern HTTP_URL = Pattern.compile("^(https?://)?(([0-9a-z]+(-[0-9a-z]+)*\\.)*[0-9a-z]+)(:[0-9]{1,5})?/?");
    private static final Pattern PATH = Pattern.compile("^/([^/]*)(/.*)?");
    private static final Pattern PARAMS = Pattern.compile("\\$\\{([a-zA-Z0-9\\.]*)\\}");
    public static boolean matchUrl(@NonNull String url){
        return HTTP_URL.matcher(url).find();
    }
    public static String getHttpHost(@NonNull String url, String defaultValue){
        Matcher matcher = HTTP_URL.matcher(url);
        if(matcher.find()){
            return matcher.group(2);
        }else{
            Logger.warn("url({}) not match HTTP_URL",url);
            return defaultValue;
        }
    }
    public static String getRepositoryID(@NonNull String url){
        Matcher matcher = PATH.matcher(url);
        if(matcher.find()){
            return matcher.group(1);
        }else{
            Logger.warn("url({}) not match PATH",url);
            return null;
        }
    }
    public static String getRepositoryPath(@NonNull String url){
        Matcher matcher = PATH.matcher(url);
        if(matcher.find()){
            return null==matcher.group(2)?"":matcher.group(2);
        }else{
            Logger.warn("url({}) not match PATH",url);
            return "";
        }
    }
    public static String replaceEnvVariable(String path){
        StringBuffer sb = new StringBuffer();
        Matcher matcher = PARAMS.matcher(path);
        while(matcher.find()){
            matcher.appendReplacement(sb,System.getProperty(matcher.group(1)));
        }
        matcher.appendTail(sb);
        while(sb.toString().endsWith("/")){sb.setLength(sb.length()-1);}
        return sb.toString();
    }
    /**
     * 判断某个IP是否在网段中 IPV4 仅限IPV4
     * @param ip aaa.bbb.ccc.ddd
     * @param ipRange aa1.bb1.cc1.dd1/mask1,aa2.bb2.cc2.dd2/mask2
     * @return 是否在网段中
     */
    public static boolean inIPRange(String ip,String ipRange){
        if(IPV4.matcher(ip).find()){
            String cacheKey = String.format("GET-%s-%s",ipRange,ip);
            Boolean cache = CacheUtil.getCache(cacheKey,Boolean.class);
            if(null!=cache)return cache;
            String[] ipRanges = ipRange.split(",");
            int ipAddress = calAddress(ip);
            for(String range:ipRanges){
                String[] ranges = range.split("/");
                int rangeAddress = calAddress(ranges[0]);
                int type = 24;
                if(ranges.length>1){
                    type = Integer.parseInt(ranges[1]);
                }
                int mask = 0xFFFFFFFF << (32 - type);
                if((ipAddress & mask) == (rangeAddress & mask)){
                    CacheUtil.putCache(cacheKey,true,60*60*1000,true);
                    return true;
                }
            }
            CacheUtil.putCache(cacheKey,false,60*60*1000,true);
            return false;
        }
        Logger.warn("传入IP不符合规范:{}",ip);
        return false;
    }
    private static int calAddress(String ip){
        String[] ips = ip.split("\\.");
        return (Integer.parseInt(ips[0]) << 24)
                | (Integer.parseInt(ips[1]) << 16)
                | (Integer.parseInt(ips[2]) << 8)
                | Integer.parseInt(ips[3]);
    }
    public static boolean isEmpty(String str){
        if(str==null){return true;}
        return str.isEmpty();
    }
    public static boolean isBlank(String str){
        if(str==null){return true;}
        return str.trim().isEmpty();
    }
    public static boolean isEmpty(Collection<?> collection){
        if(null==collection){return true;}
        return collection.isEmpty();
    }
}
