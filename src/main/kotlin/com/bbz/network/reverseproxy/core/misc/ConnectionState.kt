package com.bbz.network.reverseproxy.core.misc

enum class ConnectionState {
    /**
     * 远程服务器连接成功
     */
    ESTABLISHED,
    /**
     * Connection attempting to connect.
     */
    CONNECTING,

    /**
     * In the middle of doing an SSL handshake.
     */
    HANDSHAKING,

    /**
     * In the process of negotiating an HTTP CONNECT from the client.
     */
    NEGOTIATING_CONNECT,

    /**
     * When forwarding a CONNECT to a chained proxy, we await the CONNECTION_OK
     * message from the proxy.
     */
    AWAITING_CONNECT_OK,

    /**
     * Connected but waiting for proxy authentication.
     */
    AWAITING_PROXY_AUTHENTICATION,

    /**
     * Connected and awaiting initial message (e.g. HttpRequest or
     * HttpResponse).
     */
    AWAITING_INITIAL,


    /**
     * Connected and awaiting HttpContent chunk.
     */
    AWAITING_CHUNK,

    /**
     * We've asked the client to disconnect, but it hasn't yet.
     */
//    DISCONNECT_REQUESTED,

    /**
     * Disconnected
     */
    BACKEND_SERVER_DISCONNECTED;

//    private final boolean partOfConnectionFlow;
//
//    ConnectionState(boolean partOfConnectionFlow) {
//        this.partOfConnectionFlow = partOfConnectionFlow;
//    }

//    ConnectionState() {
//        this(false);
//    }

    /**
     * Indicates whether this ConnectionState corresponds to a step in a
     * {@link ConnectionFlow}. This is useful to distinguish so that we know
     * whether or not we're in the process of establishing a connection.
     *
     * @return true if part of connection flow, otherwise false
     */
//    public fun  isPartOfConnectionFlow():Boolean {
//        return partOfConnectionFlow
//    }

    /**
     * Indicates whether this ConnectionState is no longer waiting for messages and is either in the process of disconnecting
     * or is already disconnected.
     *
     * @return true if the connection state is {@link #DISCONNECT_REQUESTED} or {@link #BACKEND_SERVER_DISCONNECTED}, otherwise false
     */
//    fun isDisconnectingOrDisconnected():Boolean {
//        return this == DISCONNECT_REQUESTED || this == BACKEND_SERVER_DISCONNECTED;
//    }
//
//    fun isPartOfConnectionFlow(): Boolean {
//        return partOfConnectionFlow
//
//    }
}
