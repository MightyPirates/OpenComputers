package li.cil.oc.common.component

import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.common.tileentity
import li.cil.oc.server.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.PackedColor.Depth
import li.cil.oc.{Settings, common}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

class Terminal(val rack: tileentity.Rack, val number: Int) extends Buffer.Owner {
  val buffer = new common.component.Buffer(this)
  val keyboard = if (buffer.node != null) {
    buffer.node.setVisibility(Visibility.Neighbors)
    new component.Keyboard {
      override def isUseableByPlayer(p: EntityPlayer) = true // TODO if player has bound terminal
    }
  }
  else null

  def isServer = rack.isServer

  def connect(node: Node) {
    node.connect(buffer.node)
    node.connect(keyboard.node)
    buffer.node.connect(keyboard.node)
  }

  def tier = 1

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) {
    buffer.load(nbt.getCompoundTag(Settings.namespace + "buffer"))
    keyboard.load(nbt.getCompoundTag(Settings.namespace + "keyboard"))
  }

  def save(nbt: NBTTagCompound) {
    nbt.setNewCompoundTag(Settings.namespace + "buffer", buffer.save)
    nbt.setNewCompoundTag(Settings.namespace + "keyboard", keyboard.save)
  }

  // ----------------------------------------------------------------------- //

  def onScreenColorChange(foreground: Int, background: Int) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenColorChange(buffer, foreground, background)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenCopy(buffer, col, row, w, h, tx, ty)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  def onScreenDepthChange(depth: Depth.Value) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenDepthChange(buffer, depth)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenFill(buffer, col, row, w, h, c)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  def onScreenResolutionChange(w: Int, h: Int) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenResolutionChange(buffer, w, h)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  def onScreenSet(col: Int, row: Int, s: String) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenSet(buffer, col, row, s)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }
}
