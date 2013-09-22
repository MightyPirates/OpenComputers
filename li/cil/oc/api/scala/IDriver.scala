package li.cil.oc.api.scala

import java.io.InputStream
import li.cil.oc.api.{ IDriver => IJavaDriver }
import li.cil.oc.api.{ IComputerContext => IJavaComputerContext }

trait IDriver extends IJavaDriver {
  def componentName: String

  def apiName: Option[String] = None

  def apiCode: Option[InputStream] = None

  def onInstall(computer: IComputerContext, component: AnyRef) {}

  def onUninstall(computer: IComputerContext, component: AnyRef) {}

  // ----------------------------------------------------------------------- //

  def getComponentName = componentName

  def getApiName = apiName.orNull

  def getApiCode = apiCode.orNull

  def onInstall(computer: IJavaComputerContext, component: Object) =
    onInstall(wrapIJavaComputerContext(computer), component)

  def onUninstall(computer: IJavaComputerContext, component: Object) =
    onUninstall(wrapIJavaComputerContext(computer), component)

  private def wrapIJavaComputerContext(c: IJavaComputerContext) =
    new IComputerContext {
      def world = c.getWorld

      def signal(name: String, args: Any*) =
        c.signal(name, args.map(_.asInstanceOf[AnyRef]).toArray)

      def component[T <: AnyRef](id: Int) = c.getComponent[T](id)
    }
}