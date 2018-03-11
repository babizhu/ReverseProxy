package com.bbz.network.reverseproxy.impl

import com.bbz.network.reverseproxy.impl.ProxyToServerConnection.Companion.LOG
import io.netty.util.concurrent.Future
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Construct a new [ConnectionFlow] for the given client and server
 * connections.
 *
 * @param clientConnection
 * @param serverConnection
 * @param connectLock
 * an object that's shared by [ConnectionFlow] and
 * [ProxyToServerConnection] and that is used for
 * synchronizing the reader and writer threads that are both
 * involved during the establishing of a connection.
 */
class ConnectionFlow(private val clientConnection: ClientToProxyConnection,
                     private val serverConnection: ProxyToServerConnection,
                     private val connectLock: Any) {
    private val steps = ConcurrentLinkedQueue<ConnectionFlowStep>()


    @Volatile
    private var currentStep: ConnectionFlowStep? = null
    @Volatile
    private var suppressInitialRequest = false


    /**
     * Add a [ConnectionFlowStep] to this flow.
     *
     * @param step
     * @return
     */
    fun then(step: ConnectionFlowStep): ConnectionFlow {
        steps.add(step)
        return this
    }

    /**
     * While we're in the process of connecting, any messages read by the
     * [ProxyToServerConnection] are passed to this method, which passes
     * it on to [ConnectionFlowStep.read] for the
     * current [ConnectionFlowStep].
     *
     * @param msg
     */
    fun read(msg: Any) {

        currentStep?.read(this, msg)
    }

    /**
     * Starts the connection flow, notifying the [ClientToProxyConnection]
     * that we've started.
     */
    fun start() {
        clientConnection.serverConnectionFlowStarted()
        advance()
    }

    /**
     *
     *
     * Advances the flow. [.advance] will be called until we're either
     * out of steps, or a step has failed.
     *
     */
    fun advance() {
        currentStep = steps.poll()
        if (currentStep == null) {
            succeed()
        } else {
            processCurrentStep()
        }
    }

    /**
     *
     *
     * Process the current [ConnectionFlowStep]. With each step, we:
     *
     *
     *
     *  1. Change the state of the associated [ProxyConnection] to the
     * value of [ConnectionFlowStep.getState]
     *  1. Call [ConnectionFlowStep.execute]
     *  1. On completion of the [Future] returned by
     * [ConnectionFlowStep.execute], check the success.
     *  1. If successful, we call back into
     * [ConnectionFlowStep.onSuccess].
     *  1. If unsuccessful, we call [.fail], stopping the connection
     * flow
     *
     */
    private fun processCurrentStep() {

        val connection = currentStep!!.connection
//        val LOG = connection.getLOG()

        LOG.debug("Processing connection flow step: {}", currentStep)

        connection.become(currentStep!!.state)
        suppressInitialRequest = suppressInitialRequest || currentStep!!.shouldSuppressInitialRequest()

        if (currentStep!!.shouldExecuteOnEventLoop()) {
            connection.ctx.executor().submit { doProcessCurrentStep() }
        } else {
            doProcessCurrentStep()
        }
    }

    /**
     * Does the work of processing the current step, checking the result and
     * handling success/failure.
     *
     */
    private fun doProcessCurrentStep() {
        currentStep?.execute()?.addListener(
                { future ->
                    synchronized(connectLock) {
                        if (future.isSuccess) {
                            LOG.debug("ConnectionFlowStep succeeded")
                            currentStep?.onSuccess(this@ConnectionFlow)
                        } else {
                            LOG.debug("ConnectionFlowStep failed", future.cause())
                            fail(future.cause())
                        }
                    }
                })
    }

    /**
     * Called when the flow is complete and successful. Notifies the
     * [ProxyToServerConnection] that we succeeded.
     */
    private fun succeed() {
        synchronized(connectLock) {
            LOG.debug("Connection flow completed successfully: {}", currentStep)
            serverConnection.connectionSucceeded(!suppressInitialRequest)
            notifyThreadsWaitingForConnection()
        }
    }

    /**
     * Called when the flow fails at some [ConnectionFlowStep].
     * Disconnects the [ProxyToServerConnection] and informs the
     * [ClientToProxyConnection] that our connection failed.
     */
    private fun fail(cause: Throwable) {
        val lastStateBeforeFailure = serverConnection.currentState

        serverConnection.disconnect()?.addListener {
            synchronized(connectLock) {
                if (!clientConnection.serverConnectionFailed(
                                serverConnection,
                                lastStateBeforeFailure,
                                cause)) {
                    // the connection to the server failed and we are not retrying, so transition to the
                    // DISCONNECTED state
                    serverConnection.become(ConnectionState.DISCONNECTED)

                    // We are not retrying our connection, let anyone waiting for a connection know that we're done
                    notifyThreadsWaitingForConnection()
                }
            }
        }
    }

    /**
     * Like [.fail] but with no cause.
     */
//    fun fail() {
//        fail(null)
//    }

    /**
     * Once we've finished recording our connection and written our initial
     * request, we can notify anyone who is waiting on the connection that it's
     * okay to proceed.
     */
    private fun notifyThreadsWaitingForConnection() {
//        connectLock.notifyAll()
        (connectLock as java.lang.Object).notifyAll()
    }

}
