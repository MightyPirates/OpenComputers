package li.cil.oc.common.multipart

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.lighting.LazyLightMatrix
import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.multipart._
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.{Message, Node, Visibility}
import li.cil.oc.api.{Items, network}
import li.cil.oc.common.block.{Cable, Delegator}
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api, client, common}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{AxisAlignedBB, MovingObjectPosition}

import scala.collection.convert.WrapAsJava
import scala.collection.convert.WrapAsScala._

class CablePart(val original: Option[tileentity.Cable] = None) extends DelegatePart with TCuboidPart with TNormalOcclusion with network.Environment {
  val node = api.Network.newNode(this, Visibility.None).create()

  private var _color = 0

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

  override def delegate = Delegator.subBlock(Items.get("cable").createItemStack(1)).get

  def getType = Settings.namespace + "cable"

  override def doesTick = false

  override def getBounds = new Cuboid6(Cable.bounds(world, x, y, z))

  override def getOcclusionBoxes = WrapAsJava.asJavaIterable(Iterable(new Cuboid6(AxisAlignedBB.getBoundingBox(-0.125 + 0.5, -0.125 + 0.5, -0.125 + 0.5, 0.125 + 0.5, 0.125 + 0.5, 0.125 + 0.5))))

  override def getRenderBounds = new Cuboid6(Cable.bounds(world, x, y, z).offset(x, y, z))

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
    common.EventHandler.schedule(() => tile)
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
  override def renderStatic(pos: Vector3, olm: LazyLightMatrix, pass: Int) {
    val (x, y, z) = (pos.x.toInt, pos.y.toInt, pos.z.toInt)
    val block = api.Items.get("cable").block()
    val renderer = Minecraft.getMinecraft.renderGlobal.globalRenderBlocks
    block match {
      case delegator: Delegator[_] =>
        delegator.colorMultiplierOverride = Some(_color)
        client.renderer.block.Cable.render(world, x, y, z, block, renderer)
        delegator.colorMultiplierOverride = None
      case _ =>
        client.renderer.block.Cable.render(world, x, y, z, block, renderer)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) {}

  override def onDisconnect(node: Node) {}

  override def onConnect(node: Node) {}
}
