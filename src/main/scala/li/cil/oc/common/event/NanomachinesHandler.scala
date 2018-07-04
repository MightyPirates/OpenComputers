package li.cil.oc.common.event

import java.io.FileInputStream
import java.io.FileOutputStream

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Controller
import li.cil.oc.client.Textures
import li.cil.oc.common.EventHandler
import li.cil.oc.common.nanomachines.ControllerImpl
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent
import org.lwjgl.opengl.GL11

object NanomachinesHandler {

  object Client {
    @SubscribeEvent
    def onRenderGameOverlay(e: RenderGameOverlayEvent.Post): Unit = {
      if (e.getType == RenderGameOverlayEvent.ElementType.TEXT) {
        val mc = Minecraft.getMinecraft
        api.Nanomachines.getController(mc.player) match {
          case controller: Controller =>
            val res = new ScaledResolution(mc)
            val sizeX = 8
            val sizeY = 12
            val width = res.getScaledWidth
            val height = res.getScaledHeight
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
            Minecraft.getMinecraft.getTextureManager.bindTexture(Textures.GUI.Nanomachines)
            drawRect(left.toInt, top.toInt, sizeX, sizeY, sizeX, sizeY)
            Minecraft.getMinecraft.getTextureManager.bindTexture(Textures.GUI.NanomachinesBar)
            drawRect(left.toInt, top.toInt, sizeX, sizeY, sizeX, sizeY, fill)
          case _ => // Nothing to show.
        }
      }
    }

    private def drawRect(x: Int, y: Int, w: Int, h: Int, tw: Int, th: Int, fill: Double = 1) {
      val sx = 1f / tw
      val sy = 1f / th
      val t = Tessellator.getInstance
      val r = t.getBuffer
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
      r.pos(x, y + h, 0).tex(0, h * sy).endVertex()
      r.pos(x + w, y + h, 0).tex(w * sx, h * sy).endVertex()
      r.pos(x + w, y + h * (1 - fill), 0).tex(w * sx, 1 - fill).endVertex()
      r.pos(x, y + h * (1 - fill), 0).tex(0, 1 - fill).endVertex()
      t.draw()
    }
  }

  object Common {
    @SubscribeEvent
    def onPlayerRespawn(e: PlayerRespawnEvent): Unit = {
      api.Nanomachines.getController(e.player) match {
        case controller: Controller => controller.changeBuffer(-controller.getLocalBuffer)
        case _ => // Not a player with nanomachines.
      }
    }

    @SubscribeEvent
    def onLivingUpdate(e: LivingEvent.LivingUpdateEvent): Unit = {
      e.getEntity match {
        case player: EntityPlayer => api.Nanomachines.getController(player) match {
          case controller: ControllerImpl =>
            if (controller.player eq player) {
              controller.update()
            }
            else {
              // Player entity instance changed (e.g. respawn), recreate the controller.
              val nbt = new NBTTagCompound()
              controller.save(nbt)
              api.Nanomachines.uninstallController(controller.player)
              api.Nanomachines.installController(player) match {
                case newController: ControllerImpl =>
                  newController.load(nbt)
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
      api.Nanomachines.getController(e.getEntityPlayer) match {
        case controller: ControllerImpl =>
          try {
            val nbt = new NBTTagCompound()
            controller.save(nbt)
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
        api.Nanomachines.getController(e.getEntityPlayer) match {
          case controller: ControllerImpl =>
            try {
              val fis = new FileInputStream(file)
              try controller.load(CompressedStreamTools.readCompressed(fis)) catch {
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
      api.Nanomachines.getController(e.player) match {
        case controller: ControllerImpl =>
          // Wait a tick because saving is done after this event.
          EventHandler.scheduleServer(() => api.Nanomachines.uninstallController(e.player))
        case _ => // Not a player with nanomachines.
      }
    }
  }

}
