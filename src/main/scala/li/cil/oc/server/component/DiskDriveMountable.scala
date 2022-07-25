package li.cil.oc.server.component

import java.util

import li.cil.oc.{Constants, OpenComputers, api}
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.Driver
import li.cil.oc.api.component.RackBusConnectable
import li.cil.oc.api.component.RackMountable
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.common.{GuiType, Slot, Sound}
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.inventory.ItemStackInventory
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraft.util.Hand

import scala.collection.convert.ImplicitConversionsToJava._

class DiskDriveMountable(val rack: api.internal.Rack, val slot: Int) extends AbstractManagedEnvironment with ItemStackInventory with ComponentInventory with RackMountable with Analyzable with DeviceInfo {
  // Stored for filling data packet when queried.
  var lastAccess = 0L

  def filesystemNode: Option[Node] = components(0) match {
    case Some(environment) => Option(environment.node)
    case _ => None
  }

  // ----------------------------------------------------------------------- //
  // DeviceInfo

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Disk,
    DeviceAttribute.Description -> "Floppy disk drive",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "RackDrive 100 Rev. 2"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //
  // Environment

  override val node: Component = api.Network.newNode(this, Visibility.Network).
    withComponent("disk_drive").
    create()

  @Callback(doc = """function():boolean -- Checks whether some medium is currently in the drive.""")
  def isEmpty(context: Context, args: Arguments): Array[AnyRef] = {
    result(filesystemNode.isEmpty)
  }

  @Callback(doc = """function([velocity:number]):boolean -- Eject the currently present medium from the drive.""")
  def eject(context: Context, args: Arguments): Array[AnyRef] = {
    val velocity = args.optDouble(0, 0) max 0 min 1
    val ejected = removeItem(0, 1)
    if (!ejected.isEmpty) {
      val entity = InventoryUtils.spawnStackInWorld(BlockPosition(rack), ejected, Option(rack.facing))
      if (entity != null) {
        val vx = rack.facing.getStepX * velocity
        val vy = rack.facing.getStepY * velocity
        val vz = rack.facing.getStepZ * velocity
        entity.push(vx, vy, vz)
      }
      result(true)
    }
    else result(false)
  }

  // ----------------------------------------------------------------------- //
  // Analyzable

  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = filesystemNode.fold(null: Array[Node])(Array(_))

  // ----------------------------------------------------------------------- //
  // ItemStackInventory

  override def host: EnvironmentHost = rack

  // ----------------------------------------------------------------------- //
  // IInventory

  override def getContainerSize: Int = 1

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Floppy
    case _ => false
  }

  override def stillValid(player: PlayerEntity): Boolean = rack.stillValid(player)

  // ----------------------------------------------------------------------- //
  // ComponentInventory

  override def container: ItemStack = rack.getItem(slot)

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    components(slot) match {
      case Some(environment) => environment.node match {
        case component: Component => component.setVisibility(Visibility.Network)
      }
      case _ =>
    }
    if (!rack.world.isClientSide) {
      rack.markChanged(this.slot)
      Sound.playDiskInsert(rack)
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (!rack.world.isClientSide) {
      rack.markChanged(this.slot)
      Sound.playDiskEject(rack)
    }
  }

  // ----------------------------------------------------------------------- //
  // ManagedEnvironment

  override def canUpdate: Boolean = false

  // ----------------------------------------------------------------------- //
  // Persistable

  override def loadData(nbt: CompoundNBT) {
    super[AbstractManagedEnvironment].loadData(nbt)
    super[ComponentInventory].loadData(nbt)
    connectComponents()
  }

  override def saveData(nbt: CompoundNBT) {
    super[AbstractManagedEnvironment].saveData(nbt)
    super[ComponentInventory].saveData(nbt)
  }

  // ----------------------------------------------------------------------- //
  // RackMountable

  override def getData: CompoundNBT = {
    val nbt = new CompoundNBT()
    nbt.putLong("lastAccess", lastAccess)
    nbt.put("disk", toNbt(getItem(0)))
    nbt
  }

  override def getConnectableCount: Int = 0

  override def getConnectableAt(index: Int): RackBusConnectable = null

  override def onActivate(player: PlayerEntity, hand: Hand, heldItem: ItemStack, hitX: Float, hitY: Float): Boolean = {
    if (player.isCrouching) {
      val isDiskInDrive = !getItem(0).isEmpty
      val isHoldingDisk = canPlaceItem(0, heldItem)
      if (isDiskInDrive) {
        if (!rack.world.isClientSide) {
          InventoryUtils.dropSlot(BlockPosition(rack), this, 0, 1, Option(rack.facing))
        }
      }
      if (isHoldingDisk) {
        // Insert the disk.
        setItem(0, player.inventory.removeItem(player.inventory.selected, 1))
      }
      isDiskInDrive || isHoldingDisk
    }
    else {
      val position = BlockPosition(rack)
      OpenComputers.openGui(player, GuiType.DiskDriveMountableInRack.id, rack.world, position.x, GuiType.embedSlot(position.y, slot), position.z)
      true
    }
  }

  // ----------------------------------------------------------------------- //
  // StateAware

  override def getCurrentState: util.EnumSet[api.util.StateAware.State] = util.EnumSet.noneOf(classOf[api.util.StateAware.State])
}
