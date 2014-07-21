package li.cil.oc.common.multipart

import codechicken.lib.lighting.LazyLightMatrix
import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.multipart._
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.{Message, Node, Visibility}
import li.cil.oc.api.{Items, network}
import li.cil.oc.client.renderer.block.BlockRenderer
import li.cil.oc.common.block.{Cable, Delegator}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api, common}
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB

import scala.collection.convert.WrapAsJava
import scala.collection.convert.WrapAsScala._

class CablePart(val original: Option[Node] = None) extends DelegatePart with TCuboidPart with TNormalOcclusion with network.Environment {
  val node = api.Network.newNode(this, Visibility.None).create()

  override def delegate = Delegator.subBlock(Items.get("cable").createItemStack(1)).get

  def getType = Settings.namespace + "cable"

  override def doesTick = false

  override def getBounds = new Cuboid6(Cable.bounds(world, x, y, z))

  override def getOcclusionBoxes = WrapAsJava.asJavaIterable(Iterable(new Cuboid6(AxisAlignedBB.getBoundingBox(-0.125 + 0.5, -0.125 + 0.5, -0.125 + 0.5, 0.125 + 0.5, 0.125 + 0.5, 0.125 + 0.5))))

  override def getRenderBounds = new Cuboid6(Cable.bounds(world, x, y, z).offset(x, y, z))

  override def invalidateConvertedTile() {
    super.invalidateConvertedTile()
    original.foreach(_.neighbors.foreach(_.connect(this.node)))
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

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    node.load(nbt.getCompoundTag(Settings.namespace + "node"))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    // Null check for Waila (and other mods that may call this client side).
    if (node != null) {
      nbt.setNewCompoundTag(Settings.namespace + "node", node.save)
    }
  }

  @SideOnly(Side.CLIENT)
  override def renderStatic(pos: Vector3, olm: LazyLightMatrix, pass: Int) {
    val (x, y, z) = (pos.x.toInt, pos.y.toInt, pos.z.toInt)
    val block = api.Items.get("cable").block()
    val metadata = world.getBlockMetadata(x, y, z)
    val renderer = Minecraft.getMinecraft.renderGlobal.globalRenderBlocks
    BlockRenderer.renderCable(Cable.neighbors(world, x, y, z), block, metadata, x, y, z, renderer)
  }

  override def onMessage(message: Message) {}

  override def onDisconnect(node: Node) {}

  override def onConnect(node: Node) {}
}
