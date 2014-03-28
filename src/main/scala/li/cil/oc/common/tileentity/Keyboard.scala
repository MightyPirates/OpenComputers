package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.api.network.{Analyzable, SidedEnvironment}
import li.cil.oc.common.EventHandler
import li.cil.oc.server.component
import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class Keyboard(isRemote: Boolean) extends traits.Environment with traits.Rotatable with SidedEnvironment with Analyzable {
  def this() = this(false)

  override def validFacings = ForgeDirection.VALID_DIRECTIONS

  val keyboard = if (isRemote) null
  else new component.Keyboard {
    override def isUseableByPlayer(p: EntityPlayer) =
      world.getTileEntity(x, y, z) == Keyboard.this &&
        p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
  }

  override def node = if (isClient) null else keyboard.node

  override lazy val isClient = keyboard == null

  def hasNodeOnSide(side: ForgeDirection) =
    side == facing.getOpposite || side == forward || (isOnWall && side == forward.getOpposite)

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side == facing.getOpposite

  override def sidedNode(side: ForgeDirection) =
    if (hasNodeOnSide(side)) node else null

  // Override automatic analyzer implementation for sided environments.
  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = Array(node)

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override def validate() {
    super.validate()
    EventHandler.schedule(this)
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

  // ----------------------------------------------------------------------- //

  private def isOnWall = facing != ForgeDirection.UP && facing != ForgeDirection.DOWN

  private def forward = if (isOnWall) ForgeDirection.UP else yaw
}
