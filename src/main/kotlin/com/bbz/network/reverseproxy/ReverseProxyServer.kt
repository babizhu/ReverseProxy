package com.bbz.network.reverseproxy

import java.net.InetSocketAddress

interface ReverseProxyServer {
    fun getIdleConnectionTimeout(): Int

    fun setIdleConnectionTimeout(idleConnectionTimeout: Int)

    /**
     * Returns the maximum time to wait, in milliseconds, to connect to a server.
     */
    fun getConnectTimeout(): Int

    /**
     * Sets the maximum time to wait, in milliseconds, to connect to a server.
     */
    fun setConnectTimeout(connectTimeoutMs: Int)

    /**
     *
     *
     * Clone the existing server, with a port 1 higher and everything else the
     * same. If the proxy was started with port 0 (JVM-assigned port), the cloned proxy will also use a JVM-assigned
     * port.
     *
     *
     *
     *
     * The new server will share event loops with the original server. The event
     * loops will use whatever name was given to the first server in the clone
     * group. The server group will not terminate until the original server and all clones terminate.
     *
     *
     * @return a bootstrap that allows customizing and starting the cloned
     * server
     */
//     fun clone(): ReverseProxyServerBootstrap

    /**
     * Stops the server and all related clones. Waits for traffic to stop before shutting down.
     */
    fun stop()

    /**
     * Stops the server and all related clones immediately, without waiting for traffic to stop.
     */
    fun abort()

    /**
     * Return the address on which this proxy is listening.
     *
     * @return
     */
    fun getListenAddress(): InetSocketAddress

    /**
     *
     *
     * Set the read/write throttle bandwidths (in bytes/second) for this proxy.
     *
     * @param readThrottleBytesPerSecond
     * @param writeThrottleBytesPerSecond
     */
    fun setThrottle(readThrottleBytesPerSecond: Long, writeThrottleBytesPerSecond: Long)
}
