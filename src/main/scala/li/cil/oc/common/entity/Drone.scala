package li.cil.oc.common.entity

import java.lang
import java.lang.Iterable
import java.util.UUID

import li.cil.oc.Constants
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.Machine
import li.cil.oc.api.driver.item
import li.cil.oc.api.internal
import li.cil.oc.api.internal.MultiTank
import li.cil.oc.api.machine
import li.cil.oc.api.machine.Context
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network._
import li.cil.oc.common.EventHandler
import li.cil.oc.common.GuiType
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.inventory.Inventory
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.agent
import li.cil.oc.server.component
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.BlockState
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.entity.EntitySize
import net.minecraft.entity.EntityType
import net.minecraft.entity.MoverType
import net.minecraft.entity.Pose
import net.minecraft.entity.item.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.network.datasync.DataParameter
import net.minecraft.network.datasync.DataSerializers
import net.minecraft.network.datasync.EntityDataManager
import net.minecraft.tags.FluidTags
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.text.ITextComponent
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fml.network.NetworkHooks

import scala.collection.JavaConverters.asJavaIterable
import scala.collection.convert.ImplicitConversionsToJava._

object Drone {
  val DataRunning: DataParameter[lang.Boolean] = EntityDataManager.defineId(classOf[Drone], DataSerializers.BOOLEAN)
  val DataTargetX: DataParameter[lang.Float] = EntityDataManager.defineId(classOf[Drone], DataSerializers.FLOAT)
  val DataTargetY: DataParameter[lang.Float] = EntityDataManager.defineId(classOf[Drone], DataSerializers.FLOAT)
  val DataTargetZ: DataParameter[lang.Float] = EntityDataManager.defineId(classOf[Drone], DataSerializers.FLOAT)
  val DataMaxAcceleration: DataParameter[lang.Float] = EntityDataManager.defineId(classOf[Drone], DataSerializers.FLOAT)
  val DataSelectedSlot: DataParameter[Integer] = EntityDataManager.defineId(classOf[Drone], DataSerializers.INT)
  val DataCurrentEnergy: DataParameter[Integer] = EntityDataManager.defineId(classOf[Drone], DataSerializers.INT)
  val DataMaxEnergy: DataParameter[Integer] = EntityDataManager.defineId(classOf[Drone], DataSerializers.INT)
  val DataStatusText: DataParameter[String] = EntityDataManager.defineId(classOf[Drone], DataSerializers.STRING)
  val DataInventorySize: DataParameter[Integer] = EntityDataManager.defineId(classOf[Drone], DataSerializers.INT)
  val DataLightColor: DataParameter[Integer] = EntityDataManager.defineId(classOf[Drone], DataSerializers.INT)
}

abstract class DroneInventory(val drone: Drone) extends Inventory

// internal.Rotatable is also in internal.Drone, but it wasn't since the start
// so this is to ensure it is implemented here, in the very unlikely case that
// someone decides to ship that specific version of the API.
class Drone(selfType: EntityType[Drone], world: World) extends Entity(selfType, world) with MachineHost with internal.Drone with internal.Rotatable with Analyzable with Context {
  override def world: World = level

  // Some basic constants.
  val gravity = 0.05f
  // low for slow fall (float down)
  val drag = 0.8f
  val maxAcceleration = 0.1f
  val maxVelocity = 0.4f
  val maxInventorySize = 8

  // Rendering stuff, purely eyecandy.
  val targetFlapAngles: Array[Array[Float]] = Array.fill(4, 2)(0f)
  val flapAngles: Array[Array[Float]] = Array.fill(4, 2)(0f)
  var nextFlapChange = 0
  var bodyAngle: Float = math.random.toFloat * 90
  var angularVelocity = 0f
  var nextAngularVelocityChange = 0
  var lastEnergyUpdate = 0

  // Logic stuff, components, machine and such.
  val info = new DroneData()
  val machine: api.machine.Machine = if (!world.isClientSide) {
    val m = Machine.create(this)
    m.node.asInstanceOf[Connector].setLocalBufferSize(0)
    m
  } else null
  val control: component.Drone = if (!world.isClientSide) new component.Drone(this) else null
  val components = new ComponentInventory {
    override def host: Drone = Drone.this

    override def items: Array[ItemStack] = info.components

    override def getContainerSize: Int = info.components.length

    override def setChanged() {}

    override def canPlaceItem(slot: Int, stack: ItemStack) = true

    override def stillValid(player: PlayerEntity) = true

    override def node: Node = Option(machine).map(_.node).orNull

    override def onConnect(node: Node) {}

    override def onDisconnect(node: Node) {}

    override def onMessage(message: Message) {}
  }
  val equipmentInventory = new Inventory {
    val items = Array.empty[ItemStack]

    override def getContainerSize = 0

    override def getMaxStackSize = 0

    override def setChanged(): Unit = {}

    override def canPlaceItem(slot: Int, stack: ItemStack) = false

    override def stillValid(player: PlayerEntity) = false
  }
  val mainInventory = new DroneInventory(this) {
    val items: Array[ItemStack] = Array.fill[ItemStack](8)(ItemStack.EMPTY)

    override def getContainerSize: Int = inventorySize

    override def getMaxStackSize = 64

    override def setChanged() {} // TODO update client GUI?

    override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = slot >= 0 && slot < getContainerSize

    override def stillValid(player: PlayerEntity): Boolean = player.distanceToSqr(drone) < 64
  }
  val tank = new MultiTank {
    override def tankCount: Int = components.components.count {
      case Some(tank: IFluidTank) => true
      case _ => false
    }

    override def getFluidTank(index: Int): IFluidTank = components.components.collect {
      case Some(tank: IFluidTank) => tank
    }.apply(index)
  }
  var selectedTank = 0

  override def setSelectedTank(index: Int): Unit = selectedTank = index

  override def tier: Int = info.tier

  override def player(): PlayerEntity = {
    agent.Player.updatePositionAndRotation(player_, facing, facing)
    agent.Player.setPlayerInventoryItems(player_)
    player_
  }

  override def name: String = info.name

  override def setName(name: String): Unit = info.name = name

  var ownerName: String = Settings.get.fakePlayerName

  var ownerUUID: UUID = Settings.get.fakePlayerProfile.getId

  private lazy val player_ = new agent.Player(this)

  // ----------------------------------------------------------------------- //
  // Forward context stuff to our machine. Interface needed for some components
  // to work correctly (such as the chunkloader upgrade).

  override def node: Node = machine.node

  override def canInteract(player: String): Boolean = machine.canInteract(player)

  override def isPaused: Boolean = machine.isPaused

  override def start(): Boolean = {
    if (world.isClientSide || machine.isRunning) {
      return false
    }
    preparePowerUp()
    machine.start()
  }

  override def pause(seconds: Double): Boolean = machine.pause(seconds)

  override def stop(): Boolean = machine.stop()

  override def consumeCallBudget(callCost: Double): Unit = machine.consumeCallBudget(callCost)

  override def signal(name: String, args: AnyRef*): Boolean = machine.signal(name, args: _*)

  // ----------------------------------------------------------------------- //

  override def getTarget = new Vector3d(targetX.floatValue(), targetY.floatValue(), targetZ.floatValue())

  override def setTarget(value: Vector3d): Unit = {
    targetX = value.x.toFloat
    targetY = value.y.toFloat
    targetZ = value.z.toFloat
  }

  override def getVelocity = getDeltaMovement

  // ----------------------------------------------------------------------- //

  override def canBeCollidedWith = true

  override def isPushable = true

  // ----------------------------------------------------------------------- //

  override def xPosition: Double = getX

  override def yPosition: Double = getY

  override def zPosition: Double = getZ

  override def markChanged() {}

  @OnlyIn(Dist.CLIENT)
  override def getRopeHoldPosition(dt: Float): Vector3d =
    getPosition(dt).add(0.0, -0.056, 0.0) // Offset: height * 0.85 * 0.7 - 0.25

  // ----------------------------------------------------------------------- //

  override def facing = Direction.SOUTH

  override def toLocal(value: Direction): Direction = value

  override def toGlobal(value: Direction): Direction = value

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = Array(machine.node)

  // ----------------------------------------------------------------------- //

  override def internalComponents(): Iterable[ItemStack] = asJavaIterable(info.components)

  override def componentSlot(address: String): Int = components.components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  override def onMachineConnect(node: Node) {}

  override def onMachineDisconnect(node: Node) {}

  def computeInventorySize(): Int = math.min(maxInventorySize, info.components.foldLeft(0)((acc, component) => acc + (Option(component) match {
    case Some(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: item.Inventory) => math.max(1, driver.inventoryCapacity(stack) / 4)
      case _ => 0
    }
    case _ => 0
  })))

  // ----------------------------------------------------------------------- //

  override def defineSynchedData() {
    entityData.define(Drone.DataRunning, java.lang.Boolean.FALSE)
    entityData.define(Drone.DataTargetX, Float.box(0f))
    entityData.define(Drone.DataTargetY, Float.box(0f))
    entityData.define(Drone.DataTargetZ, Float.box(0f))
    entityData.define(Drone.DataMaxAcceleration, Float.box(0f))
    entityData.define(Drone.DataSelectedSlot, Int.box(0))
    entityData.define(Drone.DataCurrentEnergy, Int.box(0))
    entityData.define(Drone.DataMaxEnergy, Int.box(100))
    entityData.define(Drone.DataStatusText, "")
    entityData.define(Drone.DataInventorySize, Int.box(0))
    entityData.define(Drone.DataLightColor, Int.box(0x66DD55))
  }

  def initializeAfterPlacement(stack: ItemStack, player: PlayerEntity, position: Vector3d) {
    info.loadData(stack)
    control.node.changeBuffer(info.storedEnergy - control.node.localBuffer)
    wireThingsTogether()
    inventorySize = computeInventorySize()
    setPos(position.x, position.y, position.z)
  }

  def preparePowerUp() {
    targetX = math.floor(getX).toFloat + 0.5f
    targetY = math.round(getY).toFloat + 0.5f
    targetZ = math.floor(getZ).toFloat + 0.5f
    targetAcceleration = maxAcceleration

    wireThingsTogether()
  }

  private def wireThingsTogether(): Unit = {
    api.Network.joinNewNetwork(machine.node)
    machine.node.connect(control.node)
    machine.setCostPerTick(Settings.get.droneCost)
    components.connectComponents()
  }

  def isRunning: Boolean = entityData.get(Drone.DataRunning)

  def targetX: lang.Float = entityData.get(Drone.DataTargetX)

  def targetY: lang.Float = entityData.get(Drone.DataTargetY)

  def targetZ: lang.Float = entityData.get(Drone.DataTargetZ)

  def targetAcceleration: lang.Float = entityData.get(Drone.DataMaxAcceleration)

  def selectedSlot: Int = entityData.get(Drone.DataSelectedSlot) & 0xFF

  def globalBuffer: Integer = entityData.get(Drone.DataCurrentEnergy)

  def globalBufferSize: Integer = entityData.get(Drone.DataMaxEnergy)

  def statusText: String = entityData.get(Drone.DataStatusText)

  def inventorySize: Int = entityData.get(Drone.DataInventorySize) & 0xFF

  def lightColor: Integer = entityData.get(Drone.DataLightColor)

  def setRunning(value: Boolean): Unit = entityData.set(Drone.DataRunning, Boolean.box(value))

  // Round target values to low accuracy to avoid floating point errors accumulating.
  def targetX_=(value: Float): Unit = entityData.set(Drone.DataTargetX, Float.box(math.round(value * 4) / 4f))

  def targetY_=(value: Float): Unit = entityData.set(Drone.DataTargetY, Float.box(math.round(value * 4) / 4f))

  def targetZ_=(value: Float): Unit = entityData.set(Drone.DataTargetZ, Float.box(math.round(value * 4) / 4f))

  def targetAcceleration_=(value: Float): Unit = entityData.set(Drone.DataMaxAcceleration, Float.box(math.max(0, math.min(maxAcceleration, value))))

  def setSelectedSlot(value: Int): Unit = entityData.set(Drone.DataSelectedSlot, Int.box(value.toByte))

  def globalBuffer_=(value: Int): Unit = entityData.set(Drone.DataCurrentEnergy, Int.box(value))

  def globalBufferSize_=(value: Int): Unit = entityData.set(Drone.DataMaxEnergy, Int.box(value))

  def statusText_=(value: String): Unit = entityData.set(Drone.DataStatusText, Option(value).fold("")(_.lines.map(_.take(10)).take(2).mkString("\n")))

  def inventorySize_=(value: Int): Unit = entityData.set(Drone.DataInventorySize, Int.box(value.toByte))

  def lightColor_=(value: Int): Unit = entityData.set(Drone.DataLightColor, Int.box(value))

  override def lerpTo(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, posRotationIncrements: Int, teleport: Boolean): Unit = {
    // Only set exact position if we're too far away from the server's
    // position, otherwise keep interpolating. This removes jitter and
    // is good enough for drones.
    if (!isRunning || distanceToSqr(x, y, z) > 1) {
      super.absMoveTo(x, y, z, yaw, pitch)
    }
    else {
      targetX = x.toFloat
      targetY = y.toFloat
      targetZ = z.toFloat
    }
  }

  override def tick() {
    super.tick()

    if (!world.isClientSide) {
      if (isInWater || isInLava) {
        // We're not water-proof!
        machine.stop()
      }
      machine.update()
      components.updateComponents()
      setRunning(machine.isRunning)

      val buffer = math.round(machine.node.asInstanceOf[Connector].globalBuffer).toInt
      if (math.abs(lastEnergyUpdate - buffer) > 1 || world.getGameTime % 200 == 0) {
        lastEnergyUpdate = buffer
        globalBuffer = buffer
        globalBufferSize = machine.node.asInstanceOf[Connector].globalBufferSize.toInt
      }
    }
    else {
      if (isRunning) {
        // Client side update; occasionally update wing pitch and rotation to
        // make the drones look a bit more dynamic.
        val rng = world.random
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
    }

    xo = getX
    yo = getY
    zo = getZ
    moveTowardsClosestSpace(getX, (getBoundingBox.minY + getBoundingBox.maxY) / 2, getZ)
    noPhysics = true

    if (isRunning) {
      val toTarget = new Vector3d(targetX - getX, targetY - getY, targetZ - getZ)
      val distance = toTarget.length()
      val velocity = getDeltaMovement
      if (distance > 0 && (distance > 0.005f || velocity.dot(velocity) > 0.005f)) {
        val acceleration = math.min(targetAcceleration.floatValue(), distance) / distance
        val velocityX = velocity.x + toTarget.x * acceleration
        val velocityY = velocity.y + toTarget.y * acceleration
        val velocityZ = velocity.z + toTarget.z * acceleration
        setDeltaMovement(new Vector3d(math.max(-maxVelocity, math.min(maxVelocity, velocityX)),
          math.max(-maxVelocity, math.min(maxVelocity, velocityY)),
          math.max(-maxVelocity, math.min(maxVelocity, velocityZ))))
      }
      else {
        setDeltaMovement(Vector3d.ZERO)
        setPos(targetX.floatValue(), targetY.floatValue(), targetZ.floatValue())
      }
    }
    else {
      // No power, free fall: engage!
      setDeltaMovement(getDeltaMovement.subtract(0, gravity, 0))
    }

    move(MoverType.SELF, getDeltaMovement)

    // Make sure we don't get infinitely faster.
    if (isRunning) {
      setDeltaMovement(getDeltaMovement.scale(drag))
    }
    else {
      val groundDrag = world.getBlock(BlockPosition(this: Entity).offset(Direction.DOWN)).getFriction * drag
      setDeltaMovement(getDeltaMovement.multiply(groundDrag, drag * (if (isOnGround) -0.5 else 1), groundDrag))
    }
  }

  override def skipAttackInteraction(entity: Entity): Boolean = {
    if (isRunning) {
      val direction = new Vector3d(entity.getX - getX, entity.getY + entity.getEyeHeight - getY, entity.getZ - getZ).normalize()
      if (!world.isClientSide) {
        if (Settings.get.inputUsername)
          machine.signal("hit", Double.box(direction.x), Double.box(direction.z), Double.box(direction.y), entity.getName)
        else
          machine.signal("hit", Double.box(direction.x), Double.box(direction.z), Double.box(direction.y))
      }
      setDeltaMovement(getDeltaMovement.subtract(direction).scale(0.5))
    }
    super.skipAttackInteraction(entity)
  }

  override def interact(player: PlayerEntity, hand: Hand): ActionResultType = {
    if (!isAlive) return ActionResultType.PASS
    if (player.isCrouching) {
      if (Wrench.isWrench(player.getItemInHand(Hand.MAIN_HAND))) {
        if(!world.isClientSide) {
          outOfWorld()
        }
      }
      else if (!world.isClientSide && !machine.isRunning) {
        start()
      }
    }
    else if (!world.isClientSide) {
      OpenComputers.openGui(player, GuiType.Drone.id, world, getId, 0, 0)
    }
    ActionResultType.sidedSuccess(world.isClientSide)
  }

  // No step sounds. Except on that one day.
  override def playStepSound(pos: BlockPos, state: BlockState): Unit = {
    if (EventHandler.isItTime) super.playStepSound(pos, state)
  }

  // ----------------------------------------------------------------------- //

  private var isChangingDimension = false

  override def changeDimension(dimension: ServerWorld): Entity = {
    // Store relative target as target, to allow adding that in our "new self"
    // (entities get re-created after changing dimension).
    targetX = (targetX - getX).toFloat
    targetY = (targetY - getY).toFloat
    targetZ = (targetZ - getZ).toFloat
    try {
      isChangingDimension = true
      super.changeDimension(dimension)
    }
    finally {
      isChangingDimension = false
      remove() // Again, to actually close old machine state after copying it.
    }
  }

  override def restoreFrom(entity: Entity): Unit = {
    super.restoreFrom(entity)
    // Compute relative target based on old position and update, because our
    // frame of reference most certainly changed (i.e. we'll spawn at different
    // coordinates than the ones we started traveling from, e.g. when porting
    // to the nether it'll be oldpos / 8).
    entity match {
      case drone: Drone =>
        targetX = (getX + drone.targetX).toFloat
        targetY = (getY + drone.targetY).toFloat
        targetZ = (getZ + drone.targetZ).toFloat
      case _ =>
        targetX = getX.toFloat
        targetY = getY.toFloat
        targetZ = getZ.toFloat
    }
  }

  override def remove() {
    super.remove()
    if (!world.isClientSide && !isChangingDimension) {
      machine.stop()
      machine.node.remove()
      components.disconnectComponents()
      components.saveComponents()
    }
  }

  override def outOfWorld(): Unit = {
    if (!isAlive) return
    super.outOfWorld()
    if (!world.isClientSide) {
      val stack = api.Items.get(Constants.ItemName.Drone).createItemStack(1)
      info.storedEnergy = control.node.localBuffer.toInt
      info.saveData(stack)
      val entity = new ItemEntity(world, getX, getY, getZ, stack)
      entity.setPickUpDelay(15)
      world.addFreshEntity(entity)
      InventoryUtils.dropAllSlots(BlockPosition(this: Entity), mainInventory)
    }
  }

  override def getName: ITextComponent = Localization.localizeLater("entity.oc.Drone.name")

  override protected def getAddEntityPacket = NetworkHooks.getEntitySpawningPacket(this)

  override protected def readAdditionalSaveData(nbt: CompoundNBT) {
    info.loadData(nbt.getCompound("info"))
    inventorySize = computeInventorySize()
    if (!world.isClientSide) {
      machine.loadData(nbt.getCompound("machine"))
      control.loadData(nbt.getCompound("control"))
      components.loadData(nbt.getCompound("components"))
      mainInventory.loadData(nbt.getCompound("inventory"))

      wireThingsTogether()
    }
    targetX = nbt.getFloat("targetX")
    targetY = nbt.getFloat("targetY")
    targetZ = nbt.getFloat("targetZ")
    targetAcceleration = nbt.getFloat("targetAcceleration")
    setSelectedSlot(nbt.getByte("selectedSlot") & 0xFF)
    setSelectedTank(nbt.getByte("selectedTank") & 0xFF)
    statusText = nbt.getString("statusText")
    lightColor = nbt.getInt("lightColor")
    if (nbt.contains("owner")) {
      ownerName = nbt.getString("owner")
    }
    if (nbt.contains("ownerUuid")) {
      ownerUUID = UUID.fromString(nbt.getString("ownerUuid"))
    }
  }

  override protected def addAdditionalSaveData(nbt: CompoundNBT) {
    if (world.isClientSide) return
    components.saveComponents()
    info.storedEnergy = globalBuffer.toInt
    nbt.setNewCompoundTag("info", info.saveData)
    if (!world.isClientSide) {
      nbt.setNewCompoundTag("machine", machine.saveData)
      nbt.setNewCompoundTag("control", control.saveData)
      nbt.setNewCompoundTag("components", components.saveData)
      nbt.setNewCompoundTag("inventory", mainInventory.saveData)
    }
    nbt.putFloat("targetX", targetX)
    nbt.putFloat("targetY", targetY)
    nbt.putFloat("targetZ", targetZ)
    nbt.putFloat("targetAcceleration", targetAcceleration)
    nbt.putByte("selectedSlot", selectedSlot.toByte)
    nbt.putByte("selectedTank", selectedTank.toByte)
    nbt.putString("statusText", statusText)
    nbt.putInt("lightColor", lightColor)
    nbt.putString("owner", ownerName)
    nbt.putString("ownerUuid", ownerUUID.toString)
  }
}
