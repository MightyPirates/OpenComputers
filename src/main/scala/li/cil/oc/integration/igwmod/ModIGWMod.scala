package li.cil.oc.integration.igwmod

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import igwmod.api.BlockWikiEvent
import igwmod.api.ItemWikiEvent
import li.cil.oc.{api, OpenComputers}
import li.cil.oc.common.GuiType
import li.cil.oc.common.block._
import li.cil.oc.common.item._
import li.cil.oc.OpenComputers.log
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge

object ModIGWMod {
  def init() {
    log.info(s"IGWMod integration loading")
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  def onPageRequest(event: BlockWikiEvent) {
    //Blocks
    if (event.isInstanceOf[BlockWikiEvent]) {
      if (api.Items.get(new ItemStack(event.block)) != null) {
        log.info(s"Here goes the implemenatation for " + api.Items.get(new ItemStack(event.block)).name)
      }
    }
  }

  @SubscribeEvent
  def onPageRequest(event: ItemWikiEvent) {
    //Items + changing pages inside the Wiki GUI
    if (event.isInstanceOf[ItemWikiEvent]) {
      if (api.Items.get(event.itemStack) != null) {
        log.info(s"Here goes the implemenatation for " + api.Items.get(event.itemStack).name)
      }
    }
  }
}