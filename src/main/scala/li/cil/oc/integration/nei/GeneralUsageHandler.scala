package li.cil.oc.integration.nei

import codechicken.nei.recipe.IUsageHandler
import li.cil.oc.Localization
import li.cil.oc.api
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsScala._

class GeneralUsageHandler(pages: Option[Array[String]]) extends PagedUsageHandler(pages) {
  def this() = this(None)

  override def getRecipeName = "Manual"

  override def getUsageHandler(input: String, ingredients: AnyRef*): IUsageHandler = {
    if (input == "item") {
      ingredients.collectFirst {
        case stack: ItemStack if api.Items.get(stack) != null && Localization.canLocalize(usageKey(stack)) =>
          val fullDocumentation = wrap(Localization.localizeImmediately(usageKey(stack)).replaceAllLiterally("[nl]", "\n"), 160).mkString("\n")
          val pages = fullDocumentation.lines.grouped(12).map(_.mkString("\n")).toArray
          new GeneralUsageHandler(Option(pages))
      }.getOrElse(this)
    }
    else this
  }

  private def usageKey(stack: ItemStack) = stack.getUnlocalizedName.stripSuffix(".name").replaceFirst("""\d+$""", "") + ".usage"
}
