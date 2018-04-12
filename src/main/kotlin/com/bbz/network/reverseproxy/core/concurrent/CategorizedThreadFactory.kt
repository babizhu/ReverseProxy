package com.bbz.network.reverseproxy.core.concurrent

import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

@Suppress("PrivatePropertyName")
/**
 * @param name the user-supplied name of this proxy
 * @param category the type of threads this factory is creating (acceptor, proxy worker)
 * @param uniqueServerGroupId a unique number for the server group creating this concurrent factory, to differentiate multiple proxy instances with the same name
 */
class CategorizedThreadFactory(private val name: String, private val category: String, private val uniqueServerGroupId: Int) : ThreadFactory {
    companion object {

        private val log = LoggerFactory.getLogger(CategorizedThreadFactory::class.java)
        private val UNCAUGHT_EXCEPTION_HANDLER: (Thread, Throwable) -> Unit =
                { t, e -> log.error("Uncaught throwable in concurrent: {}", t.name, e) }

    }

    private val threadCount = AtomicInteger(0)

    /**
     * Exception handler for proxy threads. Logs the name of the concurrent and the exception that was caught.
     */
    override fun newThread(r: Runnable): Thread {
        val t = Thread(r, name + "-" + uniqueServerGroupId + "-" + category + "-" + threadCount.getAndIncrement())

        t.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER)

        return t
    }

}
