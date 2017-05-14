package li.cil.oc.integration.util

import li.cil.oc.integration.Mods
import net.minecraft.item.ItemStack

object PortalGun {
  private lazy val portalGunClass = try {
    Class.forName("portalgun.common.item.ItemPortalGun")
  }
  catch {
    case _: Throwable => null
  }

  def isPortalGun(stack: ItemStack) =
    stack != null && stack.stackSize > 0 &&
      Mods.PortalGun.isModAvailable &&
      portalGunClass != null &&
      portalGunClass.isAssignableFrom(stack.getItem.getClass)

  def isStandardPortalGun(stack: ItemStack) = isPortalGun(stack) && stack.getItemDamage == 0
}
