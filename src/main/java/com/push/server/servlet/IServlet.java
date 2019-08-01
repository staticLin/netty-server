package com.push.server.servlet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * @author linyh
 */
public interface IServlet {

    /**
     * 具体执行处理的逻辑方法
     *
     * @param request  为了方便，复用了Netty封装的请求对象
     * @param response Netty包装的http响应对象
     * @param ctx      假冒伪劣的Servlet规范,为了方便在这里传一个channel的上下文,方便写数据
     */
    void service(FullHttpRequest request, HttpResponse response, ChannelHandlerContext ctx);

}
