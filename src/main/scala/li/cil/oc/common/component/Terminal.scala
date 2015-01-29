package li.cil.oc.common.component

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.component.Keyboard.UsabilityChecker
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.mutable

class Terminal(val rack: tileentity.ServerRack, val number: Int) {
  val buffer = {
    val screenItem = api.Items.get("screen1").createItemStack(1)
    val buffer = api.Driver.driverFor(screenItem, rack.getClass).createEnvironment(screenItem, rack).asInstanceOf[api.component.TextBuffer]
    val (maxWidth, maxHeight) = Settings.screenResolutionsByTier(1)
    buffer.setMaximumResolution(maxWidth, maxHeight)
    buffer.setMaximumColorDepth(Settings.screenDepthsByTier(1))
    buffer
  }

  val keyboard = {
    val keyboardItem = api.Items.get("keyboard").createItemStack(1)
    val keyboard = api.Driver.driverFor(keyboardItem, rack.getClass).createEnvironment(keyboardItem, rack).asInstanceOf[api.component.Keyboard]
    keyboard.setUsableOverride(new UsabilityChecker {
      override def isUsableByPlayer(keyboard: api.component.Keyboard, player: EntityPlayer) = {
        val stack = player.getCurrentEquippedItem
        Delegator.subItem(stack) match {
          case Some(t: item.Terminal) if stack.hasTagCompound => keys.contains(stack.getTagCompound.getString(Settings.namespace + "key"))
          case _ => false
        }
      }
    })
    keyboard
  }

  if (buffer.node != null) {
    buffer.node.asInstanceOf[Component].setVisibility(Visibility.Neighbors)
    keyboard.node.asInstanceOf[Component].setVisibility(Visibility.Neighbors)
  }

  val keys = mutable.ListBuffer.empty[String]

  def connect(node: Node) {
    if (keys.size > 0) {
      node.connect(buffer.node)
      node.connect(keyboard.node)
      buffer.node.connect(keyboard.node)
    }
  }

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) {
    buffer.load(nbt.getCompoundTag(Settings.namespace + "buffer"))
    keyboard.load(nbt.getCompoundTag(Settings.namespace + "keyboard"))
    // Compatibility for previous dev versions where there was only one term.
    if (nbt.hasKey(Settings.namespace + "key")) {
      keys += nbt.getString(Settings.namespace + "key")
    }
    nbt.getTagList(Settings.namespace + "keys", NBT.TAG_STRING).foreach((tag: NBTTagString) => keys += tag.getString)
  }

  def save(nbt: NBTTagCompound) {
    nbt.setNewCompoundTag(Settings.namespace + "buffer", buffer.save)
    nbt.setNewCompoundTag(Settings.namespace + "keyboard", keyboard.save)
    nbt.setNewTagList(Settings.namespace + "keys", keys)
  }

  @SideOnly(Side.CLIENT)
  def readFromNBTForClient(nbt: NBTTagCompound) {
    buffer.load(nbt)
    nbt.getTagList("keys", NBT.TAG_STRING).foreach((tag: NBTTagString) => keys += tag.getString)
  }

  def writeToNBTForClient(nbt: NBTTagCompound) {
    buffer.save(nbt)
    nbt.setNewTagList("keys", keys)
  }
}
