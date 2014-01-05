package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.api.network.{Analyzable, SidedEnvironment}
import li.cil.oc.server.component
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Blocks, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

class Keyboard(isRemote: Boolean) extends Environment with SidedEnvironment with Analyzable with Rotatable with PassiveNode {
  def this() = this(false)

  val keyboard = if (isRemote) null else new component.Keyboard(this)

  def node = if (isClient) null else keyboard.node

  override lazy val isClient = keyboard == null

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = node

  @SideOnly(Side.CLIENT)
  def canConnect(side: ForgeDirection) = side == facing.getOpposite

  def sidedNode(side: ForgeDirection) =
    if (hasNodeOnSide(side)) node else null

  def hasNodeOnSide(side: ForgeDirection) =
    side == facing.getOpposite || side == forward || (isOnWall && side == forward.getOpposite)

  def isOnWall = facing != ForgeDirection.UP && facing != ForgeDirection.DOWN

  def forward = if (isOnWall) ForgeDirection.UP else yaw

  override def canUpdate = false

  override def validate() {
    super.validate()
    world.scheduleBlockUpdateFromLoad(x, y, z, Blocks.keyboard.parent.blockID, 0, 0)
  }

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
}
