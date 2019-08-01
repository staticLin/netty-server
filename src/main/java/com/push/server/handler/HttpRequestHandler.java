package com.push.server.handler;

import com.push.server.servlet.AbstractServlet;
import com.push.server.servlet.IServlet;
import com.push.server.servlet.ServletUtils;
import com.push.server.servlet.StaticServlet;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * http协议的处理类
 *
 * @author linyh
 */
@Slf4j
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final String WS_URI = "/Websocket";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        if (request.uri().equals(WS_URI)) {
            log.info("webSocket握手http请求");

            // 将读事件传递下去
            ctx.fireChannelRead(request.retain());
            return;
        }

        // 400-错误的客户端请求
        if (request.decoderResult().isFailure()) {
            sendErrorResponse(ctx, BAD_REQUEST);
            return;
        }

        // 只支持GET方法
        if (!request.method().equals(GET)) {
            sendErrorResponse(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        String requestUri = request.uri();
        log.info("http请求的uri为: [{}]", requestUri);


        String newUri = requestUri;
        if (requestUri.contains("?")) {
            newUri = requestUri.substring(0, requestUri.indexOf("?"));
        }

        // 构建响应对象
        HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);

        // 若是静态资源的请求，交给StaticServlet去做
        if (!handleStaticResourceRequest(newUri, request, response, ctx)) {
            IServlet servlet = ServletUtils.findUriMapping(newUri);

            // 404-NOT FOUND
            if (servlet == null) {
                sendErrorResponse(ctx, NOT_FOUND);
                return;
            }

            // servlet负责构建响应对象
            servlet.service(request, response, ctx);
        }

        // 业务逻辑处理时出现了异常-500
        if (response.status().equals(HttpResponseStatus.INTERNAL_SERVER_ERROR)) {
            sendErrorResponse(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private boolean handleStaticResourceRequest(String uri, FullHttpRequest request, HttpResponse response, ChannelHandlerContext ctx) {

        // 默认响应的类型
        String contextType = "text/html;";

        boolean isStaticResource = false;
        if (uri.endsWith(".css")) {

            contextType = "text/css;";
            isStaticResource = true;

        } else if (uri.endsWith(".js")) {

            contextType = "text/javascript;";
            isStaticResource = true;

        } else if (uri.toLowerCase().matches(".*\\.(jpg|png|gif)$")) {

            String ext = uri.substring(uri.lastIndexOf("."));
            contextType = "image/" + ext;
            isStaticResource = true;

        }

        response.headers().set(CONTENT_TYPE, contextType + "charset=utf-8;");

        if (!isStaticResource) {
            return false;
        }

        AbstractServlet staticServlet = new StaticServlet();
        staticServlet.service(request, response, ctx);

        return true;
    }

    public static void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {

        HttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                status, Unpooled.copiedBuffer(status.toString(), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "text/plain; charset=utf-8;");

        // 直接写数据,然后关闭连接
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
