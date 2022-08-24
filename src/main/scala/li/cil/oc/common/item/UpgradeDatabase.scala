package li.cil.oc.common.item

import li.cil.oc.CreativeTab
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.inventory.DatabaseInventory
import li.cil.oc.util.Rarity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeItem

class UpgradeDatabase(val tier: Int, props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tooltipName = Option(unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.databaseEntriesPerTier(tier))

  @Deprecated
  override def getRarity(stack: ItemStack) = Rarity.byTier(tier)

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (!player.isCrouching) {
      if (!world.isClientSide) player match {
        case srvPlr: ServerPlayerEntity => ContainerTypes.openDatabaseGui(srvPlr, new DatabaseInventory {
            override def container = stack

            override def stillValid(player: PlayerEntity) = player == srvPlr
          })
        case _ =>
      }
      player.swing(Hand.MAIN_HAND)
    }
    else {
      stack.removeTagKey(Settings.namespace + "items")
      player.swing(Hand.MAIN_HAND)
    }
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }
}
