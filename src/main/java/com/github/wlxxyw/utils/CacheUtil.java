package com.github.wlxxyw.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存工具
 * 缓存固定资源()和定时失效的资源
 */
public class CacheUtil {
    static final long autoClearCacheTime = 10*1000L;
    static final int cacheSize = 32;
    static final long default_alive_time = 60*1000L;
    static final Map<String,Cache<? super Object>> CACHE_POOL = new ConcurrentHashMap<>();
    static final ScheduledThreadPoolExecutor clearThread = new ScheduledThreadPoolExecutor(1);

    protected void finalize(){
        clearThread.shutdown();
    }
    /**
     * 添加一个缓存内容
     * @param key 键
     * @param value 值
     * @param aliveTime 有效时间
     * @param refresh 再次访问后重置有效时间
     * @param <T> 值类型
     */
    public static <T> void putCache(String key, T value,long aliveTime,boolean refresh){
        Logger.debug("放入缓存:{},有效时间:{}ms,{}循环",key,aliveTime,refresh?"":"不");
        CACHE_POOL.put(key,new Cache<>(key,value,aliveTime,refresh));
        if(CACHE_POOL.size()>cacheSize)clearThread.execute(fullClearWorker);
    }
    /**
     * @see #putCache(String key, T value,long aliveTime,boolean refresh)
     */
    public static <T> void putCache(String key, T value,long aliveTime){
        putCache(key,value,aliveTime,false);
    }
    /**
     * @see #putCache(String key, T value,long aliveTime,boolean refresh)
     */
    public static <T> void putCache(String key, T value,boolean refresh){
        putCache(key,value,default_alive_time,refresh);
    }
    /**
     * @see #putCache(String key, T value,long aliveTime,boolean refresh)
     */
    public static <T> void putCache(String key, T value){
        putCache(key,value,default_alive_time);
    }

    /**
     * 获取缓存
     * @param key 键
     * @param clazz 缓存类型Class
     * @param <T> 缓存类型
     * @return 缓存内容/null
     */
    public static <T> T getCache(String key,Class<T> clazz){
        Cache<?> data = CACHE_POOL.get(key);
        if(null==data){
            Logger.debug("未获取到缓存:{}",key);
            return null;
        }
        if(data.failure()){
            Logger.debug("获取到失效缓存:{}",key);
            data.clear();
            return null;
        }
        Object value = data.value();
        if(clazz.isAssignableFrom(value.getClass())){
            Logger.debug("获取到有效缓存:{}",key);
            return (T)value;
        }else{
            Logger.debug("缓存({})类型({})不匹配!",key,clazz);
            return null;
        }
    }

    /**
     * 移除缓存
     * @param key 缓存键
     */
    public static void removeCache(String key){
        Logger.debug("移除缓存:{}",key);
        Cache<?> cache = CACHE_POOL.get(key);
        if(null!=cache)cache.clear();
    }
    /**
     * 移除缓存
     * @param key 缓存键的前部分
     */
    public static void removeCacheLike(String key){
        Logger.debug("移除缓存:{}*",key);
        Set<String> keys = CACHE_POOL.keySet().parallelStream().filter(one->one.startsWith(key)).collect(Collectors.toSet());
        keys.forEach(CacheUtil::removeCache);
    }
    /**
     * 定时清理缓存
     */
    private static final Runnable clockClearWorker = ()->{
        Set<String> clearKeys = new HashSet<>();
        CACHE_POOL.forEach((k, v)->{
            if(v.failure()){clearKeys.add(k);}
        });
        clearKeys.parallelStream().forEach(key->{
            if(CACHE_POOL.get(key).failure()){
                CACHE_POOL.remove(key);}
        });
        Logger.debug("清理失效缓存!");
    };
    /**
     * 容量超限清理缓存(按失效时间清理)
     */
    private static final Runnable fullClearWorker = ()->{
        if(CACHE_POOL.size()<=cacheSize){return;}
        List<Cache<?>> caches = new ArrayList<>(CACHE_POOL.values());
        Collections.sort(caches);
        caches.subList(0,caches.size()-cacheSize).parallelStream().forEach(cache-> CACHE_POOL.remove(cache.key));
        Logger.debug("清理超额缓存!");
    };
    static{
        clearThread.scheduleAtFixedRate(clockClearWorker,autoClearCacheTime,autoClearCacheTime,TimeUnit.MILLISECONDS);
    }
    static class Cache<T> implements Comparable<Cache<?>>{
        public Cache(String key,T data,long aliveTime,boolean refresh){
            if(null==data) throw new RuntimeException("Cache.data can`t be null!");
            if(aliveTime<=0)throw new RuntimeException("Cache.aliveTime must more than zero");
            this.key = key;
            this.data = data;
            this.aliveTime = aliveTime;
            this.refresh = refresh;
            update(true);
        }
        private long clearTime;
        private long aliveTime;
        private final String key;
        private final T data;
        private boolean refresh;
        public T value(){
            update(false);
            return this.data;
        }
        public void clear(){
            aliveTime = 0;
            clearTime = Long.MIN_VALUE;
        }
        public boolean failure(){
            if(aliveTime<0)return false;
            if(aliveTime==0){return true;}
            return clearTime < System.currentTimeMillis();
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cache<?> cache = (Cache<?>) o;
            return data == cache.data;
        }
        @Override
        public int hashCode() {
            return data.hashCode();
        }
        private void update(boolean doRefresh) {
            if((refresh||doRefresh)&&aliveTime>0){
                long now = System.currentTimeMillis();
                this.clearTime = now+aliveTime;
                if(now>clearTime){//超过最大值
                    clearTime = Long.MAX_VALUE;
                    refresh = false;
                }
            }
        }
        @Override
        public int compareTo(Cache o) {
            return Long.compare(this.clearTime,o.clearTime);
        }
    }
}
