package li.cil.oc.common.event

import java.io.FileInputStream
import java.io.FileOutputStream

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.vertex.IVertexBuilder
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Controller
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.EventHandler
import li.cil.oc.common.nanomachines.ControllerImpl
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent

object NanomachinesHandler {

  object Client {
    val TexNanomachines = RenderTypes.createTexturedQuad("nanomachines", Textures.GUI.Nanomachines, DefaultVertexFormats.POSITION_TEX, false)
    val TexNanomachinesBar = RenderTypes.createTexturedQuad("nanomachines_bar", Textures.GUI.NanomachinesBar, DefaultVertexFormats.POSITION_TEX, false)

    @SubscribeEvent
    def onRenderGameOverlay(e: RenderGameOverlayEvent.Post): Unit = {
      if (e.getType == RenderGameOverlayEvent.ElementType.TEXT) {
        val mc = Minecraft.getInstance
        api.Nanomachines.getController(mc.player) match {
          case controller: Controller =>
            val stack = e.getMatrixStack
            val window = mc.getWindow
            val sizeX = 8
            val sizeY = 12
            val width = window.getGuiScaledWidth
            val height = window.getGuiScaledHeight
            val (x, y) = Settings.get.nanomachineHudPos
            val left =
              math.min(width - sizeX,
                if (x < 0) width / 2 - 91 - 12
                else if (x < 1) width * x
                else x)
            val top =
              math.min(height - sizeY,
                if (y < 0) height - 39
                else if (y < 1) y * height
                else y)
            val fill = controller.getLocalBuffer / controller.getLocalBufferSize
            val buffer = IRenderTypeBuffer.immediate(Tessellator.getInstance.getBuilder)
            drawRect(stack, buffer.getBuffer(TexNanomachines), left.toInt, top.toInt, sizeX, sizeY, sizeX, sizeY)
            drawRect(stack, buffer.getBuffer(TexNanomachinesBar), left.toInt, top.toInt, sizeX, sizeY, sizeX, sizeY, fill.toFloat)
            buffer.endBatch()
          case _ => // Nothing to show.
        }
      }
    }

    private def drawRect(stack: MatrixStack, r: IVertexBuilder, x: Int, y: Int, w: Int, h: Int, tw: Int, th: Int, fill: Float = 1) {
      val sx = 1f / tw
      val sy = 1f / th
      r.vertex(stack.last.pose, x, y + h, 0).uv(0, h * sy).endVertex()
      r.vertex(stack.last.pose, x + w, y + h, 0).uv(w * sx, h * sy).endVertex()
      r.vertex(stack.last.pose, x + w, y + h * (1 - fill), 0).uv(w * sx, 1 - fill).endVertex()
      r.vertex(stack.last.pose, x, y + h * (1 - fill), 0).uv(0, 1 - fill).endVertex()
    }
  }

  object Common {
    @SubscribeEvent
    def onPlayerRespawn(e: PlayerRespawnEvent): Unit = {
      api.Nanomachines.getController(e.getPlayer) match {
        case controller: Controller => controller.changeBuffer(-controller.getLocalBuffer)
        case _ => // Not a player with nanomachines.
      }
    }

    @SubscribeEvent
    def onLivingUpdate(e: LivingEvent.LivingUpdateEvent): Unit = {
      e.getEntity match {
        case player: PlayerEntity => api.Nanomachines.getController(player) match {
          case controller: ControllerImpl =>
            if (controller.player eq player) {
              controller.update()
            }
            else {
              // Player entity instance changed (e.g. respawn), recreate the controller.
              val nbt = new CompoundNBT()
              controller.saveData(nbt)
              api.Nanomachines.uninstallController(controller.player)
              api.Nanomachines.installController(player) match {
                case newController: ControllerImpl =>
                  newController.loadData(nbt)
                  newController.reset()
                case _ => // Eh?
              }
            }
          case _ => // Not a player with nanomachines.
        }
        case _ => // Not a player.
      }
    }

    @SubscribeEvent
    def onPlayerSave(e: PlayerEvent.SaveToFile): Unit = {
      val file = e.getPlayerFile("ocnm")
      api.Nanomachines.getController(e.getPlayer) match {
        case controller: ControllerImpl =>
          try {
            val nbt = new CompoundNBT()
            controller.saveData(nbt)
            val fos = new FileOutputStream(file)
            try CompressedStreamTools.writeCompressed(nbt, fos) catch {
              case t: Throwable =>
                OpenComputers.log.warn("Error saving nanomachine state.", t)
            }
            fos.close()
          }
          catch {
            case t: Throwable =>
              OpenComputers.log.warn("Error saving nanomachine state.", t)
          }
        case _ => // Not a player with nanomachines.
      }
    }

    @SubscribeEvent
    def onPlayerLoad(e: PlayerEvent.LoadFromFile): Unit = {
      val file = e.getPlayerFile("ocnm")
      if (file.exists()) {
        api.Nanomachines.getController(e.getPlayer) match {
          case controller: ControllerImpl =>
            try {
              val fis = new FileInputStream(file)
              try controller.loadData(CompressedStreamTools.readCompressed(fis)) catch {
                case t: Throwable =>
                  OpenComputers.log.warn("Error loading nanomachine state.", t)
              }
              fis.close()
            }
            catch {
              case t: Throwable =>
                OpenComputers.log.warn("Error loading nanomachine state.", t)
            }
          case _ => // Not a player with nanomachines.
        }
      }
    }

    @SubscribeEvent
    def onPlayerDisconnect(e: PlayerLoggedOutEvent): Unit = {
      api.Nanomachines.getController(e.getPlayer) match {
        case controller: ControllerImpl =>
          // Wait a tick because saving is done after this event.
          EventHandler.scheduleServer(() => api.Nanomachines.uninstallController(e.getPlayer))
        case _ => // Not a player with nanomachines.
      }
    }
  }

}
