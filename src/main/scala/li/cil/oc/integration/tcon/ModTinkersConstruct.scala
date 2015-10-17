package li.cil.oc.integration.tcon

import li.cil.oc.api
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge

object ModTinkersConstruct extends ModProxy {
  override def getMod = Mods.TinkersConstruct

  override def initialize() {
    api.IMC.registerToolDurabilityProvider("li.cil.oc.integration.tcon.EventHandlerTinkersConstruct.getDurability")

    MinecraftForge.EVENT_BUS.register(EventHandlerTinkersConstruct)
  }

  def isInfiTool(stack: ItemStack) = stack != null && stack.getItem.getClass.getName.startsWith("tconstruct.")
}
