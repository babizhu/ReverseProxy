package com.bbz.network.reverseproxy.impl

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleStateEvent
import org.slf4j.LoggerFactory

abstract class ProxyConnection(protected val proxyServer: DefaultReverseProxyServer) : ChannelInboundHandlerAdapter() {
    protected lateinit var channel: Channel
    companion object {
        private val log = LoggerFactory.getLogger(ProxyConnection::class.java)


    }
    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    private fun closeOnFlush() {

        if (channel.isActive) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
    }

    fun resumeRead() {
        channel.read()
    }

    /**
     * This method is called when the underlying [Channel] times out due
     * to an idle timeout.
     */
    private fun timedOut() {
        log.debug("{} timeout",channel)
        disconnect()
    }

    fun disconnect() {
        closeOnFlush()
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        try {
            if (evt is IdleStateEvent) {

                timedOut()
            }
        } finally {
            super.userEventTriggered(ctx, evt)
        }
    }

}