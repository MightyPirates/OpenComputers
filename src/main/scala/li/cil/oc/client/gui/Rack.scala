package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.widget.button.Button
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.Direction
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import org.lwjgl.opengl.GL11

import scala.collection.JavaConverters.asJavaCollection

class Rack(state: container.Rack, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {

  imageHeight = 210

  final val busMasterBlankUVs = (195, 14, 3, 5)
  final val busMasterPresentUVs = (194, 20, 5, 5)
  final val busSlaveBlankUVs = (195, 1, 3, 4)
  final val busSlavePresentUVs = (194, 6, 5, 4)

  final val connectorMasterUVs = (194, 26, 1, 3)
  final val connectorSlaveUVs = (194, 11, 1, 2)

  final val hoverMasterSize = (3, 3)
  final val hoverSlaveSize = (3, 2)

  final val wireMasterUVs = Array(
    (186, 16, 6, 3),
    (186, 20, 6, 3),
    (186, 24, 6, 3),
    (186, 28, 6, 3),
    (186, 32, 6, 3)
  )
  final val wireSlaveUVs = Array(
    (186, 1, 6, 2),
    (186, 4, 6, 2),
    (186, 7, 6, 2),
    (186, 10, 6, 2),
    (186, 13, 6, 2)
  )

  final val busStart = Array(
    (45, 22),
    (56, 22),
    (67, 22),
    (78, 22),
    (89, 22)
  )

  final val busGap = 3

  final val connectorStart = Array(
    (37, 23),
    (37, 43),
    (37, 63),
    (37, 83)
  )

  final val connectorGap = 2

  final val relayModeUVs = (195, 30, 4, 2)

  final val wireRelay = Array(
    (50, 104),
    (61, 104),
    (72, 104),
    (83, 104)
  )

  final val busToSide = Direction.values().filter(_ != Direction.SOUTH)
  final val sideToBus = busToSide.zipWithIndex.toMap

  var relayButton: ImageButton = _

  // bus -> mountable -> connectable
  var wireButtons = Array.fill(inventoryContainer.otherInventory.getContainerSize)(Array.fill(4)(Array.fill(5)(null: ImageButton)))

  def sideName(side: Direction) = side match {
    case Direction.UP => Localization.Rack.Top
    case Direction.DOWN => Localization.Rack.Bottom
    case Direction.EAST => Localization.Rack.Left
    case Direction.WEST => Localization.Rack.Right
    case Direction.NORTH => Localization.Rack.Back
    case _ => Localization.Rack.None
  }

  protected def onRackButton(mountable: Int, connectable: Int, bus: Int) {
    if (inventoryContainer.nodeMapping(mountable)(connectable).contains(busToSide(bus))) {
      ClientPacketSender.sendRackMountableMapping(inventoryContainer, mountable, connectable, None)
    }
    else {
      ClientPacketSender.sendRackMountableMapping(inventoryContainer, mountable, connectable, Option(busToSide(bus)))
    }
  }

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, dt: Float) {
    for (bus <- 0 until 5) {
      for (mountable <- 0 until inventoryContainer.otherInventory.getContainerSize) {
        val presence = inventoryContainer.nodePresence(mountable)
        for (connectable <- 0 until 4) {
          wireButtons(mountable)(connectable)(bus).visible = presence(connectable)
        }
      }
    }
    val relayMessage = if (inventoryContainer.isRelayEnabled) Localization.Rack.RelayEnabled else Localization.Rack.RelayDisabled
    relayButton.setMessage(new StringTextComponent(relayMessage))
    super.render(stack, mouseX, mouseY, dt)
  }

  override protected def init() {
    super.init()

    relayButton = new ImageButton(leftPos + 101, topPos + 96, 65, 18, new Button.IPressable {
      override def onPress(b: Button) = ClientPacketSender.sendRackRelayState(inventoryContainer, !inventoryContainer.isRelayEnabled)
    }, Textures.GUI.ButtonRelay, new StringTextComponent(Localization.Rack.RelayDisabled), textIndent = 18)
    addButton(relayButton)

    val (mw, mh) = hoverMasterSize
    val (sw, sh) = hoverSlaveSize
    val (_, _, _, mbh) = busMasterBlankUVs
    val (_, _, _, sbh) = busSlaveBlankUVs
    for (bus <- 0 until 5) {
      for (mountable <- 0 until inventoryContainer.otherInventory.getContainerSize) {
        val offset = mountable * (mbh + sbh * 3 + busGap)
        val (bx, by) = busStart(bus)

        {
          val button = new ImageButton(leftPos + bx, topPos + by + offset + 1, mw, mh, new Button.IPressable {
            override def onPress(b: Button) = onRackButton(mountable, 0, bus)
          })
          addButton(button)
          wireButtons(mountable)(0)(bus) = button
        }

        for (connectable <- 0 until 3) {
          val button = new ImageButton(leftPos + bx, topPos + by + offset + 1 + mbh + sbh * connectable, sw, sh, new Button.IPressable {
            override def onPress(b: Button) = onRackButton(mountable, connectable + 1, bus)
          })
          addButton(button)
          wireButtons(mountable)(connectable + 1)(bus) = button
        }
      }
    }
  }

  override def drawSecondaryForegroundLayer(stack: MatrixStack, mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(stack, mouseX, mouseY)
    RenderState.pushAttrib() // Prevents NEI render glitch.

    RenderSystem.color4f(1, 1, 1, 1)
    RenderState.makeItBlend()
    minecraft.getTextureManager.bind(Textures.GUI.Rack)

    if (inventoryContainer.isRelayEnabled) {
      val (left, top, w, h) = relayModeUVs
      for ((x, y) <- wireRelay) {
        drawRect(stack, x, y, w, h, left, top)
      }
    }

    val (mcx, mcy, mcw, mch) = connectorMasterUVs
    val (mbx, mby, mbw, mbh) = busMasterBlankUVs
    val (mpx, mpy, mpw, mph) = busMasterPresentUVs
    val (scx, scy, scw, sch) = connectorSlaveUVs
    val (sbx, sby, sbw, sbh) = busSlaveBlankUVs
    val (spx, spy, spw, sph) = busSlavePresentUVs
    for (mountable <- 0 until inventoryContainer.otherInventory.getContainerSize) {
      val presence = inventoryContainer.nodePresence(mountable)

      // Draw connectable indicators next to item slots.
      val (cx, cy) = connectorStart(mountable)
      if (presence(0)) {
        drawRect(stack, cx, cy, mcw, mch, mcx, mcy)
        inventoryContainer.nodeMapping(mountable)(0) match {
          case Some(side) =>
            val bus = sideToBus(side)
            val (mwx, mwy, mww, mwh) = wireMasterUVs(bus)
            for (i <- 0 to bus) {
              val xOffset = mcw + i * (mpw + mww)
              drawRect(stack, cx + xOffset, cy, mww, mwh, mwx, mwy)
            }
          case _ =>
        }
        for (connectable <- 1 until 4) {
          inventoryContainer.nodeMapping(mountable)(connectable) match {
            case Some(side) =>
              val bus = sideToBus(side)
              val (swx, swy, sww, swh) = wireSlaveUVs(bus)
              val yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1)
              for (i <- 0 to bus) {
                val xOffset = scw + i * (spw + sww)
                drawRect(stack, cx + xOffset, cy + yOffset, sww, swh, swx, swy)
              }
            case _ =>
          }
        }
      }
      for (connectable <- 1 until 4) {
        if (presence(connectable)) {
          val yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1)
          drawRect(stack, cx, cy + yOffset, scw, sch, scx, scy)
        }
      }

      // Draw connection points on buses.
      val yOffset = mountable * (mbh + sbh * 3 + busGap)
      for (bus <- 0 until 5) {
        val (bx, by) = busStart(bus)
        if (presence(0)) {
          drawRect(stack, bx - 1, by + yOffset, mpw, mph, mpx, mpy)
        }
        else {
          drawRect(stack, bx, by + yOffset, mbw, mbh, mbx, mby)
        }
        for (connectable <- 0 until 3) {
          if (presence(connectable + 1)) {
            drawRect(stack, bx - 1, by + yOffset + mph + sph * connectable, spw, sph, spx, spy)
          }
          else {
            drawRect(stack, bx, by + yOffset + mbh + sbh * connectable, sbw, sbh, sbx, sby)
          }
        }
      }
    }

    for (bus <- 0 until 5) {
      val x = 122
      val y = 20 + bus * 11

      font.draw(stack,
        Localization.localizeImmediately(sideName(busToSide(bus))),
        x, y, 0x404040)
    }

    if (relayButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(asJavaCollection(Localization.Rack.RelayModeTooltip.lines.toIterable))
      copiedDrawHoveringText(stack, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }

    RenderState.popAttrib()
  }

  override def drawSecondaryBackgroundLayer(stack: MatrixStack) {
    RenderSystem.color3f(1, 1, 1) // Required under Linux.
    minecraft.getTextureManager.bind(Textures.GUI.Rack)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }

  private def drawRect(stack: MatrixStack, x: Int, y: Int, w: Int, h: Int, u: Int, v: Int): Unit = {
    val u0 = u / 256f
    val v0 = v / 256f
    val u1 = u0 + w / 256f
    val v1 = v0 + h / 256f
    val t = Tessellator.getInstance()
    val r = t.getBuilder
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
    r.vertex(stack.last.pose, x, y, windowZ).uv(u0, v0).endVertex()
    r.vertex(stack.last.pose, x, y + h, windowZ).uv(u0, v1).endVertex()
    r.vertex(stack.last.pose, x + w, y + h, windowZ).uv(u1, v1).endVertex()
    r.vertex(stack.last.pose, x + w, y, windowZ).uv(u1, v0).endVertex()
    t.end()
  }
}
