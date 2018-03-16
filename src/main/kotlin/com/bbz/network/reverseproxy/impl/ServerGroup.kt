package com.bbz.network.reverseproxy.impl

import com.bbz.network.reverseproxy.ReverseProxyServer
import com.bbz.network.reverseproxy.config.DefaultThreadPoolConfig
import io.netty.channel.EventLoopGroup
import org.slf4j.LoggerFactory
import java.nio.channels.spi.SelectorProvider
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ServerGroup(name: String, threadPoolConfiguration: ThreadPoolConfiguration) {
    companion object {

        private val log = LoggerFactory.getLogger(ServerGroup::class.java)

    }


    /**
     * Global counter for the [.serverGroupId].
     */
    private val serverGroupCount = AtomicInteger(0)


    /**
     * The ID of this server group. Forms part of the name of each thread created for this server group. Useful for
     * differentiating threads when multiple proxy instances are running.
     */
    private val serverGroupId: Int

    private val threadPools: ProxyThreadPools

    /**
     * True when this ServerGroup is stopped.
     */
    private val stopped = AtomicBoolean(false)

    /**
     * Creates a new ServerGroup instance for a proxy. Threads created for this ServerGroup will have the specified
     * ServerGroup name in the Thread name. This constructor does not actually initialize any thread pools; instead,
     * thread pools for specific transport protocols are lazily initialized as needed.
     *
     */
    init {

        this.serverGroupId = serverGroupCount.getAndIncrement()

        threadPools = ProxyThreadPools(SelectorProvider.provider(),
                threadPoolConfiguration.getAcceptorThreadsNum(),
                threadPoolConfiguration.getWorkerThreadsNum(),
                name,
                serverGroupId)

    }


    /**
     * List of all servers registered to use this ServerGroup. Any access to this list should be synchronized using the
     * [.serverRegisterLock].
     */
    private val registeredServers: MutableList<ReverseProxyServer> = ArrayList(1)

    /**
     * Lock controlling access to the [.registerProxyServer] and [.unregisterProxyServer]
     * methods.
     */
    private val serverRegisterLock = Any()

    /**
     * Registers the specified proxy server as a consumer of this server group. The server group will not be shut down
     * until the proxy unregisters itself.
     *
    //     * @param proxyServer proxy server instance to register
     */
    fun registerProxyServer(proxyServer: ReverseProxyServer) {
        synchronized(serverRegisterLock) {
            registeredServers.add(proxyServer)
        }
    }

    /**
     * Unregisters the specified proxy server from this server group. If this was the last registered proxy server, the
     * server group will be shut down.
     *
     * @param proxyServer proxy server instance to unregister
     * @param graceful when true, the server group shutdown (if necessary) will be graceful
     */
    fun unregisteProxyServer(proxyServer: ReverseProxyServer, graceful: Boolean) {
        synchronized(serverRegisterLock) {
            val wasRegistered = registeredServers.remove(proxyServer)
            if (!wasRegistered) {
                log.warn("Attempted to unregister proxy server from ServerGroup that it was not registered with. Was the proxy unregistered twice?")
                return
            }

            if (registeredServers.isEmpty()) {
                log.debug("Proxy server unregistered from ServerGroup. No proxy servers remain registered, so shutting down ServerGroup.")

                shutdown(graceful)
            } else {
                log.debug("Proxy server unregistered from ServerGroup. Not shutting down ServerGroup ({} proxy servers remain registered).", registeredServers.size)
            }
        }
    }

    /**
     * Shuts down all event loops owned by this server group.
     *
     * @param graceful when true, event loops will "gracefully" terminate, waiting for submitted tasks to finish
     */
    private fun shutdown(graceful: Boolean) {
        if (!stopped.compareAndSet(false, true)) {
            log.info("Shutdown requested, but ServerGroup is already stopped. Doing nothing.")

            return
        }

        log.info("Shutting down server group event loops " + if (graceful) "(graceful)" else "(non-graceful)")

        // loop through all event loops managed by this server group. this includes acceptor and worker event loops
        // for both TCP and UDP transport protocols.
        val allEventLoopGroups = threadPools.getAllEventLoops()




        for (group in allEventLoopGroups) {
            if (graceful) {
                group.shutdownGracefully()
            } else {
                group.shutdownGracefully(0, 0, TimeUnit.SECONDS)
            }
        }

        if (graceful) {
            for (group in allEventLoopGroups) {
                try {
                    group.awaitTermination(60, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()

                    log.warn("Interrupted while shutting down event loop")
                }

            }
        }

        log.debug("Done shutting down server group")
    }

    /**
     * Retrieves the client-to-proxy acceptor thread pool for the specified protocol
     *
     * @return the client-to-proxy acceptor thread pool
     */
    fun getAcceptorPool(): EventLoopGroup {
        return threadPools.acceptorPool
//        return getThreadPoolsForProtocol(protocol).getAcceptorPool()
    }

    /**
     * Retrieves the client-to-proxy worker pool for the specified protocol
     *
     * @return the client-to-proxy worker thread pool
     */
    fun getWorkerPool(): EventLoopGroup {
        return threadPools.workerPool
    }


    /**
     * @return true if this ServerGroup has already been stopped
     */
    fun isStopped(): Boolean {
        return stopped.get()
    }

}
