package com.bbz.network.reverseproxy.impl

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpMessage
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.timeout.IdleStateEvent
import io.netty.util.ReferenceCounted
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.Promise
import org.slf4j.LoggerFactory

@Suppress("UNCHECKED_CAST")
abstract class ProxyConnection<in I : HttpObject>(
        protected val proxyServer: DefaultReverseProxyServer,
        @Volatile var currentState: ConnectionState)
    : SimpleChannelInboundHandler<Any>() {

    companion object {
        protected val LOG = LoggerFactory.getLogger(ProxyConnection::class.java.name)!!

    }

    @Volatile
    internal lateinit var ctx: ChannelHandlerContext
    @Volatile
    protected lateinit var channel: Channel

    @Volatile
    protected var lastReadTime: Long = 0


    /***************************************************************************
     * Adapting the Netty API
     **************************************************************************/
    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        read(msg)
    }

    @Throws(Exception::class)
    override fun channelRegistered(ctx: ChannelHandlerContext) {
        try {
            this.ctx = ctx
            this.channel = ctx.channel()
            this.proxyServer.registerChannel(ctx.channel())
        } finally {
            super.channelRegistered(ctx)
        }
    }

    /**
     * Only once the Netty Channel is active to we recognize the ProxyConnection
     * as connected.
     */
    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        try {
            connected()
        } finally {
            super.channelActive(ctx)
        }
    }

    /**
     * As soon as the Netty Channel is inactive, we recognize the
     * ProxyConnection as disconnected.
     */
    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        try {
            disconnected()
        } finally {
            super.channelInactive(ctx)
        }
    }

    @Throws(Exception::class)
    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        LOG.debug("Writability changed. Is writable: {}", channel.isWritable)
        try {
            if (ctx.channel().isWritable) {
                becameWritable()
            } else {
                becameSaturated()
            }
        } finally {
            super.channelWritabilityChanged(ctx)
        }
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        exceptionCaught(cause)
    }

    /**
     *
     *
     * We're looking for [IdleStateEvent]s to see if we need to
     * disconnect.
     *
     *
     *
     *
     * Note - we don't care what kind of IdleState we got. Thanks to [qbast](https://github.com/qbast) for pointing this out.
     *
     */
    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        try {
            if (evt is IdleStateEvent) {
                LOG.debug("Got idle")
                timedOut()
            }
        } finally {
            super.userEventTriggered(ctx, evt)
        }
    }

    protected var tunneling: Boolean = false

    private fun read(msg: Any) {
        LOG.debug("Reading: {}", msg)

        lastReadTime = System.currentTimeMillis()

        if (tunneling) {
            // In tunneling mode, this connection is simply shoveling bytes
            readRaw(msg as ByteBuf)
        } else {
            // If not tunneling, then we are always dealing with HttpObjects.
            readHTTP(msg as HttpObject)
        }

    }

    abstract fun readRaw(msg: ByteBuf)

    private fun readHTTP(httpObject: HttpObject) {
        var nextState = currentState
        when (currentState) {
            ConnectionState.AWAITING_INITIAL ->
                if (httpObject is HttpMessage) {
                    nextState = readHTTPInitial(httpObject as I)
                } else {
                    // Similar to the AWAITING_PROXY_AUTHENTICATION case below, we may enter an AWAITING_INITIAL
                    // state if the proxy responded to an earlier request with a 502 or 504 response, or a short-circuit
                    // response from a filter. The client may have sent some chunked HttpContent associated with the request
                    // after the short-circuit response was sent. We can safely drop them.
                    LOG.debug("Dropping message because HTTP object was not an HttpMessage. HTTP object may be orphaned content from a short-circuited response. Message: {}", httpObject)
                }
            ConnectionState.AWAITING_CHUNK -> {
                val chunk = httpObject as HttpContent
                readHTTPChunk(chunk)
                nextState = if (ProxyUtils.isLastChunk(chunk))
                    ConnectionState.AWAITING_INITIAL
                else
                    ConnectionState.AWAITING_CHUNK
            }
            ConnectionState.AWAITING_PROXY_AUTHENTICATION -> if (httpObject is HttpRequest) {
                // Once we get an HttpRequest, try to process it as usual
                nextState = readHTTPInitial(httpObject as I)
            } else {
                // Anything that's not an HttpRequest that came in while
                // we're pending authentication gets dropped on the floor. This
                // can happen if the connected host already sent us some chunks
                // (e.g. from a POST) after an initial request that turned out
                // to require authentication.
            }
            ConnectionState.CONNECTING -> LOG.warn("Attempted to read from connection that's in the process of connecting.  This shouldn't happen.")
            ConnectionState.NEGOTIATING_CONNECT -> LOG.debug("Attempted to read from connection that's in the process of negotiating an HTTP CONNECT.  This is probably the LastHttpContent of a chunked CONNECT.")
            ConnectionState.AWAITING_CONNECT_OK -> LOG.warn("AWAITING_CONNECT_OK should have been handled by ProxyToServerConnection.read()")
            ConnectionState.HANDSHAKING -> LOG.warn(
                    "Attempted to read from connection that's in the process of handshaking.  This shouldn't happen.",
                    channel)
            ConnectionState.DISCONNECT_REQUESTED, ConnectionState.DISCONNECTED -> LOG.info("Ignoring message since the connection is closed or about to close")
        }
        become(nextState)
    }

    internal fun become(state: ConnectionState) {
        this.currentState = state
    }


    protected abstract fun readHTTPChunk(chunk: HttpContent)

    protected abstract fun readHTTPInitial(httpObject: I): ConnectionState

    open fun write(msg: Any) {
        if (msg is ReferenceCounted) {
            LOG.debug("Retaining reference counted message")
            msg.retain()
        }

        doWrite(msg)
    }

    fun doWrite(msg: Any) {
        LOG.debug("Writing: {}", msg)

        try {
            if (msg is HttpObject) {
                writeHttp(msg)
            } else {
                writeRaw(msg as ByteBuf)
            }
        } finally {
            LOG.debug("Wrote: {}", msg)
        }
    }

    /**
     * Writes HttpObjects to the connection asynchronously.
     *
     * @param httpObject
     */
    open fun writeHttp(httpObject: HttpObject) {
        if (ProxyUtils.isLastChunk(httpObject)) {
            channel.write(httpObject)
            LOG.debug("Writing an empty buffer to signal the end of our chunked transfer")
            writeToChannel(Unpooled.EMPTY_BUFFER)
        } else {
            writeToChannel(httpObject)
        }
    }

    /**
     * Writes raw buffers to the connection.
     *
     * @param buf
     */
    private fun writeRaw(buf: ByteBuf) {
        writeToChannel(buf)
    }

    private fun writeToChannel(msg: Any): ChannelFuture {
        return channel.writeAndFlush(msg)
    }


    /***************************************************************************
     * Lifecycle
     **************************************************************************/

    /**
     * This method is called as soon as the underlying [Channel] is
     * connected. Note that for proxies with complex [ConnectionFlow]s
     * that include SSL handshaking and other such things, just because the
     * [Channel] is connected doesn't mean that our connection is fully
     * established.
     */
    protected fun connected() {
        LOG.debug("Connected")
    }

    /**
     * This method is called as soon as the underlying [Channel] becomes
     * disconnected.
     */
    protected fun disconnected() {
        become(ConnectionState.DISCONNECTED)
        LOG.debug("Disconnected")
    }

    /**
     * This method is called when the underlying [Channel] times out due
     * to an idle timeout.
     */
    protected fun timedOut() {
        disconnect()
    }

    /**
     * Callback that's invoked if this connection becomes saturated.
     */
    protected fun becameSaturated() {
        LOG.debug("Became saturated")
    }

    /**
     * Callback that's invoked when this connection becomes writeable again.
     */
    protected fun becameWritable() {
        LOG.debug("Became writeable")
    }

    /**
     * Override this to handle exceptions that occurred during asynchronous
     * processing on the [Channel].
     *
     * @param cause
     */
    protected abstract fun exceptionCaught(cause: Throwable)

    /**
     * Disconnects. This will wait for pending writes to be flushed before
     * disconnecting.
     *
     * @return Future<Void> for when we're done disconnecting. If we weren't
     *         connected, this returns null.
     */
    fun disconnect(): Future<Void>? {
        return if (channel == null) {
            null
        } else {
            val promise = channel.newPromise()
            writeToChannel(Unpooled.EMPTY_BUFFER).addListener { closeChannel(promise) }
            promise
        }
    }

    private fun closeChannel(promise: Promise<Void>) {
        channel.close().addListener { future ->
            if (future.isSuccess) {
                promise.setSuccess(null)
            } else {
                promise.setFailure(future.cause())
            }
        }
    }

    protected fun resumeReading() {

        LOG.debug("Resumed reading")
        this.channel.config().isAutoRead = true
    }

    fun isConnecting(): Boolean {
        return currentState.isPartOfConnectionFlow()

    }

    @Suppress("LeakingThis")
    internal val startTunneling = object : ConnectionFlowStep(this,
            ConnectionState.NEGOTIATING_CONNECT) {
        override fun shouldSuppressInitialRequest(): Boolean {
            return true
        }

        override fun execute(): Future<*> {
            try {
                val pipeline = ctx.pipeline()
                if (pipeline.get("encoder") != null) {
                    pipeline.remove("encoder")
                }
//                if (pipeline.get("responseWrittenMonitor") != null) {
//                    pipeline.remove("responseWrittenMonitor")
//                }
                if (pipeline.get("decoder") != null) {
                    pipeline.remove("decoder")
                }
//                if (pipeline.get("requestReadMonitor") != null) {
//                    pipeline.remove("requestReadMonitor")
//                }
                tunneling = true
                return channel.newSucceededFuture()
            } catch (t: Throwable) {
                return channel.newFailedFuture(t)
            }

        }
    }
}


