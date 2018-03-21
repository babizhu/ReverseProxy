package com.bbz.network.reverseproxy.impl

import com.bbz.network.reverseproxy.utils.ProxyUtils
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoop
import io.netty.handler.codec.http.*
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
        super.channelRegistered(ctx)
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
        proxyToServerConnection!!.writeToServer(msg as HttpObject)

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
                disconnect()

            }
        })
    }

    fun eventloop(): EventLoop {
        return channel.eventLoop()
    }

    internal fun serverConnectionFailed(serverConnection: ProxyToServerConnection,
                                        cause: Throwable) {
        log.debug(
                "Connection to upstream server or chained proxy failed: {} ",
                serverConnection.remoteAddress,
                cause)
        writeBadGateway(cause)
    }

    private fun writeBadGateway(cause: Throwable) {
        val body = "Bad Gateway: " + currentRequest?.uri() +"<br />" + cause.message
        val response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, body)



        respondWithShortCircuitResponse(response)
    }

    private fun respondWithShortCircuitResponse(httpResponse: HttpResponse) {
        // we are sending a response to the client, so we are done handling this request
        this.currentRequest = null



        // allow short-circuit messages to close the connection. normally the Connection header would be stripped when modifying
        // the message for proxying, so save the keep-alive status before the modifications are made.
//        val isKeepAlive = HttpHeaders.isKeepAlive(httpResponse)

        // if the response is not a Bad Gateway or Gateway Timeout, modify the headers "as if" the short-circuit response were proxied
//        val statusCode = httpResponse.status.code()
//        if (statusCode != HttpResponseStatus.BAD_GATEWAY.code() && statusCode != HttpResponseStatus.GATEWAY_TIMEOUT.code()) {
//            modifyResponseHeadersToReflectProxying(httpResponse)
//        }

        // restore the keep alive status, if it was overwritten when modifying headers for proxying
//        HttpHeaders.setKeepAlive(httpResponse, isKeepAlive)

        writeToClient(httpResponse)

//        if (ProxyUtils.isLastChunk(httpResponse)) {
//            writeEmptyBuffer()
//        }

//        if (!HttpHeaders.isKeepAlive(httpResponse)) {
//            disconnect()
//            return false
//        }
//
//        return true

        disconnect()
    }


}