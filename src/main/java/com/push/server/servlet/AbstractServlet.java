package com.push.server.servlet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.io.RandomAccessFile;

/**
 * 抽象的Servlet
 *
 * @author linyh
 */
@Slf4j
public abstract class AbstractServlet implements IServlet {

    volatile static boolean successInit;

    @Override
    public void service(FullHttpRequest request, HttpResponse response, ChannelHandlerContext ctx) {
        if (successInit) {

            try {
                doGet(request, response, ctx);
            } catch (Exception e) {
                log.info("服务端处理数据时发送了异常: [{}]", e.getMessage());
                response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }

        } else {
            log.info("没有成功初始化servlet，不进行处理");
        }
    }

    protected void responseByFileContent(String path, HttpResponse response, ChannelHandlerContext ctx) throws Exception {

        // 将html文件中的html内容写入response
        RandomAccessFile file = new RandomAccessFile(ServletUtils.getResource(path, ServletUtils.WEB_INF), "r");

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());

        // 写出之前构造好的response
        ctx.write(response);

        // 将文件写出
        ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));

        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        // 请求头的 keep-alive 信息
        //boolean isKeepAlive = HttpUtil.isKeepAlive(request);

//        if (!isKeepAlive) {
//            future.addListener(ChannelFutureListener.CLOSE);
//        }
    }

    /**
     * 具体的业务逻辑由子类实现
     *
     * @param request  请求
     * @param response 响应
     * @param ctx      假冒伪劣的Servlet规范,为了方便在这里传一个channel的上下文,方便写数据
     * @throws Exception 大概率是没有找到文件的异常 -> fileNotFoundEx
     */
    protected abstract void doGet(FullHttpRequest request, HttpResponse response, ChannelHandlerContext ctx) throws Exception;
}
