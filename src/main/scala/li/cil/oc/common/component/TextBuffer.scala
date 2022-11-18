package li.cil.oc.common.component

import com.google.common.base.Strings
import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.font.TextBufferRenderData
import li.cil.oc.client.{ComponentTracker => ClientComponentTracker}
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common._
import li.cil.oc.common.item.data.NodeData
import li.cil.oc.common.component.traits.TextBufferProxy
import li.cil.oc.common.component.traits.VideoRamRasterizer
import li.cil.oc.server.component.Keyboard
import li.cil.oc.server.{ComponentTracker => ServerComponentTracker}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.PackedColor
import li.cil.oc.util.SideTracker
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Hand
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

class TextBuffer(val host: EnvironmentHost) extends AbstractManagedEnvironment with traits.TextBufferProxy with VideoRamRasterizer with DeviceInfo {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("screen").
    withConnector().
    create()

  private var maxResolution: (Int, Int) = Settings.screenResolutionsByTier(Tier.One)

  private var maxDepth = Settings.screenDepthsByTier(Tier.One)

  private var aspectRatio = (1.0, 1.0)

  private var powerConsumptionPerTick = Settings.get.screenCost

  private var precisionMode = false

  // For client side only.
  private var isRendering = true

  private var isDisplaying = true

  private var hasPower = true

  private var relativeLitArea = -1.0

  private var _pendingCommands: Option[PacketBuilder] = None

  private val syncInterval = 100

  private var syncCooldown = syncInterval

  private def pendingCommands = _pendingCommands.getOrElse {
    val pb = new CompressedPacketBuilder(PacketType.TextBufferMulti)
    pb.writeUTF(node.address)
    _pendingCommands = Some(pb)
    pb
  }

  var fullyLitCost: Double = computeFullyLitCost()

  // This computes the energy cost (per tick) to keep the screen running if
  // every single "pixel" is lit. This cost increases with higher tiers as
  // their maximum resolution (pixel density) increases. For a basic screen
  // this is simply the configured cost.
  def computeFullyLitCost(): Double = {
    val (w, h) = Settings.screenResolutionsByTier(0)
    val mw = getMaximumWidth
    val mh = getMaximumHeight
    powerConsumptionPerTick * (mw * mh) / (w * h)
  }

  val proxy: TextBuffer.Proxy =
    if (SideTracker.isClient) new TextBuffer.ClientProxy(this)
    else new TextBuffer.ServerProxy(this)

  val data = new util.TextBuffer(maxResolution, PackedColor.Depth.format(maxDepth))

  var viewport: (Int, Int) = data.size

  def markInitialized(): Unit = {
    syncCooldown = -1 // Stop polling for init state.
    relativeLitArea = -1 // Recompute lit area, avoid screens blanking out until something changes.
  }

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Display,
    DeviceAttribute.Description -> "Text buffer",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Text Screen V0",
    DeviceAttribute.Capacity -> (maxResolution._1 * maxResolution._2).toString,
    DeviceAttribute.Width -> Array("1", "4", "8").apply(maxDepth.ordinal())
  )

  override def getDeviceInfo: java.util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    super.update()
    if (isDisplaying && host.world.getGameTime % Settings.get.tickFrequency == 0) {
      if (relativeLitArea < 0) {
        // The relative lit area is the number of pixels that are not blank
        // versus the number of pixels in the *current* resolution. This is
        // scaled to multi-block screens, since we only compute this for the
        // origin.
        val w = getViewportWidth
        val h = getViewportHeight
        var acc = 0f
        for (y <- 0 until h) {
          val line = data.buffer(y)
          val colors = data.color(y)
          for (x <- 0 until w) {
            val char = line(x)
            val color = colors(x)
            val bg = PackedColor.unpackBackground(color, data.format)
            val fg = PackedColor.unpackForeground(color, data.format)
            acc += (if (char == ' ') if (bg == 0) 0 else 1
            else if (char == 0x2588) if (fg == 0) 0 else 1
            else if (fg == 0 && bg == 0) 0 else 1)
          }
        }
        relativeLitArea = acc / (w * h).toDouble
      }
      if (node != null) {
        val hadPower = hasPower
        val neededPower = relativeLitArea * fullyLitCost * Settings.get.tickFrequency
        hasPower = node.tryChangeBuffer(-neededPower)
        if (hasPower != hadPower) {
          ServerPacketSender.sendTextBufferPowerChange(node.address, isDisplaying && hasPower, host)
        }
      }
    }

    this.synchronized {
      _pendingCommands.foreach(_.sendToPlayersNearHost(host, Option(Settings.get.maxWirelessRange(Tier.Two) * Settings.get.maxWirelessRange(Tier.Two))))
      _pendingCommands = None
    }

    if (SideTracker.isClient && syncCooldown > 0) {
      syncCooldown -= 1
      if (syncCooldown == 0) {
        syncCooldown = syncInterval
        ClientPacketSender.sendTextBufferInit(proxy.nodeAddress)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():boolean -- Returns whether the screen is currently on.""")
  def isOn(computer: Context, args: Arguments): Array[AnyRef] = result(isDisplaying)

  @Callback(doc = """function():boolean -- Turns the screen on. Returns true if it was off.""")
  def turnOn(computer: Context, args: Arguments): Array[AnyRef] = {
    val oldPowerState = isDisplaying
    setPowerState(value = true)
    result(isDisplaying != oldPowerState, isDisplaying)
  }

  @Callback(doc = """function():boolean -- Turns off the screen. Returns true if it was on.""")
  def turnOff(computer: Context, args: Arguments): Array[AnyRef] = {
    val oldPowerState = isDisplaying
    setPowerState(value = false)
    result(isDisplaying != oldPowerState, isDisplaying)
  }

  @Callback(direct = true, doc = """function():number, number -- The aspect ratio of the screen. For multi-block screens this is the number of blocks, horizontal and vertical.""")
  def getAspectRatio(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    result(aspectRatio._1, aspectRatio._2)
  }

  @Callback(doc = """function():table -- The list of keyboards attached to the screen.""")
  def getKeyboards(context: Context, args: Arguments): Array[AnyRef] = {
    context.pause(0.25)
    host match {
      case screen: tileentity.Screen =>
        Array(screen.screens.map(_.node).flatMap(_.neighbors.filter(_.host.isInstanceOf[Keyboard]).map(_.address)).toArray)
      case _ =>
        Array(node.neighbors.filter(_.host.isInstanceOf[Keyboard]).map(_.address).toArray)
    }
  }

  @Callback(direct = true, doc = """function():boolean -- Returns whether the screen is in high precision mode (sub-pixel mouse event positions).""")
  def isPrecise(computer: Context, args: Arguments): Array[AnyRef] = result(precisionMode)

  @Callback(doc = """function(enabled:boolean):boolean -- Set whether to use high precision mode (sub-pixel mouse event positions).""")
  def setPrecise(computer: Context, args: Arguments): Array[AnyRef] = {
    // Available for T3 screens only... easiest way to check for us is to
    // base it off of the maximum color depth.
    if (maxDepth == Settings.screenDepthsByTier(Tier.Three)) {
      val oldValue = precisionMode
      precisionMode = args.checkBoolean(0)
      result(oldValue)
    }
    else result((), "unsupported operation")
  }

  // ----------------------------------------------------------------------- //

  override def setEnergyCostPerTick(value: Double) {
    powerConsumptionPerTick = value
    fullyLitCost = computeFullyLitCost()
  }

  override def getEnergyCostPerTick: Double = powerConsumptionPerTick

  override def setPowerState(value: Boolean) {
    if (isDisplaying != value) {
      isDisplaying = value
      if (isDisplaying) {
        val neededPower = fullyLitCost * Settings.get.tickFrequency
        hasPower = node.changeBuffer(-neededPower) == 0
      }
      ServerPacketSender.sendTextBufferPowerChange(node.address, isDisplaying && hasPower, host)
    }
  }

  override def getPowerState: Boolean = isDisplaying

  override def setMaximumResolution(width: Int, height: Int) {
    if (width < 1) throw new IllegalArgumentException("width must be larger or equal to one")
    if (height < 1) throw new IllegalArgumentException("height must be larger or equal to one")
    maxResolution = (width, height)
    fullyLitCost = computeFullyLitCost()
    proxy.onBufferMaxResolutionChange(width, width)
  }

  override def getMaximumWidth: Int = maxResolution._1

  override def getMaximumHeight: Int = maxResolution._2

  override def setAspectRatio(width: Double, height: Double): Unit = this.synchronized(this.aspectRatio = (width, height))

  override def getAspectRatio: Double = aspectRatio._1 / aspectRatio._2

  override def setResolution(w: Int, h: Int): Boolean = {
    val (mw, mh) = maxResolution
    if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
      throw new IllegalArgumentException("unsupported resolution")
    // Always send to clients, their state might be dirty.
    proxy.onBufferResolutionChange(w, h)
    // Force set viewport to new resolution. This is partially for
    // backwards compatibility, and partially to enforce a valid one.
    val sizeChanged = data.size = (w, h)
    val viewportChanged = setViewport(w, h)
    if (sizeChanged || viewportChanged) {
      if (!viewportChanged && node != null) {
        node.sendToReachable("computer.signal", "screen_resized", Int.box(w), Int.box(h))
      }
      true
    }
    else false
  }

  override def setViewport(w: Int, h: Int): Boolean = {
    val (mw, mh) = data.size
    if (w < 1 || h < 1 || w > mw || h > mh)
      throw new IllegalArgumentException("unsupported viewport resolution")
    // Always send to clients, their state might be dirty.
    proxy.onBufferViewportResolutionChange(w, h)
    val (cw, ch) = viewport
    if (w != cw || h != ch) {
      viewport = (w, h)
      if (node != null) {
        node.sendToReachable("computer.signal", "screen_resized", Int.box(w), Int.box(h))
      }
      true
    }
    else false
  }

  override def getViewportWidth: Int = viewport._1

  override def getViewportHeight: Int = viewport._2

  override def setMaximumColorDepth(depth: api.internal.TextBuffer.ColorDepth): Unit = maxDepth = depth

  override def getMaximumColorDepth: api.internal.TextBuffer.ColorDepth = maxDepth

  override def setColorDepth(depth: api.internal.TextBuffer.ColorDepth): Boolean = {
    val colorDepthChanged: Boolean = super.setColorDepth(depth)
    // Always send to clients, their state might be dirty.
    proxy.onBufferDepthChange(depth)
    colorDepthChanged
  }

  override def onBufferPaletteChange(index: Int): Unit =
    proxy.onBufferPaletteChange(index)

  override def onBufferColorChange(): Unit =
    proxy.onBufferColorChange()

  override def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit = {
    proxy.onBufferCopy(col, row, w, h, tx, ty)
  }

  override def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Char): Unit = {
    proxy.onBufferFill(col, row, w, h, c)
  }

  override def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean): Unit = {
    proxy.onBufferSet(col, row, s, vertical)
  }

  override def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, ram: component.GpuTextBuffer, fromCol: Int, fromRow: Int): Unit = {
    proxy.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow)
  }

  override def onBufferRamInit(ram: component.GpuTextBuffer): Unit = {
    proxy.onBufferRamInit(ram)
  }

  override def onBufferRamDestroy(ram: component.GpuTextBuffer): Unit = {
    proxy.onBufferRamDestroy(ram)
  }

  override def rawSetText(col: Int, row: Int, text: Array[Array[Char]]): Unit = {
    super.rawSetText(col, row, text)
    proxy.onBufferRawSetText(col, row, text)
  }

  override def rawSetBackground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
    super.rawSetBackground(col, row, color)
    // Better for bandwidth to send packed shorts here. Would need a special case for handling on client,
    // though, so let's be wasteful for once...
    proxy.onBufferRawSetBackground(col, row, color)
  }

  override def rawSetForeground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
    super.rawSetForeground(col, row, color)
    // Better for bandwidth to send packed shorts here. Would need a special case for handling on client,
    // though, so let's be wasteful for once...
    proxy.onBufferRawSetForeground(col, row, color)
  }

  @OnlyIn(Dist.CLIENT)
  override def renderText(stack: MatrixStack): Boolean = relativeLitArea != 0 && proxy.render(stack)

  @OnlyIn(Dist.CLIENT)
  override def renderWidth: Int = TextBufferRenderCache.renderer.charRenderWidth * getViewportWidth

  @OnlyIn(Dist.CLIENT)
  override def renderHeight: Int = TextBufferRenderCache.renderer.charRenderHeight * getViewportHeight

  @OnlyIn(Dist.CLIENT)
  override def setRenderingEnabled(enabled: Boolean): Unit = isRendering = enabled

  @OnlyIn(Dist.CLIENT)
  override def isRenderingEnabled: Boolean = isRendering

  override def keyDown(character: Char, code: Int, player: PlayerEntity): Unit =
    proxy.keyDown(character, code, player)

  override def keyUp(character: Char, code: Int, player: PlayerEntity): Unit =
    proxy.keyUp(character, code, player)

  override def textInput(codePt: Int, player: PlayerEntity): Unit =
    proxy.textInput(codePt, player)

  override def clipboard(value: String, player: PlayerEntity): Unit =
    proxy.clipboard(value, player)

  override def mouseDown(x: Double, y: Double, button: Int, player: PlayerEntity): Unit =
    proxy.mouseDown(x, y, button, player)

  override def mouseDrag(x: Double, y: Double, button: Int, player: PlayerEntity): Unit =
    proxy.mouseDrag(x, y, button, player)

  override def mouseUp(x: Double, y: Double, button: Int, player: PlayerEntity): Unit =
    proxy.mouseUp(x, y, button, player)

  override def mouseScroll(x: Double, y: Double, delta: Int, player: PlayerEntity): Unit =
    proxy.mouseScroll(x, y, delta, player)

  def copyToAnalyzer(line: Int, player: PlayerEntity): Unit = {
    proxy.copyToAnalyzer(line, player)
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      ServerComponentTracker.add(host.world, node.address, this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      ServerComponentTracker.remove(host.world, this)
    }
  }

  // ----------------------------------------------------------------------- //

  private def bufferPath = node.address + "_buffer"
  private final val IsOnTag = Settings.namespace + "isOn"
  private final val HasPowerTag = Settings.namespace + "hasPower"
  private final val MaxWidthTag = Settings.namespace + "maxWidth"
  private final val MaxHeightTag = Settings.namespace + "maxHeight"
  private final val PreciseTag = Settings.namespace + "precise"
  private final val ViewportWidthTag = Settings.namespace + "viewportWidth"
  private final val ViewportHeightTag = Settings.namespace + "viewportHeight"

  override def loadData(nbt: CompoundNBT) {
    super.loadData(nbt)
    if (SideTracker.isClient) {
      if (!Strings.isNullOrEmpty(proxy.nodeAddress)) return // Only load once.
      proxy.nodeAddress = nbt.getCompound(NodeData.NodeTag).getString(NodeData.AddressTag)
      TextBuffer.registerClientBuffer(this)
    }
    else {
      if (nbt.contains(NodeData.BufferTag)) {
        data.loadData(nbt.getCompound(NodeData.BufferTag))
      }
      else if (!Strings.isNullOrEmpty(node.address)) {
        data.loadData(SaveHandler.loadNBT(nbt, bufferPath))
      }
    }

    if (nbt.contains(IsOnTag)) {
      isDisplaying = nbt.getBoolean(IsOnTag)
    }
    if (nbt.contains(HasPowerTag)) {
      hasPower = nbt.getBoolean(HasPowerTag)
    }
    if (nbt.contains(MaxWidthTag) && nbt.contains(MaxHeightTag)) {
      val maxWidth = nbt.getInt(MaxWidthTag)
      val maxHeight = nbt.getInt(MaxHeightTag)
      maxResolution = (maxWidth, maxHeight)
    }
    precisionMode = nbt.getBoolean(PreciseTag)

    if (nbt.contains(ViewportWidthTag)) {
      val vpw = nbt.getInt(ViewportWidthTag)
      val vph = nbt.getInt(ViewportHeightTag)
      viewport = (vpw min data.width max 1, vph min data.height max 1)
    } else {
      viewport = data.size
    }
  }

  // Null check for Waila (and other mods that may call this client side).
  override def saveData(nbt: CompoundNBT): Unit = if (node != null) {
    super.saveData(nbt)
    // Happy thread synchronization hack! Here's the problem: GPUs allow direct
    // calls for modifying screens to give a more responsive experience. This
    // causes the following problem: when saving, if the screen is saved first,
    // then the executor runs in parallel and changes the screen *before* the
    // server thread begins saving that computer, the saved computer will think
    // it changed the screen, although the saved screen wasn't. To avoid that we
    // wait for all computers the screen is connected to to finish their current
    // execution and pausing them (which will make them resume in the next tick
    // when their update() runs).
    if (node.network != null) {
      for (node <- node.network.nodes) node.host match {
        case computer: tileentity.traits.Computer if !computer.machine.isPaused =>
          computer.machine.pause(0.1)
        case _ =>
      }
    }

    SaveHandler.scheduleSave(host, nbt, bufferPath, data.saveData _)
    nbt.putBoolean(IsOnTag, isDisplaying)
    nbt.putBoolean(HasPowerTag, hasPower)
    nbt.putInt(MaxWidthTag, maxResolution._1)
    nbt.putInt(MaxHeightTag, maxResolution._2)
    nbt.putBoolean(PreciseTag, precisionMode)
    nbt.putInt(ViewportWidthTag, viewport._1)
    nbt.putInt(ViewportHeightTag, viewport._2)
  }
}

object TextBuffer {
  var clientBuffers = mutable.ListBuffer.empty[TextBuffer]

  @SubscribeEvent
  def onChunkUnloaded(e: ChunkEvent.Unload) {
    val chunk = e.getChunk
    clientBuffers = clientBuffers.filter(t => {
      val blockPos = BlockPosition(t.host)
      val chunkPos = chunk.getPos
      val keep = t.host.world != e.getWorld || ((blockPos.x >> 4) != chunkPos.x || (blockPos.z >> 4) != chunkPos.z)
      if (!keep) {
        ClientComponentTracker.remove(t.host.world, t)
      }
      keep
    })
  }

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    clientBuffers = clientBuffers.filter(t => {
      val keep = t.host.world != e.getWorld
      if (!keep) {
        ClientComponentTracker.remove(t.host.world, t)
      }
      keep
    })
  }

  def registerClientBuffer(t: TextBuffer) {
    ClientPacketSender.sendTextBufferInit(t.proxy.nodeAddress)
    ClientComponentTracker.add(t.host.world, t.proxy.nodeAddress, t)
    clientBuffers += t
  }

  abstract class Proxy {
    def owner: TextBuffer

    var dirty = false

    var nodeAddress = ""

    def setChanged() {
      dirty = true
    }

    @OnlyIn(Dist.CLIENT)
    def render(stack: MatrixStack) = false

    def onBufferColorChange(): Unit

    def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
      owner.relativeLitArea = -1
    }

    def onBufferDepthChange(depth: api.internal.TextBuffer.ColorDepth): Unit

    def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Char) {
      owner.relativeLitArea = -1
    }

    def onBufferPaletteChange(index: Int): Unit

    def onBufferResolutionChange(w: Int, h: Int) {
      owner.relativeLitArea = -1
    }

    def onBufferViewportResolutionChange(w: Int, h: Int) {
      owner.relativeLitArea = -1
    }

    def onBufferMaxResolutionChange(w: Int, h: Int) {
    }

    def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean) {
      owner.relativeLitArea = -1
    }

    def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, ram: component.GpuTextBuffer, fromCol: Int, fromRow: Int): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferRamInit(ram: component.GpuTextBuffer): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferRamDestroy(ram: component.GpuTextBuffer): Unit = {
      owner.relativeLitArea = -1
    }

    def onBufferRawSetText(col: Int, row: Int, text: Array[Array[Char]]) {
      owner.relativeLitArea = -1
    }

    def onBufferRawSetBackground(col: Int, row: Int, color: Array[Array[Int]]) {
      owner.relativeLitArea = -1
    }

    def onBufferRawSetForeground(col: Int, row: Int, color: Array[Array[Int]]) {
      owner.relativeLitArea = -1
    }

    def keyDown(character: Char, code: Int, player: PlayerEntity): Unit

    def keyUp(character: Char, code: Int, player: PlayerEntity): Unit

    def textInput(codePt: Int, player: PlayerEntity): Unit

    def clipboard(value: String, player: PlayerEntity): Unit

    def mouseDown(x: Double, y: Double, button: Int, player: PlayerEntity): Unit

    def mouseDrag(x: Double, y: Double, button: Int, player: PlayerEntity): Unit

    def mouseUp(x: Double, y: Double, button: Int, player: PlayerEntity): Unit

    def mouseScroll(x: Double, y: Double, delta: Int, player: PlayerEntity): Unit

    def copyToAnalyzer(line: Int, player: PlayerEntity): Unit
  }

  class ClientProxy(val owner: TextBuffer) extends Proxy {
    val renderer = new TextBufferRenderData {
      override def dirty = ClientProxy.this.dirty

      override def dirty_=(value: Boolean) = ClientProxy.this.dirty = value

      override def data = owner.data

      override def viewport: (Int, Int) = owner.viewport
    }

    @OnlyIn(Dist.CLIENT)
    override def render(stack: MatrixStack) = {
      val wasDirty = dirty
      TextBufferRenderCache.render(stack, renderer)
      wasDirty
    }

    override def onBufferColorChange() {
      setChanged()
    }

    override def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
      super.onBufferCopy(col, row, w, h, tx, ty)
      setChanged()
    }

    override def onBufferDepthChange(depth: api.internal.TextBuffer.ColorDepth) {
      setChanged()
    }

    override def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Char) {
      super.onBufferFill(col, row, w, h, c)
      setChanged()
    }

    override def onBufferPaletteChange(index: Int) {
      setChanged()
    }

    override def onBufferResolutionChange(w: Int, h: Int) {
      super.onBufferResolutionChange(w, h)
      setChanged()
    }

    override def onBufferViewportResolutionChange(w: Int, h: Int) {
      super.onBufferViewportResolutionChange(w, h)
      setChanged()
    }

    override def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean) {
      super.onBufferSet(col, row, s, vertical)
      setChanged()
    }

    override def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, ram: component.GpuTextBuffer, fromCol: Int, fromRow: Int): Unit = {
      super.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow)
      setChanged()
    }

    override def onBufferRamInit(ram: component.GpuTextBuffer): Unit = {
      super.onBufferRamInit(ram)
    }

    override def onBufferRamDestroy(ram: component.GpuTextBuffer): Unit = {
      super.onBufferRamDestroy(ram)
    }

    override def keyDown(character: Char, code: Int, player: PlayerEntity) {
      debug(s"{type = keyDown, char = $character, code = $code}")
      ClientPacketSender.sendKeyDown(nodeAddress, character, code)
    }

    override def keyUp(character: Char, code: Int, player: PlayerEntity) {
      debug(s"{type = keyUp, char = $character, code = $code}")
      ClientPacketSender.sendKeyUp(nodeAddress, character, code)
    }

    override def textInput(codePt: Int, player: PlayerEntity) {
      debug(s"{type = textInput, codePt = $codePt}")
      ClientPacketSender.sendTextInput(nodeAddress, codePt)
    }

    override def clipboard(value: String, player: PlayerEntity) {
      debug(s"{type = clipboard}")
      ClientPacketSender.sendClipboard(nodeAddress, value)
    }

    override def mouseDown(x: Double, y: Double, button: Int, player: PlayerEntity) {
      debug(s"{type = mouseDown, x = $x, y = $y, button = $button}")
      ClientPacketSender.sendMouseClick(nodeAddress, x, y, drag = false, button)
    }

    override def mouseDrag(x: Double, y: Double, button: Int, player: PlayerEntity) {
      debug(s"{type = mouseDrag, x = $x, y = $y, button = $button}")
      ClientPacketSender.sendMouseClick(nodeAddress, x, y, drag = true, button)
    }

    override def mouseUp(x: Double, y: Double, button: Int, player: PlayerEntity) {
      debug(s"{type = mouseUp, x = $x, y = $y, button = $button}")
      ClientPacketSender.sendMouseUp(nodeAddress, x, y, button)
    }

    override def mouseScroll(x: Double, y: Double, delta: Int, player: PlayerEntity) {
      debug(s"{type = mouseScroll, x = $x, y = $y, delta = $delta}")
      ClientPacketSender.sendMouseScroll(nodeAddress, x, y, delta)
    }

    override def copyToAnalyzer(line: Int, player: PlayerEntity): Unit = {
      ClientPacketSender.sendCopyToAnalyzer(nodeAddress, line)
    }

    private lazy val Debugger = api.Items.get(Constants.ItemName.Debugger)

    private def debug(message: String) {
      if (Minecraft.getInstance != null && Minecraft.getInstance.player != null && api.Items.get(Minecraft.getInstance.player.getItemInHand(Hand.MAIN_HAND)) == Debugger) {
        OpenComputers.log.info(s"[NETWORK DEBUGGER] Sending packet to node $nodeAddress: " + message)
      }
    }
  }

  class ServerProxy(val owner: TextBuffer) extends Proxy {
    override def onBufferColorChange() {
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferColorChange(owner.pendingCommands, owner.data.foreground, owner.data.background))
    }

    override def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
      super.onBufferCopy(col, row, w, h, tx, ty)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferCopy(owner.pendingCommands, col, row, w, h, tx, ty))
    }

    override def onBufferDepthChange(depth: api.internal.TextBuffer.ColorDepth) {
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferDepthChange(owner.pendingCommands, depth))
    }

    override def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Char) {
      super.onBufferFill(col, row, w, h, c)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferFill(owner.pendingCommands, col, row, w, h, c))
    }

    override def onBufferPaletteChange(index: Int) {
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferPaletteChange(owner.pendingCommands, index, owner.getPaletteColor(index)))
    }

    override def onBufferResolutionChange(w: Int, h: Int) {
      super.onBufferResolutionChange(w, h)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferResolutionChange(owner.pendingCommands, w, h))
    }

    override def onBufferViewportResolutionChange(w: Int, h: Int) {
      super.onBufferViewportResolutionChange(w, h)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferViewportResolutionChange(owner.pendingCommands, w, h))
    }

    override def onBufferMaxResolutionChange(w: Int, h: Int) {
      if (owner.node.network != null) {
        super.onBufferMaxResolutionChange(w, h)
        owner.host.markChanged()
        owner.synchronized(ServerPacketSender.appendTextBufferMaxResolutionChange(owner.pendingCommands, w, h))
      }
    }

    override def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean) {
      super.onBufferSet(col, row, s, vertical)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferSet(owner.pendingCommands, col, row, s, vertical))
    }

    override def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, ram: component.GpuTextBuffer, fromCol: Int, fromRow: Int): Unit = {
      super.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferBitBlt(owner.pendingCommands, col, row, w, h, ram.owner, ram.id, fromCol, fromRow))
    }

    override def onBufferRamInit(ram: component.GpuTextBuffer): Unit = {
      super.onBufferRamInit(ram)
      owner.host.markChanged()
      val nbt = new CompoundNBT()
      ram.saveData(nbt)
      owner.synchronized(ServerPacketSender.appendTextBufferRamInit(owner.pendingCommands, ram.owner, ram.id, nbt))
    }

    override def onBufferRamDestroy(ram: component.GpuTextBuffer): Unit = {
      super.onBufferRamDestroy(ram)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferRamDestroy(owner.pendingCommands, ram.owner, ram.id))
    }

    override def onBufferRawSetText(col: Int, row: Int, text: Array[Array[Char]]) {
      super.onBufferRawSetText(col, row, text)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferRawSetText(owner.pendingCommands, col, row, text))
    }

    override def onBufferRawSetBackground(col: Int, row: Int, color: Array[Array[Int]]) {
      super.onBufferRawSetBackground(col, row, color)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferRawSetBackground(owner.pendingCommands, col, row, color))
    }

    override def onBufferRawSetForeground(col: Int, row: Int, color: Array[Array[Int]]) {
      super.onBufferRawSetForeground(col, row, color)
      owner.host.markChanged()
      owner.synchronized(ServerPacketSender.appendTextBufferRawSetForeground(owner.pendingCommands, col, row, color))
    }

    override def keyDown(character: Char, code: Int, player: PlayerEntity) {
      sendToKeyboards("keyboard.keyDown", player, Char.box(character), Int.box(code))
    }

    override def keyUp(character: Char, code: Int, player: PlayerEntity) {
      sendToKeyboards("keyboard.keyUp", player, Char.box(character), Int.box(code))
    }

    override def textInput(codePt: Int, player: PlayerEntity) {
      sendToKeyboards("keyboard.textInput", player, Int.box(codePt))
    }

    override def clipboard(value: String, player: PlayerEntity) {
      sendToKeyboards("keyboard.clipboard", player, value)
    }

    override def mouseDown(x: Double, y: Double, button: Int, player: PlayerEntity) {
      sendMouseEvent(player, "touch", x, y, button)
    }

    override def mouseDrag(x: Double, y: Double, button: Int, player: PlayerEntity) {
      sendMouseEvent(player, "drag", x, y, button)
    }

    override def mouseUp(x: Double, y: Double, button: Int, player: PlayerEntity) {
      sendMouseEvent(player, "drop", x, y, button)
    }

    override def mouseScroll(x: Double, y: Double, delta: Int, player: PlayerEntity) {
      sendMouseEvent(player, "scroll", x, y, delta)
    }

    override def copyToAnalyzer(line: Int, player: PlayerEntity): Unit = {
      val stack = player.getItemInHand(Hand.MAIN_HAND)
      if (!stack.isEmpty) {
        stack.removeTagKey(Settings.namespace + "clipboard")

        if (line >= 0 && line < owner.getViewportHeight) {
          val text = new String(owner.data.buffer(line)).trim
          if (!Strings.isNullOrEmpty(text)) {
            stack.getOrCreateTag.putString(Settings.namespace + "clipboard", text)
          }
        }
      }
    }

    private def sendMouseEvent(player: PlayerEntity, name: String, x: Double, y: Double, data: Int) = {
      val args = mutable.ArrayBuffer.empty[AnyRef]

      args += player
      args += name
      if (owner.precisionMode) {
        args += Double.box(x)
        args += Double.box(y)
      }
      else {
        args += Int.box(x.toInt + 1)
        args += Int.box(y.toInt + 1)
      }
      args += Int.box(data)
      if (Settings.get.inputUsername) {
        args += player.getName.getString
      }

      owner.node.sendToReachable("computer.checked_signal", args.toSeq: _*)
    }

    private def sendToKeyboards(name: String, values: AnyRef*) {
      owner.host match {
        case screen: tileentity.Screen =>
          screen.screens.foreach(_.node.sendToNeighbors(name, values: _*))
        case _ =>
          owner.node.sendToNeighbors(name, values: _*)
      }
    }
  }

}
