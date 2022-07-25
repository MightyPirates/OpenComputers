package li.cil.oc.integration.jei

import li.cil.oc.common.EventHandler
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.runtime.IIngredientManager
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsJava.seqAsJavaList
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.convert.WrapAsScala._

object ModJEI {
  var runtime: Option[IJeiRuntime] = None

  var ingredientRegistry: Option[IIngredientManager] = None

  private val disksForRuntime: ArrayBuffer[ItemStack] = mutable.ArrayBuffer.empty

  private var scheduled: Boolean = false

  def addDiskAtRuntime(stack: ItemStack): Unit = ingredientRegistry.foreach { registry =>
    if (!registry.getAllIngredients(VanillaTypes.ITEM).exists(ItemStack.matches(_, stack))) {
      disksForRuntime += stack
      if (!scheduled) {
        EventHandler.scheduleClient { () =>
          ingredientRegistry.foreach(_.addIngredientsAtRuntime(VanillaTypes.ITEM, seqAsJavaList(disksForRuntime)))
          disksForRuntime.clear()
          scheduled = false
        }
        scheduled = true
      }
    }
  }
}
