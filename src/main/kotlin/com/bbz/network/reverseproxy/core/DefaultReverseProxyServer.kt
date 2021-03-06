package com.bbz.network.reverseproxy.core

import com.bbz.network.reverseproxy.ReverseProxyServer
import com.bbz.network.reverseproxy.ReverseProxyServerBootstrap
import com.bbz.network.reverseproxy.config.DefaultNetWorkConfig
import com.bbz.network.reverseproxy.config.DefaultServerConfig
import com.bbz.network.reverseproxy.config.DefaultThreadPoolConfig
import com.bbz.network.reverseproxy.core.concurrent.ServerGroup
import com.bbz.network.reverseproxy.core.concurrent.ThreadPoolConfiguration
import com.bbz.network.reverseproxy.core.filter.HttpFilter
import com.bbz.network.reverseproxy.route.RoutePolicy
import com.bbz.network.reverseproxy.route.impl.RoundRobinPolicy
import com.bbz.network.reverseproxy.utils.ProxyUtils
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.EpollChannelOption
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.traffic.GlobalTrafficShapingHandler
import io.netty.util.concurrent.GlobalEventExecutor
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unused")
class DefaultReverseProxyServer private constructor(private val serverGroup: ServerGroup,
                                                    private var listenAddress: InetSocketAddress,
                                                    private var idleConnectionTimeout: Int,
                                                    private var connectTimeoutMs: Int,
                                                    readThrottleBytesPerSecond: Long,
                                                    writeThrottleBytesPerSecond: Long,
                                                    private var proxyAlias: String,
                                                    val maxInitialLineLength: Int,
                                                    val maxHeaderSize: Int,
                                                    val maxChunkSize: Int,
                                                    private val routePolicy: RoutePolicy,
                                                    val httpFilter: HttpFilter?) : ReverseProxyServer {

    private val stopped = AtomicBoolean(false)
    private val allChannels = DefaultChannelGroup("Reverse-Proxy-Server", GlobalEventExecutor.INSTANCE)

    private val jvmShutdownHook = Thread(Runnable { abort() }, "Reverse-Proxy-JVM-shutdown-hook")
    @Volatile
    private var globalTrafficShapingHandler: GlobalTrafficShapingHandler? = null

    companion object {

        private val log = LoggerFactory.getLogger(DefaultReverseProxyServer::class.java)
        fun bootstrap(): ReverseProxyServerBootstrap {
            return DefaultReverseProxyServerBootstrap()
        }

    }

    override fun getRoutePolice(): RoutePolicy {
        return routePolicy
    }

    override fun getIdleConnectionTimeout(): Int {
        return idleConnectionTimeout
    }

    override fun setIdleConnectionTimeout(idleConnectionTimeout: Int) {
        this.idleConnectionTimeout = idleConnectionTimeout
    }

    override fun getConnectTimeoutMs(): Int {
        return connectTimeoutMs
    }

    override fun setConnectTimeoutMs(connectTimeoutMs: Int) {
        this.connectTimeoutMs = connectTimeoutMs
    }

    override fun stop() {
        doStop(true)
    }

    override fun abort() {
        doStop(false)

    }

    private fun doStop(graceful: Boolean) {
        // only stop the server if it hasn't already been stopped
        if (stopped.compareAndSet(false, true)) {
            if (graceful) {
                log.info("Shutting down proxy server gracefully")
            } else {
                log.info("Shutting down proxy server immediately (non-graceful)")
            }

            closeAllChannels(graceful)
            serverGroup.unregisteProxyServer(this, graceful)

            // remove the shutdown hook that was added when the proxy was started, since it has now been stopped
            try {
                Runtime.getRuntime().removeShutdownHook(jvmShutdownHook)
            } catch (e: IllegalStateException) {
                // ignore -- IllegalStateException means the VM is already shutting down
            }

            log.info("Done shutting down proxy server")
        }
    }

    private fun closeAllChannels(graceful: Boolean) {
        log.info("Closing all channels " + if (graceful) "(graceful)" else "(non-graceful)")

        val future = allChannels.close()

        // if this is a graceful shutdown, log any channel closing failures. if this isn't a graceful shutdown, ignore them.
        if (graceful) {
            try {
                future.await(10, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("Interrupted while waiting for channels to shut down gracefully.")
            }

            if (!future.isSuccess) {
                for (cf in future) {
                    if (!cf.isSuccess) {
                        log.info("Unable to close channel.  Cause of failure for {} is {}", cf.channel(), cf.cause())
                    }
                }
            }
        }
    }

    override fun getListenAddress(): InetSocketAddress {
        return listenAddress
    }

    override fun setThrottle(readThrottleBytesPerSecond: Long, writeThrottleBytesPerSecond: Long) {

        if (globalTrafficShapingHandler != null) {
            globalTrafficShapingHandler!!.configure(writeThrottleBytesPerSecond, readThrottleBytesPerSecond)
        } else {
            // don't create a GlobalTrafficShapingHandler if throttling was not enabled and is still not enabled
            if (readThrottleBytesPerSecond > 0 || writeThrottleBytesPerSecond > 0) {
                globalTrafficShapingHandler = createGlobalTrafficShapingHandler(readThrottleBytesPerSecond, writeThrottleBytesPerSecond)
            }
        }
    }


    /**
     * Creates a new GlobalTrafficShapingHandler for this HttpProxyServer, using this proxy's proxyToServerEventLoop.
     *
     * @param readThrottleBytesPerSecond
     * @param writeThrottleBytesPerSecond
     *
     * @return
     */
    private fun createGlobalTrafficShapingHandler(readThrottleBytesPerSecond: Long, writeThrottleBytesPerSecond: Long): GlobalTrafficShapingHandler {
        val proxyToServerEventLoop = this.serverGroup.getWorkerPool()
        return GlobalTrafficShapingHandler(proxyToServerEventLoop,
                writeThrottleBytesPerSecond,
                readThrottleBytesPerSecond,
                DefaultNetWorkConfig.TRAFFIC_SHAPING_CHECK_INTERVAL_MS,
                java.lang.Long.MAX_VALUE)
    }

    private fun start(): DefaultReverseProxyServer {
        if (!serverGroup.isStopped()) {
            log.info("Starting reverse proxy at address: " + this.listenAddress)

            serverGroup.registerProxyServer(this)

            doStart()
        } else {
            throw IllegalStateException("Attempted to start reverse proxy, but proxy's server group is already stopped")
        }


        return this
    }

    private fun doStart() {
//        val initializer = object : ChannelInitializer<Channel>() {
//            @Throws(Exception::class)
//            override fun initChannel(ch: Channel) {
//                ClientToProxyConnection(
//                        this@DefaultReverseProxyServer,
//                        ch.pipeline(),
//                        globalTrafficShapingHandler)
//            }
//        }
        val serverBootstrap = ServerBootstrap()
                .group(
                        serverGroup.getAcceptorPool(),
                        serverGroup.getWorkerPool())
                .channel(NioServerSocketChannel::class.java)
                .option(ChannelOption.SO_BACKLOG, 1024)          // (5)
                .option(ChannelOption.SO_REUSEADDR, true)
//                .option(ChannelOption.SO_RCVBUF, 10 * 1024)
//                .option(ChannelOption.SO_SNDBUF, 10 * 1024)
                .option(EpollChannelOption.SO_REUSEPORT, true)
//                .childOption(ChannelOption.AUTO_READ,false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
//                .childHandler(initializer)
                .childHandler(ReverseProxyInitializer(this))

        val future = serverBootstrap.bind(listenAddress)

        future.addListener({
            if (future.isSuccess) {
                registerChannel(future.channel())
                this.listenAddress = future.channel().localAddress() as InetSocketAddress

            }
        }).awaitUninterruptibly()


        val cause = future.cause()
        if (cause != null) {
            throw RuntimeException(cause)
        }
        this.listenAddress = future.channel().localAddress() as InetSocketAddress
        log.info("Reverse Proxy started at address: " + this.listenAddress)
//        this.boundAddress = future.channel().localAddress() as InetSocketAddress

        Runtime.getRuntime().addShutdownHook(jvmShutdownHook)
    }

    /**
     * Register a new [Channel] with this server, for later closing.
     *
     * @param channel
     */
    fun registerChannel(channel: Channel) {
        allChannels.add(channel)
    }

    private class DefaultReverseProxyServerBootstrap : ReverseProxyServerBootstrap {


        private var threadName = DefaultThreadPoolConfig.THREAD_NAME
        private val serverGroup: ServerGroup? = null
        private var listenAddress: InetSocketAddress? = null
        private var port = DefaultNetWorkConfig.PORT
        //    private var allowLocalOnly = true
        //    private var sslEngineSource: SslEngineSource? = null
//    private var authenticateSslClients = true
        //    private var proxyAuthenticator: ProxyAuthenticator? = null
//    private var chainProxyManager: ChainedProxyManager? = null
//    private var mitmManager: MitmManager? = null
//    private var filtersSource: HttpFiltersSource = HttpFiltersSourceAdapter()
//    private var transparent = false
        private var idleConnectionTimeout = DefaultNetWorkConfig.IDLE_CONNECTION_TIMEOUT_SECOND
        //    private val activityTrackers = ConcurrentLinkedQueue<ActivityTracker>()
        private var connectTimeoutMs = DefaultNetWorkConfig.CONNECT_TIME_OUT_MS
        //    private var serverResolver: HostResolver = DefaultHostResolver()
        private var readThrottleBytesPerSecond: Long = 0
        private var writeThrottleBytesPerSecond: Long = 0
        //    private var localAddress: InetSocketAddress? = null
        private var proxyAlias: String? = null

        private var maxInitialLineLength = DefaultNetWorkConfig.MAX_INITIAL_LINE_LENGTH_DEFAULT
        private var maxHeaderSize = DefaultNetWorkConfig.MAX_HEADER_SIZE_DEFAULT
        private var maxChunkSize = DefaultNetWorkConfig.MAX_CHUNK_SIZE_DEFAULT
        private var threadPoolConfiguration = ThreadPoolConfiguration()
        private var httpFilter:HttpFilter? = null
        /**
         * 路由策略，缺省采用轮训策略
         */
        private var routePolice: RoutePolicy = RoundRobinPolicy()

        override fun withName(name: String): ReverseProxyServerBootstrap {
            this.threadName = name
            return this
        }
        override fun withHttpFilter(httpFilter: HttpFilter):ReverseProxyServerBootstrap {
            this.httpFilter = httpFilter
            return this
        }
        override fun withListenAddress(address: InetSocketAddress): ReverseProxyServerBootstrap {
            this.listenAddress = address
            return this
        }

        override fun withPort(port: Int): ReverseProxyServerBootstrap {
            this.port = port
            return this
        }

        override fun withIdleConnectionTimeout(idleConnectionTimeout: Int): ReverseProxyServerBootstrap {
            this.idleConnectionTimeout = idleConnectionTimeout
            return this
        }

        override fun withConnectTimeout(connectTimeout: Int): ReverseProxyServerBootstrap {
            this.connectTimeoutMs = connectTimeout
            return this
        }

        override fun withThrottling(readThrottleBytesPerSecond: Long, writeThrottleBytesPerSecond: Long): ReverseProxyServerBootstrap {
            this.readThrottleBytesPerSecond = readThrottleBytesPerSecond
            this.writeThrottleBytesPerSecond = writeThrottleBytesPerSecond
            return this
        }

        override fun withMaxInitialLineLength(maxInitialLineLength: Int): ReverseProxyServerBootstrap {
            this.maxInitialLineLength = maxInitialLineLength
            return this
        }

        override fun withMaxHeaderSize(maxHeaderSize: Int): ReverseProxyServerBootstrap {
            this.maxHeaderSize = maxHeaderSize
            return this
        }


        override fun withRoutePolice(routePolicy: RoutePolicy): ReverseProxyServerBootstrap {
            this.routePolice = routePolicy
            return this

        }

        override fun withMaxChunkSize(maxChunkSize: Int): ReverseProxyServerBootstrap {
            this.maxChunkSize = maxChunkSize
            return this
        }

        override fun withProxyAlias(alias: String): ReverseProxyServerBootstrap {
            this.proxyAlias = alias
            return this
        }

        override fun withThreadPoolConfiguration(threadPoolConfiguration: ThreadPoolConfiguration): ReverseProxyServerBootstrap {
            this.threadPoolConfiguration = threadPoolConfiguration
            return this
        }

        override fun start(): ReverseProxyServer {
            return build().start()
        }

        private fun build(): DefaultReverseProxyServer {
            val serverGroup: ServerGroup = serverGroup
                    ?: ServerGroup(threadName, threadPoolConfiguration)

            val listenAddress = listenAddress ?: InetSocketAddress(port)
            val proxyAlias = (proxyAlias) ?: ProxyUtils.getHostName()
            ?: DefaultServerConfig.FALLBACK_PROXY_ALIAS

            return DefaultReverseProxyServer(serverGroup,
                    listenAddress,
                    idleConnectionTimeout, connectTimeoutMs, readThrottleBytesPerSecond, writeThrottleBytesPerSecond,
                    proxyAlias, maxInitialLineLength, maxHeaderSize, maxChunkSize, routePolice,httpFilter
            )
        }
    }

    init {
        if (writeThrottleBytesPerSecond > 0 || readThrottleBytesPerSecond > 0) {
            this.globalTrafficShapingHandler = createGlobalTrafficShapingHandler(readThrottleBytesPerSecond, writeThrottleBytesPerSecond)
        } else {
            this.globalTrafficShapingHandler = null
        }
    }


}
