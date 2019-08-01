package com.push.server;

import com.push.server.handler.HttpRequestHandler;
import com.push.server.handler.WebSocketChatHandler;
import com.push.server.handler.WebSocketOrderHandler;
import com.push.server.servlet.ServletUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务器启动类
 *
 * @author linyh
 */
@Slf4j
public class CommunicationServer {

    private static final int DEFAULT_PORT = 8888;

    public static void start(int port) {

        EventLoopGroup bossEventLoop = new NioEventLoopGroup(1);
        EventLoopGroup workerEventLoop = new NioEventLoopGroup();

        try {
            // 初始化servlet的映射关系
            ServletUtils.init();

            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossEventLoop, workerEventLoop)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();

                            // http协议编解码器
                            pipeline.addLast(new HttpServerCodec());
                            // 聚合http请求对象 -> FullHttpRequest
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024, true));
                            //pipeline.addLast(new ChunkedWriteHandler());
                            // 自定义的Http处理
                            pipeline.addLast(new HttpRequestHandler());

                            // 这个handler在新连接接入时,pipeline添加handler后添加一个握手handler
                            // 若uri为/websocket,自动升级协议为websocket
                            // 并且在握手成功后会新增WebSocket的编解码器,并移除HttpObjectAggregator这个handler
                            pipeline.addLast(new WebSocketServerProtocolHandler("/Websocket"));
                            // 自定义的订单类型的WebSocket处理
                            pipeline.addLast(new WebSocketOrderHandler());
                            // 自定义的聊天类型的WebSocket处理
                            pipeline.addLast(new WebSocketChatHandler());

                        }
                    });

            ChannelFuture channelFuture = bootstrap.bind(port);
            log.info("服务已启动,监听端口: " + port);
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.info("线程被中断: [{}]", e.getMessage());
        } catch (Exception e) {
            log.info("服务器异常");
            e.printStackTrace();
        } finally {
            bossEventLoop.shutdownGracefully();
            workerEventLoop.shutdownGracefully();
        }
    }

    public static void main(String[] args) {

        try {
            if (args == null || args.length == 0) {
                CommunicationServer.start(DEFAULT_PORT);
            } else {
                CommunicationServer.start(Integer.parseInt(args[0]));
            }
        } catch (Exception e) {
            System.out.println("发生了错误,启动服务器失败: " + e.getMessage());
        }
    }
}
