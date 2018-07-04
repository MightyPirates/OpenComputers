package li.cil.oc.integration.jei

import li.cil.oc.common.EventHandler
import mezz.jei.api.IJeiRuntime
import mezz.jei.api.ingredients.IIngredientRegistry
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsJava.seqAsJavaList
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.convert.WrapAsScala._

object ModJEI {
  var runtime: Option[IJeiRuntime] = None

  var ingredientRegistry: Option[IIngredientRegistry] = None

  private val disksForRuntime: ArrayBuffer[ItemStack] = mutable.ArrayBuffer.empty

  private var scheduled: Boolean = false

  def addDiskAtRuntime(stack: ItemStack): Unit = ingredientRegistry.foreach { registry =>
    if (!registry.getIngredients(classOf[ItemStack]).exists(ItemStack.areItemStacksEqual(_, stack))) {
      disksForRuntime += stack
      if (!scheduled) {
        EventHandler.scheduleClient { () =>
          ingredientRegistry.foreach(_.addIngredientsAtRuntime(classOf[ItemStack], seqAsJavaList(disksForRuntime)))
          disksForRuntime.clear()
          scheduled = false
        }
        scheduled = true
      }
    }
  }
}
