package com.github.wlxxyw.maven.core;

import com.github.wlxxyw.maven.config.MavenConfig;
import com.github.wlxxyw.maven.exception.MavenException;
import com.github.wlxxyw.utils.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.github.wlxxyw.maven.core.MavenResponse.*;

/**
 * 处理请求
 */
public class MavenRepositoryStore {
    static final String indexHtml;
    static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static{
        indexHtml = new String(IoUtil.readAsByteArray("maven_server_index.html"), StandardCharsets.UTF_8);
    }
    private final MavenConfig config;
    public MavenRepositoryStore(MavenConfig config){
        this.config = config;
        if (!System.getProperties().containsKey("system.start")) {
            System.setProperty("system.start", String.valueOf(System.currentTimeMillis()));
        }
    }
    /**
     * 处理请求
     * @param request 请求
     * @return 响应
     */
    public MavenResponse handle(MavenRequest request){
        if(!config.validaIp(request.getClientIp())){
            return new MavenResponse(FORBIDDEN,TEXT_INFO,"IP NOT IN AllowList");
        }
        String requestPath = request.getPath();
        String repositoryName = RegularUtil.getRepositoryID(requestPath);
        String path = RegularUtil.getRepositoryPath(requestPath);
        //pom文件使用HEAD请求 jar/sha1文件使用GET请求
        if ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) {
            byte[] data;
            try {
                data = load(repositoryName, path, request, 0);
            } catch (Exception e) {
                Logger.warn("load failed,lib = {}, path = {}", repositoryName,path);
                return new MavenResponse(NOT_FOUND,TEXT_INFO, e.getLocalizedMessage());
            }
            if (null == data) {
                return new MavenResponse(NOT_FOUND,TEXT_INFO, "not found, lib = " + repositoryName +", path = "+path);
            }
            return new MavenResponse(request.getPath(),OK, data);
        }
        //上传使用PUT请求
        if ("PUT".equals(request.getMethod())) {
            byte[] data = request.getBody();
            if (null == data || data.length == 0) {
                return new MavenResponse(NOT_FOUND, TEXT_INFO,"stream has error, lib = " + repositoryName +", path = "+path);
            }
            if(save(repositoryName, path, request)){
                return new MavenResponse(OK, TEXT_INFO,"OK");
            }else{
                return new MavenResponse(INTERNAL_SERVER_ERROR, "save failed, path = {}", path);
            }
        }
        return new MavenResponse(METHOD_NOT_ALLOWED,TEXT_INFO, request.getMethod() + " not support!");
    }
    /**
     * 加载资源文件.
     *
     * @param repositoryName 仓库
     * @param path    路径
     * @param request 请求
     * @param mode    模式
     * @param fromRepositoryNames 转发来源 避免循环转发
     * @return 资源
     */
    private byte[] load(String repositoryName, String path, MavenRequest request, int mode, String...fromRepositoryNames) {
        List<String> fromRepositoryNameList = Arrays.asList(fromRepositoryNames);
        if(fromRepositoryNameList.contains(repositoryName)){
            throw new MavenException(500,"cycle target !"+String.join("->",IoUtil.join(fromRepositoryNames,repositoryName)));
        }
        //优先尝试从缓存获取
        String cacheKey = String.format("GET-%s-%s-%s",repositoryName,path,mode);
        byte[] data = CacheUtil.getCache(cacheKey, byte[].class);
        if(null!=data){ return data; }
        // 验证仓库是否可用.
        MavenConfig.Repository repository = config.validaRepository(repositoryName);
        if(null==repository){
            throw new MavenException(404, "lib " + repositoryName + " not active");
        }
        //mode权限.
        mode |= repository.getMode();
        if((mode & 0b10) == 0){
            throw new MavenException(403, repositoryName + " not support read.");
        }
        File target = new File(config.localRepository()+File.separator+repositoryName+path);
        if(target.isDirectory()){//目标是文件夹
            data = explorer(target, request);
            CacheUtil.putCache(cacheKey,data);//默认缓存时间
            return data;
        }
        if(target.isFile()&&target.length()>0){//目标是文件
            data = IoUtil.readAsByteArray(target);
            CacheUtil.putCache(cacheKey,data,10*60*1000L,true);//重复刷新缓存
            return data;
        }
        if(!RegularUtil.isEmpty(repository.getUrl())){
            // URL转发
            for (String httpUrl : repository.getUrl()) {
                data = HttpUtil.doGet(httpUrl + path);
                if (null != data) {
                    Logger.info("download success, url = {}{} ", httpUrl, path);
                    IoUtil.writeFile(target, data);
                    CacheUtil.putCache(cacheKey,data,10*60*1000L,true);//重复缓存10min
                    return data;
                }
                Logger.debug("download failed, url = {}{} ", httpUrl, path);
            }
        }
        if(!RegularUtil.isBlank(repository.getTarget())){
            // 转发到其它仓库执行
            Logger.debug("redirect repository({})",repository.getTarget());
            data = load(repository.getTarget(), path, request, mode, IoUtil.join(fromRepositoryNames,repositoryName));
            if(null!=data)CacheUtil.putCache(cacheKey,data,true);//重复缓存1min
            return data;
        }
        return null;
    }

    private byte[] explorer(File target, MavenRequest request) {
        String path = request.getPath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        File[] files = target.listFiles();
        StringBuilder dirContent = new StringBuilder();
        StringBuilder fileContent = new StringBuilder();
        if (null != files) {
            StringBuilder li = new StringBuilder();
            for (File file : files) {
                String encodeFileName = "";
                try{
                    encodeFileName = URLEncoder.encode(file.getName(),"UTF-8");
                }catch (UnsupportedEncodingException ignored){}
                li.append("<li><span class=\"name\"><a href=\"")
                        .append(encodeFileName)
                        .append("\">").append(file.getName()).append("</a></span><span class=\"modified\">")
                        .append(sdf.format(new Date(file.lastModified())))
                        .append("</span><span class=\"length\">")
                        .append(file.isDirectory()?"---":file.length())
                        .append("</span></li>");
                if (file.isDirectory()) {
                    dirContent.append(li);
                } else {
                    fileContent.append(li);
                }
                li.setLength(0);
            }
        }
        dirContent.append(fileContent);
        return indexHtml.replace("${path}", path).replace("${content}", dirContent.toString()).replace("${time}",format(System.currentTimeMillis())).getBytes(StandardCharsets.UTF_8);
    }
    private String format(long current){
        long start = Long.parseLong(System.getProperty("system.start"));
        long ms = current - start;
        long temp = ms / (1000*60);
        int minute =  Math.toIntExact(temp % 60);
        temp = temp / 60;
        int hour = Math.toIntExact(temp % 60);
        temp = temp / 24;
        int day = Math.toIntExact(temp % 24);
        return day + " days," + hour + " hours," + minute + " minutes";
    }

    /**
     * 保存jar文件
     * @param repositoryName 仓库
     * @param path    位置
     * @param request    请求
     */
    private boolean save(String repositoryName, String path, MavenRequest request) {
        String userName = config.validaAuth(request.getAuthorization());
        if(null==userName){
            throw new MavenException(401,"Unauthorised");
        }
        MavenConfig.Repository repository = config.validaRepository(repositoryName);
        if(null==repository){
            throw new MavenException(404, "lib " + repositoryName + " not active");
        }
        if((repository.getMode() & 0b01) == 0){
            throw new MavenException(403, repositoryName + " not support write.");
        }
        IoUtil.writeFile(config.localRepository()+File.separator+repositoryName+path, request.getBody());
        //清理相关缓存
        String cacheKey = String.format("GET-%s-%s",repositoryName,path);
        CacheUtil.removeCacheLike(cacheKey);
        //写入记录
        return SQLiteUtil.uploadLog(userName,request.getClientIp(),repositoryName,path);
    }
}
