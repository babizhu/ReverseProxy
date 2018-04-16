package com.bbz.network.reverseproxy.utils

import com.bbz.network.reverseproxy.ReverseProxyServer
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.charset.Charset

object SocketClientUtil {

    @Throws(IOException::class)

    fun writeStringToSocket(msg: String, socket: Socket) {
        val out = socket.getOutputStream()
        out.write(msg.toByteArray(Charset.forName("UTF-8")))
        out.flush()
    }

    /**
     * Reads all available data from the socket and returns a String containing that content, interpreted in the
     * UTF-8 charset.
     *
     * @param socket socket to read UTF-8 bytes from
     * @return String containing the contents of whatever was read from the socket
     * @throws EOFException if the socket has been closed
     */
    @Throws(IOException::class)
    fun readStringFromSocket(socket: Socket): String {
        val inputStream = socket.getInputStream()
        val bytes = ByteArray(10000)
        val bytesRead = inputStream.read(bytes)
        if (bytesRead == -1) {
            throw EOFException("Unable to read from socket. The socket is closed.")
        }

        return String(bytes, 0, bytesRead, Charset.forName("UTF-8"))
    }

    /**
     * Determines if the socket can be written to. This method tests the writability of the socket by writing to the socket,
     * so it should only be used immediately before closing the socket.
     *
     * @param socket socket to test
     * @return true if the socket is open and can be written to, otherwise false
     * @throws IOException
     */
    @Throws(IOException::class)
    fun isSocketReadyToWrite(socket: Socket): Boolean {
        val out = socket.getOutputStream()
        try {
            repeat(500) {
                out.write(0)
                out.flush()
            }
        } catch (e: SocketException) {
            return false
        }

        return true
    }

    /**
     * Determines if the socket can be read from. This method tests the readability of the socket by attempting to read
     * a byte from the socket. If successful, the byte will be lost, so this method should only be called immediately
     * before closing the socket.
     *
     * @param socket socket to test
     * @return true if the socket is open and can be read from, otherwise false
     * @throws IOException
     */
    fun isSocketReadyToRead(socket: Socket): Boolean {
        val inputStream = socket.getInputStream()
        return try {
            val readByte = inputStream.read()

            // we just lost that byte but it doesn't really matter for testing purposes
            readByte != -1
        } catch (e: SocketException) {
            // the socket couldn't be read, perhaps because the connection was reset or some other error. it cannot be read.
            false
        } catch (e: SocketTimeoutException) {
            // the read timed out, which means the socket is still connected but there's no data on it
            true
        }

    }

    /**
     * Opens a socket to the specified proxy server with a 3s timeout. The socket should be closed after it has been used.
     *
     * @param proxyServer proxy server to open the socket to
     * @return the new socket
     * @throws IOException
     */
    @Throws(IOException::class)
    fun getSocketToProxyServer(proxyServer: ReverseProxyServer): Socket {
        val socket = Socket()
        socket.connect(InetSocketAddress("localhost", proxyServer.getListenAddress().port), 1000)
        socket.soTimeout = 300000
        return socket
    }
}
