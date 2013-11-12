package li.cil.oc.common.tileentity

import cpw.mods.fml.common.IPlayerTracker
import li.cil.oc.api.Network
import li.cil.oc.api.network.{Visibility, Message}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.{Event, ForgeSubscribe}
import scala.collection.mutable

class Keyboard extends Environment with Rotatable {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("keyboard").
    create()

  val pressedKeys = mutable.Map.empty[EntityPlayer, mutable.Map[Integer, Character]]

  // ----------------------------------------------------------------------- //

  override def validate() {
    super.validate()
    MinecraftForge.EVENT_BUS.register(this)
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    MinecraftForge.EVENT_BUS.unregister(this)
  }

  override def invalidate() {
    super.invalidate()
    MinecraftForge.EVENT_BUS.unregister(this)
  }

  // ----------------------------------------------------------------------- //

  @ForgeSubscribe
  def onReleasePressedKeys(e: Keyboard.ReleasePressedKeys) {
    pressedKeys.get(e.player) match {
      case Some(keys) => for ((code, char) <- keys)
        node.sendToReachable("computer.checked_signal", e.player, "key_up", char, code, e.player.getCommandSenderName)
      case _ =>
    }
    pressedKeys.remove(e.player)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    node.load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    node.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) = {
    message.data match {
      case Array(p: EntityPlayer, char: Character, code: Integer) if message.name == "keyboard.keyDown" =>
        if (isUseableByPlayer(p)) {
          pressedKeys.getOrElseUpdate(p, mutable.Map.empty[Integer, Character]) += code -> char
          node.sendToReachable("computer.checked_signal", p, "key_down", char, code, p.getCommandSenderName)
        }
      case Array(p: EntityPlayer, char: Character, code: Integer) if message.name == "keyboard.keyUp" =>
        pressedKeys.get(p) match {
          case Some(keys) if keys.contains(code) =>
            keys -= code
            node.sendToReachable("computer.checked_signal", p, "key_up", char, code, p.getCommandSenderName)
          case _ =>
        }
      case Array(p: EntityPlayer, value: String) if message.name == "keyboard.clipboard" =>
        if (isUseableByPlayer(p)) {
          node.sendToReachable("computer.checked_signal", p, "clipboard", value, p.getCommandSenderName)
        }
      case _ =>
    }
    super.onMessage(message)
  }

  // ----------------------------------------------------------------------- //

  private def isUseableByPlayer(p: EntityPlayer) =
    worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
      p.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 64
}

object Keyboard extends IPlayerTracker {

  def onPlayerRespawn(player: EntityPlayer) {
    MinecraftForge.EVENT_BUS.post(new ReleasePressedKeys(player))
  }

  def onPlayerChangedDimension(player: EntityPlayer) {
    MinecraftForge.EVENT_BUS.post(new ReleasePressedKeys(player))
  }

  def onPlayerLogout(player: EntityPlayer) {
    MinecraftForge.EVENT_BUS.post(new ReleasePressedKeys(player))
  }

  def onPlayerLogin(player: EntityPlayer) {}

  class ReleasePressedKeys(val player: EntityPlayer) extends Event

}