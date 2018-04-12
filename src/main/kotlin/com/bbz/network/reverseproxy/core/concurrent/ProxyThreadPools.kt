package com.bbz.network.reverseproxy.core.concurrent

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import java.nio.channels.spi.SelectorProvider
import java.util.concurrent.ConcurrentHashMap

class ProxyThreadPools(selectorProvider: SelectorProvider, incomingAcceptorThreads: Int, incomingWorkerThreads: Int, serverGroupName: String, serverGroupId: Int) {

    /**
     * These [EventLoopGroup]s accept incoming connections to the
     * proxies. A different EventLoopGroup is used for each
     * TransportProtocol, since these have to be configured differently.
     */
    val acceptorPool: NioEventLoopGroup = NioEventLoopGroup(incomingAcceptorThreads, CategorizedThreadFactory(serverGroupName, "ClientToProxyAcceptor", serverGroupId), selectorProvider)

    /**
     * These [EventLoopGroup]s process incoming requests to the
     * proxies. A different EventLoopGroup is used for each
     * TransportProtocol, since these have to be configured differently.
     */
    val workerPool: NioEventLoopGroup = NioEventLoopGroup(incomingWorkerThreads, CategorizedThreadFactory(serverGroupName, "ReverseProxyWorker", serverGroupId), selectorProvider)


    init {
        workerPool.setIoRatio(90)
    }


    /**
     * Returns all event loops (acceptor and worker concurrent pools) in this pool.
     */
    fun getAllEventLoops(): List<EventLoopGroup> {
        return listOf(acceptorPool, workerPool)
    }

}
