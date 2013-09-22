package li.cil.oc.api.scala

import li.cil.oc.api.ComponentType
import li.cil.oc.api.{ IItemDriver => IJavaItemDriver }
import net.minecraft.item.ItemStack

trait IItemDriver extends IJavaItemDriver with IDriver {
  def componentType(item: ItemStack): ComponentType

  def component(item: ItemStack): Option[AnyRef]

  // ----------------------------------------------------------------------- //

  def getComponentType(item: ItemStack) = componentType(item)

  def getComponent(item: ItemStack) = component(item).orNull
}