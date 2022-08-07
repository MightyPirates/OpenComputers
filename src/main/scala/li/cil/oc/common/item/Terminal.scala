package li.cil.oc.common.item

import java.util

import li.cil.oc.Constants
import li.cil.oc.CreativeTab
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.model.ModelBakery
import net.minecraft.client.renderer.model.ModelResourceLocation
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.extensions.IForgeItem

class Terminal(props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem with CustomModel {
  override def maxStackSize = 1

  def hasServer(stack: ItemStack) = stack.hasTag && stack.getTag.contains(Settings.namespace + "server")

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    super.appendHoverText(stack, world, tooltip, flag)
    if (hasServer(stack)) {
      val server = stack.getTag.getString(Settings.namespace + "server")
      tooltip.add(new StringTextComponent("§8" + server.substring(0, 13) + "...§7"))
    }
  }

  @OnlyIn(Dist.CLIENT)
  private def modelLocationFromState(running: Boolean) = {
    new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.ItemName.Terminal + (if (running) "_on" else "_off"), "inventory")
  }

  @OnlyIn(Dist.CLIENT)
  override def getModelLocation(stack: ItemStack): ModelResourceLocation = {
    modelLocationFromState(hasServer(stack))
  }

  @OnlyIn(Dist.CLIENT)
  override def registerModelLocations(): Unit = {
    for (state <- Seq(true, false)) {
      ModelLoader.addSpecialModel(modelLocationFromState(state))
    }
  }

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (!player.isCrouching && stack.hasTag) {
      val key = stack.getTag.getString(Settings.namespace + "key")
      val server = stack.getTag.getString(Settings.namespace + "server")
      if (key != null && !key.isEmpty && server != null && !server.isEmpty) {
        if (world.isClientSide) {
          OpenComputers.openGui(player, GuiType.Terminal.id, world, 0, 0, 0)
        }
        player.swing(Hand.MAIN_HAND)
      }
    }
    super.use(stack, world, player)
  }
}
