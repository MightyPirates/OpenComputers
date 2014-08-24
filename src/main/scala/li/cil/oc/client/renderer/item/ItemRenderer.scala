package li.cil.oc.client.renderer.item

import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.client.Textures
import li.cil.oc.server.driver.item.Item
import li.cil.oc.util.RenderState
import li.cil.oc.{Settings, api}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.entity.{RenderItem, RenderManager}
import net.minecraft.item.ItemStack
import net.minecraft.util.{AxisAlignedBB, EnumChatFormatting}
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{ItemRenderType, ItemRendererHelper}
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsScala._

object ItemRenderer extends IItemRenderer {
  val renderItem = new RenderItem()
  renderItem.setRenderManager(RenderManager.instance)

  def bounds = AxisAlignedBB.getBoundingBox(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1)

  def isUpgrade(descriptor: ItemInfo) =
    descriptor == api.Items.get("craftingUpgrade") ||
      descriptor == api.Items.get("generatorUpgrade") ||
      descriptor == api.Items.get("inventoryUpgrade")

  def isFloppy(descriptor: ItemInfo) =
    descriptor == api.Items.get("floppy") ||
      descriptor == api.Items.get("lootDisk") ||
      descriptor == api.Items.get("openOS")

  override def handleRenderType(stack: ItemStack, renderType: ItemRenderType) =
    (renderType == ItemRenderType.EQUIPPED && isUpgrade(api.Items.get(stack))) ||
      (renderType == ItemRenderType.INVENTORY && isFloppy(api.Items.get(stack)))

  override def shouldUseRenderHelper(renderType: ItemRenderType, stack: ItemStack, helper: ItemRendererHelper) =
  // Note: it's easier to revert changes introduced by this "helper" than by
  // the code that applies if no helper is used...
    helper == ItemRendererHelper.EQUIPPED_BLOCK

  override def renderItem(renderType: ItemRenderType, stack: ItemStack, data: AnyRef*) {
    RenderState.checkError(getClass.getName + ".renderItem: entering (aka: wasntme)")

    val mc = Minecraft.getMinecraft
    val tm = mc.getTextureManager
    val descriptor = api.Items.get(stack)
    if (isUpgrade(descriptor)) {

      // Revert offset introduced by the render "helper".
      GL11.glTranslatef(0.5f, 0.5f, 0.5f)

      if (descriptor == api.Items.get("craftingUpgrade")) {
        tm.bindTexture(Textures.upgradeCrafting)
        drawSimpleBlock()

        RenderState.checkError(getClass.getName + ".renderItem: crafting upgrade")
      }

      else if (descriptor == api.Items.get("generatorUpgrade")) {
        tm.bindTexture(Textures.upgradeGenerator)
        drawSimpleBlock(if (Item.dataTag(stack).getInteger("remainingTicks") > 0) 0.5f else 0)

        RenderState.checkError(getClass.getName + ".renderItem: generator upgrade")
      }

      else if (descriptor == api.Items.get("inventoryUpgrade")) {
        tm.bindTexture(Textures.upgradeInventory)
        drawSimpleBlock()

        RenderState.checkError(getClass.getName + ".renderItem: inventory upgrade")
      }
    }

    else if (isFloppy(descriptor)) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
      renderItem.renderItemIntoGUI(null, tm, stack, 0, 0)
      val res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight)
      val fontRenderer = renderItem.getFontRendererFromRenderManager
      if (fontRenderer != null && res.getScaleFactor > 1) {
        GL11.glPushMatrix()
        GL11.glTranslatef(4f + 2f / res.getScaleFactor, 9f + 2f / res.getScaleFactor, 0)
        GL11.glScalef(1f / res.getScaleFactor, 1f / res.getScaleFactor, 1f)
        val maxLength = (res.getScaleFactor * 7.5).toInt
        val label =
          if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data") && stack.getTagCompound.getCompoundTag(Settings.namespace + "data").hasKey(Settings.namespace + "fs.label")) {
            stack.getTagCompound.getCompoundTag(Settings.namespace + "data").getString(Settings.namespace + "fs.label")
          }
          else "disk"
        val lines = fontRenderer.listFormattedStringToWidth(EnumChatFormatting.getTextWithoutFormattingCodes(label), maxLength).take(math.max(1, res.getScaleFactor / 2))
        for (line <- lines) {
          fontRenderer.drawString(line.asInstanceOf[String], 0, 0, 0)
          GL11.glTranslatef(0, fontRenderer.FONT_HEIGHT, 0)
        }
        GL11.glPopMatrix()
      }
      GL11.glPopAttrib()

      RenderState.checkError("ItemRenderer.renderItem: floppy")
    }

    RenderState.checkError("ItemRenderer.renderItem: leaving")
  }

  private def drawSimpleBlock(frontOffset: Float = 0) {
    GL11.glBegin(GL11.GL_QUADS)

    // Front.
    GL11.glNormal3f(0, 0, 1)
    GL11.glTexCoord2f(frontOffset, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(frontOffset + 0.5f, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(frontOffset + 0.5f, 0)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(frontOffset, 0)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.maxZ)

    // Top.
    GL11.glNormal3f(0, 1, 0)
    GL11.glTexCoord2f(1, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(1, 1)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(0.5f, 1)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.maxZ)

    // Bottom.
    GL11.glNormal3f(0, -1, 0)
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(0.5f, 1)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(1, 1)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(1, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.maxZ)

    // Left.
    GL11.glNormal3f(1, 0, 0)
    GL11.glTexCoord2f(0, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(0, 1)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(0.5f, 1)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.minZ)

    // Right.
    GL11.glNormal3f(-1, 0, 0)
    GL11.glTexCoord2f(0, 1)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(0, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(0.5f, 1)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.minZ)

    GL11.glEnd()
  }
}
