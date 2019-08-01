package com.push.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.push.server.protocol.IMDecoder;
import com.push.server.protocol.IMEncoder;
import com.push.server.protocol.IMMessage;
import com.push.server.protocol.IMP;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import static com.push.server.protocol.IMP.*;

/**
 * @author linyh
 */
@Slf4j
public class WebSocketChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static IMDecoder decoder = new IMDecoder();

    private static IMEncoder encoder = new IMEncoder();

    /**
     * 记录在线用户
     */
    private static ChannelGroup onlineUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private static final AttributeKey<String> NICK_NAME = AttributeKey.valueOf("nickName");
    private static final AttributeKey<String> IP_ADDR = AttributeKey.valueOf("ipAddr");
    private static final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");
    private static final AttributeKey<String> FROM = AttributeKey.valueOf("from");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

        IMMessage request = decoder.decode(msg.text());

        if (null == request) {
            log.error("解析请求失败");

            ctx.fireChannelRead(msg);
            return;
        }

        Channel client = ctx.channel();

        String addr = getAddress(client);

        if (request.getCmd().equals(LOGIN.getName())) {
            client.attr(NICK_NAME).set(request.getSender());
            client.attr(IP_ADDR).set(addr);
            client.attr(FROM).set(request.getTerminal());
            onlineUsers.add(client);

            for (Channel channel : onlineUsers) {

                // 判断请求是否来自自己
                boolean fromSelf = (channel == client);

                if (!fromSelf) {
                    request = new IMMessage(SYSTEM.getName(), sysTime(), onlineUsers.size(), getNickName(client) + "加入");
                } else {
                    request = new IMMessage(SYSTEM.getName(), sysTime(), onlineUsers.size(), "已与服务器建立连接！");
                }

                String content = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        } else if (request.getCmd().equals(CHAT.getName())) {

            for (Channel channel : onlineUsers) {

                boolean fromSelf = (channel == client);
                if (fromSelf) {
                    request.setSender("自己");
                } else {
                    request.setSender(getNickName(client));
                }
                request.setTime(sysTime());

                String content = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        } else if (request.getCmd().equals(FLOWER.getName())) {

            JSONObject attrs = getAttrs(client);

            long currTime = sysTime();
            if (null != attrs) {

                long lastTime = attrs.getLongValue("lastFlowerTime");
                //60秒之内不允许重复刷鲜花
                int secends = 10;
                long sub = currTime - lastTime;
                if (sub < 1000 * secends) {

                    request.setSender("you");
                    request.setCmd(IMP.SYSTEM.getName());
                    request.setContent("您送鲜花太频繁," + (secends - Math.round(sub / 1000)) + "秒后再试");

                    String content = encoder.encode(request);
                    client.writeAndFlush(new TextWebSocketFrame(content));
                    return;
                }
            }

            //正常送花
            for (Channel channel : onlineUsers) {

                if (channel == client) {
                    request.setSender("you");
                    request.setContent("你给大家送了一波鲜花雨");
                    setAttrs(client, "lastFlowerTime", currTime);
                } else {
                    request.setSender(getNickName(client));
                    request.setContent(getNickName(client) + "送来一波鲜花雨");
                }
                request.setTime(sysTime());

                String content = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        }
    }

    /**
     * 获取扩展属性
     */
    private void setAttrs(Channel client, String key, Object value) {
        try {
            JSONObject json = client.attr(ATTRS).get();
            json.put(key, value);
            client.attr(ATTRS).set(json);
        } catch (Exception e) {
            JSONObject json = new JSONObject();
            json.put(key, value);
            client.attr(ATTRS).set(json);
        }
    }

    /**
     * 获取扩展属性
     */
    private JSONObject getAttrs(Channel client) {
        try {
            return client.attr(ATTRS).get();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取地址
     */
    private String getAddress(Channel client) {
        return client.remoteAddress().toString().replaceFirst("/", "");
    }

    /**
     * 获取系统时间
     */
    private Long sysTime() {
        return System.currentTimeMillis();
    }

    /**
     * 获取用户昵称
     */
    private String getNickName(Channel client) {
        return client.attr(NICK_NAME).get();
    }
}
