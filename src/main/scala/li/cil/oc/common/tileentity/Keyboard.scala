package li.cil.oc.common.tileentity

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class Keyboard extends TileEntity(null) with traits.Environment with traits.Rotatable with traits.ImmibisMicroblock with SidedEnvironment with Analyzable {
  override def validFacings = Direction.values

  val keyboard = {
    val keyboardItem = api.Items.get(Constants.BlockName.Keyboard).createItemStack(1)
    api.Driver.driverFor(keyboardItem, getClass).createEnvironment(keyboardItem, this)
  }

  override def node = keyboard.node

  def hasNodeOnSide(side: Direction) : Boolean =
    side != facing && (isOnWall || side.getOpposite != forward)

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override def canConnect(side: Direction) = hasNodeOnSide(side)

  override def sidedNode(side: Direction) = if (hasNodeOnSide(side)) node else null

  // Override automatic analyzer implementation for sided environments.
  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = Array(node)

  // ----------------------------------------------------------------------- //

  // ----------------------------------------------------------------------- //

  private final val KeyboardTag = Settings.namespace + "keyboard"

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    if (isServer) {
      keyboard.loadData(nbt.getCompound(KeyboardTag))
    }
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    if (isServer) {
      nbt.setNewCompoundTag(KeyboardTag, keyboard.saveData)
    }
  }

  // ----------------------------------------------------------------------- //

  private def isOnWall = facing != Direction.UP && facing != Direction.DOWN

  private def forward = if (isOnWall) Direction.UP else yaw
}
