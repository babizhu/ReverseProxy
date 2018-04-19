package com.bbz.network.reverseproxy.core

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.timeout.IdleStateHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

@Suppress("OverridingDeprecatedMember")
class ProxyToServerConnection(proxyServer: DefaultReverseProxyServer,
                              private val clientToProxyConnection: ClientToProxyConnection,
                              private val backendServerAddress: InetSocketAddress)
    : ProxyConnection(proxyServer) {

//    internal lateinit var remoteAddress: InetSocketAddress

    companion object {
        private val log = LoggerFactory.getLogger(ProxyToServerConnection::class.java)
    }

    init {
        connect()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
//        proxyServer.httpFilter?.let {
//            val response = it.proxyToClientResponse(msg as HttpObject)
//            response?.let {
//                clientToProxyConnection.writeToClient(it)
//                disconnect()
//                releaseHttpContent(msg)//这里有很大问题
//                return
//            }
//        }

        ctx.channel().config().isAutoRead = false
        clientToProxyConnection.writeToClient(msg)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        log.debug("{} channelInactive", ctx.channel())
        clientToProxyConnection.disconnect()
    }


    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        clientToProxyConnection.disconnect()
    }


    private fun connect() {
        val b = Bootstrap()
        b.group(clientToProxyConnection.eventloop())
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, proxyServer.getConnectTimeoutMs())
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("codec", HttpClientCodec())
                        ch.pipeline().addLast(
                                "idle",
                                IdleStateHandler(0, 0, proxyServer.getIdleConnectionTimeout()))

                        ch.pipeline().addLast("handler", this@ProxyToServerConnection)
                    }
                })
        b.connect(backendServerAddress).addListener({
            if (it.isSuccess) {
                clientToProxyConnection.serverConnectionSucceeded()
            } else {
                clientToProxyConnection.serverConnectionFailed(it.cause())
            }
        })
    }

    fun writeToServer(msg: HttpObject) {

//        if (channel.isActive) {
        channel.writeAndFlush(msg).addListener({
            if (it.isSuccess) {
                // was able to flush out data, start to read the next chunk
                clientToProxyConnection.resumeRead()
            } else {
                exceptionOccur(it.cause())
            }
        })
    }
}