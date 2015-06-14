package li.cil.oc.common.item

import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack

class UpgradeTank(val parent: Delegator) extends traits.Delegate with traits.ItemTier {
  @SideOnly(Side.CLIENT) override
  def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) = {
    if (stack.hasTagCompound) {
      FluidStack.loadFluidStackFromNBT(stack.getTagCompound.getCompoundTag(Settings.namespace + "data")) match {
        case stack: FluidStack =>
          tooltip.add(stack.getFluid.getLocalizedName(stack) + ": " + stack.amount + "/16000")
        case _ =>
      }
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }
}
