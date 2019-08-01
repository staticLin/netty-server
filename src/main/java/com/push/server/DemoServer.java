package com.push.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 仅供个人测试的用例
 *
 * @author linyh
 */
public class DemoServer {

    private static final int DEFAULT_PORT = 8888;

    public static void start(int port) {

        EventLoopGroup bossEventLoop = new NioEventLoopGroup(1);
        EventLoopGroup workerEventLoop = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossEventLoop, workerEventLoop)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加某个Handler
                            pipeline.addLast(null);
                        }
                    });


            // 绑定端口号
            ChannelFuture channelFuture = bootstrap.bind(port);
            // 阻塞在此，直到被关闭
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 优雅关闭资源
            bossEventLoop.shutdownGracefully();
            workerEventLoop.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        CommunicationServer.start(Integer.parseInt(args[0]));
    }
}
