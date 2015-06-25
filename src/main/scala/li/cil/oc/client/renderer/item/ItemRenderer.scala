package li.cil.oc.client.renderer.item

import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.renderer.block.Print
import li.cil.oc.client.renderer.entity.DroneRenderer
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsScala._

object ItemRenderer extends IItemRenderer {
  val renderItem = new RenderItem()
  renderItem.setRenderManager(RenderManager.instance)

  lazy val drone = api.Items.get(Constants.ItemName.Drone)

  lazy val floppy = api.Items.get(Constants.ItemName.Floppy)
  lazy val lootDisk = api.Items.get(Constants.ItemName.LootDisk)
  lazy val print = api.Items.get(Constants.BlockName.Print)

  lazy val nullShape = new PrintData.Shape(ExtendedAABB.unitBounds, Settings.resourceDomain + ":White", Some(Color.Lime))

  def isFloppy(descriptor: ItemInfo) = descriptor == floppy || descriptor == lootDisk

  override def handleRenderType(stack: ItemStack, renderType: ItemRenderType) = {
    val descriptor = api.Items.get(stack)
    (renderType == ItemRenderType.INVENTORY && isFloppy(api.Items.get(stack))) ||
      ((renderType == ItemRenderType.INVENTORY || renderType == ItemRenderType.ENTITY || renderType == ItemRenderType.EQUIPPED || renderType == ItemRenderType.EQUIPPED_FIRST_PERSON) && descriptor == drone) ||
      ((renderType == ItemRenderType.INVENTORY || renderType == ItemRenderType.ENTITY || renderType == ItemRenderType.EQUIPPED || renderType == ItemRenderType.EQUIPPED_FIRST_PERSON) && api.Items.get(stack) == print)
  }

  override def shouldUseRenderHelper(renderType: ItemRenderType, stack: ItemStack, helper: ItemRendererHelper) =
    if (renderType == ItemRenderType.ENTITY) true
    else if (renderType == ItemRenderType.INVENTORY && api.Items.get(stack) == print) helper == ItemRendererHelper.INVENTORY_BLOCK
    // Note: it's easier to revert changes introduced by this "helper" than by
    // the code that applies if no helper is used...
    else helper == ItemRendererHelper.EQUIPPED_BLOCK

  override def renderItem(renderType: ItemRenderType, stack: ItemStack, data: AnyRef*) {
    RenderState.checkError(getClass.getName + ".renderItem: entering (aka: wasntme)")

    val mc = Minecraft.getMinecraft
    val tm = mc.getTextureManager
    val descriptor = api.Items.get(stack)

    if (isFloppy(descriptor)) {
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

    else if (descriptor == print) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
      GL11.glPushMatrix()

      if (renderType == ItemRenderType.ENTITY) {
        GL11.glTranslatef(-0.5f, 0, -0.5f)
      }

      val data = new PrintData(stack)
      Minecraft.getMinecraft.renderEngine.bindTexture(TextureMap.locationBlocksTexture)
      val state =
        if (data.hasActiveState && KeyBindings.showExtendedTooltips)
          data.stateOn
        else
          data.stateOff
      for (shape <- state) {
        drawShape(shape)
      }
      if (state.isEmpty) {
        drawShape(nullShape) // Avoid tessellator erroring.
      }

      GL11.glPopMatrix()
      GL11.glPopAttrib()

      RenderState.checkError("ItemRenderer.renderItem: print")
    }

    RenderState.checkError("ItemRenderer.renderItem: leaving")
  }

  private def drawShape(shape: PrintData.Shape) {
    val bounds = shape.bounds
    val texture = Print.resolveTexture(shape.texture)

    if (Strings.isNullOrEmpty(shape.texture)) {
      RenderState.makeItBlend()
      GL11.glColor4f(1, 1, 1, 0.25f)
    }

    shape.tint.foreach(color => {
      val r = (color >> 16).toByte
      val g = (color >> 8).toByte
      val b = color.toByte
      GL11.glColor3ub(r, g, b)
    })

    GL11.glBegin(GL11.GL_QUADS)
    GL11.glDisable(GL11.GL_CULL_FACE)

    // Front.
    GL11.glNormal3f(0, 0, 1)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minX * 16), texture.getInterpolatedV(16 - bounds.minY * 16))
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxX * 16), texture.getInterpolatedV(16 - bounds.minY * 16))
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxX * 16), texture.getInterpolatedV(16 - bounds.maxY * 16))
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minX * 16), texture.getInterpolatedV(16 - bounds.maxY * 16))
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.maxZ)

    // Back.
    GL11.glNormal3f(0, 0, -1)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxX * 16), texture.getInterpolatedV(16 - bounds.minY * 16))
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minX * 16), texture.getInterpolatedV(16 - bounds.minY * 16))
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minX * 16), texture.getInterpolatedV(16 - bounds.maxY * 16))
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxX * 16), texture.getInterpolatedV(16 - bounds.maxY * 16))
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.minZ)

    // Top.
    GL11.glNormal3f(0, 1, 0)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxX * 16), texture.getInterpolatedV(bounds.maxZ * 16))
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxX * 16), texture.getInterpolatedV(bounds.minZ * 16))
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minX * 16), texture.getInterpolatedV(bounds.minZ * 16))
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minX * 16), texture.getInterpolatedV(bounds.maxZ * 16))
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.maxZ)

    // Bottom.
    GL11.glNormal3f(0, -1, 0)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minX * 16), texture.getInterpolatedV(bounds.maxZ * 16))
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minX * 16), texture.getInterpolatedV(bounds.minZ * 16))
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxX * 16), texture.getInterpolatedV(bounds.minZ * 16))
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxX * 16), texture.getInterpolatedV(bounds.maxZ * 16))
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.maxZ)

    // Left.
    GL11.glNormal3f(1, 0, 0)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxZ * 16), texture.getInterpolatedV(16 - bounds.maxY * 16))
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxZ * 16), texture.getInterpolatedV(16 - bounds.minY * 16))
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minZ * 16), texture.getInterpolatedV(16 - bounds.minY * 16))
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minZ * 16), texture.getInterpolatedV(16 - bounds.maxY * 16))
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.minZ)

    // Right.
    GL11.glNormal3f(-1, 0, 0)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxZ * 16), texture.getInterpolatedV(16 - bounds.minY * 16))
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.maxZ * 16), texture.getInterpolatedV(16 - bounds.maxY * 16))
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minZ * 16), texture.getInterpolatedV(16 - bounds.maxY * 16))
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(texture.getInterpolatedU(bounds.minZ * 16), texture.getInterpolatedV(16 - bounds.minY * 16))
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.minZ)

    GL11.glEnd()
    GL11.glEnable(GL11.GL_CULL_FACE)

    GL11.glColor3f(1, 1, 1)
  }
}
