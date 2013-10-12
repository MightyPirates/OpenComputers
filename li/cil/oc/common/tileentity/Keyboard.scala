package li.cil.oc.common.tileentity

import cpw.mods.fml.common.network.Player
import li.cil.oc.api.network.{Visibility, Node, Message}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

class Keyboard extends Rotatable with Node {
  override val name = "keyboard"

  override val visibility = Visibility.Network

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
    load(nbt.getCompoundTag("data"))
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)

    val dataNbt = new NBTTagCompound
    save(dataNbt)
    nbt.setCompoundTag("data", dataNbt)
  }

  def isUseableByPlayer(p: Player) = worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
    p.asInstanceOf[EntityPlayer].getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 16
}