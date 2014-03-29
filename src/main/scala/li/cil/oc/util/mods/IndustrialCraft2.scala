package li.cil.oc.util.mods

import net.minecraft.item.ItemStack

object IndustrialCraft2 {
  private lazy val miningLaser = try {
    val clazz = Class.forName("ic2.core.Ic2Items")
    val field = clazz.getField("miningLaser")
    Option(field.get(null).asInstanceOf[ItemStack])
  }
  catch {
    case _: Throwable => None
  }

  def isMiningLaser(stack: ItemStack) = stack != null && Mods.IndustrialCraft2.isAvailable && (miningLaser match {
    case Some(laser) => laser.itemID == stack.itemID
    case _ => false
  })
}
