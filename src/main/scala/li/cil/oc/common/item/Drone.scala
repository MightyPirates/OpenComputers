package li.cil.oc.common.item

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.renderer.block.DroneModel
import li.cil.oc.common.init.Items
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.common.entity
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.server.agent
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.model.ModelResourceLocation
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.NonNullList
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.common.extensions.IForgeItem
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class Drone(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem with CustomModel {
  ItemBlacklist.hide(this)

  @OnlyIn(Dist.CLIENT)
  override def getModelLocation(stack: ItemStack) = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.ItemName.Drone, "inventory")

  @OnlyIn(Dist.CLIENT)
  override def bakeModels(bakeEvent: ModelBakeEvent): Unit = {
    bakeEvent.getModelRegistry.put(getModelLocation(createItemStack()), DroneModel)
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[ITextComponent]): Unit = {
    if (KeyBindings.showExtendedTooltips) {
      val info = new DroneData(stack)
      for (component <- info.components if !component.isEmpty) {
        tooltip.add(new StringTextComponent("- " + component.getHoverName.getString).setStyle(Tooltip.DefaultStyle))
      }
    }
  }

  @Deprecated
  override def getRarity(stack: ItemStack) = {
    val data = new DroneData(stack)
    Rarity.byTier(data.tier)
  }

  // Must be assembled to be usable so we hide it in the item list.
  override def fillItemCategory(tab: ItemGroup, list: NonNullList[ItemStack]) {}

  override def onItemUse(stack: ItemStack, player: PlayerEntity, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = {
    val world = position.world.get
    if (!world.isClientSide) {
      val drone = entity.EntityTypes.DRONE.create(world)
      player match {
        case fakePlayer: agent.Player =>
          drone.ownerName = fakePlayer.agent.ownerName
          drone.ownerUUID = fakePlayer.agent.ownerUUID
        case _ =>
          drone.ownerName = player.getName.getString
          drone.ownerUUID = player.getGameProfile.getId
      }
      drone.initializeAfterPlacement(stack, player, position.offset(hitX * 1.1f, hitY * 1.1f, hitZ * 1.1f))
      world.addFreshEntity(drone)
    }
    stack.shrink(1)
    true
  }
}
