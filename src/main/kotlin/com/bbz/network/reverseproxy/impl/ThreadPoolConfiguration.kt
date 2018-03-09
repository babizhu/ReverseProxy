package com.bbz.network.reverseproxy.impl

class ThreadPoolConfiguration {
    private var acceptorThreads = ServerGroup.DEFAULT_INCOMING_ACCEPTOR_THREADS
    private var clientToProxyWorkerThreads = ServerGroup.DEFAULT_INCOMING_WORKER_THREADS
    private var proxyToServerWorkerThreads = ServerGroup.DEFAULT_OUTGOING_WORKER_THREADS

    fun getClientToProxyWorkerThreads(): Int {
        return clientToProxyWorkerThreads
    }

    /**
     * Set the number of client-to-proxy worker threads to create. Worker threads perform the actual processing of
     * client requests. The default value is [ServerGroup.DEFAULT_INCOMING_WORKER_THREADS].
     *
     * @param clientToProxyWorkerThreads number of client-to-proxy worker threads to create
     * @return this thread pool configuration instance, for chaining
     */
    fun withClientToProxyWorkerThreads(clientToProxyWorkerThreads: Int): ThreadPoolConfiguration {
        this.clientToProxyWorkerThreads = clientToProxyWorkerThreads
        return this
    }

    fun getAcceptorThreads(): Int {
        return acceptorThreads
    }

    /**
     * Set the number of acceptor threads to create. Acceptor threads accept HTTP connections from the client and queue
     * them for processing by client-to-proxy worker threads. The default value is
     * [ServerGroup.DEFAULT_INCOMING_ACCEPTOR_THREADS].
     *
     * @param acceptorThreads number of acceptor threads to create
     * @return this thread pool configuration instance, for chaining
     */
    fun withAcceptorThreads(acceptorThreads: Int): ThreadPoolConfiguration {
        this.acceptorThreads = acceptorThreads
        return this
    }

    fun getProxyToServerWorkerThreads(): Int {
        return proxyToServerWorkerThreads
    }

    /**
     * Set the number of proxy-to-server worker threads to create. Proxy-to-server worker threads make requests to
     * upstream servers and process responses from the server. The default value is
     * [ServerGroup.DEFAULT_OUTGOING_WORKER_THREADS].
     *
     * @param proxyToServerWorkerThreads number of proxy-to-server worker threads to create
     * @return this thread pool configuration instance, for chaining
     */
    fun withProxyToServerWorkerThreads(proxyToServerWorkerThreads: Int): ThreadPoolConfiguration {
        this.proxyToServerWorkerThreads = proxyToServerWorkerThreads
        return this
    }

}
