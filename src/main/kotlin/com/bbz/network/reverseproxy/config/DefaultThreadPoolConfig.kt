package com.bbz.network.reverseproxy.config

object DefaultThreadPoolConfig {
    /**
     * acceptor concurrent number
     */
    const val ACCEPTOR_THREAD_NUM = 1

    /**
     * 工作线程数量
     */
    val WORKER_THREAD_NUM = Runtime.getRuntime().availableProcessors() * 2

    const val THREAD_NAME = "BigBangReverseProxy"

}