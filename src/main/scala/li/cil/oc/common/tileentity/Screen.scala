package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network._
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.Settings
import li.cil.oc.util.Color
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.ForgeDirection
import scala.collection.mutable
import scala.language.postfixOps

class Screen(var tier: Int) extends traits.TextBuffer with SidedEnvironment with traits.Rotatable with traits.RedstoneAware with traits.Colored with Analyzable with Ordered[Screen] {
  def this() = this(0)

  color = Color.byTier(tier)

  _isOutputEnabled = true

  override def validFacings = ForgeDirection.VALID_DIRECTIONS

  // ----------------------------------------------------------------------- //

  override protected val _buffer = new component.Buffer(this) {
    @Callback(doc = """function():boolean -- Returns whether the screen is currently on.""")
    def isOn(computer: Context, args: Arguments): Array[AnyRef] = result(origin.isOn)

    @Callback(doc = """function():boolean -- Turns the screen on. Returns true if it was off.""")
    def turnOn(computer: Context, args: Arguments): Array[AnyRef] = {
      if (!origin.isOn) {
        origin.turnOn()
        result(true, origin.isOn)
      }
      else result(false, origin.isOn)
    }

    @Callback(doc = """function():boolean -- Turns off the screen. Returns true if it was on.""")
    def turnOff(computer: Context, args: Arguments): Array[AnyRef] = {
      if (origin.isOn) {
        origin.turnOff()
        result(true, origin.isOn)
      }
      else result(false, origin.isOn)
    }
  }

  // This is the energy cost (per tick) to keep the screen running if every
  // single "pixel" is lit. This cost increases with higher tiers as their
  // maximum resolution (pixel density) increases. For a basic screen this is
  // simply the configured cost.
  val fullyLitCost = {
    val (w, h) = Settings.screenResolutionsByTier(0)
    val (mw, mh) = buffer.maxResolution
    Settings.get.screenCost * (mw * mh) / (w * h)
  }

  /**
   * Check for multi-block screen option in next update. We do this in the
   * update to avoid unnecessary checks on chunk unload.
   */
  var shouldCheckForMultiBlock = true

  var width, height = 1

  var origin = this

  val screens = mutable.Set(this)

  var relativeLitArea = -1.0

  var hasPower = true

  var isOn = true

  var hadRedstoneInput = false

  var cachedBounds: Option[AxisAlignedBB] = None

  private val arrows = mutable.Set.empty[EntityArrow]

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = toLocal(side) != ForgeDirection.SOUTH

  // Allow connections from front for keyboards, just don't render cables as connected...
  override def sidedNode(side: ForgeDirection) = node

  // ----------------------------------------------------------------------- //

  def isOrigin = origin == this

  def localPosition = {
    val (lx, ly, _) = project(this)
    val (ox, oy, _) = project(origin)
    (lx - ox, ly - oy)
  }

  override def hasKeyboard = screens.exists(screen =>
    ForgeDirection.VALID_DIRECTIONS.map(side => (side, world.getBlockTileEntity(screen.x + side.offsetX, screen.y + side.offsetY, screen.z + side.offsetZ))).exists {
      case (side, keyboard: Keyboard) => keyboard.hasNodeOnSide(side.getOpposite)
      case _ => false
    })

  def checkMultiBlock() {
    shouldCheckForMultiBlock = true
    width = 1
    height = 1
    origin = this
    screens.clear()
    screens += this
    cachedBounds = None
  }

  def click(player: EntityPlayer, hitX: Double, hitY: Double, hitZ: Double): Boolean = {
    // Compute absolute position of the click on the face, measured in blocks.
    def dot(f: ForgeDirection) = f.offsetX * hitX + f.offsetY * hitY + f.offsetZ * hitZ
    val (hx, hy) = (dot(toGlobal(ForgeDirection.EAST)), dot(toGlobal(ForgeDirection.UP)))
    val tx = if (hx < 0) 1 + hx else hx
    val ty = 1 - (if (hy < 0) 1 + hy else hy)
    val (lx, ly) = localPosition
    val (ax, ay) = (lx + tx, height - 1 - ly + ty)

    // Get the relative position in the *display area* of the face.
    val border = 2.25 / 16.0
    if (ax <= border || ay <= border || ax >= width - border || ay >= height - border) {
      return false
    }
    val (iw, ih) = (width - border * 2, height - border * 2)
    val (rx, ry) = ((ax - border) / iw, (ay - border) / ih)

    // Make it a relative position in the displayed buffer.
    val (bw, bh) = origin.buffer.resolution
    val (bpw, bph) = (bw * MonospaceFontRenderer.fontWidth / iw.toDouble, bh * MonospaceFontRenderer.fontHeight / ih.toDouble)
    val (brx, bry) = if (bpw > bph) {
      val rh = bph.toDouble / bpw.toDouble
      val bry = (ry - (1 - rh) * 0.5) / rh
      if (bry <= 0 || bry >= 1) {
        return false
      }
      (rx, bry)
    }
    else if (bph > bpw) {
      val rw = bpw.toDouble / bph.toDouble
      val brx = (rx - (1 - rw) * 0.5) / rw
      if (brx <= 0 || brx >= 1) {
        return false
      }
      (brx, ry)
    }
    else {
      (rx, ry)
    }

    // Convert to absolute coordinates and send the packet to the server.
    if (world.isRemote) {
      ClientPacketSender.sendMouseClick(this.buffer, (brx * bw).toInt + 1, (bry * bh).toInt + 1, drag = false, 0)
    }
    true
  }

  def walk(entity: Entity) {
    val (x, y) = localPosition
    entity match {
      case player: EntityPlayer if Settings.get.inputUsername =>
        origin.node.sendToReachable("computer.signal", "walk", Int.box(x + 1), Int.box(height - y), player.getCommandSenderName)
      case _ =>
        origin.node.sendToReachable("computer.signal", "walk", Int.box(x + 1), Int.box(height - y))
    }
  }

  def shot(arrow: EntityArrow, hitX: Double, hitY: Double, hitZ: Double) {
    // This is nasty, but I see no other way: arrows can trigger two collisions,
    // once on their own when hitting a block, a second time via their entity's
    // common collision checker. The second one (collision checker) has the
    // better coordinates (arrow moved back out of the block it collided with),
    // so use that when possible, otherwise resolve in next update.
    if (!arrows.add(arrow)) {
      arrows.remove(arrow)
      arrow.shootingEntity match {
        case player: EntityPlayer => click(player, hitX, hitY, hitZ)
        case _ =>
      }
    }
  }

  def turnOn() {
    origin.isOn = true
    val neededPower = width * height * Settings.get.screenCost * Settings.get.tickFrequency
    origin.hasPower = buffer.node.changeBuffer(-neededPower) == 0
    ServerPacketSender.sendScreenPowerChange(origin, origin.isOn && origin.hasPower)
  }

  def turnOff() {
    origin.isOn = false
    ServerPacketSender.sendScreenPowerChange(origin, origin.isOn && origin.hasPower)
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer && isOn && isOrigin && world.getWorldTime % Settings.get.tickFrequency == 0) {
      if (relativeLitArea < 0) {
        // The relative lit area is the number of pixels that are not blank
        // versus the number of pixels in the *current* resolution. This is
        // scaled to multi-block screens, since we only compute this for the
        // origin.
        val (w, h) = buffer.resolution
        relativeLitArea = width * height * buffer.lines.foldLeft(0) {
          (acc, line) => acc + line.count(' ' !=)
        } / (w * h).toDouble
      }
      val hadPower = hasPower
      val neededPower = relativeLitArea * fullyLitCost * Settings.get.tickFrequency
      hasPower = buffer.node.tryChangeBuffer(-neededPower)
      if (hasPower != hadPower) {
        ServerPacketSender.sendScreenPowerChange(this, isOn && hasPower)
      }
    }
    if (shouldCheckForMultiBlock) {
      // Make sure we merge in a deterministic order, to avoid getting
      // different results on server and client due to the update order
      // differing between the two. This also saves us from having to save
      // any multi-block specific state information.
      val pending = mutable.SortedSet(this)
      val queue = mutable.Queue(this)
      while (queue.nonEmpty) {
        val current = queue.dequeue()
        val (lx, ly, lz) = project(current)
        def tryQueue(dx: Int, dy: Int) {
          val (nx, ny, nz) = unproject(lx + dx, ly + dy, lz)
          world.getBlockTileEntity(nx, ny, nz) match {
            case s: Screen if s.pitch == pitch && s.yaw == yaw && pending.add(s) => queue += s
            case _ => // Ignore.
          }
        }
        tryQueue(-1, 0)
        tryQueue(1, 0)
        tryQueue(0, -1)
        tryQueue(0, 1)
      }
      // Perform actual merges.
      while (pending.nonEmpty) {
        val current = pending.firstKey
        while (current.tryMerge()) {}
        current.screens.foreach {
          screen =>
            screen.shouldCheckForMultiBlock = false
            screen.bufferIsDirty = true
            pending.remove(screen)
            queue += screen
        }
        if (isClient) {
          val bounds = current.origin.getRenderBoundingBox
          world.markBlockRangeForRenderUpdate(bounds.minX.toInt, bounds.minY.toInt, bounds.minZ.toInt,
            bounds.maxX.toInt, bounds.maxY.toInt, bounds.maxZ.toInt)
        }
      }
      // Update visibility after everything is done, to avoid noise.
      queue.foreach(screen =>
        if (screen.isOrigin) {
          if (isServer) {
            screen.buffer.node.setVisibility(Visibility.Network)
          }
        }
        else {
          if (isServer) {
            screen.buffer.node.setVisibility(Visibility.None)
          }
          val buffer = screen.buffer
          val (w, h) = buffer.resolution
          buffer.foreground = 0xFFFFFF
          buffer.background = 0x000000
          if (buffer.buffer.fill(0, 0, w, h, ' ')) {
            onScreenFill(0, 0, w, h, ' ')
          }
        }
      )
    }
    if (arrows.size > 0) {
      for (arrow <- arrows) {
        val hitX = math.max(0, math.min(1, arrow.posX - x))
        val hitY = math.max(0, math.min(1, arrow.posY - y))
        val hitZ = math.max(0, math.min(1, arrow.posZ - z))
        shot(arrow, hitX, hitY, hitZ)
      }
      assert(arrows.isEmpty)
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    cleanup()
  }

  override def invalidate() {
    super.invalidate()
    cleanup()
  }

  protected def cleanup() {
    if (currentGui.isDefined) {
      Minecraft.getMinecraft.displayGuiScreen(null)
    }
    screens.clone().foreach(_.checkMultiBlock())
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    tier = nbt.getByte(Settings.namespace + "tier") max 0 min 2
    color = Color.byTier(tier)
    super.readFromNBT(nbt)
    // This check is just to avoid powering off any screens that have been
    // placed before this was introduced.
    if (nbt.hasKey(Settings.namespace + "isOn")) {
      isOn = nbt.getBoolean(Settings.namespace + "isOn")
    }
    if (nbt.hasKey(Settings.namespace + "isOn")) {
      hasPower = nbt.getBoolean(Settings.namespace + "hasPower")
    }
    hadRedstoneInput = nbt.getBoolean(Settings.namespace + "hadRedstoneInput")
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    nbt.setByte(Settings.namespace + "tier", tier.toByte)
    super.writeToNBT(nbt)
    nbt.setBoolean(Settings.namespace + "isOn", isOn)
    nbt.setBoolean(Settings.namespace + "hasPower", hasPower)
    nbt.setBoolean(Settings.namespace + "hadRedstoneInput", hadRedstoneInput)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    hasPower = nbt.getBoolean("hasPower")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("hasPower", isOn && hasPower)
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def getRenderBoundingBox =
    if ((width == 1 && height == 1) || !isOrigin) super.getRenderBoundingBox
    else cachedBounds match {
      case Some(bounds) => bounds
      case _ =>
        val (sx, sy, sz) = unproject(width, height, 1)
        val ox = x + (if (sx < 0) 1 else 0)
        val oy = y + (if (sy < 0) 1 else 0)
        val oz = z + (if (sz < 0) 1 else 0)
        val b = AxisAlignedBB.getBoundingBox(ox, oy, oz, ox + sx, oy + sy, oz + sz)
        b.setBounds(math.min(b.minX, b.maxX), math.min(b.minY, b.maxY), math.min(b.minZ, b.maxZ),
          math.max(b.minX, b.maxX), math.max(b.minY, b.maxY), math.max(b.minZ, b.maxZ))
        cachedBounds = Some(b)
        b
    }

  @SideOnly(Side.CLIENT)
  override def getMaxRenderDistanceSquared = if (isOrigin) super.getMaxRenderDistanceSquared else 0

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = Array(origin.node)

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    val hasRedstoneInput = screens.map(_.maxInput).max > 0
    if (hasRedstoneInput != hadRedstoneInput) {
      hadRedstoneInput = hasRedstoneInput
      if (hasRedstoneInput) {
        if (origin.isOn) turnOff() else turnOn()
      }
    }
  }

  override def onRotationChanged() {
    super.onRotationChanged()
    screens.clone().foreach(_.checkMultiBlock())
  }

  override def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
    super.onScreenCopy(col, row, w, h, tx, ty)
    relativeLitArea = -1
  }

  override def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {
    super.onScreenFill(col, row, w, h, c)
    relativeLitArea = -1
  }

  override def onScreenResolutionChange(w: Int, h: Int) {
    super.onScreenResolutionChange(w, h)
    relativeLitArea = -1
  }

  override def onScreenSet(col: Int, row: Int, s: String) {
    super.onScreenSet(col, row, s)
    relativeLitArea = -1
  }

  @SideOnly(Side.CLIENT)
  override protected def markForRenderUpdate() {
    super.markForRenderUpdate()
    currentGui.foreach(_.recompileDisplayLists())
  }

  // ----------------------------------------------------------------------- //

  override def compare(that: Screen) =
    if (x != that.x) x - that.x
    else if (y != that.y) y - that.y
    else z - that.z

  // ----------------------------------------------------------------------- //

  private def tryMerge() = {
    val (ox, oy, oz) = project(origin)
    def tryMergeTowards(dx: Int, dy: Int) = {
      val (nx, ny, nz) = unproject(ox + dx, oy + dy, oz)
      world.getBlockTileEntity(nx, ny, nz) match {
        case s: Screen if s.tier == tier && s.pitch == pitch && s.yaw == yaw && !screens.contains(s) =>
          val (sx, sy, _) = project(s.origin)
          val canMergeAlongX = sy == oy && s.height == height && s.width + width <= Settings.get.maxScreenWidth
          val canMergeAlongY = sx == ox && s.width == width && s.height + height <= Settings.get.maxScreenHeight
          if (canMergeAlongX || canMergeAlongY) {
            val (newOrigin) =
              if (canMergeAlongX) {
                if (sx < ox) s.origin else origin
              }
              else {
                if (sy < oy) s.origin else origin
              }
            val (newWidth, newHeight) =
              if (canMergeAlongX) (width + s.width, height)
              else (width, height + s.height)
            val newScreens = screens ++ s.screens
            for (screen <- newScreens) {
              screen.width = newWidth
              screen.height = newHeight
              screen.origin = newOrigin
              screen.screens ++= newScreens // It's a set, so there won't be duplicates.
              screen.cachedBounds = None
            }
            true
          }
          else false // Cannot merge.
        case _ => false
      }
    }
    tryMergeTowards(0, height) || tryMergeTowards(0, -1) || tryMergeTowards(width, 0) || tryMergeTowards(-1, 0)
  }

  private def project(t: Screen) = {
    def dot(f: ForgeDirection, s: Screen) = f.offsetX * s.x + f.offsetY * s.y + f.offsetZ * s.z
    (dot(toGlobal(ForgeDirection.EAST), t), dot(toGlobal(ForgeDirection.UP), t), dot(toGlobal(ForgeDirection.SOUTH), t))
  }

  private def unproject(x: Int, y: Int, z: Int) = {
    def dot(f: ForgeDirection) = f.offsetX * x + f.offsetY * y + f.offsetZ * z
    (dot(toLocal(ForgeDirection.EAST)), dot(toLocal(ForgeDirection.UP)), dot(toLocal(ForgeDirection.SOUTH)))
  }
}