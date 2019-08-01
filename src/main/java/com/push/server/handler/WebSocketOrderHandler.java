package com.push.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author linyh
 */
@Slf4j
public class WebSocketOrderHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final String GENERATE_ORDER_NO = "generateOrderNo";

    private static final AtomicInteger ORDER_COUNT = new AtomicInteger(0);

    public static ChannelGroup orderGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static final AttributeKey<String> ORDER_NO = AttributeKey.valueOf("orderNo");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

        if (GENERATE_ORDER_NO.equals(msg.text())) {

            log.info("客户端请求生成一个orderNo");
            StringBuilder orderNo = new StringBuilder("CN");

            orderNo.append(System.currentTimeMillis())
                    .append(ORDER_COUNT.addAndGet(1));

            // 将orderNo这条channel保存起来
            ctx.channel().attr(ORDER_NO).set(orderNo.toString());
            orderGroup.add(ctx.channel());

            ctx.writeAndFlush(new TextWebSocketFrame("orderNo:" + orderNo.toString()));
        } else {

            ctx.fireChannelRead(msg.retain());
        }
    }
}
