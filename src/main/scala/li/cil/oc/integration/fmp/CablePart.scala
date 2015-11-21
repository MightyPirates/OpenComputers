package li.cil.oc.integration.fmp

import codechicken.lib.data.MCDataInput
import codechicken.lib.data.MCDataOutput
import codechicken.lib.vec.Cuboid6
import codechicken.lib.vec.Vector3
import codechicken.microblock.ISidedHollowConnect
import codechicken.multipart._
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Items
import li.cil.oc.api.network
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.client
import li.cil.oc.common
import li.cil.oc.common.block.Cable
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MovingObjectPosition

import scala.collection.convert.WrapAsJava
import scala.collection.convert.WrapAsScala._

class CablePart(val original: Option[tileentity.Cable] = None) extends SimpleBlockPart with TCuboidPart with TSlottedPart with ISidedHollowConnect with TNormalOcclusion with network.Environment {
  val node = api.Network.newNode(this, Visibility.None).create()

  private var _color = Color.LightGray

  original.foreach(cable => _color = cable.color)

  // ----------------------------------------------------------------------- //

  def color = _color

  def color_=(value: Int) = if (value != _color) {
    _color = value
    onColorChanged()
  }

  protected def onColorChanged() {
    if (world != null && !world.isRemote) {
      sendDescUpdate()
      api.Network.joinOrCreateNetwork(tile)
    }
  }

  // ----------------------------------------------------------------------- //

  override def simpleBlock = Items.get(Constants.BlockName.Cable).block().asInstanceOf[Cable]

  def getType = Settings.namespace + Constants.BlockName.Cable

  override def getStrength(hit: MovingObjectPosition, player: EntityPlayer): Float = api.Items.get(Constants.BlockName.Cable).block().getBlockHardness(world, hit.blockX, hit.blockY, hit.blockZ)

  override def doesTick = false

  override def getBounds = new Cuboid6(Cable.bounds(world, x, y, z))

  override def getOcclusionBoxes = WrapAsJava.asJavaIterable(Iterable(new Cuboid6(AxisAlignedBB.getBoundingBox(-0.125 + 0.5, -0.125 + 0.5, -0.125 + 0.5, 0.125 + 0.5, 0.125 + 0.5, 0.125 + 0.5))))

  override def getRenderBounds = new Cuboid6(Cable.bounds(world, x, y, z).offset(x, y, z))

  override def getHollowSize(side: Int) = 4 // 4 pixels as this is width of cable.

  override def getSlotMask = 1 << 6 // 6 is center part.

  // ----------------------------------------------------------------------- //

  override def activate(player: EntityPlayer, hit: MovingObjectPosition, item: ItemStack) = {
    if (Color.isDye(player.getHeldItem)) {
      color = Color.dyeColor(player.getHeldItem)
      tile.markDirty()
      true
    }
    else super.activate(player, hit, item)
  }

  // ----------------------------------------------------------------------- //

  override def invalidateConvertedTile() {
    super.invalidateConvertedTile()
    original.foreach(_.node.neighbors.foreach(_.connect(this.node)))
  }

  override def onPartChanged(part: TMultiPart) {
    super.onPartChanged(part)
    api.Network.joinOrCreateNetwork(tile)
  }

  override def onWorldJoin() {
    super.onWorldJoin()
    common.EventHandler.scheduleFMP(() => tile)
  }

  override def onWorldSeparate() {
    super.onWorldSeparate()
    Option(node).foreach(_.remove)
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    node.load(nbt.getCompoundTag(Settings.namespace + "node"))
    if (nbt.hasKey(Settings.namespace + "renderColor")) {
      _color = nbt.getInteger(Settings.namespace + "renderColor")
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    // Null check for Waila (and other mods that may call this client side).
    if (node != null) {
      nbt.setNewCompoundTag(Settings.namespace + "node", node.save)
    }
    nbt.setInteger(Settings.namespace + "renderColor", _color)
  }

  override def readDesc(packet: MCDataInput) {
    super.readDesc(packet)
    _color = packet.readInt()
  }

  override def writeDesc(packet: MCDataOutput) {
    super.writeDesc(packet)
    packet.writeInt(_color)
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def renderStatic(pos: Vector3, pass: Int) = {
    val (x, y, z) = (pos.x.toInt, pos.y.toInt, pos.z.toInt)
    val renderer = RenderBlocks.getInstance
    renderer.blockAccess = world
    simpleBlock.colorMultiplierOverride = Some(_color)
    client.renderer.block.Cable.render(world, x, y, z, simpleBlock, renderer)
    simpleBlock.colorMultiplierOverride = None
    true
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) {}

  override def onDisconnect(node: Node) {}

  override def onConnect(node: Node) {}
}
