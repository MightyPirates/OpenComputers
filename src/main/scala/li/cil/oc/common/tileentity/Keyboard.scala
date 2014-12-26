package li.cil.oc.common.tileentity

import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

class Keyboard extends traits.Environment with traits.Rotatable with traits.ImmibisMicroblock with SidedEnvironment with Analyzable {
  override def validFacings = EnumFacing.values

  val keyboard = {
    val keyboardItem = api.Items.get("keyboard").createItemStack(1)
    api.Driver.driverFor(keyboardItem, getClass).createEnvironment(keyboardItem, this)
  }

  override def node = keyboard.node

  def hasNodeOnSide(side: EnumFacing) =
    side == facing.getOpposite || side == forward || (isOnWall && side == forward.getOpposite)

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: EnumFacing) = hasNodeOnSide(side)

  override def sidedNode(side: EnumFacing) = if (hasNodeOnSide(side)) node else null

  // Override automatic analyzer implementation for sided environments.
  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = Array(node)

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (isServer) {
      keyboard.load(nbt.getCompoundTag(Settings.namespace + "keyboard"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (isServer) {
      nbt.setNewCompoundTag(Settings.namespace + "keyboard", keyboard.save)
    }
  }

  // ----------------------------------------------------------------------- //

  private def isOnWall = facing != EnumFacing.UP && facing != EnumFacing.DOWN

  private def forward = if (isOnWall) EnumFacing.UP else yaw
}
