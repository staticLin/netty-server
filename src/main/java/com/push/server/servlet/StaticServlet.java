package com.push.server.servlet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 静态资源的servlet处理
 *
 * @author linyh
 */
@Slf4j
public class StaticServlet extends AbstractServlet {

    @Override
    protected void doGet(FullHttpRequest request, HttpResponse response, ChannelHandlerContext ctx) throws Exception {

        // 将文件内容写出去
        responseByFileContent(request.uri(), response, ctx);
    }
}
