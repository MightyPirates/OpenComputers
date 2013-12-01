package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.api.network.{SidedEnvironment, Analyzable, Visibility}
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.ForgeDirection
import scala.collection.mutable

class Screen(var tier: Int) extends Buffer with SidedEnvironment with Rotatable with Analyzable with Ordered[Screen] {
  def this() = this(0)

  // ----------------------------------------------------------------------- //

  val pixelCost = {
    val (w, h) = Settings.screenResolutionsByTier(0)
    Settings.get.screenCost / (w * h)
  }

  /**
   * Check for multi-block screen option in next update. We do this in the
   * update to avoid unnecessary checks on chunk unload.
   */
  var shouldCheckForMultiBlock = true

  var width, height = 1

  var origin = this

  val screens = mutable.Set(this)

  var litPixels = -1

  var hasPower = true

  def canConnect(side: ForgeDirection) = toLocal(side) != ForgeDirection.SOUTH

  def sidedNode(side: ForgeDirection) = if (canConnect(side)) node else null

  // ----------------------------------------------------------------------- //

  def isOrigin = origin == this

  def localPosition = {
    val (lx, ly, _) = project(this)
    val (ox, oy, _) = project(origin)
    val (px, py) = (lx - ox, ly - oy)
    (px, py)
  }

  override def hasKeyboard = screens.exists(screen => ForgeDirection.VALID_DIRECTIONS.
    map(side => (side, world.getBlockTileEntity(screen.x + side.offsetX, screen.y + side.offsetY, screen.z + side.offsetZ))).
    collect {
    case (side, keyboard: Keyboard) if keyboard.facing == side => keyboard
  }.nonEmpty)

  def checkMultiBlock() {
    shouldCheckForMultiBlock = true
    width = 1
    height = 1
    origin = this
    screens.clear()
    screens += this
  }

  def click(player: EntityPlayer, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
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
    val (rx, ry) = ((ax - border) / (width - border * 2), (ay - border) / (height - border * 2))

    // Make it a relative position in the displayed buffer.
    val (bw, bh) = buffer.resolution
    val (bpw, bph) = (bw * MonospaceFontRenderer.fontWidth, bh * MonospaceFontRenderer.fontHeight)
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

    // Convert to absolute coordinates and send the (checked) signal.
    if (!world.isRemote) {
      val (bx, by) = (brx * bw, bry * bh)
      origin.node.sendToReachable("computer.checked_signal", player, "click", Int.box(bx.toInt + 1), Int.box(by.toInt + 1), player.getCommandSenderName)
    }
    true
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      if (litPixels < 0) {
        litPixels = buffer.lines.foldLeft(0)((acc, line) => acc + line.count(_ != ' '))
      }
      val hadPower = hasPower
      val neededPower = Settings.get.screenCost + pixelCost * litPixels
      hasPower = buffer.node.tryChangeBuffer(-neededPower)
      if (hasPower != hadPower) {
        ServerPacketSender.sendScreenPowerChange(this, hasPower)
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
          worldObj.getBlockTileEntity(nx, ny, nz) match {
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
          worldObj.markBlockRangeForRenderUpdate(bounds.minX.toInt, bounds.minY.toInt, bounds.minZ.toInt,
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
          val s = screen.buffer
          val (w, h) = s.resolution
          s.buffer.fill(0, 0, w, h, ' ')
        }
      )
    }
  }

  override def invalidate() {
    super.invalidate()
    if (currentGui.isDefined) {
      Minecraft.getMinecraft.displayGuiScreen(null)
    }
    screens.clone().foreach(_.checkMultiBlock())
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    tier = nbt.getByte(Settings.namespace + "tier")
    super.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    nbt.setByte(Settings.namespace + "tier", tier.toByte)
    super.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def getRenderBoundingBox =
    if ((width == 1 && height == 1) || !isOrigin) super.getRenderBoundingBox
    else {
      val (sx, sy, sz) = unproject(width, height, 1)
      val ox = x + (if (sx < 0) 1 else 0)
      val oy = y + (if (sy < 0) 1 else 0)
      val oz = z + (if (sz < 0) 1 else 0)
      val b = AxisAlignedBB.getAABBPool.getAABB(ox, oy, oz, ox + sx, oy + sy, oz + sz)
      b.setBounds(b.minX min b.maxX, b.minY min b.maxY, b.minZ min b.maxZ,
        b.minX max b.maxX, b.minY max b.maxY, b.minZ max b.maxZ)
      b
    }

  @SideOnly(Side.CLIENT)
  override def getMaxRenderDistanceSquared = if (isOrigin) super.getMaxRenderDistanceSquared else 0

  // ----------------------------------------------------------------------- //

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = origin.node

  override def onRotationChanged() {
    super.onRotationChanged()
    screens.clone().foreach(_.checkMultiBlock())
  }

  override def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
    super.onScreenCopy(col, row, w, h, tx, ty)
    litPixels = -1
  }

  override def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {
    super.onScreenFill(col, row, w, h, c)
    litPixels = -1
  }

  override def onScreenResolutionChange(w: Int, h: Int) {
    super.onScreenResolutionChange(w, h)
    litPixels = -1
  }

  override def onScreenSet(col: Int, row: Int, s: String) {
    super.onScreenSet(col, row, s)
    litPixels = -1
  }

  @SideOnly(Side.CLIENT)
  override protected def markForRenderUpdate() {
    super.markForRenderUpdate()
    currentGui.foreach(_.recompileDisplayLists())
  }

  // ----------------------------------------------------------------------- //

  def compare(that: Screen) =
    if (x != that.x) x - that.x
    else if (y != that.y) y - that.y
    else z - that.z

  // ----------------------------------------------------------------------- //

  private def tryMerge() = {
    val (ox, oy, oz) = project(origin)
    def tryMergeTowards(dx: Int, dy: Int) = {
      val (nx, ny, nz) = unproject(ox + dx, oy + dy, oz)
      worldObj.getBlockTileEntity(nx, ny, nz) match {
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
            }
            true
          }
          else false // Cannot merge.
        case _ => false
      }
    }
    tryMergeTowards(width, 0) || tryMergeTowards(0, height) || tryMergeTowards(-1, 0) || tryMergeTowards(0, -1)
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