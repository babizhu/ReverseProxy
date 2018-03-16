package com.bbz.network.reverseproxy.impl1

import io.netty.channel.*

@Suppress("OverridingDeprecatedMember")
class ReverseProxyBackendHandler(private val inboundChannel: Channel) : ChannelInboundHandlerAdapter() {

    init {

    }
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        inboundChannel.writeAndFlush(msg).addListener(ChannelFutureListener { it: ChannelFuture ->
            if (it.isSuccess) {
                ctx.channel().read()
            } else {
                it.cause().printStackTrace()
                it.channel().close()

            }
        })
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        ReverseProxyFrontendHandler.closeOnFlush(inboundChannel)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ReverseProxyFrontendHandler.closeOnFlush(ctx.channel())
    }
}