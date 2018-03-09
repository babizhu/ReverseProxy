package com.bbz.network.reverseproxy.impl

import io.netty.util.concurrent.Future

abstract class  ConnectionFlowStep(
        val connection: ProxyConnection<*>,
        val state: ConnectionState) {


    /**
     * Indicates whether or not to suppress the initial request. Defaults to
     * false, can be overridden.
     *
     * @return
     */
    open fun shouldSuppressInitialRequest(): Boolean {
        return false
    }

    /**
     *
     *
     * Indicates whether or not this step should be executed on the channel's
     * event loop. Defaults to true, can be overridden.
     *
     * If this step modifies the pipeline, for example by adding/removing
     * handlers, it's best to make it execute on the event loop.
     *
     * @return
     */
    open fun shouldExecuteOnEventLoop(): Boolean {
        return true
    }

    /**
     * Implement this method to actually do the work involved in this step of
     * the flow.
     *
     * @return
     */
    internal abstract fun execute(): Future<*>

    /**
     * When the flow determines that this step was successful, it calls into
     * this method. The default implementation simply continues with the flow.
     * Other implementations may choose to not continue and instead wait for a
     * message or something like that.
     *
     * @param flow
     */
    fun onSuccess(flow: ConnectionFlow) {
        flow.advance()
    }

    /**
     *
     *
     * Any messages that are read from the underlying connection while we're at
     * this step of the connection flow are passed to this method.
     *
     *
     *
     *
     * The default implementation ignores the message and logs this, since we
     * weren't really expecting a message here.
     *
     *
     *
     *
     * Some [ConnectionFlowStep]s do need to read the messages, so they
     * override this method as appropriate.
     *
     *
     * @param flow
     * our [ConnectionFlow]
     * @param msg
     * the message read from the underlying connection
     */
    fun read(flow: ConnectionFlow, msg: Any) {
//        LOG.debug("Received message while in the middle of connecting: {}", msg)
    }

    override fun toString(): String {
        return state.toString()
    }

}
