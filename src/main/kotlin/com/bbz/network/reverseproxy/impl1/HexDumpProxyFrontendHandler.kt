package com.bbz.network.reverseproxy.impl1

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*

class HexDumpProxyFrontendHandler(private val remoteHost: String,
                                  private val remotePort: Int) : ChannelInboundHandlerAdapter() {

    // As we use inboundChannel.eventLoop() when building the Bootstrap this does not need to be volatile as
    // the outboundChannel will use the same EventLoop (and therefore Thread) as the inboundChannel.
    private lateinit var outboundChannel: Channel

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

        // Start the connection attempt.
        val b = Bootstrap()
        b.group(inboundChannel.eventLoop())
                .channel(ctx.channel()::class.java)
                .handler(HexDumpProxyBackendHandler(inboundChannel))
                .option(ChannelOption.AUTO_READ, false)
        val f = b.connect(remoteHost, remotePort)
        outboundChannel = f.channel()
        f.addListener({


            if (it.isSuccess) {
                // connection complete start to read first data
                println("连接成功！！！")
                inboundChannel.read()
            } else {
                // Close the connection if the connection attempt has failed.
                inboundChannel.close()
            }

        })
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (outboundChannel.isActive) {
            outboundChannel.writeAndFlush(msg).addListener({
                    if (it.isSuccess) {
                        // was able to flush out data, start to read the next chunk
                        ctx.channel().read()
                    } else {
                        ctx.channel().close()
                    }

            })
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        closeOnFlush(ctx.channel())
    }


}