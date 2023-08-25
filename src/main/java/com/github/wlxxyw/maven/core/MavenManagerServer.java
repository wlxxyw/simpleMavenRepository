package com.github.wlxxyw.maven.core;

import com.github.wlxxyw.maven.config.MavenConfig;
import com.github.wlxxyw.maven.exception.MavenException;
import com.github.wlxxyw.utils.*;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.Executors;

import static com.github.wlxxyw.maven.core.MavenResponse.BINARY;
import static com.github.wlxxyw.maven.core.MavenResponse.TEXT_INFO;

/**
 * @author Xu
 */
public class MavenManagerServer implements Runnable{
    private final MavenConfig config;
    public MavenManagerServer(MavenConfig config){
        this.config = config;
    }
    @Override
    public void run() {
        MavenRepositoryStore store = new MavenRepositoryStore(config);
        HeadersFilter headersFilter = new HeadersFilter();
        HttpServer httpServer;
        try {
            //初始一个http服务. 最大并发接收100个请求.
            httpServer = HttpServer.create(new InetSocketAddress(config.httpPort()), 100);
            //并发10个请求.
            httpServer.setExecutor(Executors.newFixedThreadPool(10));
            HttpContext context = httpServer.createContext("/", (httpExchange -> handle(httpExchange, store)));
            // java 自带的http server 响应时会将 head 中的key 中第二段变成小写开头.如，http协议：Content-Length, 但httpServer 则返回 Content-length.
            // 有些http的请求端可能会无法处理.所以将http头部转化一下.
            context.getFilters().add(headersFilter);
        } catch (IOException e) {
            Logger.error("create context failed, port = {}",config.httpPort(), e);
            return;
        }
        httpServer.start();
        Logger.info("server on port = {}", config.httpPort());
        for (MavenConfig.Repository repository : config.repositories().values()) {
            Logger.info("{} -> http://{}:{}/{}",repository.getName(), HttpUtil.serverIp(), config.httpPort(), repository.getId());
        }
        //永久缓存 GET /favicon.ico 访问响应
        CacheUtil.putCache("GET-favicon.ico--0", IoUtil.readAsByteArray("favicon.ico"),Long.MAX_VALUE);
        //永久缓存 GET / 访问响应, maven的示例配置
        CacheUtil.putCache("GET---0",IoUtil.readAsByteArray("maven_client_setting.xml"),Long.MAX_VALUE);
    }

    private void handle(HttpExchange httpExchange, MavenRepositoryStore store) throws IOException{
        String method = httpExchange.getRequestMethod();
        MavenRequest mavenRequest = new MavenRequest();
        mavenRequest.setMethod(method.toUpperCase(Locale.ROOT));
        mavenRequest.setPath(httpExchange.getRequestURI().getPath());
        mavenRequest.setClientIp(httpExchange.getRemoteAddress().getAddress().getHostAddress());
        if("PUT".equals(method)){
            String contentLength = httpExchange.getRequestHeaders().getFirst("Content-Length");
            byte[] data = IoUtil.readAsByteArray(httpExchange.getRequestBody(), Integer.parseInt(contentLength));
            if (Integer.parseInt(contentLength) != data.length) {
                Logger.error("data error, contentLength = {}, data.len = {}, path = {}", contentLength, data.length,
                        httpExchange.getRequestURI().getPath());
                sendResponse(httpExchange, 500,TEXT_INFO, "data read failed".getBytes());
                return;
            }
            mavenRequest.setBody(data);
            String authorization = httpExchange.getRequestHeaders().getFirst("Authorization");
            if (null == authorization || !authorization.startsWith("Basic ")) {
                httpExchange.getResponseHeaders().add("WWW-Authenticate","Basic realm=\"upload\"");
                sendResponse(httpExchange, 401,TEXT_INFO, "Unauthorised".getBytes());
                return;
            }
            authorization = authorization.substring(6).trim();
            mavenRequest.setAuthorization(authorization);
        }
        MavenResponse mavenResponse;
        try{
            mavenResponse = store.handle(mavenRequest);
        }catch (MavenException e){
            mavenResponse = new MavenResponse(e.getCode(),TEXT_INFO,e.getMessage());
        }
        if (null != mavenResponse) {
            Logger.info("SUCCESS({}),{} - {}", mavenResponse.getStatusCode(), mavenRequest.getMethod(), mavenRequest.getPath());
            sendResponse(httpExchange, mavenResponse.getStatusCode(), mavenResponse.getContentType(), mavenResponse.getBody());
            return;
        }
        Logger.info("FAIL,{} - {}",  mavenRequest.getMethod(), mavenRequest.getPath());
        sendResponse(httpExchange, 404, TEXT_INFO, "404".getBytes());
    }
    private void sendResponse(HttpExchange httpExchange, int statusCode, String contentType, byte[] responseBody) throws IOException {
        if (null == responseBody) {
            responseBody = new byte[0];
        }
        httpExchange.getResponseHeaders().add("Content-Type",contentType);
        httpExchange.sendResponseHeaders(statusCode, responseBody.length);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(responseBody);
        outputStream.flush();
        outputStream.close();
    }
}
