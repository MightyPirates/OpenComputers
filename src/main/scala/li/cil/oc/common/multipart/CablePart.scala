package li.cil.oc.common.multipart

import codechicken.lib.vec.{Cuboid6, Vector3}
import codechicken.multipart._
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.{Message, Node, Visibility}
import li.cil.oc.api.{Network, Items, network}
import li.cil.oc.client.renderer.tileentity.CableRenderer
import li.cil.oc.common.block.{Cable, Delegator}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api, common}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import org.lwjgl.opengl.GL11

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
    if (world != null && !world.isRemote) {
      common.EventHandler.schedule(() => tile)
    }
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
  override def renderDynamic(pos: Vector3, frame: Float, pass: Int) {
    super.renderDynamic(pos, frame, pass)
    GL11.glTranslated(pos.x, pos.y, pos.z)
    CableRenderer.renderCable(Cable.neighbors(world, x, y, z))
    GL11.glTranslated(-pos.x, -pos.y, -pos.z)
  }

  override def onMessage(message: Message) {}

  override def onDisconnect(node: Node) {}

  override def onConnect(node: Node) {}
}
