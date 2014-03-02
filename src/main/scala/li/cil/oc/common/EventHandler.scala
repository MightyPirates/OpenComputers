package li.cil.oc.common

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.PlayerEvent._
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import cpw.mods.fml.common.{Loader, FMLCommonHandler}
import li.cil.oc.api.Network
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.mods.ProjectRed
import li.cil.oc.{UpdateCheck, Items, Settings}
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.{ItemMap, ItemStack}
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{ChatComponentTranslation, ChatComponentText}
import scala.collection.mutable

object EventHandler {
  val pendingAdds = mutable.Buffer.empty[() => Unit]

  def schedule(tileEntity: TileEntity) = pendingAdds.synchronized {
    pendingAdds += (() => Network.joinOrCreateNetwork(tileEntity))
  }

  /* TODO FMP
  @Optional.Method(modid = "ForgeMultipart")
  def schedule(part: TMultiPart) = pendingAdds.synchronized {
    pendingAdds += (() => Network.joinOrCreateNetwork(part.tile))
  }
  */

  @SubscribeEvent
  def onTick(e: ServerTickEvent) = pendingAdds.synchronized {
    for (callback <- pendingAdds) {
      callback()
    }
    pendingAdds.clear()
  }

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
        // Do update check in local games and for OPs.
        if (!MinecraftServer.getServer.isDedicatedServer || MinecraftServer.getServer.getConfigurationManager.isPlayerOpped(player.getCommandSenderName)) {
          UpdateCheck.checkForPlayer(player)
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onCrafting(e: ItemCraftedEvent) = {
    val player = e.player
    val craftedStack = e.crafting
    val inventory = e.craftMatrix
    if (craftedStack.isItemEqual(Items.acid.createItemStack())) {
      for (i <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (stack != null && stack.getItem == net.minecraft.init.Items.water_bucket) {
          stack.stackSize = 0
          inventory.setInventorySlotContents(i, null)
        }
      }
    }

    if (craftedStack.isItemEqual(Items.pcb.createItemStack())) {
      for (i <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (stack != null && stack.isItemEqual(Items.acid.createItemStack())) {
          val container = new ItemStack(net.minecraft.init.Items.bucket, 1)
          if (!player.inventory.addItemStackToInventory(container)) {
            player.entityDropItem(container, 0)
          }
        }
      }
    }

    if (craftedStack.isItemEqual(Items.upgradeNavigation.createItemStack())) {
      Registry.itemDriverFor(craftedStack) match {
        case Some(driver) =>
          var oldMap = None: Option[ItemStack]
          for (i <- 0 until inventory.getSizeInventory) {
            val stack = inventory.getStackInSlot(i)
            if (stack != null) {
              if (stack.isItemEqual(Items.upgradeNavigation.createItemStack())) {
                // Restore the map currently used in the upgrade.
                val nbt = driver.dataTag(stack)
                oldMap = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(Settings.namespace + "map")))
              }
              else if (stack.getItem == net.minecraft.init.Items.map) {
                // Store information of the map used for crafting in the result.
                val nbt = driver.dataTag(craftedStack)
                val map = stack.getItem.asInstanceOf[ItemMap]
                val info = map.getMapData(stack, player.getEntityWorld)
                nbt.setInteger(Settings.namespace + "xCenter", info.xCenter)
                nbt.setInteger(Settings.namespace + "zCenter", info.zCenter)
                nbt.setInteger(Settings.namespace + "scale", 128 * (1 << info.scale))
                nbt.setNewCompoundTag(Settings.namespace + "map", stack.writeToNBT)
              }
            }
          }
          if (oldMap.isDefined) {
            val map = oldMap.get
            if (!player.inventory.addItemStackToInventory(map)) {
              player.dropPlayerItemWithRandomChoice(map, false)
            }
          }
        case _ =>
      }
    }
  }
}