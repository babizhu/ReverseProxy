package com.bbz.network.reverseproxy.impl

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.timeout.IdleStateHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

@Suppress("OverridingDeprecatedMember")
class ProxyToServerConnection(proxyServer: DefaultReverseProxyServer,
                              private val clientToProxyConnection: ClientToProxyConnection) : ProxyConnection(proxyServer) {

    init {

    }

    companion object {
        private val log = LoggerFactory.getLogger(ProxyToServerConnection::class.java)

    }

    /**
     * 远程服务器的连接状态
     */
    private var remoteConnectionState = ConnectionState.DISCONNECTED

    private lateinit var currentRequest: HttpRequest
    private lateinit var waitHttpContent: HttpObject
    //    override fun channelRegistered(ctx: ChannelHandlerContext) {
//        this.channel = ctx.channel()
//    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        clientToProxyConnection.writeToClient(msg)

    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        clientToProxyConnection.close()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        clientToProxyConnection.close()

    }

    private fun connectAndWrite(remoteAddress: InetSocketAddress) {
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
//                .option(ChannelOption.AUTO_READ, false)

        val f = b.connect(remoteAddress)
        remoteConnectionState = ConnectionState.CONNECTING

        channel = f.channel()
        f.addListener({
            if (it.isSuccess) {
                remoteConnectionState = ConnectionState.AWAITING_INITIAL
                channel.write(currentRequest)
                channel.writeAndFlush(waitHttpContent).addListener {
                    clientToProxyConnection.resumeRead()
                }
                // connection complete start to read first data
            } else {
                // Close the connection if the connection attempt has failed.
                it.cause().printStackTrace()
                log.error(it.cause().localizedMessage)
                clientToProxyConnection.close()
            }

        })
    }

    fun writeToServer(msg: Any) {
        when (remoteConnectionState) {
            ConnectionState.AWAITING_INITIAL -> {
                if (channel.isActive) {

                    channel.writeAndFlush(msg).addListener(ChannelFutureListener { it: ChannelFuture ->
                        if (it.isSuccess) {
                            // was able to flush out data, start to read the next chunk
//                            ctx.channel().read()
                            clientToProxyConnection.resumeRead()
                        } else {
                            it.cause().printStackTrace()
                            it.channel().close()
                        }

                    })
                } else {
                    log.error("连接断了")
                }
            }
            ConnectionState.DISCONNECTED -> {
                val remoteAddress = getRemoteAddress(msg as HttpRequest)
                this.currentRequest = msg
                connectAndWrite(remoteAddress)
            }
            ConnectionState.CONNECTING -> {
                waitHttpContent = msg as HttpObject

            }

            else -> {
                log.error("奇怪的状态啊{}", remoteConnectionState)
            }
        }

    }

    /**
     * 根据request计算应该连接哪个远程服务器，未来重点扩充地点
     */
    private fun getRemoteAddress(currentRequest: HttpRequest): InetSocketAddress {
        return InetSocketAddress(8080)
    }

    fun close() {
        closeOnFlush()
    }


}