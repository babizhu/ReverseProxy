package com.bbz.network.reverseproxy.core.filter

import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpResponse

interface HttpFilter {

    fun clientToProxyRequest(httpObject: HttpObject): HttpResponse?
}