package com.bbz.network.reverseproxy.core

import com.bbz.network.reverseproxy.config.DefaultThreadPoolConfig

class ThreadPoolConfiguration {
    private var acceptorThreadsNum = DefaultThreadPoolConfig.ACCEPTOR_THREAD_NUM
    private var workerThreadsNum = DefaultThreadPoolConfig.WORKER_THREAD_NUM

    fun workerThreadsNum(): Int {
        return workerThreadsNum
    }

    /**
     * Set the number of client-to-proxy worker threads to create. Worker threads perform the actual processing of
     * client requests. The default value is [DefaultThreadPoolConfig.ACCEPTOR_THREAD_NUM].
     *
     * @param workThreadsNum number of client-to-proxy worker threads to create
     * @return this thread pool configuration instance, for chaining
     */
    fun withWorkerThreadsNum(workThreadsNum: Int): ThreadPoolConfiguration {
        this.workerThreadsNum = workThreadsNum
        return this
    }

    fun getWorkerThreadsNum():Int {
        return  workerThreadsNum
    }

    /**
     * Set the number of acceptor threads to create. Acceptor threads accept HTTP connections from the client and queue
     * them for processing by client-to-proxy worker threads. The default value is
     * [DefaultThreadPoolConfig.ACCEPTOR_THREAD_NUM].
     *
     * @param acceptorThreadsNum number of acceptor threads to create
     * @return this thread pool configuration instance, for chaining
     */
    fun withAcceptorThreadsNum(acceptorThreadsNum: Int): ThreadPoolConfiguration {
        this.acceptorThreadsNum = acceptorThreadsNum
        return this
    }


    fun getAcceptorThreadsNum():Int {
        return  acceptorThreadsNum
    }

}
