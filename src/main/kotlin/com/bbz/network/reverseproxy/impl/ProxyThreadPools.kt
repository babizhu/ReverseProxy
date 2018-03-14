package com.bbz.network.reverseproxy.impl

import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.EpollEventLoopGroup
import java.nio.channels.spi.SelectorProvider

class ProxyThreadPools(selectorProvider: SelectorProvider, incomingAcceptorThreads: Int, incomingWorkerThreads: Int, outgoingWorkerThreads: Int, serverGroupName: String, serverGroupId: Int) {

    /**
     * These [EventLoopGroup]s accept incoming connections to the
     * proxies. A different EventLoopGroup is used for each
     * TransportProtocol, since these have to be configured differently.
     */
//    val clientToProxyAcceptorPool: EpollEventLoopGroup = EpollEventLoopGroup(incomingAcceptorThreads, CategorizedThreadFactory(serverGroupName, "ClientToProxyAcceptor", serverGroupId), selectorProvider)
    val clientToProxyAcceptorPool: EpollEventLoopGroup = EpollEventLoopGroup(incomingAcceptorThreads, CategorizedThreadFactory(serverGroupName, "ClientToProxyAcceptor", serverGroupId))

    /**
     * These [EventLoopGroup]s process incoming requests to the
     * proxies. A different EventLoopGroup is used for each
     * TransportProtocol, since these have to be configured differently.
     */
//    val clientToProxyWorkerPool: EpollEventLoopGroup = EpollEventLoopGroup(incomingWorkerThreads, CategorizedThreadFactory(serverGroupName, "ClientToProxyWorker", serverGroupId), selectorProvider)
    val clientToProxyWorkerPool: EpollEventLoopGroup = EpollEventLoopGroup(incomingWorkerThreads, CategorizedThreadFactory(serverGroupName, "ClientToProxyWorker", serverGroupId))

    /**
     * These [EventLoopGroup]s are used for making outgoing
     * connections to servers. A different EventLoopGroup is used for each
     * TransportProtocol, since these have to be configured differently.
     */
//    val proxyToServerWorkerPool: EpollEventLoopGroup = EpollEventLoopGroup(outgoingWorkerThreads, CategorizedThreadFactory(serverGroupName, "ProxyToServerWorker", serverGroupId), selectorProvider)
    val proxyToServerWorkerPool: EpollEventLoopGroup = EpollEventLoopGroup(outgoingWorkerThreads, CategorizedThreadFactory(serverGroupName, "ProxyToServerWorker", serverGroupId))


    init {

        clientToProxyWorkerPool.setIoRatio(90)
        proxyToServerWorkerPool.setIoRatio(90)
    }


    /**
     * Returns all event loops (acceptor and worker thread pools) in this pool.
     */
    fun getAllEventLoops(): List<EventLoopGroup> {
        return listOf(clientToProxyAcceptorPool, clientToProxyWorkerPool, proxyToServerWorkerPool)
    }


}
