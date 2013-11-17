package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.api.network.{Analyzable, Visibility}
import li.cil.oc.client.gui
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.component.Buffer
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.ForgeDirection
import scala.collection.mutable

class Screen(var tier: Int) extends Buffer.Environment with Rotatable with Analyzable with Ordered[Screen] {
  def this() = this(0)

  // ----------------------------------------------------------------------- //

  def node = buffer.node

  var currentGui: Option[gui.Screen] = None

  /**
   * Check for multi-block screen option in next update. We do this in the
   * update to avoid unnecessary checks on chunk unload.
   */
  var shouldCheckForMultiBlock = true

  var width, height = 1

  var origin = this

  val screens = mutable.Set(this)

  var hasPower = true

  // ----------------------------------------------------------------------- //

  def isOrigin = origin == this

  def localPosition = {
    val (x, y, _) = project(this)
    val (ox, oy, _) = project(origin)
    ((ox - x).abs, (oy - y).abs)
  }

  // ----------------------------------------------------------------------- //

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = origin

  def compare(that: Screen) =
    if (x != that.x) x - that.x
    else if (y != that.y) y - that.y
    else z - that.z

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      val hadPower = hasPower
      hasPower = buffer.node.changeBuffer(-Config.screenCost)
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
        val (x, y, z) = project(current)
        def tryQueue(dx: Int, dy: Int) {
          val (nx, ny, nz) = unproject(x + dx, y + dy, z)
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

  override def validate() {
    super.validate()
    if (isClient) {
      ClientPacketSender.sendRotatableStateRequest(this)
      ClientPacketSender.sendScreenBufferRequest(this)
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
    tier = nbt.getByte(Config.namespace + "tier")
    super.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    nbt.setByte(Config.namespace + "tier", tier.toByte)
    super.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //

  def checkMultiBlock() {
    shouldCheckForMultiBlock = true
    width = 1
    height = 1
    origin = this
    screens.clear()
    screens += this
  }

  private def tryMerge() = {
    val (x, y, z) = project(origin)
    def tryMergeTowards(dx: Int, dy: Int) = {
      val (nx, ny, nz) = unproject(x + dx, y + dy, z)
      worldObj.getBlockTileEntity(nx, ny, nz) match {
        case s: Screen if s.tier == tier && s.pitch == pitch && s.yaw == yaw && !screens.contains(s) =>
          val (sx, sy, _) = project(s.origin)
          val canMergeAlongX = sy == y && s.height == height && s.width + width <= Config.maxScreenWidth
          val canMergeAlongY = sx == x && s.width == width && s.height + height <= Config.maxScreenHeight
          if (canMergeAlongX || canMergeAlongY) {
            val (newOrigin) =
              if (canMergeAlongX) {
                if (sx < x) s.origin else origin
              }
              else {
                if (sy < y) s.origin else origin
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
    def dot(f: ForgeDirection, s: Screen) = f.offsetX * s.xCoord + f.offsetY * s.yCoord + f.offsetZ * s.zCoord
    (dot(toGlobal(ForgeDirection.EAST), t), dot(toGlobal(ForgeDirection.UP), t), dot(toGlobal(ForgeDirection.SOUTH), t))
  }

  private def unproject(x: Int, y: Int, z: Int) = {
    def dot(f: ForgeDirection) = f.offsetX * x + f.offsetY * y + f.offsetZ * z
    (dot(toLocal(ForgeDirection.EAST)), dot(toLocal(ForgeDirection.UP)), dot(toLocal(ForgeDirection.SOUTH)))
  }

  // ----------------------------------------------------------------------- //

  override def getRenderBoundingBox =
    if ((width == 1 && height == 1) || !isOrigin) super.getRenderBoundingBox
    else {
      val (sx, sy, sz) = unproject(width, height, 1)
      val ox = xCoord + (if (sx < 0) 1 else 0)
      val oy = yCoord + (if (sy < 0) 1 else 0)
      val oz = zCoord + (if (sz < 0) 1 else 0)
      val b = AxisAlignedBB.getAABBPool.getAABB(ox, oy, oz, ox + sx, oy + sy, oz + sz)
      b.setBounds(b.minX min b.maxX, b.minY min b.maxY, b.minZ min b.maxZ,
        b.minX max b.maxX, b.minY max b.maxY, b.minZ max b.maxZ)
      b
    }

  override def getMaxRenderDistanceSquared = if (isOrigin) super.getMaxRenderDistanceSquared else 0

  // ----------------------------------------------------------------------- //

  override def onRotationChanged() {
    super.onRotationChanged()
    screens.clone().foreach(_.checkMultiBlock())
  }

  override protected def markForRenderUpdate() {
    super.markForRenderUpdate()
    currentGui.foreach(_.recompileDisplayLists())
  }
}