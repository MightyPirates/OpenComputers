package li.cil.oc.util

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ThreadFactory, Executors}

object ThreadPoolFactory {
  def create(name: String, threads: Int) = Executors.newScheduledThreadPool(threads,
    new ThreadFactory() {
      private val baseName = "OpenComputers-" + name + "-"

      private val threadNumber = new AtomicInteger(1)

      private val group = System.getSecurityManager match {
        case null => Thread.currentThread().getThreadGroup
        case s => s.getThreadGroup
      }

      def newThread(r: Runnable) = {
        val thread = new Thread(group, r, baseName + threadNumber.getAndIncrement)
        if (!thread.isDaemon) {
          thread.setDaemon(true)
        }
        if (thread.getPriority != Thread.MIN_PRIORITY) {
          thread.setPriority(Thread.MIN_PRIORITY)
        }
        thread
      }
    })
}
