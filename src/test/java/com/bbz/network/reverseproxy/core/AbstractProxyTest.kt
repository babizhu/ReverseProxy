package com.bbz.network.reverseproxy.core

import com.bbz.network.reverseproxy.ReverseProxyServer
import com.bbz.network.reverseproxy.ReverseProxyServerBootstrap
import org.junit.Before

abstract class AbstractProxyTest {
    protected var proxyServer: ReverseProxyServer? = null
    @Before
    fun runSetUp() {
        setUp()
    }

    abstract fun setUp()

    protected fun bootstrapProxy(): ReverseProxyServerBootstrap {
        return DefaultReverseProxyServer.bootstrap()
//            .bootstrapFromFile("./littleproxy.properties")
                .withPort(0)
//            .withRoutePolice(IpHashPolicy())
                .withConnectTimeout(3000)
    }
}