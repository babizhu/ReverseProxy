package com.bbz.network.reverseproxy.core

import com.bbz.network.reverseproxy.core.misc.ConnectionState
import com.bbz.network.reverseproxy.core.misc.ErrorCode
import com.bbz.network.reverseproxy.utils.ProxyUtils
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoop
import io.netty.handler.codec.http.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class ClientToProxyConnection(proxyServer: DefaultReverseProxyServer) : ProxyConnection(proxyServer) {

    /**
     * 当前的状态
     */
    private var state = ConnectionState.BACKEND_SERVER_DISCONNECTED

    private var proxyToServerConnection: ProxyToServerConnection? = null
    private var currentRequest: HttpRequest? = null
    private var waitToWriteHttpContent: HttpContent? = null
    private var backendServerAddress: InetSocketAddress? = null

    companion object {
        private val log = LoggerFactory.getLogger(ClientToProxyConnection::class.java)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        proxyServer.httpFilter?.let {
            val response = it.clientToProxyConnected(ctx)
            response?.let {
                respondWithShortCircuitResponse(response)
            }
        }
    }

    private fun connectToBackendServer(request: HttpRequest) {
        backendServerAddress = getBackendServerAddress(request)
        if (backendServerAddress != null) {
            proxyToServerConnection = ProxyToServerConnection(proxyServer, this, backendServerAddress!!)
            this.currentRequest = request
        } else {
            writeBadGateway(ErrorCode.BACKEND_SERVER_NOT_FOUND)
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val httpObject = msg as HttpObject
        proxyServer.httpFilter?.let {
            val response = it.clientToProxyRequest(httpObject)
            response?.let {
                respondWithShortCircuitResponse(response)
                return
            }
        }


        when (state) {
            ConnectionState.ESTABLISHED -> {
                proxyToServerConnection!!.writeToServer(httpObject)
                if (msg is HttpContent) {
                    if (msg.content().refCnt() != 0 && msg.content() != Unpooled.EMPTY_BUFFER) {
                        throw Exception("{} refCnt() != 0")
                    }
                }
            }
//            ConnectionState.DISCONNECT_REQUESTED -> {
//                /**
//                 * netty自带的http解码器一次性会解析两个部分出来：
//                 * 一个是http request，一个是http content（如果没有则是empty content）
//                 * 如果在获取http request之后解析backend server address失败，即使调用了close()，
//                 * 系统还是会调用channelRead()，这里不手动释放msg的话，会造成内存泄漏
//                 */
//                releaseHttpContent(msg)
//            }
            ConnectionState.BACKEND_SERVER_DISCONNECTED -> {
                when (msg) {
                    is HttpRequest -> {
                        if (msg.decoderResult().isFailure) {
                            writeBadGateway(ErrorCode.DECODE_FAILURE)
                            return
                        }
                        readHttpRequestInit(msg)
                    }
                    is HttpContent -> this.waitToWriteHttpContent = msg
                    else -> log.error("为什么达到这个状态？msg = {}", msg)
                }
            }
            else -> log.error("为什么达到这个状态？msg = {}", msg)
        }
    }

    /**
     * 客户端连接建立后，第一次收到Http Request请求，后面再收到的Http Request请求不会执行到这里
     * 建立和backend server到连接
     */
    private fun readHttpRequestInit(msg: HttpRequest) {
        channel.config().isAutoRead = false
        connectToBackendServer(msg)
    }

    override fun disconnect() {
        log.debug("disconnect:{}", channel)
        super.disconnect()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        log.debug("{} channelInactive", ctx.channel())
        waitToWriteHttpContent?.let {
            val byteBuf = it.content()
            if (byteBuf.refCnt() != 0 && byteBuf != Unpooled.EMPTY_BUFFER) {
                releaseHttpContent(it)
            }
        }
        proxyToServerConnection?.disconnect()
    }

    @Suppress("OverridingDeprecatedMember")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        exceptionOccurAndDisconnect(cause)
    }


    fun writeToClient(msg: Any) {

//        channel.writeAndFlush(msg)//
        channel.writeAndFlush(msg).addListener({
            if (it.isSuccess) {
                proxyToServerConnection!!.resumeRead()
            } else {
                exceptionOccurAndDisconnect(it.cause())
            }
        })
    }

    fun eventloop(): EventLoop {
        return channel.eventLoop()
    }

    /**
     * 服务器连接失败
     */
    internal fun serverConnectionFailed(cause: Throwable) {
        log.debug(
                "Connection to upstream server or chained proxy failed: {} ",
                backendServerAddress,
                cause)
        writeBadGateway(cause)
    }

    /**
     * 服务器连接成功
     */
    internal fun serverConnectionSucceeded() {
        log.debug("Connection to upstream server success: {}", backendServerAddress)
        state = ConnectionState.ESTABLISHED
        proxyToServerConnection!!.writeToServer(currentRequest!!)
        waitToWriteHttpContent?.let {
            proxyToServerConnection!!.writeToServer(it)
            if (it.content().refCnt() != 0 && it.content() != Unpooled.EMPTY_BUFFER) {
                throw Exception("{} refCnt() != 0")
            }
        }

    }

    private fun writeBadGateway(errorCode: ErrorCode) {

        log.error("writeBadGateway failed {}", errorCode)

        val body = "Bad Gateway: " + currentRequest?.uri() + "<br />" + errorCode
        val response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.BAD_GATEWAY, body)
        respondWithShortCircuitResponse(response)
    }

    private fun writeBadGateway(cause: Throwable) {
        val body = "Bad Gateway: " + currentRequest?.uri() + "<br />" + cause.message
        val response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.BAD_GATEWAY, body)
        respondWithShortCircuitResponse(response)
    }

    /**
     * 直接响应客户端，通常是报500错，完成之后主动关闭连接
     */
    private fun respondWithShortCircuitResponse(httpResponse: HttpResponse) {
        // we are sending a response to the client, so we are done handling this request
        this.currentRequest = null
//        state = ConnectionState.DISCONNECT_REQUESTED


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
//        if (!HttpUtil.isKeepAlive(httpResponse)) {///要注意各种情况下的内存泄漏
//            disconnect()
//        }

        disconnect()

    }

    /**
     * 根据request计算应该连接哪个远程服务器
     */
    private fun getBackendServerAddress(currentRequest: HttpRequest): InetSocketAddress? {

        return proxyServer.getRoutePolice().getBackendServerAddress(currentRequest, channel)
    }


}