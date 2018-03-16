package com.bbz.network.reverseproxy.impl

import io.netty.channel.*

@Suppress("OverridingDeprecatedMember")
class ProxyToServerConnection(proxyServer: DefaultReverseProxyServer,
                              private val inboundChannel: Channel) : ProxyConnection(proxyServer) {
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
        ClientToProxyConnection.closeOnFlush(inboundChannel)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ClientToProxyConnection.closeOnFlush(ctx.channel())
    }
}