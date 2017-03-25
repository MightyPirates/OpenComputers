//package li.cil.oc.common.tileentity
//
//import java.util
//
//import li.cil.oc._
//import li.cil.oc.api.driver.DeviceInfo
//import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
//import li.cil.oc.api.driver.DeviceInfo.DeviceClass
//import li.cil.oc.api.machine.Arguments
//import li.cil.oc.api.machine.Callback
//import li.cil.oc.api.machine.Context
//import li.cil.oc.api.network.Analyzable
//import li.cil.oc.api.network._
//import li.cil.oc.common.SaveHandler
//import li.cil.oc.common.tileentity.capabilities.RotatableImpl
//import li.cil.oc.server.{PacketSender => ServerPacketSender}
//import net.minecraft.entity.player.EntityPlayer
//import net.minecraft.nbt.NBTTagCompound
//import net.minecraft.util.EnumFacing
//import net.minecraft.util.math.AxisAlignedBB
//import net.minecraft.util.math.Vec3d
//import net.minecraftforge.fml.relauncher.Side
//import net.minecraftforge.fml.relauncher.SideOnly
//
//import scala.collection.convert.WrapAsJava._
//import scala.collection.mutable
//
//class Hologram(var tier: Int) extends traits.Environment with SidedEnvironment with Analyzable with RotatableImpl with traits.Tickable with DeviceInfo {
//  def this() = this(0)
//
//  val getNode = api.Network.newNode(this, Visibility.NETWORK).
//    withComponent("hologram").
//    withConnector().
//    create()
//
//  final val width = 3 * 16
//
//  final val height = 2 * 16 // 32 bit in an int
//
//  private final lazy val deviceInfo = Map(
//    DeviceAttribute.Class -> DeviceClass.Display,
//    DeviceAttribute.Description -> "Holographic projector",
//    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
//    DeviceAttribute.Product -> ("VirtualViewer H1-" + (tier + 1).toString),
//    DeviceAttribute.Capacity -> (width * width * height).toString,
//    DeviceAttribute.Width -> colors.length.toString
//  )
//
//  override def getDeviceInfo: util.Map[String, String] = deviceInfo
//
//  // ----------------------------------------------------------------------- //
//
//  // Layout is: first half is lower bit, second half is higher bit for the
//  // voxels in the cube. This is to retain compatibility with pre 1.3 saves.
//  val volume = new Array[Int](width * width * 2)
//
//  // Render scale.
//  var scale = 1.0
//
//  // Projection Y position offset - consider adding X,Z later perhaps
//  var translation = new Vec3d(0, 0, 0)
//
//  // Relative number of lit columns (for energy cost).
//  var litRatio = -1.0
//
//  // Whether we need to recompile our display list.
//  var needsRendering = false
//
//  // Store it here for convenience, this is the number of visible voxel faces
//  // as determined in the last VBO index update. See HologramRenderer.
//  var visibleQuads = 0
//
//  // What parts of the hologram changed and need an update packet.
//  var dirty = mutable.Set.empty[Short]
//
//  // Interval of dirty columns.
//  var dirtyFromX = Int.MaxValue
//  var dirtyUntilX = -1
//  var dirtyFromZ = Int.MaxValue
//  var dirtyUntilZ = -1
//
//  var hasPower = true
//
//  // Rotation base state. Current rotation is based on world time. See HologramRenderer.
//  var rotationAngle = 0f
//  var rotationX = 0f
//  var rotationY = 0f
//  var rotationZ = 0f
//  var rotationSpeed = 0f
//  var rotationSpeedX = 0f
//  var rotationSpeedY = 0f
//  var rotationSpeedZ = 0f
//
//  final val colorsByTier = Array(Array(0x00FF00), Array(0x0000FF, 0x00FF00, 0xFF0000)) // 0xBBGGRR for rendering convenience
//
//  // This is a def and not a val for loading (where the tier comes from the nbt and is always 0 here).
//  def colors = colorsByTier(tier)
//
//  def getColor(x: Int, y: Int, z: Int) = {
//    val lbit = (volume(x + z * width) >>> y) & 1
//    val hbit = (volume(x + z * width + width * width) >>> y) & 1
//    lbit | (hbit << 1)
//  }
//
//  def setColor(x: Int, y: Int, z: Int, value: Int) {
//    if ((value & 3) != getColor(x, y, z)) {
//      val lbit = value & 1
//      val hbit = (value >>> 1) & 1
//      volume(x + z * width) = (volume(x + z * width) & ~(1 << y)) | (lbit << y)
//      volume(x + z * width + width * width) = (volume(x + z * width + width * width) & ~(1 << y)) | (hbit << y)
//      setDirty(x, z)
//    }
//  }
//
//  private def setDirty(x: Int, z: Int) {
//    dirty += ((x.toByte << 8) | z.toByte).toShort
//    dirtyFromX = math.min(dirtyFromX, x)
//    dirtyUntilX = math.max(dirtyUntilX, x + 1)
//    dirtyFromZ = math.min(dirtyFromZ, z)
//    dirtyUntilZ = math.max(dirtyUntilZ, z + 1)
//    litRatio = -1
//  }
//
//  private def resetDirtyFlag() {
//    dirty.clear()
//    dirtyFromX = Int.MaxValue
//    dirtyUntilX = -1
//    dirtyFromZ = Int.MaxValue
//    dirtyUntilZ = -1
//  }
//
//  // ----------------------------------------------------------------------- //
//
//  @SideOnly(Side.CLIENT)
//  override def canConnect(side: EnumFacing) = toLocal(side) == EnumFacing.DOWN
//
//  override def sidedNode(side: EnumFacing) = if (toLocal(side) == EnumFacing.DOWN) getNode else null
//
//  // Override automatic analyzer implementation for sided environments.
//  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = Array(getNode)
//
//  // ----------------------------------------------------------------------- //
//
//  @Callback(doc = """function() -- Clears the hologram.""")
//  def clear(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
//    for (i <- volume.indices) volume(i) = 0
//    ServerPacketSender.sendHologramClear(this)
//    resetDirtyFlag()
//    litRatio = 0
//    null
//  }
//
//  @Callback(direct = true, doc = """function(x:number, y:number, z:number):number -- Returns the value for the specified voxel.""")
//  def get(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
//    val (x, y, z) = checkCoordinates(args)
//    result(getColor(x, y, z))
//  }
//
//  @Callback(direct = true, limit = 256, doc = """function(x:number, y:number, z:number, value:number or boolean) -- Set the value for the specified voxel.""")
//  def set(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
//    val (x, y, z) = checkCoordinates(args)
//    val value = checkColor(args, 3)
//    setColor(x, y, z, value)
//    null
//  }
//
//  @Callback(direct = true, limit = 128, doc = """function(x:number, z:number[, minY:number], maxY:number, value:number or boolean) -- Fills an interval of a column with the specified value.""")
//  def fill(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
//    val (x, _, z) = checkCoordinates(args, 0, -1, 1)
//    val (minY, maxY, value) =
//      if (args.count > 4)
//        (math.min(32, math.max(1, args.checkInteger(2))), math.min(32, math.max(1, args.checkInteger(3))), checkColor(args, 4))
//      else
//        (1, math.min(32, math.max(1, args.checkInteger(2))), checkColor(args, 3))
//    if (minY > maxY) throw new IllegalArgumentException("interval is empty")
//
//    val mask = (0xFFFFFFFF >>> (31 - (maxY - minY))) << (minY - 1)
//    val lbit = value & 1
//    val hbit = (value >>> 1) & 1
//    if (lbit == 0 || height == 0) volume(x + z * width) &= ~mask
//    else volume(x + z * width) |= mask
//    if (hbit == 0 || height == 0) volume(x + z * width + width * width) &= ~mask
//    else volume(x + z * width + width * width) |= mask
//
//    setDirty(x, z)
//    null
//  }
//
//  @Callback(doc = """function(data:string) -- Set the raw buffer to the specified byte array, where each byte represents a voxel color. Nesting is x,z,y.""")
//  def setRaw(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
//    val data = args.checkByteArray(0)
//    for (x <- 0 until width; z <- 0 until width) {
//      val offset = z * height + x * height * width
//      if (data.length >= offset + height) {
//        var lbit = 0
//        var hbit = 0
//        for (y <- (height - 1) to 0 by -1) {
//          val color = data(offset + y)
//          lbit |= (color & 1) << y
//          hbit |= ((color & 3) >>> 1) << y
//        }
//        val index = x + z * width
//        if (volume(index) != lbit || volume(index + width * width) != hbit) {
//          volume(index) = lbit
//          volume(index + width * width) = hbit
//          setDirty(x, z)
//        }
//      }
//    }
//    context.pause(Settings.Hologram.rawDelay)
//    null
//  }
//
//  @Callback(doc = """function(x:number, z:number, sx:number, sz:number, tx:number, tz:number) -- Copies an area of columns by the specified translation.""")
//  def copy(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
//    val (x, _, z) = checkCoordinates(args, 0, -1, 1)
//    val w = args.checkInteger(2)
//    val h = args.checkInteger(3)
//    val tx = args.checkInteger(4)
//    val tz = args.checkInteger(5)
//
//    // Anything to do at all?
//    if (w <= 0 || h <= 0) return null
//    if (tx == 0 && tz == 0) return null
//    // Loop over the target rectangle, starting from the directions away from
//    // the source rectangle and copy the data. This way we ensure we don't
//    // overwrite anything we still need to copy.
//    val (dx0, dx1) = (math.max(0, math.min(width - 1, x + tx + w - 1)), math.max(0, math.min(width, x + tx))) match {
//      case dx if tx > 0 => dx
//      case dx => dx.swap
//    }
//    val (dz0, dz1) = (math.max(0, math.min(width - 1, z + tz + h - 1)), math.max(0, math.min(width, z + tz))) match {
//      case dz if tz > 0 => dz
//      case dz => dz.swap
//    }
//    val (sx, sz) = (if (tx > 0) -1 else 1, if (tz > 0) -1 else 1)
//    // Copy values to destination rectangle if there source is valid.
//    for (nz <- dz0 to dz1 by sz) {
//      nz - tz match {
//        case oz if oz >= 0 && oz < width =>
//          for (nx <- dx0 to dx1 by sx) {
//            nx - tx match {
//              case ox if ox >= 0 && ox < width =>
//                volume(nz * width + nx) = volume(oz * width + ox)
//                volume(nz * width + nx + width * width) = volume(oz * width + ox + width * width)
//              case _ => /* Got no source column. */
//            }
//          }
//        case _ => /* Got no source row. */
//      }
//    }
//
//    // Mark target rectangle dirty.
//    setDirty(math.min(dx0, dx1), math.min(dz0, dz1))
//    setDirty(math.max(dx0, dx1), math.max(dz0, dz1))
//
//    // The reasoning here is: it'd take 18 ticks to do the whole are with fills,
//    // so make this slightly more efficient (15 ticks - 0.75 seconds). Make it
//    // 'free' if it's less than 0.25 seconds, i.e. for small copies.
//    val area = (math.max(dx0, dx1) - math.min(dx0, dx1)) * (math.max(dz0, dz1) - math.min(dz0, dz1))
//    val relativeArea = math.max(0, area / (width * width).toFloat - 0.25)
//    context.pause(relativeArea)
//
//    null
//  }
//
//  @Callback(direct = true, doc = """function():number -- Returns the render scale of the hologram.""")
//  def getScale(context: Context, args: Arguments): Array[AnyRef] = {
//    result(scale)
//  }
//
//  @Callback(doc = """function(value:number) -- Set the render scale. A larger scale consumes more energy.""")
//  def setScale(context: Context, args: Arguments): Array[AnyRef] = {
//    scale = math.max(0.333333, math.min(Settings.Hologram.maxScaleByTier(tier), args.checkDouble(0)))
//    ServerPacketSender.sendHologramScale(this)
//    null
//  }
//
//  @Callback(direct = true, doc = """function():number, number, number -- Returns the relative render projection offsets of the hologram.""")
//  def getTranslation(context: Context, args: Arguments): Array[AnyRef] = {
//    result(translation.xCoord, translation.yCoord, translation.zCoord)
//  }
//
//  @Callback(doc = """function(tx:number, ty:number, tz:number) -- Sets the relative render projection offsets of the hologram.""")
//  def setTranslation(context: Context, args: Arguments): Array[AnyRef] = {
//    // Validate all axes before setting the values.
//    val maxTranslation = Settings.Hologram.maxTranslationByTier(tier)
//    val tx = math.max(-maxTranslation, math.min(maxTranslation, args.checkDouble(0)))
//    val ty = math.max(0, math.min(maxTranslation * 2, args.checkDouble(1)))
//    val tz = math.max(-maxTranslation, math.min(maxTranslation, args.checkDouble(2)))
//
//    translation = new Vec3d(tx, ty, tz)
//
//    ServerPacketSender.sendHologramOffset(this)
//    null
//  }
//
//  @Callback(direct = true, doc = """function():number -- The color depth supported by the hologram.""")
//  def maxDepth(context: Context, args: Arguments): Array[AnyRef] = {
//    result(tier + 1)
//  }
//
//  @Callback(doc = """function(index:number):number -- Get the color defined for the specified value.""")
//  def getPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
//    val index = args.checkInteger(0)
//    if (index < 1 || index > colors.length) throw new ArrayIndexOutOfBoundsException()
//    // Colors are stored as 0xAABBGGRR for rendering convenience, so convert them.
//    result(convertColor(colors(index - 1)))
//  }
//
//  @Callback(doc = """function(index:number, value:number):number -- Set the color defined for the specified value.""")
//  def setPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
//    val index = args.checkInteger(0)
//    if (index < 1 || index > colors.length) throw new ArrayIndexOutOfBoundsException()
//    val value = args.checkInteger(1)
//    val oldValue = colors(index - 1)
//    // Change byte order here to allow passing stored color to OpenGL "as-is"
//    // (as whole Int, i.e. 0xAABBGGRR, alpha is unused but present for alignment)
//    colors(index - 1) = convertColor(value)
//    ServerPacketSender.sendHologramColor(this, index - 1, colors(index - 1))
//    result(oldValue)
//  }
//
//  @Callback(doc = """function(angle:number, x:number, y:number, z:number):boolean -- Set the base rotation of the displayed hologram.""")
//  def setRotation(context: Context, args: Arguments): Array[AnyRef] = {
//    if (tier > 0) {
//      val r = args.checkDouble(0) % 360
//      val x = args.checkDouble(1)
//      val y = args.checkDouble(2)
//      val z = args.checkDouble(3)
//
//      rotationAngle = r.toFloat
//      rotationX = x.toFloat
//      rotationY = y.toFloat
//      rotationZ = z.toFloat
//      ServerPacketSender.sendHologramRotation(this)
//
//      result(true)
//    }
//    else result(Unit, "not supported")
//  }
//
//  @Callback(doc = """function(speed:number, x:number, y:number, z:number):boolean -- Set the rotation speed of the displayed hologram.""")
//  def setRotationSpeed(context: Context, args: Arguments): Array[AnyRef] = {
//    if (tier > 0) {
//      val v = args.checkDouble(0) max -360 * 4 min 360 * 4
//      val x = args.checkDouble(1)
//      val y = args.checkDouble(2)
//      val z = args.checkDouble(3)
//
//      rotationSpeed = v.toFloat
//      rotationSpeedX = x.toFloat
//      rotationSpeedY = y.toFloat
//      rotationSpeedZ = z.toFloat
//      ServerPacketSender.sendHologramRotationSpeed(this)
//
//      result(true)
//    }
//    else result(Unit, "not supported")
//  }
//
//  private def checkCoordinates(args: Arguments, idxX: Int = 0, idxY: Int = 1, idxZ: Int = 2) = {
//    val x = if (idxX >= 0) args.checkInteger(idxX) - 1 else 0
//    if (x < 0 || x >= width) throw new ArrayIndexOutOfBoundsException("x")
//    val y = if (idxY >= 0) args.checkInteger(idxY) - 1 else 0
//    if (y < 0 || y >= height) throw new ArrayIndexOutOfBoundsException("y")
//    val z = if (idxZ >= 0) args.checkInteger(idxZ) - 1 else 0
//    if (z < 0 || z >= width) throw new ArrayIndexOutOfBoundsException("z")
//    (x, y, z)
//  }
//
//  private def checkColor(args: Arguments, index: Int) = {
//    val value =
//      if (args.isBoolean(index))
//        if (args.checkBoolean(index)) 1 else 0
//      else
//        args.checkInteger(index)
//    if (value < 0 || value > colors.length) throw new IllegalArgumentException("invalid value")
//    value
//  }
//
//  private def convertColor(color: Int) = {
//    ((color & 0x0000FF) << 16) | (color & 0x00FF00) | ((color & 0xFF0000) >>> 16)
//  }
//
//  // ----------------------------------------------------------------------- //
//
//  override def updateEntity() {
//    super.updateEntity()
//    if (isServer) {
//      if (dirty.nonEmpty) this.synchronized {
//        val dirtySizeX = dirtyUntilX - dirtyFromX
//        val dirtySizeZ = dirtyUntilZ - dirtyFromZ
//        // Sending the dirty area requires
//        //   dirtySizeX * dirtySizeZ * (4 + 4)
//        // bytes (2 = low + high byte).
//        // Sending a single changes requires
//        //   changes * (4 + 4 + 2)
//        // bytes (other 2 byte = coords).
//        // So at some point it'll be cheaper to just send the area:
//        // changes * (4 + 4 + 2) = dirtySizeX * dirtySizeZ * (4 + 4)
//        // changes = dirtySizeX * dirtySizeZ * (4 + 4) / (4 + 4 + 2) = dirtySizeX * dirtySizeZ * 0.8
//        // So if changes are larger than that, just send the full hologram.
//        if (dirty.size > dirtySizeX * dirtySizeZ * 0.8)
//          ServerPacketSender.sendHologramArea(this)
//        else
//          ServerPacketSender.sendHologramValues(this)
//        resetDirtyFlag()
//      }
//      if (getWorld.getTotalWorldTime % Settings.Power.tickFrequency == 0) {
//        if (litRatio < 0) this.synchronized {
//          litRatio = 0
//          for (i <- volume.indices) {
//            if (volume(i) != 0) litRatio += 1
//          }
//          litRatio /= volume.length
//        }
//
//        val hadPower = hasPower
//        val neededPower = Settings.Power.Cost.hologram * litRatio * scale * Settings.Power.tickFrequency
//        hasPower = getNode.tryChangeEnergy(-neededPower)
//        if (hasPower != hadPower) {
//          ServerPacketSender.sendHologramPowerChange(this)
//        }
//      }
//    }
//  }
//
//  // ----------------------------------------------------------------------- //
//
//  override def shouldRenderInPass(pass: Int) = pass == 1
//
//  override def getMaxRenderDistanceSquared = scale / Settings.Hologram.maxScaleByTier.max * Settings.Client.hologramRenderDistance * Settings.Client.hologramRenderDistance
//
//  def getFadeStartDistanceSquared = scale / Settings.Hologram.maxScaleByTier.max * Settings.Client.hologramFadeStartDistance * Settings.Client.hologramFadeStartDistance
//
//  private final val Sqrt2 = Math.sqrt(2)
//
//  override def getRenderBoundingBox = {
//    val cx = x + 0.5
//    val cy = y + 0.5
//    val cz = z + 0.5
//    val sh = width / 16 * scale * Sqrt2
//    // overscale to take into account 45 degree rotation
//    val sv = height / 16 * scale * Sqrt2
//    new AxisAlignedBB(
//      cx + (-0.5 + translation.xCoord) * sh,
//      cy + translation.yCoord * sv,
//      cz + (-0.5 + translation.zCoord) * sh,
//      cx + (0.5 + translation.xCoord) * sh,
//      cy + (1 + translation.yCoord) * sv,
//      cz + (0.5 + translation.xCoord) * sh)
//  }
//
//  // ----------------------------------------------------------------------- //
//
//  private def dataPath = getNode.getAddress + "_data"
//
//  private final val TierTag = Constants.namespace + "tier"
//  private final val VolumeTag = "volume"
//  private final val ColorsTag = "colors"
//  private final val ScaleTag = Constants.namespace + "scale"
//  private final val OffsetXTag = Constants.namespace + "offsetX"
//  private final val OffsetYTag = Constants.namespace + "offsetY"
//  private final val OffsetZTag = Constants.namespace + "offsetZ"
//  private final val RotationAngleTag = Constants.namespace + "rotationAngle"
//  private final val RotationXTag = Constants.namespace + "rotationX"
//  private final val RotationYTag = Constants.namespace + "rotationY"
//  private final val RotationZTag = Constants.namespace + "rotationZ"
//  private final val RotationSpeedTag = Constants.namespace + "rotationSpeed"
//  private final val RotationSpeedXTag = Constants.namespace + "rotationSpeedX"
//  private final val RotationSpeedYTag = Constants.namespace + "rotationSpeedY"
//  private final val RotationSpeedZTag = Constants.namespace + "rotationSpeedZ"
//  private final val HasPowerTag = Constants.namespace + "hasPower"
//
//  override def readFromNBTForServer(nbt: NBTTagCompound) {
//    tier = nbt.getByte(TierTag) max 0 min 1
//    super.readFromNBTForServer(nbt)
//    val tag = SaveHandler.loadNBT(nbt, dataPath)
//    tag.getIntArray(VolumeTag).copyToArray(volume)
//    tag.getIntArray(ColorsTag).map(convertColor).copyToArray(colors)
//    scale = nbt.getDouble(ScaleTag)
//    val tx = nbt.getDouble(OffsetXTag)
//    val ty = nbt.getDouble(OffsetYTag)
//    val tz = nbt.getDouble(OffsetZTag)
//    translation = new Vec3d(tx, ty, tz)
//    rotationAngle = nbt.getFloat(RotationAngleTag)
//    rotationX = nbt.getFloat(RotationXTag)
//    rotationY = nbt.getFloat(RotationYTag)
//    rotationZ = nbt.getFloat(RotationZTag)
//    rotationSpeed = nbt.getFloat(RotationSpeedTag)
//    rotationSpeedX = nbt.getFloat(RotationSpeedXTag)
//    rotationSpeedY = nbt.getFloat(RotationSpeedYTag)
//    rotationSpeedZ = nbt.getFloat(RotationSpeedZTag)
//  }
//
//  override def writeToNBTForServer(nbt: NBTTagCompound) = this.synchronized {
//    nbt.setByte(TierTag, tier.toByte)
//    super.writeToNBTForServer(nbt)
//    SaveHandler.scheduleSave(getWorld, x, z, nbt, dataPath, tag => {
//      tag.setIntArray(VolumeTag, volume)
//      tag.setIntArray(ColorsTag, colors.map(convertColor))
//    })
//    nbt.setDouble(ScaleTag, scale)
//    nbt.setDouble(OffsetXTag, translation.xCoord)
//    nbt.setDouble(OffsetYTag, translation.yCoord)
//    nbt.setDouble(OffsetZTag, translation.zCoord)
//    nbt.setFloat(RotationAngleTag, rotationAngle)
//    nbt.setFloat(RotationXTag, rotationX)
//    nbt.setFloat(RotationYTag, rotationY)
//    nbt.setFloat(RotationZTag, rotationZ)
//    nbt.setFloat(RotationSpeedTag, rotationSpeed)
//    nbt.setFloat(RotationSpeedXTag, rotationSpeedX)
//    nbt.setFloat(RotationSpeedYTag, rotationSpeedY)
//    nbt.setFloat(RotationSpeedZTag, rotationSpeedZ)
//  }
//
//  @SideOnly(Side.CLIENT)
//  override def readFromNBTForClient(nbt: NBTTagCompound) {
//    super.readFromNBTForClient(nbt)
//    nbt.getIntArray(VolumeTag).copyToArray(volume)
//    nbt.getIntArray(ColorsTag).copyToArray(colors)
//    scale = nbt.getDouble(ScaleTag)
//    hasPower = nbt.getBoolean(HasPowerTag)
//    val tx = nbt.getDouble(OffsetXTag)
//    val ty = nbt.getDouble(OffsetYTag)
//    val tz = nbt.getDouble(OffsetZTag)
//    translation = new Vec3d(tx, ty, tz)
//    rotationAngle = nbt.getFloat(RotationAngleTag)
//    rotationX = nbt.getFloat(RotationXTag)
//    rotationY = nbt.getFloat(RotationYTag)
//    rotationZ = nbt.getFloat(RotationZTag)
//    rotationSpeed = nbt.getFloat(RotationSpeedTag)
//    rotationSpeedX = nbt.getFloat(RotationSpeedXTag)
//    rotationSpeedY = nbt.getFloat(RotationSpeedYTag)
//    rotationSpeedZ = nbt.getFloat(RotationSpeedZTag)
//  }
//
//  override def writeToNBTForClient(nbt: NBTTagCompound) {
//    super.writeToNBTForClient(nbt)
//    nbt.setIntArray(VolumeTag, volume)
//    nbt.setIntArray(ColorsTag, colors)
//    nbt.setDouble(ScaleTag, scale)
//    nbt.setBoolean(HasPowerTag, hasPower)
//    nbt.setDouble(OffsetXTag, translation.xCoord)
//    nbt.setDouble(OffsetYTag, translation.yCoord)
//    nbt.setDouble(OffsetZTag, translation.zCoord)
//    nbt.setFloat(RotationAngleTag, rotationAngle)
//    nbt.setFloat(RotationXTag, rotationX)
//    nbt.setFloat(RotationYTag, rotationY)
//    nbt.setFloat(RotationZTag, rotationZ)
//    nbt.setFloat(RotationSpeedTag, rotationSpeed)
//    nbt.setFloat(RotationSpeedXTag, rotationSpeedX)
//    nbt.setFloat(RotationSpeedYTag, rotationSpeedY)
//    nbt.setFloat(RotationSpeedZTag, rotationSpeedZ)
//  }
//}
