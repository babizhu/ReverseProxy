package com.bbz.network.reverseproxy.impl1

import com.bbz.network.reverseproxy.impl.ConnectionState
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.timeout.IdleStateHandler

class ReverseProxyFrontendHandler(private val remoteHost: String,
                                  private val remotePort: Int) : ChannelInboundHandlerAdapter() {

    /**
     * 远程服务器的连接状态
     */
    private var connectionState = ConnectionState.DISCONNECTED
    // As we use inboundChannel.eventLoop() when building the Bootstrap this does not need to be volatile as
    // the outboundChannel will use the same EventLoop (and therefore Thread) as the inboundChannel.
    private  var outboundChannel: Channel? = null
    private lateinit var currentRequest: HttpRequest
    private lateinit var waitHttpContent: HttpObject

    companion object {
        /**
         * Closes the specified channel after all queued write requests are flushed.
         */
        fun closeOnFlush(ch: Channel) {
            if (ch.isActive) {
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
        }
    }


    override fun channelActive(ctx: ChannelHandlerContext) {

        val inboundChannel = ctx.channel()
        inboundChannel.read()
//
//        // Start the connection attempt.
//        val b = Bootstrap()
//        b.group(inboundChannel.eventLoop())
//                .channel(ctx.channel()::class.java)
//                .handler(object : ChannelInitializer<Channel>() {
//                    @Throws(Exception::class)
//                    override fun initChannel(ch: Channel) {
//                        ch.pipeline().addLast("codec", HttpClientCodec())
//                        ch.pipeline().addLast(
//                                "idle",
//                                IdleStateHandler(0, 0, 70))
//
//                        ch.pipeline().addLast("handler",ReverseProxyBackendHandler(inboundChannel))
//                    }
//                })
//                .option(ChannelOption.AUTO_READ, false)
//        val f = b.connect(remoteHost, remotePort)
//
//        outboundChannel = f.channel()
//        f.addListener({
//            if (it.isSuccess) {
//                // connection complete start to read first data
//                inboundChannel.read()
//            } else {
//                // Close the connection if the connection attempt has failed.
//                inboundChannel.close()
//            }
//
//        })
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            this.currentRequest = msg
        }
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                connectRemoteServer(ctx)
            }
            ConnectionState.CONNECTING -> {
                waitHttpContent = msg as HttpObject
            }
            ConnectionState.AWAITING_INITIAL -> {
                if (outboundChannel!!.isActive) {

                    outboundChannel!!.writeAndFlush(msg).addListener(ChannelFutureListener { it: ChannelFuture ->
                        if (it.isSuccess) {
                            // was able to flush out data, start to read the next chunk
                            ctx.channel().read()
                        } else {
                            it.cause().printStackTrace()
                            it.channel().close()
                        }

                    })
                }
            }
        }

    }

    private fun connectRemoteServer(ctx: ChannelHandlerContext) {

        val inboundChannel = ctx.channel()


        // Start the connection attempt.
        val b = Bootstrap()
        b.group(inboundChannel.eventLoop())
                .channel(ctx.channel()::class.java)
                .handler(object : ChannelInitializer<Channel>() {
                    @Throws(Exception::class)
                    override fun initChannel(ch: Channel) {
                        ch.pipeline().addLast("codec", HttpClientCodec())
                        ch.pipeline().addLast(
                                "idle",
                                IdleStateHandler(0, 0, 70))

                        ch.pipeline().addLast("handler", ReverseProxyBackendHandler(inboundChannel))
                    }
                })
                .option(ChannelOption.AUTO_READ, false)
        val f = b.connect(remoteHost, remotePort)
        connectionState = ConnectionState.CONNECTING

        outboundChannel = f.channel()
        f.addListener({
            if (it.isSuccess) {
                connectionState = ConnectionState.AWAITING_INITIAL
                outboundChannel!!.write(currentRequest)
                outboundChannel!!.writeAndFlush(waitHttpContent).addListener {
                    println("初始化的两个请求writeAndFlush 完毕")
                    inboundChannel.read()

                }
                // connection complete start to read first data
            } else {
                // Close the connection if the connection attempt has failed.
                inboundChannel.close()
            }

        })
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel!!)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        closeOnFlush(ctx.channel())
    }


}