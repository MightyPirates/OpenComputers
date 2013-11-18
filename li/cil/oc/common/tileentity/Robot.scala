package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.Config
import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network._
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common
import li.cil.oc.server.component
import li.cil.oc.server.component.GraphicsCard
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.fluids.FluidRegistry
import scala.Some
import scala.collection.convert.WrapAsScala._

class Robot(isRemote: Boolean) extends Computer(isRemote) with Buffer with PowerInformation {
  def this() = this(false)

  // ----------------------------------------------------------------------- //

  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("computer", Visibility.Neighbors).
    create()

  override val buffer = new common.component.Buffer(this) {
    override def maxResolution = (48, 14)
  }
  override val computer = if (isRemote) null
  else new component.Computer(this) {
    override def isRobot(context: Context, args: Arguments): Array[AnyRef] =
      Array(java.lang.Boolean.TRUE)

    // ----------------------------------------------------------------------- //

    @LuaCallback("select")
    def select(context: Context, args: Arguments): Array[AnyRef] = {
      // Get or set selected inventory slot.
      if (args.count > 0 && args.checkAny(0) != null) {
        val slot = checkSlot(args, 0)
        if (slot != selectedSlot) {
          selectedSlot = slot
          ServerPacketSender.sendRobotSelectedSlotState(Robot.this)
        }
      }
      result(selectedSlot)
    }

    @LuaCallback("count")
    def count(context: Context, args: Arguments): Array[AnyRef] =
      result(stackInSlot(selectedSlot) match {
        case Some(stack) => stack.stackSize
        case _ => 0
      })

    @LuaCallback("space")
    def space(context: Context, args: Arguments): Array[AnyRef] =
      result(stackInSlot(selectedSlot) match {
        case Some(stack) => getInventoryStackLimit - stack.stackSize
        case _ => getInventoryStackLimit
      })

    @LuaCallback("compareTo")
    def compareTo(context: Context, args: Arguments): Array[AnyRef] = {
      val slot = checkSlot(args, 0)
      result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
        case (Some(stackA), Some(stackB)) => haveSameItemType(stackA, stackB)
        case (None, None) => true
        case _ => false
      })
    }

    @LuaCallback("transferTo")
    def transferTo(context: Context, args: Arguments): Array[AnyRef] = {
      val slot = checkSlot(args, 0)
      val count = checkOptionalItemCount(args, 1)
      if (slot == selectedSlot || count == 0) {
        result(true)
      }
      else result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
        case (Some(from), Some(to)) =>
          if (haveSameItemType(from, to)) {
            val space = (getInventoryStackLimit min to.getMaxStackSize) - to.stackSize
            val amount = count min space min from.stackSize
            if (amount > 0) {
              from.stackSize -= amount
              to.stackSize += amount
              assert(from.stackSize >= 0)
              if (from.stackSize == 0) {
                setInventorySlotContents(actualSlot(selectedSlot), null)
              }
              true
            }
            else false
          }
          else {
            setInventorySlotContents(actualSlot(slot), from)
            setInventorySlotContents(actualSlot(selectedSlot), to)
            true
          }
        case (Some(from), None) =>
          setInventorySlotContents(actualSlot(slot), decrStackSize(actualSlot(selectedSlot), count))
          true
        case _ => false
      })
    }

    @LuaCallback("drop")
    def drop(context: Context, args: Arguments): Array[AnyRef] = {
      val side = checkSideForAction(args, 0)
      val count = checkOptionalItemCount(args, 1)
      result(dropSlot(actualSlot(selectedSlot), count, side))
    }

    @LuaCallback("place")
    def place(context: Context, args: Arguments): Array[AnyRef] = {
      // Place block item selected in inventory.
      val side = checkSideForAction(args, 0)
      null
    }

    @LuaCallback("suck")
    def suck(context: Context, args: Arguments): Array[AnyRef] = {
      // Pick up items lying around.
      val side = checkSideForAction(args, 0)
      null
    }

    // ----------------------------------------------------------------------- //

    @LuaCallback("compare")
    def compare(context: Context, args: Arguments): Array[AnyRef] = {
      val side = checkSideForAction(args, 0)
      stackInSlot(selectedSlot) match {
        case Some(stack) => Option(stack.getItem) match {
          case Some(item: ItemBlock) =>
            val (bx, by, bz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
            val idMatches = item.getBlockID == world.getBlockId(bx, by, bz)
            val subTypeMatches = !item.getHasSubtypes || item.getMetadata(stack.getItemDamage) == world.getBlockMetadata(bx, by, bz)
            return result(idMatches && subTypeMatches)
          case _ =>
        }
        case _ =>
      }
      result(false)
    }

    @LuaCallback("detect")
    def detect(context: Context, args: Arguments): Array[AnyRef] = {
      val side = checkSideForAction(args, 0)
      val (bx, by, bz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
      val id = world.getBlockId(bx, by, bz)
      val block = Block.blocksList(id)
      if (id == 0 || block == null || block.isAirBlock(world, bx, by, bz)) {
        closestEntity(side) match {
          case Some(entity) => result(true, "entity")
          case _ => result(false, "air")
        }
      }
      else if (FluidRegistry.lookupFluidForBlock(block) != null) {
        result(false, "liquid")
      }
      else if (block.isBlockReplaceable(world, bx, by, bz)) {
        result(false, "replaceable")
      }
      else {
        result(true, "solid")
      }
    }

    // ----------------------------------------------------------------------- //

    @LuaCallback("attack")
    def attack(context: Context, args: Arguments): Array[AnyRef] = {
      // Attack with equipped tool.
      val side = checkSideForAction(args, 0)
      null
    }

    @LuaCallback("use")
    def use(context: Context, args: Arguments): Array[AnyRef] = {
      // Use equipped tool (e.g. dig, chop, till).
      val side = checkSideForAction(args, 0)
      val sneaky = args.checkBoolean(1)
      null
    }

    // ----------------------------------------------------------------------- //

    @LuaCallback("move")
    def move(context: Context, args: Arguments): Array[AnyRef] = {
      // Try to move in the specified direction.
      val side = checkSideForMovement(args, 0)
      null
    }

    @LuaCallback("turn")
    def turn(context: Context, args: Arguments): Array[AnyRef] = {
      // Turn in the specified direction.
      val clockwise = args.checkBoolean(0)
      if (clockwise) rotate(ForgeDirection.UP)
      else rotate(ForgeDirection.DOWN)
      result(true)
    }

    // ----------------------------------------------------------------------- //

    private def closestEntity(side: ForgeDirection) = {
      val (bx, by, bz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
      val id = world.getBlockId(bx, by, bz)
      val block = Block.blocksList(id)
      if (id == 0 || block == null || block.isAirBlock(world, bx, by, bz)) {
        val bounds = AxisAlignedBB.getAABBPool.getAABB(bx, by, bz, bx + 1, by + 1, bz + 1)
        val entities = world.getEntitiesWithinAABB(classOf[EntityLivingBase], bounds)
        entities.foldLeft((Double.PositiveInfinity, None: Option[EntityLivingBase])) {
          case ((bestDistance, bestEntity), entity: EntityLivingBase) =>
            val distance = entity.getDistanceSq(x + 0.5, y + 0.5, z + 0.5)
            if (distance < bestDistance) (distance, Some(entity))
            else (bestDistance, bestEntity)
          case (best, _) => best
        } match {
          case (_, Some(entity)) => Some(entity)
          case _ => None
        }
      }
      else None
    }

    private def haveSameItemType(stackA: ItemStack, stackB: ItemStack) =
      stackA.itemID == stackB.itemID &&
        (!stackA.getHasSubtypes || stackA.getItemDamage == stackB.getItemDamage)

    private def stackInSlot(slot: Int) = items(actualSlot(slot))

    private def checkOptionalItemCount(args: Arguments, n: Int) =
      if (args.count > n && args.checkAny(n) != null) {
        args.checkInteger(n) max 0 min getInventoryStackLimit
      }
      else getInventoryStackLimit

    private def checkSlot(args: Arguments, n: Int) = {
      val slot = args.checkInteger(n) - 1
      if (slot < 0 || slot > 15) {
        throw new IllegalArgumentException("invalid slot")
      }
      slot
    }

    private def checkSideForAction(args: Arguments, n: Int) = checkSide(args, n, ForgeDirection.SOUTH, ForgeDirection.UP, ForgeDirection.DOWN)

    private def checkSideForMovement(args: Arguments, n: Int) = checkSide(args, n, ForgeDirection.SOUTH, ForgeDirection.NORTH)

    private def checkSide(args: Arguments, n: Int, allowed: ForgeDirection*) = {
      val side = args.checkInteger(n)
      if (side < 0 || side > 5) {
        throw new IllegalArgumentException("invalid side")
      }
      val direction = ForgeDirection.getOrientation(side)
      if (allowed contains direction) toGlobal(direction)
      else throw new IllegalArgumentException("unsupported side")
    }
  }
  val (battery, distributor, gpu, keyboard) = if (isServer) {
    val battery = api.Network.newNode(this, Visibility.Network).withConnector(10000).create()
    val distributor = new component.PowerDistributor(this)
    val gpu = new GraphicsCard.Tier1 {
      override val maxResolution = (48, 14)
    }
    val keyboard = new component.Keyboard(this)
    (battery, distributor, gpu, keyboard)
  }
  else (null, null, null, null)

  var selectedSlot = 0

  def actualSlot(n: Int) = n + 3

  // ----------------------------------------------------------------------- //

  def tier = 0

  //def bounds =

  override def installedMemory = 64 * 1024

  // ----------------------------------------------------------------------- //

  @LuaCallback("start")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    Array(Boolean.box(computer.start()))

  @LuaCallback("stop")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    Array(Boolean.box(computer.stop()))

  @LuaCallback(value = "isRunning", direct = true)
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    Array(Boolean.box(computer.isRunning))

  @LuaCallback(value = "isRobot", direct = true)
  def isRobot(context: Context, args: Arguments): Array[AnyRef] =
    Array(java.lang.Boolean.TRUE)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      distributor.changeBuffer(10) // just for testing
      distributor.update()
      gpu.update()
    }
  }

  override def validate() {
    super.validate()
    if (isClient) {
      ClientPacketSender.sendRotatableStateRequest(this)
      ClientPacketSender.sendScreenBufferRequest(this)
      ClientPacketSender.sendRobotSelectedSlotRequest(this)
    }
  }

  override def invalidate() {
    super.invalidate()
    if (currentGui.isDefined) {
      Minecraft.getMinecraft.displayGuiScreen(null)
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (isServer) {
      battery.load(nbt.getCompoundTag(Config.namespace + "battery"))
      buffer.load(nbt.getCompoundTag(Config.namespace + "buffer"))
      distributor.load(nbt.getCompoundTag(Config.namespace + "distributor"))
      gpu.load(nbt.getCompoundTag(Config.namespace + "gpu"))
      keyboard.load(nbt.getCompoundTag(Config.namespace + "keyboard"))
    }
    selectedSlot = nbt.getInteger(Config.namespace + "selectedSlot")
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (isServer) {
      nbt.setNewCompoundTag(Config.namespace + "battery", battery.save)
      nbt.setNewCompoundTag(Config.namespace + "buffer", buffer.save)
      nbt.setNewCompoundTag(Config.namespace + "distributor", distributor.save)
      nbt.setNewCompoundTag(Config.namespace + "gpu", gpu.save)
      nbt.setNewCompoundTag(Config.namespace + "keyboard", keyboard.save)
    }
    nbt.setInteger(Config.namespace + "selectedSlot", selectedSlot)
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) {
    if (message.source.network == node.network) {
      //computer.node.network.sendToReachable(message.source, message.name, message.data: _*)
    }
    else {
      assert(message.source.network == computer.node.network)
      //node.network.sendToReachable(message.source, message.name, message.data: _*)
    }
  }

  override def onConnect(node: Node) {
    if (node == this.node) {
      api.Network.joinNewNetwork(computer.node)

      computer.node.connect(buffer.node)
      computer.node.connect(distributor.node)
      computer.node.connect(gpu.node)
      distributor.node.connect(battery)
      buffer.node.connect(keyboard.node)
    }
    super.onConnect(node)
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      battery.remove()
      buffer.node.remove()
      computer.node.remove()
      distributor.node.remove()
      gpu.node.remove()
      keyboard.node.remove()
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def connectItemNode(node: Node) {
    computer.node.connect(node)
  }

  @SideOnly(Side.CLIENT)
  override protected def markForRenderUpdate() {
    super.markForRenderUpdate()
    currentGui.foreach(_.recompileDisplayLists())
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Config.namespace + "container.Robot"

  def getSizeInventory = 19

  override def getInventoryStackLimit = 64

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (0, Some(driver)) => driver.slot(item) == Slot.Tool
    case (1, Some(driver)) => driver.slot(item) == Slot.Card
    case (2, Some(driver)) => driver.slot(item) == Slot.HardDiskDrive
    case (i, _) if 3 until getSizeInventory contains i => true // Normal inventory.
    case _ => false // Invalid slot.
  }

  override protected def onItemAdded(slot: Int, item: ItemStack) {
    if (slot >= 0 && slot < 3) {
      super.onItemAdded(slot, item)
    }
  }
}
