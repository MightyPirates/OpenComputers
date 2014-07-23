package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.{Analyzable, SidedEnvironment}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class Keyboard extends traits.Environment with traits.Rotatable with traits.ImmibisMicroblock with SidedEnvironment with Analyzable {
  override def validFacings = ForgeDirection.VALID_DIRECTIONS

  val keyboard = {
    val keyboardItem = api.Items.get("keyboard").createItemStack(1)
    api.Driver.driverFor(keyboardItem).createEnvironment(keyboardItem, this)
  }

  override def node = keyboard.node

  def hasNodeOnSide(side: ForgeDirection) =
    side == facing.getOpposite || side == forward || (isOnWall && side == forward.getOpposite)

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = hasNodeOnSide(side)

  override def sidedNode(side: ForgeDirection) = if (hasNodeOnSide(side)) node else null

  // Override automatic analyzer implementation for sided environments.
  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = Array(node)

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

  private def isOnWall = facing != ForgeDirection.UP && facing != ForgeDirection.DOWN

  private def forward = if (isOnWall) ForgeDirection.UP else yaw
}
