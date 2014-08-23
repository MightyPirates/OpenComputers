package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network._
import li.cil.oc.common.SaveHandler
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.ForgeDirection

class Hologram(var tier: Int) extends traits.Environment with SidedEnvironment with Analyzable {
  def this() = this(0)

  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("hologram").
    withConnector().
    create()

  val width = 3 * 16

  val height = 2 * 16 // 32 bit in an int

  // Layout is: first half is lower bit, second half is higher bit for the
  // voxels in the cube. This is to retain compatibility with pre 1.3 saves.
  val volume = new Array[Int](width * width * 2)

  // Render scale.
  var scale = 1.0

  // Relative number of lit columns (for energy cost).
  var litRatio = -1.0

  // Whether we need to send an update packet/recompile our display list.
  var dirty = false

  // Store it here for convenience, this is the number of visible voxel faces
  // as determined in the last VBO index update. See HologramRenderer.
  var visibleQuads = 0

  // Interval of dirty columns.
  var dirtyFromX = Int.MaxValue
  var dirtyUntilX = -1
  var dirtyFromZ = Int.MaxValue
  var dirtyUntilZ = -1

  // Time to wait before sending another update packet.
  var cooldown = 5

  var hasPower = true

  val colorsByTier = Array(Array(0x00FF00), Array(0x0000FF, 0x00FF00, 0xFF0000)) // 0xBBGGRR for rendering convenience

  // This is a def and not a val for loading (where the tier comes from the nbt and is always 0 here).
  def colors = colorsByTier(tier)

  def getColor(x: Int, y: Int, z: Int) = {
    val lbit = (volume(x + z * width) >>> y) & 1
    val hbit = (volume(x + z * width + width * width) >>> y) & 1
    lbit | (hbit << 1)
  }

  def setColor(x: Int, y: Int, z: Int, value: Int) {
    if ((value & 3) != getColor(x, y, z)) {
      val lbit = value & 1
      val hbit = (value >>> 1) & 1
      volume(x + z * width) = (volume(x + z * width) & ~(1 << y)) | (lbit << y)
      volume(x + z * width + width * width) = (volume(x + z * width + width * width) & ~(1 << y)) | (hbit << y)
      setDirty(x, z)
    }
  }

  private def setDirty(x: Int, z: Int) {
    dirty = true
    dirtyFromX = math.min(dirtyFromX, x)
    dirtyUntilX = math.max(dirtyUntilX, x + 1)
    dirtyFromZ = math.min(dirtyFromZ, z)
    dirtyUntilZ = math.max(dirtyUntilZ, z + 1)
    litRatio = -1
  }

  private def resetDirtyFlag() {
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

  // Override automatic analyzer implementation for sided environments.
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

  @Callback(direct = true, doc = """function(x:number, y:number, z:number):number -- Returns the value for the specified voxel.""")
  def get(computer: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val (x, y, z) = checkCoordinates(args)
    result(getColor(x, y, z))
  }

  @Callback(direct = true, limit = 256, doc = """function(x:number, y:number, z:number, value:number or boolean) -- Set the value for the specified voxel.""")
  def set(computer: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val (x, y, z) = checkCoordinates(args)
    val value = checkColor(args, 3)
    setColor(x, y, z, value)
    null
  }

  @Callback(direct = true, limit = 128, doc = """function(x:number, z:number[, minY:number], maxY:number, value:number or boolean) -- Fills an interval of a column with the specified value.""")
  def fill(computer: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val (x, _, z) = checkCoordinates(args, 0, -1, 1)
    val (minY, maxY, value) =
      if (args.count > 4)
        (math.min(32, math.max(1, args.checkInteger(2))), math.min(32, math.max(1, args.checkInteger(3))), checkColor(args, 4))
      else
        (1, math.min(32, math.max(1, args.checkInteger(2))), checkColor(args, 3))
    if (minY > maxY) throw new IllegalArgumentException("interval is empty")

    val mask = (0xFFFFFFFF >>> (31 - (maxY - minY))) << (minY - 1)
    val lbit = value & 1
    val hbit = (value >>> 1) & 1
    if (lbit == 0 || height == 0) volume(x + z * width) &= ~mask
    else volume(x + z * width) |= mask
    if (hbit == 0 || height == 0) volume(x + z * width + width * width) &= ~mask
    else volume(x + z * width + width * width) |= mask

    setDirty(x, z)
    null
  }

  @Callback(doc = """function(x:number, z:number, sx:number, sz:number, tx:number, tz:number) -- Copies an area of columns by the specified translation.""")
  def copy(computer: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val (x, _, z) = checkCoordinates(args, 0, -1, 1)
    val w = args.checkInteger(2)
    val h = args.checkInteger(3)
    val tx = args.checkInteger(4)
    val tz = args.checkInteger(5)

    // Anything to do at all?
    if (w <= 0 || h <= 0) return null
    if (tx == 0 && tz == 0) return null
    // Loop over the target rectangle, starting from the directions away from
    // the source rectangle and copy the data. This way we ensure we don't
    // overwrite anything we still need to copy.
    val (dx0, dx1) = (math.max(0, math.min(width - 1, x + tx + w - 1)), math.max(0, math.min(width, x + tx))) match {
      case dx if tx > 0 => dx
      case dx => dx.swap
    }
    val (dz0, dz1) = (math.max(0, math.min(width - 1, z + tz + h - 1)), math.max(0, math.min(width, z + tz))) match {
      case dz if tz > 0 => dz
      case dz => dz.swap
    }
    val (sx, sz) = (if (tx > 0) -1 else 1, if (tz > 0) -1 else 1)
    // Copy values to destination rectangle if there source is valid.
    for (nz <- dz0 to dz1 by sz) {
      nz - tz match {
        case oz if oz >= 0 && oz < width =>
          for (nx <- dx0 to dx1 by sx) {
            nx - tx match {
              case ox if ox >= 0 && ox < width =>
                volume(nz * width + nx) = volume(oz * width + ox)
                volume(nz * width + nx + width * width) = volume(oz * width + ox + width * width)
              case _ => /* Got no source column. */
            }
          }
        case _ => /* Got no source row. */
      }
    }

    // Mark target rectangle dirty.
    setDirty(math.min(dx0, dx1), math.min(dz0, dz1))
    setDirty(math.max(dx0, dx1), math.max(dz0, dz1))

    // The reasoning here is: it'd take 18 ticks to do the whole are with fills,
    // so make this slightly more efficient (15 ticks - 0.75 seconds). Make it
    // 'free' if it's less than 0.25 seconds, i.e. for small copies.
    val area = (math.max(dx0, dx1) - math.min(dx0, dx1)) * (math.max(dz0, dz1) - math.min(dz0, dz1))
    val relativeArea = math.max(0, area / (width * width).toFloat - 0.25)
    computer.pause(relativeArea)

    null
  }

  @Callback(doc = """function():number -- Returns the render scale of the hologram.""")
  def getScale(computer: Context, args: Arguments): Array[AnyRef] = {
    result(scale)
  }

  @Callback(doc = """function(value:number) -- Set the render scale. A larger scale consumes more energy.""")
  def setScale(computer: Context, args: Arguments): Array[AnyRef] = {
    scale = math.max(0.333333, math.min(Settings.get.hologramMaxScaleByTier(tier), args.checkDouble(0)))
    ServerPacketSender.sendHologramScale(this)
    null
  }

  @Callback(direct = true, doc = """function():number -- The color depth supported by the hologram.""")
  def maxDepth(context: Context, args: Arguments): Array[AnyRef] = {
    result(tier + 1)
  }

  @Callback(doc = """function(index:number):number -- Get the color defined for the specified value.""")
  def getPaletteColor(computer: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkInteger(0)
    if (index < 1 || index > colors.length) throw new ArrayIndexOutOfBoundsException()
    // Colors are stored as 0xAABBGGRR for rendering convenience, so convert them.
    result(convertColor(colors(index - 1)))
  }

  @Callback(doc = """function(index:number, value:number):number -- Set the color defined for the specified value.""")
  def setPaletteColor(computer: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkInteger(0)
    if (index < 1 || index > colors.length) throw new ArrayIndexOutOfBoundsException()
    val value = args.checkInteger(1)
    val oldValue = colors(index - 1)
    // Change byte order here to allow passing stored color to OpenGL "as-is"
    // (as whole Int, i.e. 0xAABBGGRR, alpha is unused but present for alignment)
    colors(index - 1) = convertColor(value)
    ServerPacketSender.sendHologramColor(this, index - 1, colors(index - 1))
    result(oldValue)
  }

  private def checkCoordinates(args: Arguments, idxX: Int = 0, idxY: Int = 1, idxZ: Int = 2) = {
    val x = if (idxX >= 0) args.checkInteger(idxX) - 1 else 0
    if (x < 0 || x >= width) throw new ArrayIndexOutOfBoundsException("x")
    val y = if (idxY >= 0) args.checkInteger(idxY) - 1 else 0
    if (y < 0 || y >= height) throw new ArrayIndexOutOfBoundsException("y")
    val z = if (idxZ >= 0) args.checkInteger(idxZ) - 1 else 0
    if (z < 0 || z >= width) throw new ArrayIndexOutOfBoundsException("z")
    (x, y, z)
  }

  private def checkColor(args: Arguments, index: Int) = {
    val value =
      if (args.isBoolean(index))
        if (args.checkBoolean(index)) 1 else 0
      else
        args.checkInteger(index)
    if (value < 0 || value > colors.length) throw new IllegalArgumentException("invalid value")
    value
  }

  private def convertColor(color: Int) = {
    ((color & 0x0000FF) << 16) | (color & 0x00FF00) | ((color & 0xFF0000) >>> 16)
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

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
      if (world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
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

  override def getMaxRenderDistanceSquared = scale / Settings.get.hologramMaxScaleByTier.max * Settings.get.hologramRenderDistance * Settings.get.hologramRenderDistance

  def getFadeStartDistanceSquared = scale / Settings.get.hologramMaxScaleByTier.max * Settings.get.hologramFadeStartDistance * Settings.get.hologramFadeStartDistance

  override def getRenderBoundingBox = AxisAlignedBB.getAABBPool.getAABB(xCoord + 0.5 - 1.5 * scale, yCoord, zCoord - scale, xCoord + 0.5 + 1.5 * scale, yCoord + 0.25 + 2 * scale, zCoord + 0.5 + 1.5 * scale)

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    tier = nbt.getByte(Settings.namespace + "tier") max 0 min 1
    super.readFromNBT(nbt)
    if (nbt.hasKey(Settings.namespace + "volume") && nbt.hasKey(Settings.namespace + "colors")) {
      nbt.getIntArray(Settings.namespace + "volume").copyToArray(volume)
      nbt.getIntArray(Settings.namespace + "colors").map(convertColor).copyToArray(colors)
    }
    else {
      val tag = SaveHandler.loadNBT(nbt, node.address + "_data")
      tag.getIntArray("volume").copyToArray(volume)
      tag.getIntArray("colors").map(convertColor).copyToArray(colors)
    }
    scale = nbt.getDouble(Settings.namespace + "scale")
  }

  override def writeToNBT(nbt: NBTTagCompound) = this.synchronized {
    nbt.setByte(Settings.namespace + "tier", tier.toByte)
    super.writeToNBT(nbt)
    SaveHandler.scheduleSave(world, x, z, nbt, node.address + "_data", tag => {
      tag.setIntArray("volume", volume)
      tag.setIntArray("colors", colors.map(convertColor))
    })
    nbt.setDouble(Settings.namespace + "scale", scale)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    nbt.getIntArray("volume").copyToArray(volume)
    nbt.getIntArray("colors").copyToArray(colors)
    scale = nbt.getDouble("scale")
    hasPower = nbt.getBoolean("hasPower")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setIntArray("volume", volume)
    nbt.setIntArray("colors", colors)
    nbt.setDouble("scale", scale)
    nbt.setBoolean("hasPower", hasPower)
  }
}
