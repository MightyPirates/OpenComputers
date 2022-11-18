package li.cil.oc.common.container

import li.cil.oc.api.component.RackMountable
import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.RotationHelper
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.IntArrayNBT
import net.minecraft.util.Direction
import net.minecraftforge.common.util.Constants.NBT

class Rack(selfType: ContainerType[_ <: Rack], id: Int, playerInventory: PlayerInventory, val rack: IInventory)
  extends Player(selfType, id, playerInventory, rack) {

  override protected def getHostClass = classOf[tileentity.Rack]

  addSlotToContainer(20, 23, Slot.RackMountable)
  addSlotToContainer(20, 43, Slot.RackMountable)
  addSlotToContainer(20, 63, Slot.RackMountable)
  addSlotToContainer(20, 83, Slot.RackMountable)
  addPlayerInventorySlots(8, 128)

  final val MaxConnections = 4
  val nodePresence: Array[Array[Boolean]] = Array.fill(4)(Array.fill(4)(false))
  val nodeMapping: Array[Array[Option[Direction]]] = Array.fill(rack.getContainerSize)(Array.fill[Option[Direction]](4)(None))
  var isRelayEnabled = false

  override def updateCustomData(nbt: CompoundNBT): Unit = {
    super.updateCustomData(nbt)
    nbt.getList("nodeMapping", NBT.TAG_INT_ARRAY).map((sides: IntArrayNBT) => {
      sides.getAsIntArray.map(side => if (side >= 0) Option(Direction.from3DDataValue(side)) else None)
    }).copyToArray(nodeMapping)
    nbt.getBooleanArray("nodePresence").grouped(MaxConnections).copyToArray(nodePresence)
    isRelayEnabled = nbt.getBoolean("isRelayEnabled")
  }

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    super.detectCustomDataChanges(nbt)
    rack match {
      case te: tileentity.Rack => {
        nbt.setNewTagList("nodeMapping", te.nodeMapping.map(sides => toNbt(sides.map {
          case Some(side) => side.ordinal()
          case _ => -1
        })))
        nbt.setBooleanArray("nodePresence", (0 until te.getContainerSize).flatMap(slot => te.getMountable(slot) match {
          case mountable: RackMountable => (Seq(true) ++ (0 until math.min(MaxConnections - 1, mountable.getConnectableCount)).map(index => mountable.getConnectableAt(index) != null)).padTo(MaxConnections, false)
          case _ => Array.fill(MaxConnections)(false)
        }).toArray)
        nbt.putBoolean("isRelayEnabled", te.isRelayEnabled)
      }
      case _ =>
    }
  }
}
