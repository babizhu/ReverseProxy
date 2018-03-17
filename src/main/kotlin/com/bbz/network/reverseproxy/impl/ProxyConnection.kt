package com.bbz.network.reverseproxy.impl

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInboundHandlerAdapter

abstract class ProxyConnection(protected val proxyServer: DefaultReverseProxyServer) : ChannelInboundHandlerAdapter() {
    protected lateinit var channel: Channel

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    protected fun closeOnFlush() {

        if (channel.isActive) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
    }

    fun resumeRead() {
        channel.read()
    }

}