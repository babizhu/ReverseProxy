package com.bbz.network.reverseproxy.core

import com.bbz.network.reverseproxy.utils.SocketClientUtil
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger


class ClientToProxyConnectionTest : AbstractProxyTest() {
    val count = AtomicInteger(0)
    override fun setUp() {
        proxyServer = super.bootstrapProxy().start()
    }


    /**
     * 1、测试http请求解析失败的情况（模拟浏览器乱发请求包）
     */
    @Test
    fun decodeFailueTest() {

        var eventLoopGroup = NioEventLoopGroup()
        repeat(10000) {
            var buffer = Unpooled.copiedBuffer("反倒是 / http 1.1\r\n\r\n".toByteArray())
            connectAndWrite(buffer, eventLoopGroup)
        }
        Thread.sleep(10000000)
    }

    @Test
    fun decodeFailueTest1() {
        val socketToProxyServer = SocketClientUtil.getSocketToProxyServer(proxyServer!!)
        repeat(5) {
            SocketClientUtil.writeStringToSocket("abcddfsdfsdfsdfdsfse\r", socketToProxyServer)
        }
        val string = SocketClientUtil.readStringFromSocket(socketToProxyServer)
        println(string)

    }

    /**
     * 测试没有找到后台服务器地址的情况，考虑如下情况：
     * netty自带的http解码器一次性会解析两个部分出来：
     * 一个是http request，一个是http content（如果没有则是io.netty.handler.codec.http.LastHttpContent[empty content]）
     * 如果在获取http request之后解析backend server address失败，即使调用了close()，
     * 系统还是会调用channelRead()，这时如果没手动释放msg的话，会造成内存泄漏
     *
     *
     * 测试方法：
     * 1、删除backend_server.json配置文件中所有的后台服务器配置信息
     * 2、在Launcher.kt中设置ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID)
     * 3、java -Xms100m -Xmx128m -jar ReverseProxy-1.0-SNAPSHOT.jar >xx.log启动
     * 4、tail -f xx.log |grep leak
     * 3、ab -n 1000000  -c 100 -p 'post.txt' -T 'application/x-www-form-urlencoded' http://localhost:8000/ 进行压测
     *
     * post.txt内容：cid=4&status=1
     *
     * TODO 自动化测试
     *
     */
    @Test
    fun noBackendServerAddress() {

    }

    /**
     * 当后端服务器连接失败的时候
     * 关于内存泄漏需要考虑2种情况
     * 1、先执行channelRead()中的http content中的情况，然后再执行serverConnectionFailed()
     * 2、先执行serverConnectionFailed()，然后执行channelRead()中的http content中的情况
     *
     * 测试方法：
     * 1、修改backend_server.json配置文件内容
     * 2、第一种情况好测试，直接curl即可，第二种则需要手动发送http请求，具体如下：
     *      telnet localhost:8000
     *      POST /post HTTP/1.1
     *      Content-Length: 2
     *
     *      12
     *
     *   把最后一排的"12"延迟发送即可先触发先执行serverConnectionFailed()，再触发然后执行channelRead()方法
     *   目前触发了serverConnectionFailed()之后就直接触发channelInactive()了，没有执行到channelRead()方法
     */
    @Test
    fun connectFailed() {

    }


    /**
     * 后端服务器500错误，考虑内存泄漏
     * 测试方法：
     * 需要手动发送http请求，具体如下：
     *      telnet localhost:8000
     *      POST /post HTTP/1.1
     *      Content-Length: 2
     * 如果后端是nginx 就会产生500 错误
     */
    @Test
    fun backendServer500Error() {

    }

    private fun connectAndWrite(buffer: ByteBuf, eventLoopGroup: NioEventLoopGroup) {
        val b = Bootstrap()
        b.group(eventLoopGroup)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(object : ChannelInboundHandlerAdapter() {
                            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                                cause.printStackTrace()
                            }

                            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                                val buf = msg as ByteBuf
                                try {
                                    // println("${count.getAndIncrement()}:${buf.toString(Charset.defaultCharset())}")
                                    println("${count.getAndIncrement()}")
                                    ctx.close()
                                } finally {
                                    buf.release()
                                }
                            }
                        })
                    }
                })
        val address = proxyServer!!.getListenAddress()
        b.connect(address).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (future.isSuccess) {
                    val channel = future.channel()
                    channel.writeAndFlush(buffer)
                }
            }

        })
    }

}