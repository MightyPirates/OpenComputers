package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.container
import li.cil.oc.common.inventory.DatabaseInventory
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.StringTextComponent

class Database(id: Int, playerInventory: PlayerInventory, val databaseInventory: DatabaseInventory)
  extends DynamicGuiContainer(new container.Database(id, playerInventory, databaseInventory),
    playerInventory, StringTextComponent.EMPTY)
  with traits.LockedHotbar[container.Database] {

  imageHeight = 256

  override def lockedStack = databaseInventory.container

  override def drawSecondaryForegroundLayer(stack: MatrixStack, mouseX: Int, mouseY: Int) {}

  override protected def renderBg(stack: MatrixStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.color4f(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Database)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)

    if (databaseInventory.tier > Tier.One) {
      Textures.bind(Textures.GUI.Database1)
      blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }

    if (databaseInventory.tier > Tier.Two) {
      Textures.bind(Textures.GUI.Database2)
      blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }
  }
}
