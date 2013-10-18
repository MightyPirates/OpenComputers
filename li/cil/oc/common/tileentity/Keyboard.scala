package li.cil.oc.common.tileentity

import cpw.mods.fml.common.network.Player
import li.cil.oc.api.network.{Component, Visibility, Message}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

class Keyboard extends Rotatable with Component {
  override val name = "keyboard"

  override val visibility = Visibility.Network

  componentVisibility = visibility

  override def canUpdate = false

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array(p: Player, char: Char, code: Int) if message.name == "keyboard.keyDown" =>
        if (isUseableByPlayer(p))
          network.foreach(_.sendToVisible(this, "computer.signal", "key_down", char, code))
      case Array(p: Player, char: Char, code: Int) if message.name == "keyboard.keyUp" =>
        if (isUseableByPlayer(p))
          network.foreach(_.sendToVisible(this, "computer.signal", "key_up", char, code))
      case Array(p: Player, value: String) if message.name == "keyboard.clipboard" =>
        if (isUseableByPlayer(p))
          network.foreach(_.sendToVisible(this, "computer.signal", "clipboard", value))
      case _ => // Ignore.
    }
    None
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    load(nbt.getCompoundTag("node"))
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)

    val nodeNbt = new NBTTagCompound
    save(nodeNbt)
    nbt.setCompoundTag("node", nodeNbt)
  }

  def isUseableByPlayer(p: Player) = worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
    p.asInstanceOf[EntityPlayer].getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 16
}