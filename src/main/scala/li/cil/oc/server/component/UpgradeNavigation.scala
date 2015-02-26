package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.internal.Rotatable
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.common.item.data.NavigationUpgradeData
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class UpgradeNavigation(val host: EnvironmentHost with Rotatable) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("navigation", Visibility.Neighbors).
    create()

  val data = new NavigationUpgradeData()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number, number, number -- Get the current relative position of the robot.""")
  def getPosition(context: Context, args: Arguments): Array[AnyRef] = {
    val info = data.mapData(host.world)
    val size = 128 * (1 << info.scale)
    val relativeX = host.xPosition - info.xCenter
    val relativeZ = host.zPosition - info.zCenter

    if (math.abs(relativeX) <= size / 2 && math.abs(relativeZ) <= size / 2)
      result(relativeX, host.yPosition, relativeZ)
    else
      result(Unit, "out of range")
  }

  @Callback(doc = """function():number -- Get the current orientation of the robot.""")
  def getFacing(context: Context, args: Arguments): Array[AnyRef] = result(host.facing.ordinal)

  @Callback(doc = """function():number -- Get the operational range of the navigation upgrade.""")
  def getRange(context: Context, args: Arguments): Array[AnyRef] = {
    val info = data.mapData(host.world)
    val size = 128 * (1 << info.scale)
    result(size / 2)
  }

  override def onMessage(message: Message): Unit = {
    super.onMessage(message)
    if (message.name == "tablet.use") message.source.host match {
      case machine: api.machine.Machine => (machine.host, message.data) match {
        case (tablet: internal.Tablet, Array(nbt: NBTTagCompound, stack: ItemStack, player: EntityPlayer, blockPos: BlockPosition, side: ForgeDirection, hitX: java.lang.Float, hitY: java.lang.Float, hitZ: java.lang.Float)) =>
          val info = data.mapData(host.world)
          nbt.setInteger("posX", blockPos.x - info.xCenter)
          nbt.setInteger("posY", blockPos.y)
          nbt.setInteger("posZ", blockPos.z - info.zCenter)
        case _ => // Ignore.
      }
      case _ => // Ignore.
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    data.load(nbt)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    data.save(nbt)
  }
}
