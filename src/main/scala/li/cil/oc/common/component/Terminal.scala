package li.cil.oc.common.component

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.common.item
import li.cil.oc.common.tileentity
import li.cil.oc.server.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.PackedColor.Depth
import li.cil.oc.{Items, Settings, common}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.{NBTTagString, NBTTagCompound}
import net.minecraftforge.common.util.Constants.NBT
import scala.collection.mutable

class Terminal(val rack: tileentity.Rack, val number: Int) extends Buffer.Owner {
  val buffer = new common.component.Buffer(this)
  val keyboard = if (buffer.node != null) {
    buffer.node.setVisibility(Visibility.Neighbors)
    new component.Keyboard {
      node.setVisibility(Visibility.Neighbors)

      override def isUseableByPlayer(p: EntityPlayer) = {
        val stack = p.getCurrentEquippedItem
        Items.multi.subItem(stack) match {
          case Some(t: item.Terminal) if stack.hasTagCompound => keys.contains(stack.getTagCompound.getString(Settings.namespace + "key"))
          case _ => false
        }
      }
    }
  }
  else null

  val keys = mutable.ListBuffer.empty[String]

  def isServer = rack.isServer

  def connect(node: Node) {
    node.connect(buffer.node)
    node.connect(keyboard.node)
    buffer.node.connect(keyboard.node)
  }

  override def tier = 1

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) {
    buffer.load(nbt.getCompoundTag(Settings.namespace + "buffer"))
    keyboard.load(nbt.getCompoundTag(Settings.namespace + "keyboard"))
    // Compatibility for previous dev versions where there was only one term.
    if (nbt.hasKey(Settings.namespace + "key")) {
      keys += nbt.getString(Settings.namespace + "key")
    }
    nbt.getTagList(Settings.namespace + "keys", NBT.TAG_STRING).foreach((list, index) => keys += list.getStringTagAt(index))
  }

  def save(nbt: NBTTagCompound) {
    nbt.setNewCompoundTag(Settings.namespace + "buffer", buffer.save)
    nbt.setNewCompoundTag(Settings.namespace + "keyboard", keyboard.save)
    nbt.setNewTagList("keys", keys)
  }

  @SideOnly(Side.CLIENT)
  def readFromNBTForClient(nbt: NBTTagCompound) {
    buffer.buffer.load(nbt)
    nbt.getTagList("keys", NBT.TAG_STRING).foreach((list, index) => keys += list.getStringTagAt(index))
  }

  def writeToNBTForClient(nbt: NBTTagCompound) {
    buffer.buffer.save(nbt)
    nbt.setNewTagList("keys", keys)
  }

  // ----------------------------------------------------------------------- //

  override def onScreenColorChange(foreground: Int, background: Int) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenColorChange(buffer, foreground, background)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  override def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenCopy(buffer, col, row, w, h, tx, ty)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  override def onScreenDepthChange(depth: Depth.Value) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenDepthChange(buffer, depth)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  override def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenFill(buffer, col, row, w, h, c)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  override def onScreenResolutionChange(w: Int, h: Int) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenResolutionChange(buffer, w, h)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }

  override def onScreenSet(col: Int, row: Int, s: String) {
    if (isServer) {
      rack.markAsChanged()
      ServerPacketSender.sendScreenSet(buffer, col, row, s)
    }
    else currentGui.foreach(_.recompileDisplayLists())
  }
}
