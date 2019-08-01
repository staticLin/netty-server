package com.push.server.servlet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author linyh
 */
@Slf4j
public class ChatIndexServlet extends AbstractServlet {

    private static final String CHAT_INDEX_PATH = "/chatIndex.html";

    @Override
    protected void doGet(FullHttpRequest request, HttpResponse response, ChannelHandlerContext ctx) throws Exception {

        // 将文件内容写出去
        responseByFileContent(CHAT_INDEX_PATH, response, ctx);
    }
}
