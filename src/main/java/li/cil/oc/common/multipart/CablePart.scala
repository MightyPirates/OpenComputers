package li.cil.oc.common.multipart

import codechicken.lib.vec.{Vector3, Cuboid6}
import codechicken.multipart._
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network
import li.cil.oc.api.network.{Message, Node, Visibility}
import li.cil.oc.client.renderer.tileentity.CableRenderer
import li.cil.oc.common.block.Cable
import li.cil.oc.server.TickHandler
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import org.lwjgl.opengl.GL11
import scala.collection.convert.WrapAsJava

class CablePart extends TCuboidPart with TNormalOcclusion with network.Environment {
  val node = api.Network.newNode(this, Visibility.None).create()

  def getType = "oc:cable"

  override def doesTick = false

  override def getBounds = new Cuboid6(Cable.bounds(world, x, y, z))

  override def getOcclusionBoxes = WrapAsJava.asJavaIterable(Iterable(new Cuboid6(AxisAlignedBB.getBoundingBox(-0.125 + 0.5, -0.125 + 0.5, -0.125 + 0.5, 0.125 + 0.5, 0.125 + 0.5, 0.125 + 0.5))))

  override def getRenderBounds = new Cuboid6(Cable.bounds(world, x, y, z).offset(x, y, z))

  override def onChunkLoad() {
    super.onChunkLoad()
    TickHandler.schedule(this)
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    Option(node).foreach(_.remove)
  }

  override def onAdded() {
    super.onAdded()
    TickHandler.schedule(this)
  }

  override def onRemoved() {
    super.onRemoved()
    Option(node).foreach(_.remove)
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    node.load(nbt.getCompoundTag(Settings.namespace + "node"))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setNewCompoundTag(Settings.namespace + "node", node.save)
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
