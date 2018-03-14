package com.bbz.network.reverseproxy.impl

import com.google.common.net.HostAndPort
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.timeout.IdleStateHandler
import io.netty.handler.traffic.GlobalTrafficShapingHandler
import io.netty.util.ReferenceCounted
import io.netty.util.concurrent.Future
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.RejectedExecutionException


class ProxyToServerConnection private constructor(proxyServer: DefaultReverseProxyServer,
                                                  private val clientConnection: ClientToProxyConnection,
                                                  serverHostAndPort: String,
                                                  private val globalTrafficShapingHandler: GlobalTrafficShapingHandler?,
                                                  @Volatile var initialRequest: HttpRequest)
    : ProxyConnection<HttpResponse>(proxyServer, ConnectionState.DISCONNECTED) {

    /**
     * 远程真实服务器地址
     */
    val remoteAddress: InetSocketAddress

    @Volatile
    private var connectionFlow: ConnectionFlow? = null

    private val connectLock = Any()

    @Volatile
    private var currentHttpResponse: HttpResponse? = null


    init {
        val hostAndPort = HostAndPort.fromString(serverHostAndPort)
        val host = hostAndPort.host
        val port = hostAndPort.getPortOrDefault(80)

        remoteAddress = InetSocketAddress(host, port)
        setupConnectionParameters()
    }

    companion object {
        fun create(proxyServer: DefaultReverseProxyServer,
                   clientConnection: ClientToProxyConnection,
                   serverHostAndPort: String,
                   globalTrafficShapingHandler: GlobalTrafficShapingHandler?,
                   initialHttpRequest: HttpRequest)
                : ProxyToServerConnection {

            return ProxyToServerConnection(proxyServer,
                    clientConnection,
                    serverHostAndPort,
                    globalTrafficShapingHandler,
                    initialHttpRequest)
        }

        val LOG = LoggerFactory.getLogger(ProxyToServerConnection::class.java.name)!!

    }

    private fun setupConnectionParameters() {

    }

    override fun readHTTPInitial(httpObject: HttpResponse): ConnectionState {
        LOG.debug("Received raw response: {}", httpObject)

        var response = httpObject
        if (httpObject.decoderResult().isFailure) {
            LOG.debug("Could not parse response from server. Decoder result: {}", httpObject.decoderResult().toString())

            // create a "substitute" Bad Gateway response from the server, since we couldn't understand what the actual
            // response from the server was. set the keep-alive on the substitute response to false so the proxy closes
            // the connection to the server, since we don't know what state the server thinks the connection is in.
            val substituteResponse = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.BAD_GATEWAY,
                    "Unable to parse response from server")
//            HttpHeaders.setKeepAlive(substituteResponse, false)
            HttpUtil.setKeepAlive(substituteResponse, false)
            response = substituteResponse
        }

//        currentFilters.serverToProxyResponseReceiving()

        rememberCurrentResponse(response)
        respondWith(response)

        return if (ProxyUtils.isChunked(response)) {
            ConnectionState.AWAITING_CHUNK
        } else {

            ConnectionState.AWAITING_INITIAL
        }
    }

    override fun readHTTPChunk(chunk: HttpContent) {

        respondWith(chunk)

    }


    override fun readRaw(msg: ByteBuf) {
        clientConnection.write(msg)
    }


    override fun exceptionCaught(cause: Throwable) {
        try {
            when (cause) {
                is IOException -> {
                    // IOExceptions are expected errors, for example when a server drops the connection. rather than flood
                    // the logs with stack traces for these expected exceptions, log the message at the INFO level and the
                    // stack trace at the DEBUG level.
                    LOG.info("An IOException occurred on ProxyToServerConnection: " + cause.message)
                    LOG.debug("An IOException occurred on ProxyToServerConnection", cause)
                }
                is RejectedExecutionException -> {
                    LOG.info("An executor rejected a read or write operation on the ProxyToServerConnection (this is normal if the proxy is shutting down). Message: " + cause.message)
                    LOG.debug("A RejectedExecutionException occurred on ProxyToServerConnection", cause)
                }
                else -> LOG.error("Caught an exception on ProxyToServerConnection", cause)
            }
        } finally {
            if (currentState != ConnectionState.DISCONNECTED) {
                LOG.info("Disconnecting open connection to server")
                disconnect()
            }
        }
    }

    override fun write(msg: Any) {
        LOG.debug("Requested write of {}", msg)

        if (msg is ReferenceCounted) {
            LOG.debug("Retaining reference counted message")
            msg.retain()
        }

        if (currentState == ConnectionState.DISCONNECTED && msg is HttpRequest) {
            LOG.debug("Currently disconnected, connect and then write the message")
            connectAndWrite(msg)
        } else {
            if (isConnecting()) {
                synchronized(connectLock) {
                    if (isConnecting()) {
                        LOG.debug("Attempted to write while still in the process of connecting, waiting for connection.")
//                        clientConnection.stopReading()
                        try {
                            (connectLock as java.lang.Object).wait(30000)
                        } catch (ie: InterruptedException) {
                            LOG.warn("Interrupted while waiting for connect monitor")
                        }

                    }
                }
            }

//             only write this message if a connection was established and is not in the process of disconnecting or
//             already disconnected
            if (isConnecting() || currentState.isDisconnectingOrDisconnected()) {
                LOG.debug("Connection failed or timed out while waiting to write message to server. Message will be discarded: {}", msg)
                return
            }

            LOG.debug("Using existing connection to: {}", remoteAddress)
            doWrite(msg)
        }
    }

    /**
     * Configures the connection to the upstream server and begins the [ConnectionFlow].
     *
     * @param initialRequest the current HTTP request being handled
     */
    private fun connectAndWrite(initialRequest: HttpRequest) {
        LOG.debug("Starting new connection to: {}", remoteAddress)

        // Remember our initial request so that we can write it after connecting
        this.initialRequest = initialRequest
        initializeConnectionFlow()
        connectionFlow?.start()
    }

    /**
     * This method initializes our [ConnectionFlow] based on however this connection has been configured. If
     * the [.disableSni] value is true, this method will not pass peer information to the MitmManager when
     * handling CONNECTs.
     */
    private fun initializeConnectionFlow() {
        this.connectionFlow = ConnectionFlow(clientConnection, this, connectLock)
                .then(connectChannel)
//                .then(startTunneling)
//                .then(clientConnection.RespondCONNECTSuccessful)
//                .then(clientConnection.startTunneling)

//        if (chainedProxy != null && chainedProxy.requiresEncryption()) {
//            connectionFlow.then(serverConnection.EncryptChannel(chainedProxy
//                    .newSslEngine()))
//        }

//        if (ProxyUtils.isCONNECT(initialRequest)) {
//            // If we're chaining, forward the CONNECT request
//            if (hasUpstreamChainedProxy()) {
//                connectionFlow.then(
//                        serverConnection.HTTPCONNECTWithChainedProxy)
//            }

//            val mitmManager = proxyServer.getMitmManager()
//            val isMitmEnabled = mitmManager != null
//
//            if (isMitmEnabled) {
        // When MITM is enabled and when chained proxy is set up, remoteAddress
        // will be the chained proxy's address. So we use serverHostAndPort
        // which is the end server's address.
//                val parsedHostAndPort = HostAndPort.fromString(serverHostAndPort)

        // SNI may be disabled for this request due to a previous failed attempt to connect to the server
        // with SNI enabled.
//                if (disableSni) {
//                    connectionFlow.then(serverConnection.EncryptChannel(proxyServer.getMitmManager()
//                            .serverSslEngine()))
//                } else {
//                    connectionFlow.then(serverConnection.EncryptChannel(proxyServer.getMitmManager()
//                            .serverSslEngine(parsedHostAndPort.host, parsedHostAndPort.port)))
//                }
//
//                connectionFlow
//                        .then(clientConnection.RespondCONNECTSuccessful)
//                        .then(serverConnection.MitmEncryptClientChannel)
//            } else {
//                connectionFlow.then(serverConnection.startTunneling)
//                        .then(clientConnection.RespondCONNECTSuccessful)
//                        .then(clientConnection.startTunneling)
//            }
    }


    private val connectChannel = object : ConnectionFlowStep(this,
            ConnectionState.CONNECTING) {

        override fun shouldExecuteOnEventLoop(): Boolean {
            return false
        }

        override fun execute(): Future<*> {
            val cb = Bootstrap().group(proxyServer.serverGroup.getProxyToServerWorkerPool())
                    .channel(EpollSocketChannel::class.java)
//                    .channel(NioSocketChannel::class.java)
                    .handler(
                            object : ChannelInitializer<Channel>() {
                                @Throws(Exception::class)
                                override fun initChannel(ch: Channel) {
                                    initChannelPipeline(ch.pipeline())
                                }
                            })
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                            proxyServer.getConnectTimeout())


            return cb.connect(remoteAddress)
        }
    }


    private fun initChannelPipeline(pipeline: ChannelPipeline) {
        if (globalTrafficShapingHandler != null) {
//            pipeline.addLast("global-traffic-shaping", globalTrafficShapingHandler)
        }

//        pipeline.addLast("bytesReadMonitor", bytesReadMonitor)
//        pipeline.addLast("bytesWrittenMonitor", bytesWrittenMonitor)

        pipeline.addLast("encoder", HttpRequestEncoder())
//        pipeline.addLast("decoder", HeadAwareHttpResponseDecoder(//这里是否有必要，存疑
        pipeline.addLast("decoder", HttpResponseDecoder(
                proxyServer.maxInitialLineLength,
                proxyServer.maxChunkSize,
                proxyServer.maxChunkSize))

        // Set idle timeout
        pipeline.addLast(
                "idle",
                IdleStateHandler(0, 0, proxyServer
                        .getIdleConnectionTimeout()))

        pipeline.addLast("handler", this)
    }

     override fun timedOut() {
        super.timedOut()
        clientConnection.timedOut(this)
    }

    internal fun connectionSucceeded(shouldForwardInitialRequest: Boolean) {
        become(ConnectionState.AWAITING_INITIAL)

        clientConnection.serverConnectionSucceeded(this, shouldForwardInitialRequest)

        if (shouldForwardInitialRequest) {
            LOG.debug("Writing initial request: {}", initialRequest)
            write(initialRequest)
        } else {
            LOG.debug("Dropping initial request: {}", initialRequest)
        }

        // we're now done with the initialRequest: it's either been forwarded to the upstream server (HTTP requests), or
        // completely dropped (HTTPS CONNECTs). if the initialRequest is reference counted (typically because the HttpObjectAggregator is in
        // the pipeline to generate FullHttpRequests), we need to manually release it to avoid a memory leak.
        if (initialRequest is ReferenceCounted) {
            (initialRequest as ReferenceCounted).release()
        }
    }

    /**
     * Keeps track of the current HttpResponse so that we can associate its
     * headers with future related chunks for this same transfer.
     *
     * @param response
     */
    private fun rememberCurrentResponse(response: HttpResponse) {
        LOG.debug("Remembering the current response.")
        // We need to make a copy here because the response will be
        // modified in various ways before we need to do things like
        // analyze response headers for whether or not to close the
        // connection (which may not happen for a while for large, chunked
        // responses, for example).
        currentHttpResponse = ProxyUtils.copyMutableResponse(response)
    }

    @Volatile
    private var currentHttpRequest: HttpRequest? = null

    override fun writeHttp(httpObject: HttpObject) {

        if (httpObject is HttpRequest) {
// Remember that we issued this HttpRequest for later
            currentHttpRequest = httpObject
        }
        super.writeHttp(httpObject)
    }

    /**
     * Respond to the client with the given [HttpObject].
     *
     * @param httpObject
     */
    private fun respondWith(httpObject: HttpObject) {
        clientConnection.respond(this, currentHttpRequest!!, currentHttpResponse!!, httpObject)
    }

}