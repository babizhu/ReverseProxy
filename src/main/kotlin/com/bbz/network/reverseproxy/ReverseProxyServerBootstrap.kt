package com.bbz.network.reverseproxy

import com.bbz.network.reverseproxy.impl.ThreadPoolConfiguration
import java.net.InetSocketAddress

interface ReverseProxyServerBootstrap {

    /**
     *
     *
     * Give the server a name (used for naming threads, useful for logging).
     *
     *
     *
     * @param name
     * @return
     */
    fun withName(name: String): ReverseProxyServerBootstrap
    
    /**
     *
     *
     * Listen for incoming connections on the given address.
     *
     *
     *
     *
     * Default = [bound ip]:8080
     *
     *
     * @param address
     * @return
     */
    fun withListenAddress(address: InetSocketAddress): ReverseProxyServerBootstrap

    /**
     *
     *
     * Listen for incoming connections on the given port.
     *
     *
     *
     *
     * Default = 8080
     *
     *
     * @param port
     * @return
     */
    fun withPort(port: Int): ReverseProxyServerBootstrap

    
    
    /**
     *
     *
     * Specify an [SslEngineSource] to use for encrypting inbound
     * connections. Enabling this will enable SSL client authentication
     * by default (see [.withAuthenticateSslClients])
     *
     *
     *
     *
     * Default = null
     *
     *
     *
     *
     * Note - This and [.withManInTheMiddle] are
     * mutually exclusive.
     *
     *
     * @param sslEngineSource
     * @return
     */
//    fun withSslEngineSource(
//            sslEngineSource: SslEngineSource): ReverseProxyServerBootstrap

    /**
     *
     *
     * Specify whether or not to authenticate inbound SSL clients (only applies
     * if [.withSslEngineSource] has been set).
     *
     *
     *
     *
     * Default = true
     *
     *
     * @param authenticateSslClients
     * @return
     */
//    fun withAuthenticateSslClients(
//            authenticateSslClients: Boolean): ReverseProxyServerBootstrap

    /**
     *
     *
//     * Specify a [ProxyAuthenticator] to use for doing basic HTTP
     * authentication of clients.
     *
     *
     *
     *
     * Default = null
     *
     *
     * @param proxyAuthenticator
     * @return
     */
//    fun withProxyAuthenticator(
//            proxyAuthenticator: ProxyAuthenticator): ReverseProxyServerBootstrap

    /**
     *
     *
//     * Specify a [ChainedProxyManager] to use for chaining requests to
     * another proxy.
     *
     *
     *
     *
     * Default = null
     *
     *
     * @param chainProxyManager
     * @return
     */
//    fun withChainProxyManager(
//            chainProxyManager: ChainedProxyManager): ReverseProxyServerBootstrap

    /**
     *
     *
//     * Specify an [MitmManager] to use for making this proxy act as an SSL
     * man in the middle
     *
     *
     *
     *
     * Default = null
     *
     *
     *
     *
     * Note - This and [.withSslEngineSource] are
     * mutually exclusive.
     *
     *
     * @param mitmManager
     * @return
     */
//    fun withManInTheMiddle(
//            mitmManager: MitmManager): ReverseProxyServerBootstrap

    /**
     *
     *
     * Specify a [HttpFiltersSource] to use for filtering requests and/or
     * responses through this proxy.
     *
     *
     *
     *
     * Default = null
     *
     *
     * @param filtersSource
     * @return
     */
//    fun withFiltersSource(
//            filtersSource: HttpFiltersSource): ReverseProxyServerBootstrap

    /**
     *
     *
     * Specify whether or not to use secure DNS lookups for outbound
     * connections.
     *
     *
     *
     *
     * Default = false
     *
     *
     * @param useDnsSec
     * @return
     */
//    fun withUseDnsSec(
//            useDnsSec: Boolean): ReverseProxyServerBootstrap

    /**
     *
     *
     * Specify whether or not to run this proxy as a transparent proxy.
     *
     *
     *
     *
     * Default = false
     *
     *
     * @param transparent
     * @return
     */
//    fun withTransparent(
//            transparent: Boolean): ReverseProxyServerBootstrap

    /**
     *
     *
     * Specify the timeout after which to disconnect idle connections, in
     * seconds.
     *
     *
     *
     *
     * Default = 70
     *
     *
     * @param idleConnectionTimeout
     * @return
     */
    fun withIdleConnectionTimeout(
            idleConnectionTimeout: Int): ReverseProxyServerBootstrap

    /**
     *
     *
     * Specify the timeout for connecting to the upstream server on a new
     * connection, in milliseconds.
     *
     *
     *
     *
     * Default = 40000
     *
     *
     * @param connectTimeout
     * @return
     */
    fun withConnectTimeout(
            connectTimeout: Int): ReverseProxyServerBootstrap

    /**
     * Specify a custom [HostResolver] for resolving server addresses.
     *
     * @param serverResolver
     * @return
     */
//    fun withServerResolver(serverResolver: HostResolver): ReverseProxyServerBootstrap

    /**
     *
     *
     * Add an [ActivityTracker] for tracking activity in this proxy.
     *
     *
     * @param activityTracker
     * @return
     */
//    fun plusActivityTracker(activityTracker: ActivityTracker): ReverseProxyServerBootstrap

    /**
     *
     *
     * Specify the read and/or write bandwidth throttles for this proxy server. 0 indicates not throttling.
     *
     * @param readThrottleBytesPerSecond
     * @param writeThrottleBytesPerSecond
     * @return
     */
    fun withThrottling(readThrottleBytesPerSecond: Long, writeThrottleBytesPerSecond: Long): ReverseProxyServerBootstrap

    /**
     * All outgoing-communication of the proxy-instance is goin' to be routed via the given network-interface
     *
     * @param inetSocketAddress to be used for outgoing communication
     */
//    fun withNetworkInterface(inetSocketAddress: InetSocketAddress): ReverseProxyServerBootstrap

    fun withMaxInitialLineLength(maxInitialLineLength: Int): ReverseProxyServerBootstrap

    fun withMaxHeaderSize(maxHeaderSize: Int): ReverseProxyServerBootstrap

    fun withMaxChunkSize(maxChunkSize: Int): ReverseProxyServerBootstrap

    /**
     * When true, the proxy will accept requests that appear to be directed at an origin server (i.e. the URI in the HTTP
     * request will contain an origin-form, rather than an absolute-form, as specified in RFC 7230, section 5.3).
     * This is useful when the proxy is acting as a gateway/reverse proxy. **Note:** This feature should not be
     * enabled when running as a forward proxy; doing so may cause an infinite loop if the client requests the URI of the proxy.
     *
     * @param allowRequestToOriginServer when true, the proxy will accept origin-form HTTP requests
     */
//    fun withAllowRequestToOriginServer(allowRequestToOriginServer: Boolean): ReverseProxyServerBootstrap

    /**
     * Sets the alias to use when adding Via headers to incoming and outgoing HTTP messages. The alias may be any
     * pseudonym, or if not specified, defaults to the hostname of the local machine. See RFC 7230, section 5.7.1.
     *
     * @param alias the pseudonym to add to Via headers
     */
    fun withProxyAlias(alias: String): ReverseProxyServerBootstrap

    /**
     *
     *
     * Build and starts the server.
     *
     *
     * @return the newly built and started server
     */
    fun start(): ReverseProxyServer

    /**
     * Set the configuration parameters for the proxy's thread pools.
     *
     * @param configuration thread pool configuration
     * @return proxy server bootstrap for chaining
     */
    fun withThreadPoolConfiguration(configuration: ThreadPoolConfiguration): ReverseProxyServerBootstrap
}