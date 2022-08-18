package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.container
import li.cil.oc.common.inventory.DatabaseInventory
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent

object Database {
  def of(id: Int, playerInventory: PlayerInventory, databaseInventory: DatabaseInventory) =
    new Database(new container.Database(container.ContainerTypes.DATABASE, id, playerInventory, databaseInventory), playerInventory, StringTextComponent.EMPTY)
}

class Database(state: container.Database, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[container.Database] {

  imageHeight = 256

  override def lockedStack = inventoryContainer.container

  override def drawSecondaryForegroundLayer(stack: MatrixStack, mouseX: Int, mouseY: Int) {}

  override protected def renderBg(stack: MatrixStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.color4f(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Database)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)

    if (inventoryContainer.tier > Tier.One) {
      Textures.bind(Textures.GUI.Database1)
      blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }

    if (inventoryContainer.tier > Tier.Two) {
      Textures.bind(Textures.GUI.Database2)
      blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }
  }
}
