package li.cil.oc.server.fs

import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import li.cil.oc.OpenComputers
import li.cil.oc.util.ThreadPoolFactory

object BufferedFileSaveHandler {

  private var _threadPool: ScheduledExecutorService = _

  private def withPool(f: ScheduledExecutorService => Future[_], requiresPool: Boolean = true): Option[Future[_]] = {
    if (_threadPool == null) {
      OpenComputers.log.warn("Error handling file saving: Did the server never start?")
      if (requiresPool) {
        OpenComputers.log.warn("Creating new thread pool.")
        newThreadPool()
      } else {
        return None
      }
    } else if (_threadPool.isShutdown || _threadPool.isTerminated) {
      OpenComputers.log.warn("Error handling file saving: Thread pool shut down!")
      if (requiresPool) {
        OpenComputers.log.warn("Creating new thread pool.")
        newThreadPool()
      } else {
        return None
      }
    }
    Option(f(_threadPool))
  }

  def newThreadPool(): Unit = {
    if (_threadPool != null && !_threadPool.isTerminated) {
      _threadPool.shutdownNow()
    }
    _threadPool = ThreadPoolFactory.create("FileSystem", 1)
  }

  def scheduleSave(fs: Buffered): Option[Future[_]] = withPool(threadPool => threadPool.submit(new Runnable {
    override def run(): Unit = fs.saveFiles()
  }))

  def waitForSaving(): Unit = withPool(threadPool => {
    try {
      threadPool.shutdown()
      var terminated = threadPool.awaitTermination(15, TimeUnit.SECONDS)
      if (!terminated) {
        OpenComputers.log.warn("Warning: Saving the filesystem has already taken 15 seconds!")
        terminated = threadPool.awaitTermination(105, TimeUnit.SECONDS)
        if (!terminated) {
          OpenComputers.log.error("Warning: Saving the filesystem has already taken two minutes! Aborting")
          threadPool.shutdownNow()
        }
      }
    } catch {
      case e: InterruptedException => e.printStackTrace()
    }
    null
  }, requiresPool = false)
}
