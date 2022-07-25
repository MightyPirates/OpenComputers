package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory

class Raid(id: Int, playerInventory: PlayerInventory, val raid: tileentity.Raid)
  extends DynamicGuiContainer(new container.Raid(id, playerInventory, raid),
    playerInventory, raid.getName) {

  override def renderBg(stack: MatrixStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.color3f(1, 1, 1) // Required under Linux.
    Textures.bind(Textures.GUI.Raid)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
