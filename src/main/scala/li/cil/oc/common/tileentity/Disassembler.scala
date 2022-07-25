package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.util.StateAware
import li.cil.oc.common.template.DisassemblerTemplates
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Disassembler extends TileEntity(null) with traits.Environment with traits.PowerAcceptor with traits.Inventory with traits.StateAware with traits.PlayerInputAware with traits.Tickable with DeviceInfo {
  val node: Connector = api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferConverter).
    create()

  var isActive = false

  val queue: ArrayBuffer[ItemStack] = mutable.ArrayBuffer.empty[ItemStack]

  var totalRequiredEnergy = 0.0

  override def getMaxStackSize: Int = 1

  var buffer = 0.0

  var disassembleNextInstantly = false

  def progress: Double = if (queue.isEmpty) 0.0 else (1 - (queue.size * Settings.get.disassemblerItemCost - buffer) / totalRequiredEnergy) * 100

  private def setActive(value: Boolean) = if (value != isActive) {
    isActive = value
    ServerPacketSender.sendDisassemblerActive(this, isActive)
    getLevel.updateNeighborsAt(getBlockPos, getBlockState.getBlock)
  }

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Disassembler",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Break.3R-100"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override protected def hasConnector(side: Direction): Boolean = side != Direction.UP

  override protected def connector(side: Direction) = Option(if (side != Direction.UP) node else null)

  override def energyThroughput: Double = Settings.get.disassemblerRate

  override def getCurrentState: util.EnumSet[StateAware.State] = {
    if (isActive) util.EnumSet.of(api.util.StateAware.State.IsWorking)
    else if (queue.nonEmpty) util.EnumSet.of(api.util.StateAware.State.CanWork)
    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer && getLevel.getGameTime % Settings.get.tickFrequency == 0) {
      if (queue.isEmpty) {
        val instant = disassembleNextInstantly // Is reset via removeItem
        disassemble(removeItem(0, 1), instant)
        setActive(queue.nonEmpty)
      }
      else {
        if (buffer < Settings.get.disassemblerItemCost) {
          val want = Settings.get.disassemblerTickAmount
          val success = node.tryChangeBuffer(-want)
          setActive(success) // If energy is insufficient indicate it visually.
          if (success) {
            buffer += want
          }
        }
        while (buffer >= Settings.get.disassemblerItemCost && queue.nonEmpty) {
          buffer -= Settings.get.disassemblerItemCost
          val stack = queue.remove(0)
          if (disassembleNextInstantly || getLevel.random.nextDouble >= Settings.get.disassemblerBreakChance) {
            drop(stack)
          }
        }
      }
      disassembleNextInstantly = queue.nonEmpty // If we have nothing left to do, stop being creative.
    }
  }

  def disassemble(stack: ItemStack, instant: Boolean = false) {
    // Validate the item, never trust Minecraft / other Mods on anything!
    if (canPlaceItem(0, stack)) {
      val ingredients = ItemUtils.getIngredients(getLevel.getRecipeManager, stack)
      DisassemblerTemplates.select(stack) match {
        case Some(template) =>
          val (stacks, drops) = template.disassemble(stack, ingredients)
          stacks.foreach(queue ++= _)
          drops.foreach(_.foreach(drop))
        case _ => queue ++= ingredients
      }
      totalRequiredEnergy = queue.size * Settings.get.disassemblerItemCost
      if (instant) {
        buffer = totalRequiredEnergy
      }
    }
    else {
      drop(stack)
    }
  }

  private def drop(stack: ItemStack) {
    if (!stack.isEmpty) {
      for (side <- Direction.values if stack.getCount > 0) {
        InventoryUtils.insertIntoInventoryAt(stack, BlockPosition(this).offset(side), Some(side.getOpposite))
      }
      if (stack.getCount > 0) {
        spawnStackInWorld(stack, Option(Direction.UP))
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val QueueTag = Settings.namespace + "queue"
  private final val BufferTag = Settings.namespace + "buffer"
  private final val TotalTag = Settings.namespace + "total"
  private final val IsActiveTag = Settings.namespace + "isActive"

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    queue.clear()
    queue ++= nbt.getList(QueueTag, NBT.TAG_COMPOUND).
      map((tag: CompoundNBT) => ItemStack.of(tag))
    buffer = nbt.getDouble(BufferTag)
    totalRequiredEnergy = nbt.getDouble(TotalTag)
    isActive = queue.nonEmpty
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    nbt.setNewTagList(QueueTag, queue)
    nbt.putDouble(BufferTag, buffer)
    nbt.putDouble(TotalTag, totalRequiredEnergy)
  }

  @OnlyIn(Dist.CLIENT)
  override def loadForClient(nbt: CompoundNBT) {
    super.loadForClient(nbt)
    isActive = nbt.getBoolean(IsActiveTag)
  }

  override def saveForClient(nbt: CompoundNBT) {
    super.saveForClient(nbt)
    nbt.putBoolean(IsActiveTag, isActive)
  }

  // ----------------------------------------------------------------------- //

  override def getContainerSize = 1

  override def canPlaceItem(i: Int, stack: ItemStack): Boolean =
    allowDisassembling(stack) &&
      (((Settings.get.disassembleAllTheThings || api.Items.get(stack) != null) && ItemUtils.getIngredients(getLevel.getRecipeManager, stack).nonEmpty) ||
        DisassemblerTemplates.select(stack).isDefined)

  private def allowDisassembling(stack: ItemStack) = !stack.isEmpty && (!stack.hasTag || !stack.getTag.getBoolean(Settings.namespace + "undisassemblable"))

  override def setItem(slot: Int, stack: ItemStack): Unit = {
    super.setItem(slot, stack)
    if (!getLevel.isClientSide) {
      disassembleNextInstantly = false
    }
  }

  override def onSetInventorySlotContents(player: PlayerEntity, slot: Int, stack: ItemStack): Unit = {
    if (!getLevel.isClientSide) {
      disassembleNextInstantly = !stack.isEmpty && slot == 0 && player.isCreative
    }
  }
}
