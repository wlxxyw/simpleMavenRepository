package com.github.wlxxyw.maven;

import com.github.wlxxyw.maven.config.MavenConfig;
import com.github.wlxxyw.maven.core.MavenManagerServer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class PropertiesMavenManagerDemo {
    public static void main(String[] args) {
        String propertiesConfig = "#限制访问IP 只支持IPV4\n" +
                "clientRange=192.168.0.0/16,127.0.0.1\n" +
                "#服务端口\n" +
                "httpPort=8091\n" +
                "#本地存储路径\n" +
                "localRepository=D:\n" +
                "#上传操作支持的用户\n" +
                "user[0].name=admin\n" +
                "user[0].password=admin\n" +
                "user[1].name=root\n" +
                "user[1].password=root\n" +
                "#仓库ID\n" +
                "repository[0].id=remote\n" +
                "#仓库名称\n" +
                "repository[0].name=central repository\n" +
                "#0b01可写(上传) 0b10可读(下载)\n" +
                "repository[0].mode=2\n" +
                "#远程仓库地址\n" +
                "repository[0].url[0]=https://maven.aliyun.com/nexus/content/groups/public\n" +
                "repository[0].url[1]=http://central.maven.org/maven2\n" +
                "#仓库ID\n" +
                "repository[1].id=repository\n" +
                "repository[1].name=local repository\n" +
                "repository[1].mode=3\n" +
                "#转发仓库ID(本仓库未找到时,朝目标仓库请求)\n" +
                "repository[1].target=remote\n";
        System.setProperty("debug","true");
        new MavenManagerServer(PropertiesConfig.of(new ByteArrayInputStream(propertiesConfig.getBytes(StandardCharsets.UTF_8)))).run();
    }
    public static class PropertiesConfig extends MavenConfig{
        @Getter
        @AllArgsConstructor
        static class _User implements User{
            private String name;
            private String password;
        }
        @Getter@AllArgsConstructor
        static class _Repository implements Repository{
            private String id;
            private String name;
            private int mode;
            private String target;
            private List<String> url;
            public void formatter() {
                if(Objects.nonNull(url))for(int i = 0; i<url.size(); i++){
                    String _url = url.get(i);
                    if(_url.endsWith("/")){_url = _url.substring(0,_url.length()-1);}
                    if(_url.startsWith("http[s]?://")){_url = "http://"+_url;}
                    url.set(i,_url);
                }
            }
        }
        public static PropertiesConfig of(InputStream is){
            try{
                String clientRange = "0.0.0.0/0";
                int httpPort = 8080;
                String localRepository = "${user.home}/mavenManager";
                Properties properties = new Properties();
                properties.load(is);
                clientRange = properties.getProperty("clientRange",clientRange);
                if(properties.containsKey("httpPort")){
                    httpPort = Integer.parseInt(properties.getProperty("httpPort"));
                }
                localRepository = properties.getProperty("localRepository",localRepository);
                List<User> _users = new ArrayList<>();
                for(int i=0;;i++){
                    if(properties.containsKey("user["+i+"].name")){
                        _users.add(new _User(properties.getProperty("user["+i+"].name"),properties.getProperty("user["+i+"].password","")));
                    }else{
                        break;
                    }
                }
                List<_Repository> _repositories = new ArrayList<>();
                for(int i=0;;i++){
                    if(properties.containsKey("repository["+i+"].id")){
                        String id = properties.getProperty("repository["+i+"].id");
                        String name = properties.getProperty("repository["+i+"].name");
                        String mode = properties.getProperty("repository["+i+"].mode");
                        String target = properties.getProperty("repository["+i+"].target");
                        List<String> urls = new ArrayList<>();
                        if(null==target){
                            for(int j=0;;j++){
                                if(properties.containsKey("repository["+i+"].url["+j+"]")){
                                    urls.add(properties.getProperty("repository["+i+"].url["+j+"]"));
                                }else{
                                    break;
                                }
                            }
                        }
                        _repositories.add(new _Repository(id,name,Integer.parseInt(mode),target,urls));
                    }else{
                        break;
                    }
                }
                Map<String, Repository> repositories = _repositories.stream().peek(_Repository::formatter).collect(Collectors.toMap(Repository::getId, one -> one));
                Map<String, String> authorizations = _users.stream().collect(Collectors.toMap(one ->
                        new String(Base64.getEncoder().encode((one.getName()+":"+one.getPassword()).getBytes())),User::getName));
                return new PropertiesConfig(clientRange,httpPort,localRepository,repositories,authorizations);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        private PropertiesConfig(String clientRange, Integer httpPort, String localRepository, Map<String, Repository> repositories, Map<String, String> authorizations) {
            super(clientRange, httpPort, localRepository, repositories, authorizations);
        }
    }
}
