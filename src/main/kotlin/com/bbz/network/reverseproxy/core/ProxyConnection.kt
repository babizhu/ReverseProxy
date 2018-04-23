package com.bbz.network.reverseproxy.core

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleStateEvent
import io.netty.util.ReferenceCountUtil
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.RejectedExecutionException

abstract class ProxyConnection(protected val proxyServer: DefaultReverseProxyServer) : ChannelInboundHandlerAdapter() {
    protected lateinit var channel: Channel

    companion object {
        private val log = LoggerFactory.getLogger(ProxyConnection::class.java)
    }

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        this.channel = ctx.channel()
        proxyServer.registerChannel(channel)
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    private fun closeOnFlush() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    fun resumeRead() {
        channel.read()
    }

    /**
     * This method is called when the underlying [Channel] times out due
     * to an idle timeout.
     */
    private fun timedOut() {
        log.debug("{} timeout", channel)
        disconnect()
    }

    open fun disconnect() {
        if (channel.isActive) {
            closeOnFlush()
        }
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

    protected open fun exceptionOccur(cause: Throwable) {
        when (cause) {
            is IOException -> {
                // IOExceptions are expected errors, for example when a server drops the connection. rather than flood
                // the logs with stack traces for these expected exceptions, log the message at the INFO level and the
                // stack trace at the DEBUG level.
                log.info("An IOException occurred on {}: {}", this::class.java.name, cause.message)
            }
            is RejectedExecutionException -> {
                log.info("An executor rejected a read or write operation on the " + this::class.java.name + " (this is normal if the proxy is shutting down). Message: ", cause.message)
                log.debug("A RejectedExecutionException occurred on " + this::class.java.name, cause)
            }
            else -> log.error("Caught an exception on {} : {}" , this::class.java.name, cause)
        }
        disconnect()
    }

    protected fun releaseHttpContent(msg: Any) {
        ReferenceCountUtil.release(msg)
    }

    @Suppress("unused")
    protected fun stopAutoReading() {
        log.debug("Stopped reading")
        this.channel.config().isAutoRead = false
    }

//    /**
//     * Call this to resume reading.
//     */
//    protected fun resumeReading() {
//        log.debug("Resumed reading")
//        this.channel.config().isAutoRead = true
//    }
}