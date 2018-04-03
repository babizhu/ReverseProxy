package com.bbz.network.reverseproxy.config

object DefaultNetWorkConfig {


    const val TRAFFIC_SHAPING_CHECK_INTERVAL_MS = 250L
    const val MAX_INITIAL_LINE_LENGTH_DEFAULT = 8192
    const val MAX_HEADER_SIZE_DEFAULT = 8192 * 2
    const val MAX_CHUNK_SIZE_DEFAULT = 8192 * 2

    /**
     * 缺省监听的端口
     */
    const val PORT = 8000

    /**
     * Returns the maximum time to wait, in milliseconds, to connect to a server.
     */
    const val CONNECT_TIME_OUT_MS = 40000
    const val IDLE_CONNECTION_TIMEOUT_SECOND = 70
}