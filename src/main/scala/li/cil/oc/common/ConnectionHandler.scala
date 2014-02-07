package li.cil.oc.common

import cpw.mods.fml.common.{FMLCommonHandler, Loader}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent
import li.cil.oc.Settings
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.mods.ProjectRed
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.{ChatComponentText, ChatComponentTranslation}

object ConnectionHandler {

  @SubscribeEvent
  def playerLoggedIn(e: PlayerLoggedInEvent) {
    if (FMLCommonHandler.instance.getEffectiveSide.isServer) e.player match {
      case player: EntityPlayerMP =>
        if (!LuaStateFactory.isAvailable) {
          player.addChatMessage(new ChatComponentText("§aOpenComputers§f: ").appendSibling(
            new ChatComponentTranslation(Settings.namespace + "gui.Chat.WarningLuaFallback")))
        }
        if (ProjectRed.isAvailable && !ProjectRed.isAPIAvailable) {
          player.addChatMessage(new ChatComponentText("§aOpenComputers§f: ").appendSibling(
            new ChatComponentTranslation(Settings.namespace + "gui.Chat.WarningProjectRed")))
        }
        if (!Settings.get.pureIgnorePower && !Loader.isModLoaded("UniversalElectricity")) {
          player.addChatMessage(new ChatComponentText("§aOpenComputers§f: ").appendSibling(
            new ChatComponentTranslation(Settings.namespace + "gui.Chat.WarningPower")))
        }
      case _ =>
    }
  }
}
