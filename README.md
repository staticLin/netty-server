# netty-server
Netty server demo include WebSocket 、HTTP、IMP protocols

# 本人博客 https://blog.csdn.net/qq_41737716

# Netty大纲

> 如果只是希望学习如何使用Netty，或是想自己整一个有趣的网络通信服务端，可以略过第二部分的源码分析
>
> 如果希望了解源码，你必须了解JDK的NIO的相关知识，并且有一定的Netty使用经验，本系列不会科普BIO、NIO

 - 实现一个Netty服务端
   - [Netty实现一个小应用服务器 +消息收发推送系统](https://blog.csdn.net/qq_41737716/article/details/94553782)
 - Netty源码分析
 	- [服务端启动代码分析](https://blog.csdn.net/qq_41737716/article/details/94592342)
	- [Reactor线程模型源码分析](https://blog.csdn.net/qq_41737716/article/details/94664552)
	 - [pipeline事件传播机制源码分析](https://blog.csdn.net/qq_41737716/article/details/94734196)
	 - [Netty解码器源码分析](https://blog.csdn.net/qq_41737716/article/details/94771255)
	 - [Netty之基于长度域的动态长度解码](https://blog.csdn.net/qq_41737716/article/details/94892014)
# 什么是Netty？

在Netty官网有这么一句话：

> Netty is *an asynchronous event-driven network application framework* 
> for rapid development of maintainable high performance protocol servers & clients.

可以知道，Netty是一个**异步**、**事件驱动**的网络应用框架：

+ 异步指的是，外部线程调用write方法将数据写到channel，或者execute定时任务的时候会马上返回，通过回调Future 的方式获得调用结果，这个过程是完全异步的
+ 事件驱动指的是，Netty有一个Reactor线程组，在不断重复做一些操作，例如selector轮询事件，发现是否有channel的读、写、连接事件，然后驱动线程去对不同事件作出不同的驱动行为

NIO这种io模型属于非阻塞的io模型，其使用select/poll机制可以实现多路复用，在JDK中实现了NIO，但是不好用，主要体现在下面几点：

+ api繁琐：一个简单的来回通信，JDK的nio可能需要100行，但如果使用Netty实现nio操作，一般只需要20行左右即可
+ 有空轮询bug：nio的空轮询bug会重复selector空轮询直到CPU使用率飙升100%，虽然至今JDK声称修复了此bug，但听说好像还是偶尔会存在
+ 学习成本高：想要通过JDK的nio的API自己实现网络通信的学习成本是非常高的，首先必须要熟悉底层传输数据的数据结构ByteBuffer，其次自己处理selector的轮询事件处理
+ 可扩展性、维护性差：Netty通过Handler责任链模式可以让用户灵活的定义解码编码过程，在定制协议这一块用户是可以自由组装，所以很多自定义协议如dubbo、rocketMQ等等才能如此盛行。协议又和业务逻辑分层，然后又和底层nio读写操作分层，可维护性极高，用户一般可以灵活自由组装操作，并不需要改动太多代码。反观JDK底层nio却是做不到

基于以上若干缺点(或许还有更多)，Netty应运而生，其不仅封装了JDK底层的nio操作，还设计了reactor线程模型，其为高并发高性能打下基础。使用Netty自己开发一个高性能的网络通信服务端是非常容易的。

# NIO与BIO的对比

## BIO的劣势

为什么在高并发多连接的情况下一定要用NIO呢？
众所周知BIO这个IO模型是会阻塞的，其表现在socket.accpet，服务端在接受一个新连接的时候会进行阻塞，直到一个连接到达，此时必须启动一个新线程去处理这个新连接socket的读写，并且是一个新连接对应一个线程，为什么要这样呢？因为在读socket的inputStream时线程会阻塞，直到数据发送完毕才能开始读。
假如一个线程负责接受新连接，另一个线程负责连接的读写，不启动更多线程的话，A连接的读取过程中，B连接必须等待A连接的数据传输完成，如果还有C连接D连接，统统都要等待A连接的操作阻塞完成。那么启动一个新线程或许可以解决这个问题，A连接线程在阻塞，我可以有一个新线程去处理B连接，B连接就不需要等待A连接的操作完成了，但这样又有一个问题，也就是一个连接对应一个线程，假设有10000个客户端连接，就启动10000个线程？此时上下文切换带来的开销是非常巨大的，并且线程是操作系统宝贵的资源，服务端是承受不住这种请求的。那么会有人说，如果使用线程池的方式控制读写线程呢？确实可以避免过多的线程创建和销毁的开销，上下文切换开销也会减少一些，但如果假设线程池容量大小为100，那么也就是说我现在有100个连接同时进来，我后面来的连接还是要等待的，虽然控制住了性能，但对用户的体验并不好，很可能连接你的服务端，原本10ms的响应由于晚进来必须多等待1s。综上所述BIO这个IO模型如果只是用在几百几千的连接可能还能凑合着用一下，但如果是几千几万以上的连接数，BIO这个IO模型一定顶不住。

## NIO的优势

那么，NIO这个IO模型又是如何解决上述BIO的劣势呢？众所周知NIO是同步非阻塞的IO模型，网络上关于同步、非阻塞的介绍有很多种，这里说一下我对同步和非阻塞的理解：

+ 同步：selector多路复用这个IO模型，可以实现一个线程处理大量连接的读写连接的请求，但selector是反复轮询channel（可以视作socket的抽象），也就是说，我询问Achannel是否有新事件（读、写、连接）的时候B、C、Dchannel是会等待的，selector一个个去轮询所有连接的事件这一过程，被称为同步
+ 非阻塞：在selector轮询channel事件时，如果是没有事件的话是可以立即返回结果的，这个过程不会阻塞，所以多路复用IO模型由此而来

# 为什么要学习Netty？

前段时间研究了Dubbo，发现Dubbo中的RPC通信使用了Netty框架来实现，并且有自己的dubbo协议，不仅如此，还支持各种其他的协议。由于其轻量的协议与Nio的网络通信技术，使得其RPC通信性能很高。不仅是分布式通信领域，在网络游戏方面（Java）Netty也是被广泛运用。强烈的好奇心促使我想了解Netty这个高性能的io框架。
