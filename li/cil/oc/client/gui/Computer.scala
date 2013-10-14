package li.cil.oc.client.gui

import li.cil.oc.Config
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.{ResourceLocation, StatCollector}

class Computer(playerInventory: InventoryPlayer, val computer: tileentity.Computer) extends DynamicGuiContainer(new container.Computer(playerInventory, computer)) {
  private val iconPsu = new ResourceLocation(Config.resourceDomain, "textures/gui/icon_psu.png")
  private val iconPci = new ResourceLocation(Config.resourceDomain, "textures/gui/icon_pci.png")
  private val iconRam = new ResourceLocation(Config.resourceDomain, "textures/gui/icon_ram.png")
  private val iconHdd = new ResourceLocation(Config.resourceDomain, "textures/gui/icon_hdd.png")

  private val icons = Array(iconPsu, iconPci, iconPci, iconPci, iconRam, iconRam, iconHdd, iconHdd)

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    fontRenderer.drawString(
      StatCollector.translateToLocal("oc.container.computer"),
      8, 6, 0x404040)
  }

  override protected def bindIconBackground(slot: Slot) =
    if (slot.slotNumber < 8 && !slot.getHasStack) {
      mc.renderEngine.bindTexture(icons(slot.slotNumber))
      true
    }
    else false

  override def doesGuiPauseGame = false
}