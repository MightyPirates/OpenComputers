package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network._
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Settings, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.ForgeDirection
import net.minecraft.entity.player.EntityPlayer

class Hologram extends Environment with SidedEnvironment with Analyzable {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("hologram").
    withConnector().
    create()

  val width = 3 * 16

  val height = 2 * 16 // 32 bit in an int

  val volume = new Array[Int](width * width)

  // Render scale.
  var scale = 1.0

  // Relative number of lit columns (for energy cost).
  var litRatio = -1.0

  // Whether we need to send an update packet/recompile our display list.
  var dirty = false

  // Interval of dirty columns.
  var dirtyFromX = Int.MaxValue
  var dirtyUntilX = -1
  var dirtyFromZ = Int.MaxValue
  var dirtyUntilZ = -1

  // Time to wait before sending another update packet.
  var cooldown = 5

  var hasPower = true

  def setDirty(x: Int, z: Int) {
    dirty = true
    dirtyFromX = math.min(dirtyFromX, x)
    dirtyUntilX = math.max(dirtyUntilX, x + 1)
    dirtyFromZ = math.min(dirtyFromZ, z)
    dirtyUntilZ = math.max(dirtyUntilZ, z + 1)
    litRatio = -1
  }

  def resetDirtyFlag() {
    dirty = false
    dirtyFromX = Int.MaxValue
    dirtyUntilX = -1
    dirtyFromZ = Int.MaxValue
    dirtyUntilZ = -1
    cooldown = 5
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side == ForgeDirection.DOWN

  override def sidedNode(side: ForgeDirection) = if (side == ForgeDirection.DOWN) node else null

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = Array(node)

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function() -- Clears the hologram.""")
  def clear(computer: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    for (i <- 0 until volume.length) volume(i) = 0
    ServerPacketSender.sendHologramClear(this)
    resetDirtyFlag()
    litRatio = 0
    null
  }

  @Callback(direct = true, doc = """function(x:number, z:number):number -- Returns the bit mask representing the specified column.""")
  def get(computer: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val x = args.checkInteger(0) - 1
    if (x < 0 || x >= width) throw new ArrayIndexOutOfBoundsException()
    val z = args.checkInteger(1) - 1
    if (z < 0 || z >= width) throw new ArrayIndexOutOfBoundsException()
    result(volume(x + z * width))
  }

  @Callback(direct = true, limit = 256, doc = """function(x:number, z:number, value:number) -- Set the bit mask for the specified column.""")
  def set(computer: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val x = args.checkInteger(0) - 1
    if (x < 0 || x >= width) throw new ArrayIndexOutOfBoundsException()
    val z = args.checkInteger(1) - 1
    if (z < 0 || z >= width) throw new ArrayIndexOutOfBoundsException()
    val value = args.checkInteger(2)
    volume(x + z * width) = value
    setDirty(x, z)
    null
  }

  @Callback(direct = true, limit = 128, doc = """function(x:number, z:number, height:number) -- Fills a column to the specified height.""")
  def fill(computer: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val x = args.checkInteger(0) - 1
    if (x < 0 || x >= width) throw new ArrayIndexOutOfBoundsException()
    val z = args.checkInteger(1) - 1
    if (z < 0 || z >= width) throw new ArrayIndexOutOfBoundsException()
    val height = math.min(32, math.max(0, args.checkInteger(2)))
    // Bit shifts in the JVM only use the lowest five bits... so we have to
    // manually check the height, to avoid the shift being a no-op.
    volume(x + z * width) = if (height > 0) 0xFFFFFFFF >>> (32 - height) else 0
    setDirty(x, z)
    null
  }

  @Callback(doc = """function():number -- Returns the render scale of the hologram.""")
  def getScale(computer: Context, args: Arguments): Array[AnyRef] = {
    result(scale)
  }

  @Callback(doc = """function(value:number) -- Set the render scale. A larger scale consumes more energy.""")
  def setScale(computer: Context, args: Arguments): Array[AnyRef] = {
    scale = math.max(0.333333, math.min(3, args.checkDouble(0)))
    ServerPacketSender.sendHologramScale(this)
    null
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      if (dirty) {
        cooldown -= 1
        if (cooldown <= 0) this.synchronized {
          ServerPacketSender.sendHologramSet(this)
          resetDirtyFlag()
        }
      }
      if (world.getWorldTime % Settings.get.tickFrequency == 0) {
        if (litRatio < 0) this.synchronized {
          litRatio = 0
          for (i <- 0 until volume.length) {
            if (volume(i) != 0) litRatio += 1
          }
          litRatio /= volume.length
        }

        val hadPower = hasPower
        val neededPower = Settings.get.hologramCost * litRatio * scale * Settings.get.tickFrequency
        hasPower = node.tryChangeBuffer(-neededPower)
        if (hasPower != hadPower) {
          ServerPacketSender.sendHologramPowerChange(this)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def shouldRenderInPass(pass: Int) = pass == 1

  override def getRenderBoundingBox = AxisAlignedBB.getAABBPool.getAABB(xCoord + 0.5 - 1.5 * scale, yCoord, zCoord - scale, xCoord + 0.5 + 1.5 * scale, yCoord + 0.25 + 2 * scale, zCoord + 0.5 + 1.5 * scale)

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    nbt.getIntArray(Settings.namespace + "volume").copyToArray(volume)
    scale = nbt.getDouble(Settings.namespace + "scale")
  }

  override def writeToNBT(nbt: NBTTagCompound) = this.synchronized {
    super.writeToNBT(nbt)
    nbt.setIntArray(Settings.namespace + "volume", volume)
    nbt.setDouble(Settings.namespace + "scale", scale)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    nbt.getIntArray("volume").copyToArray(volume)
    scale = nbt.getDouble("scale")
    hasPower = nbt.getBoolean("hasPower")
    dirty = true
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setIntArray("volume", volume)
    nbt.setDouble("scale", scale)
    nbt.setBoolean("hasPower", hasPower)
  }
}
