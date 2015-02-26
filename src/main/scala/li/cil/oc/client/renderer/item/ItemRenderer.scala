package li.cil.oc.client.renderer.item

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.entity.DroneRenderer
import li.cil.oc.integration.opencomputers.Item
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsScala._

object ItemRenderer extends IItemRenderer {
  val renderItem = new RenderItem()
  renderItem.setRenderManager(RenderManager.instance)

  lazy val craftingUpgrade = api.Items.get("craftingUpgrade")
  lazy val generatorUpgrade = api.Items.get("generatorUpgrade")
  lazy val inventoryUpgrade = api.Items.get("inventoryUpgrade")
  lazy val drone = api.Items.get("drone")

  lazy val floppy = api.Items.get("floppy")
  lazy val lootDisk = api.Items.get("lootDisk")

  def bounds = AxisAlignedBB.getBoundingBox(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1)

  def isUpgrade(descriptor: ItemInfo) =
    descriptor == craftingUpgrade ||
      descriptor == generatorUpgrade ||
      descriptor == inventoryUpgrade

  def isFloppy(descriptor: ItemInfo) =
    descriptor == floppy ||
      descriptor == lootDisk

  override def handleRenderType(stack: ItemStack, renderType: ItemRenderType) = {
    val descriptor = api.Items.get(stack)
    (renderType == ItemRenderType.EQUIPPED && isUpgrade(api.Items.get(stack))) ||
      (renderType == ItemRenderType.INVENTORY && isFloppy(api.Items.get(stack))) ||
      ((renderType == ItemRenderType.INVENTORY || renderType == ItemRenderType.ENTITY || renderType == ItemRenderType.EQUIPPED || renderType == ItemRenderType.EQUIPPED_FIRST_PERSON) && descriptor == drone)
  }

  override def shouldUseRenderHelper(renderType: ItemRenderType, stack: ItemStack, helper: ItemRendererHelper) =
    if (renderType == ItemRenderType.ENTITY) true
    // Note: it's easier to revert changes introduced by this "helper" than by
    // the code that applies if no helper is used...
    else helper == ItemRendererHelper.EQUIPPED_BLOCK

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
    else if (descriptor == drone) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
      GL11.glPushMatrix()

      Minecraft.getMinecraft.renderEngine.bindTexture(DroneRenderer.model.texture)
      RenderState.makeItBlend()

      if (renderType == ItemRenderType.INVENTORY) {
        GL11.glTranslatef(8f, 9f, 0)
        GL11.glRotatef(-30, 1, 0, 0)
        GL11.glRotatef(60, 0, 1, 0)
        GL11.glScalef(13, -13, 13)
      }
      else if (renderType == ItemRenderType.EQUIPPED || renderType == ItemRenderType.EQUIPPED_FIRST_PERSON) {
        GL11.glTranslatef(0.4f, 1.05f, 0.75f)
        GL11.glRotatef(-30, 0, 1, 0)
        GL11.glRotatef(80, 0, 0, 1)
        GL11.glScalef(1.5f, 1.5f, 1.5f)
      }

      DroneRenderer.model.render()

      GL11.glPopMatrix()
      GL11.glPopAttrib()

      RenderState.checkError("ItemRenderer.renderItem: drone")
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
