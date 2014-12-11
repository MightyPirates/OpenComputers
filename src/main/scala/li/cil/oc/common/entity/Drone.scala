package li.cil.oc.common.entity

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.Machine
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.Memory
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network._
import li.cil.oc.common.Loot
import li.cil.oc.common.Slot
import li.cil.oc.common.init.Items
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.server.component
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World

class Drone(val world: World) extends Entity(world) with ComponentInventory with Environment with EnvironmentHost with MachineHost {
  // Some basic constants.
  val gravity = 0.04f
  val drag = 0.8f
  val bounds = AxisAlignedBB.getBoundingBox(-8, -3, -8, 8, 3, 8)
  val maxAcceleration = 0.1f
  val maxVelocity = 1

  // Rendering stuff, purely eyecandy.
  val targetFlapAngles = Array.fill(4, 2)(0f)
  val flapAngles = Array.fill(4, 2)(0f)
  var nextFlapChange = 0
  var bodyAngle = math.random.toFloat * 90
  var angularVelocity = 0f
  var nextAngularVelocityChange = 0

  // Logic stuff, components, machine and such.
  val info = new ItemUtils.MicrocontrollerData()
  val machine = if (!world.isRemote) Machine.create(this) else null
  val control = if (!world.isRemote) new component.Drone(this) else null

  if (!world.isRemote) {
    info.components = Array(
      api.Items.get("cpu1").createItemStack(1),
      api.Items.get("ram2").createItemStack(1),
      Items.createLuaBios(),
      Loot.createLootDisk("drone", "drone")
    )
  }

  override def node = Option(machine).map(_.node).orNull

  // ----------------------------------------------------------------------- //

  override def getBoundingBox = bounds.copy()

  override def getCollisionBox(entity: Entity) = bounds.copy()

  override def canBeCollidedWith = true

  override def canBePushed = true

  override def isEntityInvulnerable = super.isEntityInvulnerable

  // ----------------------------------------------------------------------- //

  override def xPosition = posX

  override def yPosition = posY

  override def zPosition = posZ

  override def markChanged() {}

  // ----------------------------------------------------------------------- //

  override def cpuArchitecture = info.components.map(stack => (stack, Driver.driverFor(stack, getClass))).collectFirst {
    case (stack, driver: Processor) if driver.slot(stack) == Slot.CPU => driver.architecture(stack)
  }.orNull

  override def callBudget = info.components.foldLeft(0.0)((sum, item) => sum + (Option(item) match {
    case Some(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Processor) if driver.slot(stack) == Slot.CPU => Settings.get.callBudgets(driver.tier(stack))
      case _ => 0
    }
    case _ => 0
  }))

  override def installedMemory = info.components.foldLeft(0)((sum, item) => sum + (Option(item) match {
    case Some(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Memory) => driver.amount(stack)
      case _ => 0
    }
    case _ => 0
  }))

  override def maxComponents = 32

  override def componentSlot(address: String) = -1 // TODO

  override def markForSaving() {}

  override def onMachineConnect(node: Node) {}

  override def onMachineDisconnect(node: Node) {}

  // ----------------------------------------------------------------------- //

  override def host = this

  override def items = info.components.map(Option(_))

  override def getSizeInventory = info.components.length

  override def markDirty() {}

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = false

  override def isUseableByPlayer(player: EntityPlayer) = false

  // Nope.
  override def setInventorySlotContents(slot: Int, stack: ItemStack) {}

  // Nope.
  override def decrStackSize(slot: Int, amount: Int) = null

  // ----------------------------------------------------------------------- //

  override def entityInit() {
    // Running, target x y z and acceleration.
    dataWatcher.addObject(2, byte2Byte(0: Byte))
    dataWatcher.addObject(3, float2Float(0f))
    dataWatcher.addObject(4, float2Float(0f))
    dataWatcher.addObject(5, float2Float(0f))
    dataWatcher.addObject(6, float2Float(0f))
  }

  @SideOnly(Side.CLIENT)
  override def setPositionAndRotation2(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, data: Int) {
    // Snap to rounded positions to avoid jitter on the client.
    super.setPositionAndRotation(
      math.round(x * 10) / 10.0,
      math.round(y * 10) / 10.0,
      math.round(z * 10) / 10.0,
      0, 0)
  }

  override def setDead() {
    super.setDead()
    if (!world.isRemote) {
      machine.stop()
    }
  }

  def targetX = dataWatcher.getWatchableObjectFloat(3)
  def targetY = dataWatcher.getWatchableObjectFloat(4)
  def targetZ = dataWatcher.getWatchableObjectFloat(5)
  def targetAcceleration = dataWatcher.getWatchableObjectFloat(6)

  // Round target values to low accuracy to avoid floating point errors accumulating.
  def targetX_=(value: Float): Unit = dataWatcher.updateObject(3, float2Float(math.round(value * 5) / 5f))
  def targetY_=(value: Float): Unit = dataWatcher.updateObject(4, float2Float(math.round(value * 5) / 5f))
  def targetZ_=(value: Float): Unit = dataWatcher.updateObject(5, float2Float(math.round(value * 5) / 5f))
  def targetAcceleration_=(value: Float): Unit = dataWatcher.updateObject(6, float2Float(math.max(0, math.min(maxAcceleration, value))))

  private def setRunning(value: Boolean) = dataWatcher.updateObject(2, byte2Byte(if (value) 1: Byte else 0: Byte))

  def isRunning = dataWatcher.getWatchableObjectByte(2) != 0

  override def onEntityUpdate() {
    super.onEntityUpdate()

    if (!world.isRemote) {
      if (isInWater) { // We're not water-proof!
        machine.stop()
      }
      machine.node.asInstanceOf[Connector].changeBuffer(100)
      machine.update()
      updateComponents()
      setRunning(machine.isRunning)
    }
    else if (isRunning) {
      // Client side update; occasionally update wing pitch and rotation to
      // make the drones look a bit more dynamic.
      val rng = world.rand
      nextFlapChange -= 1
      nextAngularVelocityChange -= 1

      if (nextFlapChange < 0) {
        nextFlapChange = 5 + rng.nextInt(10)
        for (i <- 0 until 2) {
          val flap = rng.nextInt(targetFlapAngles.length)
          targetFlapAngles(flap)(0) = math.toRadians(rng.nextFloat() * 4 - 2).toFloat
          targetFlapAngles(flap)(1) = math.toRadians(rng.nextFloat() * 4 - 2).toFloat
        }
      }

      if (nextAngularVelocityChange < 0) {
        if (angularVelocity != 0) {
          angularVelocity = 0
          nextAngularVelocityChange = 20
        }
        else {
          angularVelocity = if (rng.nextBoolean()) 0.1f else -0.1f
          nextAngularVelocityChange = 100
        }
      }

      // Interpolate wing rotations.
      (flapAngles, targetFlapAngles).zipped.foreach((f, t) => {
        f(0) = f(0) * 0.7f + t(0) * 0.3f
        f(1) = f(1) * 0.7f + t(1) * 0.3f
      })

      // Update body rotation.
      bodyAngle += angularVelocity
    }

    if (isRunning) {
      val dx = targetX - posX
      val dy = targetY - posY
      val dz = targetZ - posZ
      val ax = math.max(-targetAcceleration, math.min(targetAcceleration, dx))
      val ay = math.max(-targetAcceleration, math.min(targetAcceleration, dy))
      val az = math.max(-targetAcceleration, math.min(targetAcceleration, dz))
      motionX = math.max(-maxVelocity, math.min(maxVelocity, motionX + ax))
      motionY = math.max(-maxVelocity, math.min(maxVelocity, motionY + ay))
      motionZ = math.max(-maxVelocity, math.min(maxVelocity, motionZ + az))
    }
    else {
      // No power, free fall: engage!
      motionY -= gravity
    }
    moveEntity(motionX, motionY, motionZ)

    // Make sure we don't get infinitely faster.
    motionX *= drag
    motionY *= drag
    motionZ *= drag
  }

  override def interactFirst(player: EntityPlayer) = {
    if (player.isSneaking) {
      kill()
    }
    else if (!world.isRemote) {
      targetX = posX.toFloat
      targetY = posY.toFloat
      targetZ = posZ.toFloat
      targetAcceleration = maxAcceleration
      if (onGround) {
        targetY += 1.6f
      }

      api.Network.joinNewNetwork(machine.node)
      connectComponents()
      machine.node.connect(control.node)
      if (machine.isRunning) machine.stop()
      else machine.start()
    }
    true
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {}

  override def onDisconnect(node: Node) {}

  override def onMessage(message: Message) {}

  // ----------------------------------------------------------------------- //

  override def readEntityFromNBT(nbt: NBTTagCompound): Unit = {
    info.load(nbt.getCompoundTag("info"))
    if (!world.isRemote) {
      machine.load(nbt.getCompoundTag("machine"))
      control.load(nbt.getCompoundTag("control"))

      api.Network.joinNewNetwork(machine.node)
      connectComponents()
      machine.node.connect(control.node)
    }
    targetX = nbt.getFloat("targetX")
    targetY = nbt.getFloat("targetY")
    targetZ = nbt.getFloat("targetZ")
    targetAcceleration = nbt.getFloat("targetAcceleration")
  }

  override def writeEntityToNBT(nbt: NBTTagCompound): Unit = {
    saveComponents()
    nbt.setNewCompoundTag("info", info.save)
    if (!world.isRemote) {
      nbt.setNewCompoundTag("machine", machine.save)
      nbt.setNewCompoundTag("control", control.save)
    }
    nbt.setFloat("targetX", targetX)
    nbt.setFloat("targetY", targetY)
    nbt.setFloat("targetZ", targetZ)
    nbt.setFloat("targetAcceleration", targetAcceleration)
  }
}
