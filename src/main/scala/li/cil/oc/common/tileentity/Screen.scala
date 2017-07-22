package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network._
import li.cil.oc.client.gui
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable
import scala.language.postfixOps

class Screen(var tier: Int) extends traits.TextBuffer with SidedEnvironment with traits.Rotatable with traits.RedstoneAware with traits.Colored with Analyzable with Ordered[Screen] {
  def this() = this(0)

  // Enable redstone functionality.
  _isOutputEnabled = true

  override def validFacings = ForgeDirection.VALID_DIRECTIONS

  // ----------------------------------------------------------------------- //

  /**
   * Check for multi-block screen option in next update. We do this in the
   * update to avoid unnecessary checks on chunk unload.
   */
  var shouldCheckForMultiBlock = true

  /**
   * On the client we delay connecting screens a little, to avoid glitches
   * when not all tile entity data for a chunk has been received within a
   * single tick (meaning some screens are still "missing").
   */
  var delayUntilCheckForMultiBlock = 40

  var width, height = 1

  var origin = this

  val screens = mutable.Set(this)

  var hadRedstoneInput = false

  var cachedBounds: Option[AxisAlignedBB] = None

  var invertTouchMode = false

  private val arrows = mutable.Set.empty[EntityArrow]

  color = Color.byTier(tier)

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = toLocal(side) != ForgeDirection.SOUTH

  // Allow connections from front for keyboards, and keyboards only...
  override def sidedNode(side: ForgeDirection) = if (toLocal(side) != ForgeDirection.SOUTH || (world.blockExists(position.offset(side)) && world.getTileEntity(position.offset(side)).isInstanceOf[Keyboard])) node else null

  // ----------------------------------------------------------------------- //

  def isOrigin = origin == this

  def localPosition = {
    val (lx, ly, _) = project(this)
    val (ox, oy, _) = project(origin)
    (lx - ox, ly - oy)
  }

  def hasKeyboard = screens.exists(screen =>
    ForgeDirection.VALID_DIRECTIONS.map(side => (side, {
      val (nx, ny, nz) = (screen.x + side.offsetX, screen.y + side.offsetY, screen.z + side.offsetZ)
      if (world.blockExists(nx, ny, nz)) world.getTileEntity(nx, ny, nz)
      else null
    })).exists {
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
    invertTouchMode = false
  }

  def toScreenCoordinates(hitX: Double, hitY: Double, hitZ: Double): (Boolean, Option[(Double, Double)]) = {
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
      return (false, None)
    }
    if (!world.isRemote) return (true, None)

    val (iw, ih) = (width - border * 2, height - border * 2)
    val (rx, ry) = ((ax - border) / iw, (ay - border) / ih)

    // Make it a relative position in the displayed buffer.
    val bw = origin.buffer.getViewportWidth
    val bh = origin.buffer.getViewportHeight
    val (bpw, bph) = (origin.buffer.renderWidth / iw.toDouble, origin.buffer.renderHeight / ih.toDouble)
    val (brx, bry) = if (bpw > bph) {
      val rh = bph.toDouble / bpw.toDouble
      val bry = (ry - (1 - rh) * 0.5) / rh
      (rx, bry)
    }
    else if (bph > bpw) {
      val rw = bpw.toDouble / bph.toDouble
      val brx = (rx - (1 - rw) * 0.5) / rw
      (brx, ry)
    }
    else {
      (rx, ry)
    }

    val inBounds = bry >= 0 && bry <= 1 && brx >= 0 || brx <= 1
    (inBounds, Some((brx * bw, bry * bh)))
  }

  def copyToAnalyzer(hitX: Double, hitY: Double, hitZ: Double): Boolean = {
    val (inBounds, coordinates) = toScreenCoordinates(hitX, hitY, hitZ)
    coordinates match {
      case Some((x, y)) => origin.buffer match {
        case buffer: TextBuffer =>
          buffer.copyToAnalyzer(y.toInt, null)
          true
        case _ => false
      }
      case _ => inBounds
    }
  }

  def click(hitX: Double, hitY: Double, hitZ: Double): Boolean = {
    val (inBounds, coordinates) = toScreenCoordinates(hitX, hitY, hitZ)
    coordinates match {
      case Some((x, y)) =>
        // Send the packet to the server (manually, for accuracy).
        origin.buffer.mouseDown(x, y, 0, null)
        true
      case _ => inBounds
    }
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

  def shot(arrow: EntityArrow) {
    arrows.add(arrow)
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = true

  override def updateEntity() {
    super.updateEntity()
    if (shouldCheckForMultiBlock && ((isClient && isClientReadyForMultiBlockCheck) || (isServer && isConnected))) {
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
          if (world.blockExists(nx, ny, nz)) world.getTileEntity(nx, ny, nz) match {
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
      queue.foreach(screen => {
        val buffer = screen.buffer
        if (screen.isOrigin) {
          if (isServer) {
            buffer.node.asInstanceOf[Component].setVisibility(Visibility.Network)
            buffer.setEnergyCostPerTick(Settings.get.screenCost * screen.width * screen.height)
            buffer.setAspectRatio(screen.width, screen.height)
          }
        }
        else {
          if (isServer) {
            buffer.node.asInstanceOf[Component].setVisibility(Visibility.None)
            buffer.setEnergyCostPerTick(Settings.get.screenCost)
          }
          buffer.setAspectRatio(1, 1)
          val w = buffer.getWidth
          val h = buffer.getHeight
          buffer.setForegroundColor(0xFFFFFF, false)
          buffer.setBackgroundColor(0x000000, false)
          buffer.fill(0, 0, w, h, ' ')
        }
      })
    }
    if (arrows.nonEmpty) {
      for (arrow <- arrows) {
        val hitX = arrow.posX - x
        val hitY = arrow.posY - y
        val hitZ = arrow.posZ - z
        arrow.shootingEntity match {
          case player: EntityPlayer if player == Minecraft.getMinecraft.thePlayer => click(hitX, hitY, hitZ)
          case _ =>
        }
      }
      arrows.clear()
    }
  }

  private def isClientReadyForMultiBlockCheck = if (delayUntilCheckForMultiBlock > 0) {
    delayUntilCheckForMultiBlock -= 1
    false
  } else true

  override def dispose() {
    super.dispose()
    screens.clone().foreach(_.checkMultiBlock())
    if (isClient) {
      Minecraft.getMinecraft.currentScreen match {
        case screenGui: gui.Screen if screenGui.buffer == buffer =>
          Minecraft.getMinecraft.displayGuiScreen(null)
        case _ =>
      }
    }
  }

  override protected def onColorChanged() {
    super.onColorChanged()
    screens.clone().foreach(_.checkMultiBlock())
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    tier = nbt.getByte(Settings.namespace + "tier") max 0 min 2
    color = Color.byTier(tier)
    super.readFromNBTForServer(nbt)
    hadRedstoneInput = nbt.getBoolean(Settings.namespace + "hadRedstoneInput")
    invertTouchMode = nbt.getBoolean(Settings.namespace + "invertTouchMode")
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    nbt.setByte(Settings.namespace + "tier", tier.toByte)
    super.writeToNBTForServer(nbt)
    nbt.setBoolean(Settings.namespace + "hadRedstoneInput", hadRedstoneInput)
    nbt.setBoolean(Settings.namespace + "invertTouchMode", invertTouchMode)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    invertTouchMode = nbt.getBoolean("invertTouchMode")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("invertTouchMode", invertTouchMode)
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

  override protected def onRedstoneInputChanged(side: ForgeDirection, oldMaxValue: Int, newMaxValue: Int) {
    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
    val hasRedstoneInput = screens.map(_.maxInput).max > 0
    if (hasRedstoneInput != hadRedstoneInput) {
      hadRedstoneInput = hasRedstoneInput
      if (hasRedstoneInput) {
        origin.buffer.setPowerState(!origin.buffer.getPowerState)
      }
    }
  }

  override def onRotationChanged() {
    super.onRotationChanged()
    screens.clone().foreach(_.checkMultiBlock())
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
      world.blockExists(nx, ny, nz) && (world.getTileEntity(nx, ny, nz) match {
        case s: Screen if s.tier == tier && s.pitch == pitch && s.color == color && s.yaw == yaw && !screens.contains(s) =>
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
      })
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