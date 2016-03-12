package li.cil.oc.integration.nei

import codechicken.nei.recipe.IUsageHandler
import com.google.common.base.Strings
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.prefab
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.machine.Callbacks
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting

import scala.collection.convert.WrapAsScala._

class CallbackDocHandler(pages: Option[Array[String]]) extends PagedUsageHandler(pages) {
  def this() = this(None)

  private val DocPattern = """(?s)^function(\(.*?\).*?) -- (.*)$""".r

  private val VexPattern = """(?s)^function(\(.*?\).*?); (.*)$""".r

  override def getRecipeName = "OpenComputers API"

  override def getUsageHandler(input: String, ingredients: AnyRef*): IUsageHandler = {
    if (input == "item") {
      ingredients.collect {
        case stack: ItemStack if stack.getItem != null =>
          val callbacks = getCallbacks(api.Driver.environmentFor(stack)).toBuffer

          // TODO remove in OC 1.7
          if (callbacks.isEmpty) {
            callbacks ++= (Option(Registry.driverFor(stack)) match {
              case Some(driver: EnvironmentAware) =>
                getCallbacks(driver.providedEnvironment(stack))
              case _ => Registry.blocks.collect {
                case driver: prefab.DriverTileEntity with EnvironmentAware =>
                  if (driver.getTileEntityClass != null && !driver.getTileEntityClass.isInterface)
                    driver.providedEnvironment(stack)
                  else null
                case driver: EnvironmentAware => driver.providedEnvironment(stack)
              }.filter(_ != null).flatMap(getCallbacks)
            })
          }

          if (callbacks.nonEmpty) {
            val fullDocumentation = callbacks.toArray.sorted.mkString("\n\n")
            val pages = fullDocumentation.lines.grouped(12).map(_.mkString("\n")).toArray
            Option(new CallbackDocHandler(Option(pages)))
          }
          else None
      }.collectFirst {
        case Some(handler) => handler
      }.getOrElse(this)
    }
    else this
  }

  private def getCallbacks(env: Class[_]) = if (env != null) {
    Callbacks.fromClass(env).map {
      case (name, callback) =>
        val doc = callback.annotation.doc
        if (Strings.isNullOrEmpty(doc)) name
        else {
          val (signature, documentation) = doc match {
            case DocPattern(head, tail) => (name + head, tail)
            case VexPattern(head, tail) => (name + head, tail)
            case _ => (name, doc)
          }
          wrap(signature, 160).map(EnumChatFormatting.BLACK.toString + _).mkString("\n") +
            EnumChatFormatting.RESET + "\n" +
            wrap(documentation, 152).map("  " + _).mkString("\n")
        }
    }
  }
  else Seq.empty
}
