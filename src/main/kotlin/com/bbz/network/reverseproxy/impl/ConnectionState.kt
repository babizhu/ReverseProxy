package com.bbz.network.reverseproxy.impl

enum class ConnectionState(private val partOfConnectionFlow: Boolean) {
    /**
     * Connection attempting to connect.
     */
    CONNECTING(true),

    /**
     * In the middle of doing an SSL handshake.
     */
    HANDSHAKING(true),

    /**
     * In the process of negotiating an HTTP CONNECT from the client.
     */
    NEGOTIATING_CONNECT(true),

    /**
     * When forwarding a CONNECT to a chained proxy, we await the CONNECTION_OK
     * message from the proxy.
     */
    AWAITING_CONNECT_OK(true),

    /**
     * Connected but waiting for proxy authentication.
     */
    AWAITING_PROXY_AUTHENTICATION(false),

    /**
     * Connected and awaiting initial message (e.g. HttpRequest or
     * HttpResponse).
     */
    AWAITING_INITIAL(false),

    /**
     * Connected and awaiting HttpContent chunk.
     */
    AWAITING_CHUNK(false),

    /**
     * We've asked the client to disconnect, but it hasn't yet.
     */
    DISCONNECT_REQUESTED(false),

    /**
     * Disconnected
     */
    DISCONNECTED(false);

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
     * @return true if the connection state is {@link #DISCONNECT_REQUESTED} or {@link #DISCONNECTED}, otherwise false
     */
    fun isDisconnectingOrDisconnected():Boolean {
        return this == DISCONNECT_REQUESTED || this == DISCONNECTED;
    }

    fun isPartOfConnectionFlow(): Boolean {
        return partOfConnectionFlow

    }
}
