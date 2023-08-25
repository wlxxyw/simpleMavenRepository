package com.github.wlxxyw.maven.config;

import com.github.wlxxyw.utils.CacheUtil;
import com.github.wlxxyw.utils.RegularUtil;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public abstract class MavenConfig {
    //允许的客户端
    protected final String clientRange;
    //使用的端口号
    protected final Integer httpPort;
    //仓库本地的缓存位置, 支持从环境变量中获取替换
    protected final String localRepository;
    //仓库列表
    protected final Map<String,Repository> repositories;
    //用户
    protected final Map<String,String> authorizations;
    public interface User{
        String getName();
        String getPassword();
    }
    public interface Repository{
        String getId();
        String getName();
        int getMode();//0b01 可写 0b10 可读 0b11 可读可写
        String getTarget();
        List<String> getUrl();
    }

    private static final List<String> DEFAULT_WHITE_IP = Arrays.asList("localhost","127.0.0.1","ip6-localhost");
    public boolean validaIp(String clientIp){
        if(DEFAULT_WHITE_IP.contains(clientIp)) return true;
        Boolean value = CacheUtil.getCache("IP:"+clientIp,Boolean.class);
        if(null==value){
            value = RegularUtil.inIPRange(clientIp,clientRange);
            CacheUtil.putCache("IP:"+clientIp, value,value?Long.MAX_VALUE:(60*60*1000L));
        }
        return value;
    }
    public String validaAuth(String authorization){
        return authorizations.get(authorization);
    }
    public Repository validaRepository(String repositoryID){
        return repositories.get(repositoryID);
    }
    public Map<String,Repository> repositories(){
        return Collections.unmodifiableMap(repositories);
    }
    public String localRepository(){
        return RegularUtil.replaceEnvVariable(localRepository);
    }
    public Integer httpPort(){
        return httpPort;
    }
}