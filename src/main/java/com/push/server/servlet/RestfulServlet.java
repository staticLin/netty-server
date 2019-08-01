package com.push.server.servlet;

import com.push.server.handler.WebSocketOrderHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import static com.push.server.handler.HttpRequestHandler.sendErrorResponse;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

/**
 * @author linyh
 */
@Slf4j
public class RestfulServlet extends AbstractServlet {

    @Override
    protected void doGet(FullHttpRequest request, HttpResponse response, ChannelHandlerContext ctx) {

        if (!request.uri().contains("?")) {
            //sendError
            sendErrorResponse(ctx, BAD_REQUEST);
            return;
        }

        String orderNo = request.uri()
                .substring(request.uri().indexOf("?") + 1)
                .split("=")[1];

        if (orderNo == null || orderNo.length() == 0) {
            //sendError
            sendErrorResponse(ctx, BAD_REQUEST);
            return;
        }

        String channelOrderNo;
        for (Channel channel : WebSocketOrderHandler.orderGroup) {
            channelOrderNo = channel.attr(WebSocketOrderHandler.ORDER_NO).get();

            if (channelOrderNo == null || channelOrderNo.length() == 0) {
                continue;
            }

            if (orderNo.equals(channelOrderNo)) {
                channel.writeAndFlush(new TextWebSocketFrame("success"));

                // 发送数据
                ctx.writeAndFlush(buildResponse("success"));
                return;
            }
        }

        // 发送数据
        ctx.writeAndFlush(buildResponse("fail"));
    }

    private FullHttpResponse buildResponse(String result) {

        // 重新构造一个响应对象
        FullHttpResponse restFulResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        // 响应一个简单文本
        restFulResponse.headers().set(CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

        byte[] bytes = result.getBytes();

        // 响应一个字符串
        restFulResponse.content().writeBytes(bytes);

        restFulResponse.headers().set(CONTENT_LENGTH, bytes.length);

        return restFulResponse;
    }
}
