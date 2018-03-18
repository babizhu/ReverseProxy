package com.bbz.network.reverseproxy.impl

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoop
import io.netty.handler.codec.http.HttpRequest
import org.slf4j.LoggerFactory

class ClientToProxyConnection(proxyServer: DefaultReverseProxyServer) : ProxyConnection(proxyServer) {

    // As we use inboundChannel.eventLoop() when building the Bootstrap this does not need to be volatile as
    // the outboundChannel will use the same EventLoop (and therefore Thread) as the inboundChannel.

    private var proxyToServerConnection: ProxyToServerConnection? = null
    private var currentRequest: HttpRequest? = null

    companion object {
        private val log = LoggerFactory.getLogger(ClientToProxyConnection::class.java)


    }

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        this.channel = ctx.channel()
        proxyServer.registerChannel(channel)

    }

    override fun channelActive(ctx: ChannelHandlerContext) {
//        var autoRead = channel.config().isAutoRead
//
//        channel.read()
//        autoRead = channel.config().isAutoRead
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            this.currentRequest = msg
            channel.config().isAutoRead = false
            log.debug(msg.uri())
        } else {
            log.debug(msg.toString())
        }
        if (proxyToServerConnection == null) {
            proxyToServerConnection = ProxyToServerConnection(proxyServer, this)
        }
        proxyToServerConnection!!.writeToServer(msg)

    }


    override fun channelInactive(ctx: ChannelHandlerContext) {
        log.debug("{} channelInactive", ctx.channel())
        proxyToServerConnection?.disconnect()
    }

    @Suppress("OverridingDeprecatedMember")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        disconnect()

    }


    fun writeToClient(msg: Any) {
        channel.writeAndFlush(msg).addListener(ChannelFutureListener { it: ChannelFuture ->
            if (it.isSuccess) {
                proxyToServerConnection!!.resumeRead()
            } else {
                it.cause().printStackTrace()
                it.channel().close()

            }
        })
    }

    fun eventloop(): EventLoop {
        return channel.eventLoop()
    }

}