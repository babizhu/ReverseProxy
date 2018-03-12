package com.bbz.network.reverseproxy.impl

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.http.*
import io.netty.handler.timeout.IdleStateHandler
import io.netty.handler.traffic.GlobalTrafficShapingHandler
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger

open class ClientToProxyConnection(proxyServer: DefaultReverseProxyServer,
                                   pipeline: ChannelPipeline,
                                   private val globalTrafficShapingHandler: GlobalTrafficShapingHandler?)
    : ProxyConnection<HttpRequest>(proxyServer, ConnectionState.AWAITING_INITIAL) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ClientToProxyConnection::class.java.name)
    }

    /**
     * Keep track of how many servers are currently in the process of
     * connecting.
     */
    private val numberOfCurrentlyConnectingServers = AtomicInteger(0)


    init {
        initChannelPipeline(pipeline)
        LOG.debug("Created ClientToProxyConnection")

    }

    private fun initChannelPipeline(pipeline: ChannelPipeline) {
        LOG.debug("Configuring ChannelPipeline")

//        pipeline.addLast("bytesReadMonitor", bytesReadMonitor)
//        pipeline.addLast("bytesWrittenMonitor", bytesWrittenMonitor)
//        pipeline.addLast(object : ChannelInboundHandlerAdapter(){
//            override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
//                var prettyHexDump = ByteBufUtil.prettyHexDump(msg as ByteBuf)
//
//                LOG.error("{}---{}",this@ClientToProxyConnection.channel,prettyHexDump)
//                super.channelRead(ctx, msg)
//            }
//
//
//        })

        pipeline.addLast("encoder", HttpResponseEncoder())
        // We want to allow longer request lines, headers, and chunks
        // respectively.
        pipeline.addLast("decoder", HttpRequestDecoder(
                proxyServer.maxInitialLineLength,
                proxyServer.maxHeaderSize,
                proxyServer.maxChunkSize))
        pipeline.addLast(
                "idle",
                IdleStateHandler(0, 0, proxyServer
                        .getIdleConnectionTimeout()))

        pipeline.addLast("handler", this)
    }

    @Suppress("unused")
    private fun aggregateContentForFiltering(pipeline: ChannelPipeline,
                                             numberOfBytesToBuffer: Int) {
        pipeline.addLast("inflater", HttpContentDecompressor())
        pipeline.addLast("aggregator", HttpObjectAggregator(
                numberOfBytesToBuffer))
    }

    /**
     * The current HTTP request that this connection is currently servicing.
     */
    @Volatile
    private var currentRequest: HttpRequest? = null
    /**
     * This is the current server connection that we're using while transferring
     * chunked data.
     */
    @Volatile
    private var currentServerConnection: ProxyToServerConnection? = null


    override fun readRaw(msg: ByteBuf) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readHTTPChunk(chunk: HttpContent) {
        currentServerConnection!!.write(chunk)
    }

    override fun readHTTPInitial(httpObject: HttpRequest): ConnectionState {
        LOG.info("{}- Received raw request: {}", this.channel, httpObject.uri())

        // if we cannot parse the request, immediately return a 400 and close the connection, since we do not know what state
        // the client thinks the connection is in
        if (httpObject.decoderResult().isFailure) {
            LOG.debug("Could not parse request from client. Decoder result: {}", httpObject.decoderResult().toString())

            val response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.BAD_REQUEST,
                    "Unable to parse HTTP request")
//            HttpHeaders.setKeepAlive(response, false)
            HttpUtil.setKeepAlive(response, false)

            respondWithShortCircuitResponse(response)

            return ConnectionState.DISCONNECT_REQUESTED
        }
        return doReadHTTPInitial(httpObject)

    }

    private fun doReadHTTPInitial(httpRequest: HttpRequest): ConnectionState {
        // Make a copy of the original request
        this.currentRequest = copy(httpRequest)
        val serverHostAndPort = "localhost:8080"//nginx server

        if (currentServerConnection == null) {

            currentServerConnection = ProxyToServerConnection.create(
                    proxyServer,
                    this,
                    serverHostAndPort,
                    globalTrafficShapingHandler,
                    this.currentRequest!!)

        } else {
            LOG.debug("Reusing existing server connection: {}", currentServerConnection)
        }

        LOG.debug("Writing request to ProxyToServerConnection")
        currentServerConnection!!.write(httpRequest)

        // Figure out our next state
        return if (ProxyUtils.isChunked(httpRequest)) {
            ConnectionState.AWAITING_CHUNK
        } else {
            ConnectionState.AWAITING_INITIAL
        }
    }

    override fun exceptionCaught(cause: Throwable) {
        try {
            when (cause) {
                is IOException -> {
                    // IOExceptions are expected errors, for example when a browser is killed and aborts a connection.
                    // rather than flood the logs with stack traces for these expected exceptions, we log the message at the
                    // INFO level and the stack trace at the DEBUG level.
                    LOG.info("An IOException occurred on ClientToProxyConnection: " + cause.message)
                    LOG.debug("An IOException occurred on ClientToProxyConnection", cause)
                }
                is RejectedExecutionException -> {
                    LOG.info("An executor rejected a read or write operation on the ClientToProxyConnection (this is normal if the proxy is shutting down). Message: " + cause.message)
                    LOG.debug("A RejectedExecutionException occurred on ClientToProxyConnection", cause)
                }
                else -> LOG.error("Caught an exception on ClientToProxyConnection", cause)
            }
        } finally {
            // always disconnect the client when an exception occurs on the channel
            disconnect()
        }
    }


    /**
     * Responds to the client with the specified "short-circuit" response.
     *
     * @param httpResponse the response to return to the client
     * @return true if the connection will be kept open, or false if it will be disconnected.
     */
    private fun respondWithShortCircuitResponse(httpResponse: HttpResponse): Boolean {
        // we are sending a response to the client, so we are done handling this request
        this.currentRequest = null

//        HttpResponse filteredResponse = (HttpResponse) currentFilters.proxyToClientResponse(httpResponse);
//        if (filteredResponse == null) {
//            disconnect();
//            return false;
//        }

        // allow short-circuit messages to close the connection. normally the Connection header would be stripped when modifying
        // the message for proxying, so save the keep-alive status before the modifications are made.
//        val isKeepAlive = HttpUtil.isKeepAlive(httpResponse)
//        HttpUtil.setKeepAlive(httpResponse, isKeepAlive)

        write(httpResponse)

        if (ProxyUtils.isLastChunk(httpResponse)) {
            writeEmptyBuffer()
        }

        if (!HttpUtil.isKeepAlive(httpResponse)) {
            disconnect()
            return false
        }

        return true
    }


    private fun writeEmptyBuffer() {
//        write(Unpooled.EMPTY_BUFFER)
    }

    /**
     * Copy the given [HttpRequest] verbatim.
     *
     * @param original
     * @return
     */
    private fun copy(original: HttpRequest): HttpRequest {
        return if (original is FullHttpRequest) {
            original.copy()
        } else {
            val request = DefaultHttpRequest(original.protocolVersion(),
                    original.method(), original.uri())
            request.headers().set(original.headers())
            request
        }
    }

    /**
     * Tells the client that something went wrong trying to proxy its request. If the Bad Gateway is a response to
     * an HTTP HEAD request, the response will contain no body, but the Content-Length header will be set to the
     * value it would have been if this 502 Bad Gateway were in response to a GET.
     *
     * @param httpRequest the HttpRequest that is resulting in the Bad Gateway response
     * @return true if the connection will be kept open, or false if it will be disconnected
     */
    private fun writeBadGateway(httpRequest: HttpRequest): Boolean {
        val body = "Bad Gateway: " + httpRequest.uri()
        val response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, body)

//        if (ProxyUtils.isHEAD(httpRequest)) {
//            // don't allow any body content in response to a HEAD request
//            response.content().clear()
//        }

        return respondWithShortCircuitResponse(response)
    }

    fun serverConnectionFlowStarted() {
        stopReading()
        this.numberOfCurrentlyConnectingServers.incrementAndGet()
    }

    private fun stopReading() {
        LOG.debug("Stopped reading")
        this.channel.config().isAutoRead = false
    }

    fun serverConnectionFailed(serverConnection: ProxyToServerConnection,
                               lastStateBeforeFailure: ConnectionState,
                               cause: Throwable): Boolean {
        resumeReadingIfNecessary()
//        val initialRequest = serverConnection.initialRequest
//        try {
//            val retrying = serverConnection.connectionFailed(cause)
//            if (retrying) {
//                LOG.debug("Failed to connect to upstream server or chained proxy. Retrying connection. Last state before failure: {}",
//                        lastStateBeforeFailure, cause)
//                return true
//            } else {
//                LOG.debug(
//                        "Connection to upstream server or chained proxy failed: {}.  Last state before failure: {}",
//                        serverConnection.getRemoteAddress(),
//                        lastStateBeforeFailure,
//                        cause)
//                connectionFailedUnrecoverably(initialRequest, serverConnection)
//                return false
//            }
//        } catch (uhe: UnknownHostException) {
//            connectionFailedUnrecoverably(initialRequest, serverConnection)
//            return false
//        }
        LOG.error(
                "Connection to upstream server failed: {}.  Last state before failure: {}",
                serverConnection.remoteAddress,
                lastStateBeforeFailure,
                cause)
        connectionFailedUnrecoverably(this.currentRequest!!, serverConnection)
        return false


    }


    private fun resumeReadingIfNecessary() {
        if (this.numberOfCurrentlyConnectingServers.decrementAndGet() == 0) {
            LOG.debug("All servers have finished attempting to connect, resuming reading from client.")
            resumeReading()
        }
    }

    private fun connectionFailedUnrecoverably(initialRequest: HttpRequest, serverConnection: ProxyToServerConnection) {
        // the connection to the server failed, so disconnect the server and remove the ProxyToServerConnection from the
        // map of open server connections
        serverConnection.disconnect()
//        this.serverConnectionsByHostAndPort.remove(serverConnection.getServerHostAndPort())

        val keepAlive = writeBadGateway(initialRequest)
        if (keepAlive) {
            become(ConnectionState.AWAITING_INITIAL)
        } else {
            become(ConnectionState.DISCONNECT_REQUESTED)
        }
    }

    internal fun serverConnectionSucceeded(serverConnection: ProxyToServerConnection,
                                           shouldForwardInitialRequest: Boolean) {
        LOG.debug("Connection to server succeeded: {}", serverConnection.remoteAddress)
        resumeReadingIfNecessary()
        if (!shouldForwardInitialRequest) {
            become(ConnectionState.AWAITING_INITIAL)
        }

//        numberOfCurrentlyConnectedServers.incrementAndGet()
    }

    internal fun respond(serverConnection: ProxyToServerConnection,
                         currentHttpRequest: HttpRequest, currentHttpResponse: HttpResponse,
                         httpObject: HttpObject) {
        // we are sending a response to the client, so we are done handling this request
        this.currentRequest = null


//        if (httpObject is HttpResponse) {
//
//
//            // if this HttpResponse does not have any means of signaling the end of the message body other than closing
//            // the connection, convert the message to a "Transfer-Encoding: chunked" HTTP response. This avoids the need
//            // to close the client connection to indicate the end of the message. (Responses to HEAD requests "must be" empty.)
//            if (!ProxyUtils.isHEAD(currentHttpRequest) && !ProxyUtils.isResponseSelfTerminating(httpResponse)) {
//                // if this is not a FullHttpResponse,  duplicate the HttpResponse from the server before sending it to
//                // the client. this allows us to set the Transfer-Encoding to chunked without interfering with netty's
//                // handling of the response from the server. if we modify the original HttpResponse from the server,
//                // netty will not generate the appropriate LastHttpContent when it detects the connection closure from
//                // the server (see HttpObjectDecoder#decodeLast). (This does not apply to FullHttpResponses, for which
//                // netty already generates the empty final chunk when Transfer-Encoding is chunked.)
//                if (!(httpResponse instanceof FullHttpResponse)) {
//                    HttpResponse duplicateResponse = ProxyUtils . duplicateHttpResponse (httpResponse);
//
//                    // set the httpObject and httpResponse to the duplicated response, to allow all other standard processing
//                    // (filtering, header modification for proxying, etc.) to be applied.
//                    httpObject = httpResponse = duplicateResponse;
//                }
//
//                HttpHeaders.setTransferEncodingChunked(httpResponse);
//            }
//
//            fixHttpVersionHeaderIfNecessary(httpResponse);
//            modifyResponseHeadersToReflectProxying(httpResponse);
//        }
//
//        httpObject = filters.proxyToClientResponse(httpObject);
//        if (httpObject == null) {
//            forceDisconnect(serverConnection);
//            return;
//        }

        write(httpObject)

//        if (ProxyUtils.isLastChunk(httpObject)) {
//            writeEmptyBuffer()
//        }

        closeConnectionsAfterWriteIfNecessary(serverConnection,
                currentHttpRequest, currentHttpResponse, httpObject)
    }

    /**
     * This method takes care of closing client to proxy and/or proxy to server
     * connections after finishing a write.
     */
    private fun closeConnectionsAfterWriteIfNecessary(
            serverConnection: ProxyToServerConnection,
            currentHttpRequest: HttpRequest, currentHttpResponse: HttpResponse,
            httpObject: HttpObject) {

        val closeServerConnection = shouldCloseServerConnection(
                currentHttpRequest, currentHttpResponse, httpObject)
        val closeClientConnection = shouldCloseClientConnection(
                currentHttpRequest, currentHttpResponse, httpObject)

        if (closeServerConnection) {
            LOG.error("Closing remote connection after writing to client")
            serverConnection.disconnect()
        }

        if (closeClientConnection) {
            LOG.error("Closing connection to client after writes")
            disconnect()
        }
    }

//    private fun forceDisconnect(serverConnection: ProxyToServerConnection) {
//        LOG.debug("Forcing disconnect")
//        serverConnection.disconnect()
//        disconnect()
//    }

    private fun shouldCloseClientConnection(request: HttpRequest,
                                            response: HttpResponse, msg: HttpObject): Boolean {
        if (ProxyUtils.isChunked(response)) {
            // If the response is chunked, we want to return false unless it's
            // the last chunk. If it is the last chunk, then we want to pass
            // through to the same close semantics we'd otherwise use.
            if (!ProxyUtils.isLastChunk(msg)) {

                LOG.debug("Not closing client connection on middle chunk for {}", request.uri())
                return false
            } else {
                LOG.debug("Handling last chunk. Using normal client connection closing rules.")
            }

        }

        if (!HttpUtil.isKeepAlive(request)) {
            LOG.debug("Closing client connection since request is not keep alive: {}", request)
            // Here we simply want to close the connection because the
            // client itself has requested it be closed in the request.
            return true
        }

        // ignore the response's keep-alive; we can keep this client connection open as long as the client allows it.

        LOG.debug("Not closing client connection for request: {}", request)
        return false
    }

    /**
     * Determines if the remote connection should be closed based on the request
     * and response pair. If the request is HTTP 1.0 with no keep-alive header,
     * for example, the connection should be closed.
     *
     * This in part determines if we should close the connection. Here's the
     * relevant section of RFC 2616:
     *
     * "HTTP/1.1 defines the "close" connection option for the sender to signal
     * that the connection will be closed after completion of the response. For
     * example,
     *
     * Connection: close
     *
     * in either the request or the response header fields indicates that the
     * connection SHOULD NOT be considered `persistent' (section 8.1) after the
     * current request/response is complete."
     *
     * @param request
     * The request.
     * @param response
     * The response.
     * @param msg
     * The message.
     * @return Returns true if the connection should close.
     */
    private fun shouldCloseServerConnection(request: HttpRequest,
                                            response: HttpResponse, msg: HttpObject): Boolean {
        if (ProxyUtils.isChunked(response)) {
            // If the response is chunked, we want to return false unless it's
            // the last chunk. If it is the last chunk, then we want to pass
            // through to the same close semantics we'd otherwise use.
            if (!ProxyUtils.isLastChunk(msg)) {
                LOG.debug("Not closing server connection on middle chunk for {}", request.uri())
                return false
            } else {
                LOG.debug("Handling last chunk. Using normal server connection closing rules.")
            }

        }

        // ignore the request's keep-alive; we can keep this server connection open as long as the server allows it.

        if (!HttpUtil.isKeepAlive(response)) {
            LOG.debug("Closing server connection since response is not keep alive: {}", response)
            // In this case, we want to honor the Connection: close header
            // from the remote server and close that connection. We don't
            // necessarily want to close the connection to the client, however
            // as it's possible it has other connections open.
            return true
        }

        LOG.debug("Not closing server connection for response: {}", response)
        return false
    }

    fun timedOut(serverConnection: ProxyToServerConnection) {
        if (currentServerConnection === serverConnection && this.lastReadTime > serverConnection.lastReadTime) {
            // the idle timeout fired on the active server connection. send a timeout response to the client.
            LOG.warn("Server timed out: {}", currentServerConnection)
//            currentFilters.serverToProxyResponseTimedOut()
            writeGatewayTimeout()
        }
    }

    private fun writeGatewayTimeout(): Boolean {
        val body = "Gateway Timeout"
        val response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.GATEWAY_TIMEOUT, body)

        return respondWithShortCircuitResponse(response)
    }
}