package li.cil.oc.server.driver

import java.nio.charset.Charset

import com.google.common.hash.Hashing
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound

class CompoundBlockNodeContainer(val name: String, val environments: (String, NodeContainerItem)*) extends NodeContainerItem {
  // Block drivers with visibility < network usually won't make much sense,
  // but let's play it safe and use the least possible visibility based on
  // the drivers we encapsulate.
  val getNode = api.Network.newNode(this, (environments.filter(_._2.getNode != null).map(_._2.getNode.getReachability) ++ Seq(Visibility.NONE)).max).
    withComponent(name).
    create()

  val updatingEnvironments = environments.map(_._2).filter(_.canUpdate)

  // Force all wrapped components to be neighbor visible, since we as their
  // only neighbor will take care of all component-related interaction.
  for ((_, environment) <- environments) environment.getNode match {
    case component: ComponentNode => component.setVisibility(Visibility.NEIGHBORS)
    case _ =>
  }

  override def canUpdate = environments.exists(_._2.canUpdate)

  override def update() {
    for (environment <- updatingEnvironments) {
      environment.update()
    }
  }

  override def onMessage(message: Message) {}

  override def onConnect(node: Node) {
    if (node == this.getNode) {
      for ((_, environment) <- environments if environment.getNode != null) {
        node.connect(environment.getNode)
      }
    }
  }

  override def onDisconnect(node: Node) {
    if (node == this.getNode) {
      for ((_, environment) <- environments if environment.getNode != null) {
        environment.getNode.remove()
      }
    }
  }

  private final val TypeHashTag = "typeHash"

  override def load(nbt: NBTTagCompound) {
    // Ignore existing data if the underlying type is different.
    if (nbt.hasKey(TypeHashTag) && nbt.getLong(TypeHashTag) != typeHash) return
    getNode.load(nbt)
    for ((driver, environment) <- environments) {
      if (nbt.hasKey(driver)) {
        try {
          environment.load(nbt.getCompoundTag(driver))
        } catch {
          case e: Throwable => OpenComputers.log.warn(s"A block component of type '${environment.getClass.getName}' (provided by driver '$driver') threw an error while loading.", e)
        }
      }
    }
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setLong(TypeHashTag, typeHash)
    getNode.save(nbt)
    for ((driver, environment) <- environments) {
      try {
        nbt.setNewCompoundTag(driver, environment.save)
      } catch {
        case e: Throwable => OpenComputers.log.warn(s"A block component of type '${environment.getClass.getName}' (provided by driver '$driver') threw an error while saving.", e)
      }
    }
  }

  private def typeHash = {
    val hash = Hashing.sha256().newHasher()
    environments.map(_._2.getClass.getName).sorted.foreach(hash.putString(_, Charset.defaultCharset()))
    hash.hash().asLong()
  }
}