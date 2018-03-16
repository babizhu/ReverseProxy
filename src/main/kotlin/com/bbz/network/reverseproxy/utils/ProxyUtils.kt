@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.bbz.network.reverseproxy.utils

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object ProxyUtils {
    private val LOG = LoggerFactory.getLogger(ProxyUtils::class.java)
    private val HTTP_PREFIX = Pattern.compile("^https?://.*", Pattern.CASE_INSENSITIVE)
    fun getHostName(): String? {
        try {
            return InetAddress.getLocalHost().hostName
        } catch (e: IOException) {
            LOG.debug("Ignored exception", e)
        } catch (e: RuntimeException) {
            // An exception here must not stop the proxy. Android could throw a
            // runtime exception, since it not allows network access in the main
            // process.
            LOG.debug("Ignored exception", e)
        }

        LOG.info("Could not lookup localhost")
        return null
    }

    fun isLastChunk(chunk: HttpObject): Boolean {
        return chunk is LastHttpContent

    }
    fun isChunked(httpRequest: HttpObject): Boolean {
        return !isLastChunk(httpRequest)

    }
    fun createFullHttpResponse(httpVersion: HttpVersion,
                               status: HttpResponseStatus,
                               body: String): FullHttpResponse {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val content = Unpooled.copiedBuffer(bytes)

        return createFullHttpResponse(httpVersion, status, "text/html; charset=utf-8", content, bytes.size)
    }

    /**
     * Creates a new [FullHttpResponse] with the specified body.
     *
     * @param httpVersion HTTP version of the response
     * @param status HTTP status code
     * @param contentType the Content-Type of the body
     * @param body body to include in the FullHttpResponse; if null
     * @param contentLength number of bytes to send in the Content-Length header; should equal the number of bytes in the ByteBuf
     * @return new http response object
     */
    private fun createFullHttpResponse(httpVersion: HttpVersion,
                               status: HttpResponseStatus,
                               contentType: String?,
                               body: ByteBuf?,
                               contentLength: Int): FullHttpResponse {
        val response: DefaultFullHttpResponse

        if (body != null) {
            response = DefaultFullHttpResponse(httpVersion, status, body)
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength)
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType)
        } else {
            response = DefaultFullHttpResponse(httpVersion, status)
        }

        return response
    }

    fun parseHostAndPort(httpRequest: HttpRequest): String {
        return parseHostAndPort(httpRequest.uri())
    }

    /**
     * Parses the host and port an HTTP request is being sent to.
     *
     * @param uri
     * The URI.
     * @return The host and port string.
     */
    fun parseHostAndPort(uri: String): String {
        val tempUri: String = if (!HTTP_PREFIX.matcher(uri).matches()) {
            // Browsers particularly seem to send requests in this form when
            // they use CONNECT.
            uri
        } else {
            // We can't just take a substring from a hard-coded index because it
            // could be either http or https.
            StringUtils.substringAfter(uri, "://")


        }
        val hostAndPort: String
        hostAndPort = if (tempUri.contains("/")) {
            tempUri.substring(0, tempUri.indexOf("/"))
        } else {
            tempUri
        }
        return hostAndPort
    }



    fun copyMutableResponse(original: HttpResponse): HttpResponse {
        val copy: HttpResponse?
        copy = if (original is DefaultFullHttpResponse) {
            val content = original.content()
            DefaultFullHttpResponse(original.protocolVersion(),
                    original.status(), content)
        } else {
            DefaultHttpResponse(original.protocolVersion(),
                    original.status())
        }
        val headerNames = original.headers().names()
        for (name in headerNames) {
            val values = original.headers().getAll(name)
            copy.headers().set(name, values)
        }
        return copy
    }
}