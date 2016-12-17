package li.cil.oc.integration.nei

import codechicken.nei.recipe.IUsageHandler
import com.google.common.base.Strings
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.prefab
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.machine.Callbacks
import net.minecraft.item.ItemStack
import net.minecraft.util.text.TextFormatting

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class CallbackDocHandler(pages: Option[Array[String]]) extends PagedUsageHandler(pages) {
  def this() = this(None)

  private val DocPattern = """(?s)^function(\(.*?\).*?) -- (.*)$""".r

  private val VexPattern = """(?s)^function(\(.*?\).*?); (.*)$""".r

  override def getRecipeName = "OpenComputers API"

  override def getUsageHandler(input: String, ingredients: AnyRef*): IUsageHandler = {
    if (input == "item") {
      ingredients.collect {
        case stack: ItemStack if stack.getItem != null =>
          val callbacks = api.Driver.environmentsFor(stack).flatMap(getCallbacks).toBuffer

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
            val pages = mutable.Buffer.empty[String]
            val lastPage = callbacks.toArray.sorted.foldLeft("") {
              (last, doc) =>
                if (last.lines.length + 2 + doc.lines.length > 12) {
                  // We've potentially got some pretty long documentation here, split it up first
                  last.lines.grouped(12).map(_.mkString("\n")).foreach(pages += _)
                  doc
                }
                else if (last.nonEmpty) last + "\n\n" + doc
                else doc
            }
            // The last page may be too long as well.
            lastPage.lines.grouped(12).map(_.mkString("\n")).foreach(pages += _)

            Option(new CallbackDocHandler(Option(pages.toArray)))
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
          wrap(signature, 160).map(TextFormatting.BLACK.toString + _).mkString("\n") +
            TextFormatting.RESET + "\n" +
            wrap(documentation, 152).map("  " + _).mkString("\n")
        }
    }
  }
  else Seq.empty
}
