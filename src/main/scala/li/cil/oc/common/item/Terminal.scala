package li.cil.oc.common.item

import java.util

import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.gui
import li.cil.oc.common.component
import li.cil.oc.common.tileentity.traits.TileEntity
import li.cil.oc.util.Tooltip
import net.minecraft.client.Minecraft
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

class Terminal(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem with CustomModel {
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
          if (stack.hasTag) {
            val address = stack.getTag.getString(Settings.namespace + "server")
            val key = stack.getTag.getString(Settings.namespace + "key")
            if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(address)) {
              component.TerminalServer.loaded.find(address) match {
                case Some(term) if term != null && term.rack != null => term.rack match {
                  case rack: TileEntity with api.internal.Rack => {
                    def inRange = player.isAlive && !rack.isRemoved && player.distanceToSqr(rack.x + 0.5, rack.y + 0.5, rack.z + 0.5) < term.range * term.range
                    if (inRange) {
                      if (term.sidedKeys.contains(key)) showGui(stack, key, term, () => inRange)
                      else player.displayClientMessage(Localization.Terminal.InvalidKey, true)
                    }
                    else player.displayClientMessage(Localization.Terminal.OutOfRange, true)
                  }
                  case _ => // Eh?
                }
                case _ => player.displayClientMessage(Localization.Terminal.OutOfRange, true)
              }
            }
          }
        }
        player.swing(Hand.MAIN_HAND)
      }
    }
    super.use(stack, world, player)
  }

  @OnlyIn(Dist.CLIENT)
  private def showGui(stack: ItemStack, key: String, term: component.TerminalServer, inRange: () => Boolean) {
    Minecraft.getInstance.pushGuiLayer(new gui.Screen(term.buffer, true, () => true, () => {
      // Check if someone else bound a term to our server.
      if (stack.getTag.getString(Settings.namespace + "key") != key) Minecraft.getInstance.popGuiLayer
      // Check whether we're still in range.
      if (!inRange()) Minecraft.getInstance.popGuiLayer
      true
    }))
  }
}
