package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.Settings
import li.cil.oc.api.network.{Analyzable, SidedEnvironment}
import li.cil.oc.server.{TickHandler, component}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

class Keyboard(isRemote: Boolean) extends Environment with SidedEnvironment with Analyzable with Rotatable {
  def this() = this(false)

  val keyboard = if (isRemote) null
  else new component.Keyboard {
    override def isUseableByPlayer(p: EntityPlayer) =
      world.getBlockTileEntity(x, y, z) == Keyboard.this &&
        p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
  }

  override def node = if (isClient) null else keyboard.node

  override lazy val isClient = keyboard == null

  // Override automatic analyzer implementation for sided environments.
  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = Array(node)

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side == facing.getOpposite

  override def sidedNode(side: ForgeDirection) =
    if (hasNodeOnSide(side)) node else null

  def hasNodeOnSide(side: ForgeDirection) =
    side == facing.getOpposite || side == forward || (isOnWall && side == forward.getOpposite)

  def isOnWall = facing != ForgeDirection.UP && facing != ForgeDirection.DOWN

  def forward = if (isOnWall) ForgeDirection.UP else yaw

  override def canUpdate = false

  override def validate() {
    super.validate()
    TickHandler.schedule(this)
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
