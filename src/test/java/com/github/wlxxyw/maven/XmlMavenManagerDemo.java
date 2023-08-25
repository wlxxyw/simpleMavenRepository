package com.github.wlxxyw.maven;

import com.github.wlxxyw.maven.config.MavenConfig;
import com.github.wlxxyw.maven.core.MavenManagerServer;
import com.github.wlxxyw.utils.JaxbUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import javax.xml.stream.XMLStreamException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class XmlMavenManagerDemo {
    public static void main(String[] args) {
        String xmlConfig = "<Config>\n" +
                "    <clientRange>192.168.0.0/16,127.0.0.1</clientRange><!-- 限制访问IP 只支持IPV4 -->\n" +
                "    <httpPort>8091</httpPort><!--服务端口-->\n" +
                "    <localRepository>D:</localRepository><!--本地存储路径-->\n" +
                "    <user name=\"admin\" password=\"admin\"/><!--上传操作支持的用户-->\n" +
                "    <user name=\"root\" password=\"root\"/>\n" +
                "    <repository id=\"remote\" name=\"central repository\" mode=\"2\">\n" +
                "        <url>https://maven.aliyun.com/nexus/content/groups/public</url>\n" +
                "        <url>http://central.maven.org/maven2</url>\n" +
                "    </repository>\n" +
                "    <repository id=\"repository\" name=\"local repository\" mode=\"3\" target=\"remote\" />\n" +
                "</Config>";
        System.setProperty("debug","true");
        new MavenManagerServer(XmlConfig.of(xmlConfig)).run();
    }
    static class XmlConfig extends MavenConfig{
        @XmlRootElement(name = "Config")@Getter
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class XmlBean{
            @XmlElement
            private String clientRange;
            @XmlElement
            private Integer httpPort;
            @XmlElement
            private String localRepository;
            @XmlElements(@XmlElement(name = "user"))
            private List<_User> users;
            @XmlElements(@XmlElement(name = "repository"))
            private List<_Repository> repositories;
        }
        @Getter@XmlType
        @AllArgsConstructor
        @NoArgsConstructor
        public static class _User implements User {
            //用户名.
            @XmlAttribute
            private String name;
            //密码.
            @XmlAttribute
            private String password;
        }
        @Getter@XmlType
        @AllArgsConstructor@NoArgsConstructor
        public static class _Repository implements Repository {
            // 仓库id, 很重要.
            @XmlAttribute
            private String id;
            // 仓库名称.
            @XmlAttribute
            private String name;
            // 模式: 00 不可用; 01 只写; 10 只读; 11 可读可写
            @XmlAttribute
            private int mode;
            // 转发到本地其它仓库中.
            @XmlAttribute
            private String target;
            // 远程仓库转发,请求会转发到远程.
            @XmlElement
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
        public static XmlConfig of(String xml){
            try{
                XmlBean xmlBean = JaxbUtil.xml2Object(xml,XmlBean.class);
                String clientRange = xmlBean.getClientRange();
                int httpPort = xmlBean.getHttpPort();
                String localRepository = xmlBean.getLocalRepository();
                List<_User> _users = xmlBean.getUsers();
                List<_Repository> _repositories = xmlBean.getRepositories();
                Map<String, Repository> repositories = _repositories.stream().peek(_Repository::formatter).collect(Collectors.toMap(Repository::getId, one -> one));
                Map<String, String> authorizations = _users.stream().collect(Collectors.toMap(one ->
                        new String(Base64.getEncoder().encode((one.getName()+":"+one.getPassword()).getBytes())),User::getName));
                return new XmlConfig(clientRange,httpPort,localRepository,repositories,authorizations);
            } catch (JAXBException | XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }
        private XmlConfig(String clientRange, Integer httpPort, String localRepository, Map<String, Repository> repositories, Map<String, String> authorizations) {
            super(clientRange, httpPort, localRepository, repositories, authorizations);
        }
    }
}
