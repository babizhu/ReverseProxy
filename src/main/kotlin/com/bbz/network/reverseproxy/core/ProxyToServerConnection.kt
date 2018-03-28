package com.bbz.network.reverseproxy.core

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.concurrent.Future
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

@Suppress("OverridingDeprecatedMember")
class ProxyToServerConnection(proxyServer: DefaultReverseProxyServer,
                              private val clientToProxyConnection: ClientToProxyConnection)
    : ProxyConnection(proxyServer) {

    internal lateinit var remoteAddress: InetSocketAddress

    companion object {
        private val log = LoggerFactory.getLogger(ProxyToServerConnection::class.java)
    }

    /**
     * 远程服务器的连接状态
     */
    private var remoteConnectionState = ConnectionState.DISCONNECTED

    private lateinit var currentRequest: HttpRequest

    /**
     * 暂存随着HttpRequest一起解析出来的HttpContent，目前来看有且仅有一个
     */
    private var waitToWriteHttpContent: HttpContent? = null


    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
//        throw Exception("avcd")
        clientToProxyConnection.writeToClient(msg)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        clientToProxyConnection.disconnect()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        clientToProxyConnection.disconnect()
    }


    private fun connectAndWrite() {
        val b = Bootstrap()
        b.group(clientToProxyConnection.eventloop())
                .channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("codec", HttpClientCodec())
                        ch.pipeline().addLast(
                                "idle",
                                IdleStateHandler(0, 0, proxyServer.getIdleConnectionTimeout()))

                        ch.pipeline().addLast("handler", this@ProxyToServerConnection)
                    }
                })

        remoteConnectionState = ConnectionState.CONNECTING

        val writeHandler = fun(it: Future<in Void>) {
            if (it.isSuccess) {
                clientToProxyConnection.resumeRead()
            } else {
                exceptionOccur(it.cause())
            }
        }

        b.connect(remoteAddress).addListener({
            if (it.isSuccess) {
                remoteConnectionState = ConnectionState.AWAITING_INITIAL
                if (waitToWriteHttpContent == null) {
                    channel.writeAndFlush(currentRequest).addListener(writeHandler)
                } else {
                    channel.write(currentRequest)
                    channel.writeAndFlush(waitToWriteHttpContent).addListener(writeHandler)

                }
            } else {
                clientToProxyConnection.serverConnectionFailed(this, it.cause())
                releaseHttpContent(waitToWriteHttpContent)
            }
        })
    }


    fun writeToServer(msg: HttpObject,clientCtx:ChannelHandlerContext) {
        when (remoteConnectionState) {
            ConnectionState.AWAITING_INITIAL -> {
                if (channel.isActive) {
                    channel.writeAndFlush(msg).addListener(ChannelFutureListener { it: ChannelFuture ->
                        if (it.isSuccess) {
                            // was able to flush out data, start to read the next chunk
                            clientToProxyConnection.resumeRead()
                        } else {
                            exceptionOccur(it.cause())
                            disconnect()
                        }
                    })
                } else {
                    log.error("连接断了!!!!!!!!!!!!!!!!!!!!!!!!!!")
                }
            }
            ConnectionState.DISCONNECTED -> {
                remoteAddress = calcRemoteAddress(msg as HttpRequest,clientCtx)
                this.currentRequest = msg
                connectAndWrite()
            }
            ConnectionState.CONNECTING -> {
                waitToWriteHttpContent = msg as HttpContent
            }

            else -> {
                log.error("奇怪的状态啊{}", remoteConnectionState)
            }
        }

    }

    /**
     * 根据request计算应该连接哪个远程服务器，未来重点扩充地点
     */
    private fun calcRemoteAddress(currentRequest: HttpRequest,clientCtx: ChannelHandlerContext): InetSocketAddress {

        return proxyServer.getRoutePolice().getUrl(currentRequest,clientCtx)
    }


}