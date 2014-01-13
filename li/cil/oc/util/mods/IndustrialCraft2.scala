package li.cil.oc.util.mods

import cpw.mods.fml.common.Loader
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

  def isMiningLaser(stack: ItemStack) = stack != null && Loader.isModLoaded("IC2") && (miningLaser match {
    case Some(laser) => laser.itemID == stack.itemID
    case _ => false
  })
}
