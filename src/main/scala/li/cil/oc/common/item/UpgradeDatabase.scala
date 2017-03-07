package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.util.RarityUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.World

class UpgradeDatabase(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.databaseEntriesPerTier(tier))

  override def rarity(stack: ItemStack) = RarityUtils.fromTier(tier)

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    if (!player.isSneaking) {
      player.openGui(OpenComputers, GuiType.Database.id, world, 0, 0, 0)
      player.swingArm(EnumHand.MAIN_HAND)
    }
    else if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "items")) {
      stack.setTagCompound(null)
      player.swingArm(EnumHand.MAIN_HAND)
    }
    ActionResult.newResult(EnumActionResult.SUCCESS, stack)
  }
}
