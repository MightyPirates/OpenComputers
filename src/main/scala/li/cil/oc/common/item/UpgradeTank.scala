package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class UpgradeTank(val parent: Delegator) extends traits.Delegate with traits.ItemTier {
  @OnlyIn(Dist.CLIENT) override
  def tooltipLines(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag): Unit = {
    if (stack.hasTag) {
      FluidStack.loadFluidStackFromNBT(stack.getTag.getCompound(Settings.namespace + "data")) match {
        case stack: FluidStack =>
          tooltip.add(new StringTextComponent(stack.getFluid.getAttributes.getDisplayName(stack).getString + ": " + stack.getAmount + "/16000"))
        case _ =>
      }
    }
    super.tooltipLines(stack, world, tooltip, flag)
  }
}
