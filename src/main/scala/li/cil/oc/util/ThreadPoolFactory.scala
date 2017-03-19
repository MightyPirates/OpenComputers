package li.cil.oc.util

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import li.cil.oc.Settings

object ThreadPoolFactory {
  val priority = {
    val custom = Settings.Misc.threadPriority
    if (custom < 1) Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2
    else custom max Thread.MIN_PRIORITY min Thread.MAX_PRIORITY
  }

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
        if (thread.getPriority != priority) {
          thread.setPriority(priority)
        }
        thread
      }
    })
}
