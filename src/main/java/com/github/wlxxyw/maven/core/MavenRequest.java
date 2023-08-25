package com.github.wlxxyw.maven.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MavenRequest {
    private String clientIp;
    /**
     * get 获取
     * put 上传
     */
    private String method;

    /**
     * http请求地址
     */
    private String path;

    /**
     * body 上传内容
     */
    private byte[] body;

    /**
     * 访问用户信息
     */
    private String authorization;

}
