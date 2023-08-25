package com.github.wlxxyw.maven.core;

import com.github.wlxxyw.utils.Logger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;

/**
 * 响应
 */
@Data
public class MavenResponse {
    public static final int OK = 200;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final String HTML = "text/html";
    public static final String XML = "text/xml";
    public static final String TEXT_INFO = "text/plain";
    public static final String BINARY = "application/octet-stream";
    /**
     * HTTP响应码
     */
    private int statusCode;

    private String contentType;

    private String statusMessage;

    private byte[] body;

    public MavenResponse(String url, int statusCode, byte[] body) {
        this.statusCode = statusCode;
        this.statusMessage = String.valueOf(statusCode);
        this.body = body;
        initContentType(url);
    }
    public MavenResponse(int statusCode, String msg,String...params) {
        if(null != params){
            for(String param:params){
                msg = msg.replace("{}",param);
            }
        }
        this.statusCode = statusCode;
        this.statusMessage = msg;
        this.body = msg.getBytes(StandardCharsets.UTF_8);
        this.contentType = TEXT_INFO;
        if(OK!=statusCode){
            Logger.warn(msg);
        }
    }
    private void initContentType(String url){
        contentType = HTML;
        if(url.endsWith(".xml")||url.endsWith(".pom")||url.endsWith("/"))contentType = XML;
        if(url.endsWith(".jar"))contentType = BINARY;
    }
}
