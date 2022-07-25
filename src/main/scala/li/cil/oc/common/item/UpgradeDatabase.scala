package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.util.Rarity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.world.World

class UpgradeDatabase(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.databaseEntriesPerTier(tier))

  override def rarity(stack: ItemStack) = Rarity.byTier(tier)

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (!player.isCrouching) {
      OpenComputers.openGui(player, GuiType.Database.id, world, 0, 0, 0)
      player.swing(Hand.MAIN_HAND)
    }
    else {
      stack.removeTagKey(Settings.namespace + "items")
      player.swing(Hand.MAIN_HAND)
    }
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }
}
