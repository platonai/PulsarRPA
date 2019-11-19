package com.github.monkeywie.proxyee;

import com.github.monkeywie.proxyee.exception.HttpProxyExceptionHandle;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.CertDownIntercept;
import com.github.monkeywie.proxyee.intercept.common.FullRequestIntercept;
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept;
import com.github.monkeywie.proxyee.proxy.ProxyConfig;
import com.github.monkeywie.proxyee.proxy.ProxyType;
import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import com.github.monkeywie.proxyee.util.HttpUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;

import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.time.LocalDateTime;

public class InterceptFRFRProxyServer {

    public static void main(String[] args) throws Exception {
        HttpProxyServerConfig config = new HttpProxyServerConfig();
        config.setHandleSsl(true);

        HttpProxyServer server = new HttpProxyServer();
        server.serverConfig(config);
        server.proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline) {
                        pipeline.addLast(new CertDownIntercept());
                        pipeline.addLast(new FullRequestIntercept() {

                            @Override
                            public boolean match(HttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) {
                                System.out.println("\n\n" + LocalDateTime.now());
                                System.out.println(httpRequest.toString());

                                //如果是json报文
                                if (HttpUtil.checkHeader(httpRequest.headers(), HttpHeaderNames.CONTENT_TYPE, "^(?i)application/json.*$")) {
                                    return true;
                                }
                                return false;
                            }

                            @Override
                            public void handelRequest(FullHttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) {
                                ByteBuf content = httpRequest.content();
                                //打印请求信息
                                System.out.println(httpRequest.toString());
                                System.out.println(content.toString(Charset.defaultCharset()));
                                //修改请求体
                                String body = "{\"name\":\"intercept\",\"pwd\":\"123456\"}";
                                content.clear();
                                content.writeBytes(body.getBytes());
                            }
                        });

                        pipeline.addLast(new FullResponseIntercept() {
                            @Override
                            public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                                //在匹配到百度首页时插入js
                                return HttpUtil.checkUrl(pipeline.getHttpRequest(), "^www.baidu.com$")
                                        && HttpUtil.isHtml(httpRequest, httpResponse);
                            }

                            @Override
                            public void handelResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                                //打印原始响应信息
                                System.out.println(httpResponse.toString());
                                // System.out.println(httpResponse.headers().toString());
                                // System.out.println(httpResponse.content().toString(Charset.defaultCharset()));
                                //修改响应头和响应体
                                httpResponse.headers().set("handel", "edit head");
                                /*
                                    int index = ByteUtil.findText(httpResponse.content(), "<head>");
                                    ByteUtil.insertText(httpResponse.content(), index, "<script>alert(1)</script>");
                                    */
                                httpResponse.content().writeBytes("<script>alert('hello proxyee')</script>".getBytes());
                                System.out.println();
                            }
                        });
                    }
                });

        server.httpProxyExceptionHandle(new HttpProxyExceptionHandle() {
            void beforeCatch(Channel clientChannel, Throwable cause) {
                // log.warn("Internal proxy error - {}", StringUtil.stringifyException(cause))
            }

            void afterCatch(Channel clientChannel, Channel proxyChannel, Throwable cause) {
                System.out.println("Internal proxy error - " + cause);
            }
        });

        ProxyConfig proxyConfig = new ProxyConfig(ProxyType.HTTP, "127.0.0.1", 9999);
        server.proxyConfig(proxyConfig);

        server.start(8184);
    }
}
