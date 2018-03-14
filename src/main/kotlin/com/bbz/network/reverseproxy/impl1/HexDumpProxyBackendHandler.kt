package com.bbz.network.reverseproxy.impl1

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class HexDumpProxyBackendHandler(private val inboundChannel: Channel) : ChannelInboundHandlerAdapter() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        inboundChannel.writeAndFlush(msg).addListener({

            if (it.isSuccess) {
                ctx.channel().read()
            } else {
                ctx.channel().close()

            }
        })
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        HexDumpProxyFrontendHandler.closeOnFlush(inboundChannel)
    }

    @Override
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        HexDumpProxyFrontendHandler.closeOnFlush(ctx.channel())
    }
}