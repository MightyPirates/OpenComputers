package li.cil.oc.common.tileentity

import cpw.mods.fml.common.network.Player
import li.cil.oc.api
import li.cil.oc.api.network.{Visibility, Message}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

class Keyboard extends Rotatable with Environment {
  val node = api.Network.createComponent(api.Network.createNode(this, "keyboard", Visibility.Network))

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    node.load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    node.save(nbt)
  }

  override def onMessage(message: Message) = {
    message.data match {
      case Array(p: Player, char: Character, code: Integer) if message.name == "keyboard.keyDown" =>
        if (isUseableByPlayer(p))
          node.network.sendToVisible(node, "computer.signal", "key_down", char, code)
      case Array(p: Player, char: Character, code: Integer) if message.name == "keyboard.keyUp" =>
        if (isUseableByPlayer(p))
          node.network.sendToVisible(node, "computer.signal", "key_up", char, code)
      case Array(p: Player, value: String) if message.name == "keyboard.clipboard" =>
        if (isUseableByPlayer(p))
          node.network.sendToVisible(node, "computer.signal", "clipboard", value)
      case _ =>
    }
    super.onMessage(message)
  }

  def isUseableByPlayer(p: Player) = worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
    p.asInstanceOf[EntityPlayer].getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 64
}