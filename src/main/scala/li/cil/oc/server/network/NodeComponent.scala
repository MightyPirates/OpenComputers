package li.cil.oc.server.network

import li.cil.oc.api.machine.Context
import li.cil.oc.api.network
import li.cil.oc.api.network._
import li.cil.oc.api.network.{Node => ImmutableNode}
import li.cil.oc.common.item.data.NodeData
import li.cil.oc.server.driver.CompoundBlockNodeContainer
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.machine.ArgumentsImpl
import li.cil.oc.server.machine.Callbacks
import li.cil.oc.server.machine.Callbacks.ComponentCallback
import li.cil.oc.server.machine.Callbacks.PeripheralCallback
import li.cil.oc.server.machine.Machine
import li.cil.oc.util.SideTracker
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

trait NodeComponent extends NodeComponent with Node {
  def getVisibility = _visibility

  private lazy val callbacks = Callbacks(getContainer)

  private lazy val hosts = getContainer match {
    case multi: CompoundBlockNodeContainer =>
      callbacks.map {
        case (method, callback) => callback match {
          case component: ComponentCallback =>
            multi.environments.find {
              case (_, environment) => environment.getClass == component.method.getDeclaringClass
            } match {
              case Some((_, environment)) => method -> Some(environment)
              case _ => method -> None
            }
          case peripheral: PeripheralCallback =>
            multi.environments.find {
              case (_, environment: ManagedPeripheral) => environment.methods.contains(peripheral.annotation.value)
              case _ => false
            } match {
              case Some((_, environment)) => method -> Some(environment)
              case _ => method -> None
            }
        }
      }
    case _ => callbacks.map {
      case (method, callback) => method -> Some(getContainer)
    }
  }

  private var _visibility = Visibility.NONE

  def setVisibility(value: Visibility) = {
    if (value.ordinal() > getReachability.ordinal()) {
      throw new IllegalArgumentException("Trying to set computer visibility to '" + value + "' on a '" + getName +
        "' node with reachability '" + getReachability + "'. It will be limited to the node's reachability.")
    }
    if (SideTracker.isServer) {
      if (getNetwork != null) _visibility match {
        case Visibility.NEIGHBORS => value match {
          case Visibility.NETWORK => addTo(getReachableNodes)
          case Visibility.NONE => removeFrom(getNeighbors)
          case _ =>
        }
        case Visibility.NETWORK => value match {
          case Visibility.NEIGHBORS =>
            val neighborSet = getNeighbors.toSet
            removeFrom(getReachableNodes.filterNot(neighborSet.contains))
          case Visibility.NONE => removeFrom(getReachableNodes)
          case _ =>
        }
        case Visibility.NONE => value match {
          case Visibility.NEIGHBORS => addTo(getNeighbors)
          case Visibility.NETWORK => addTo(getReachableNodes)
          case _ =>
        }
      }
      _visibility = value
    }
  }

  def canBeSeenFrom(other: ImmutableNode) = getVisibility match {
    case Visibility.NONE => false
    case Visibility.NETWORK => canBeReachedFrom(other)
    case Visibility.NEIGHBORS => isNeighborOf(other)
  }

  private def addTo(nodes: Iterable[ImmutableNode]) = nodes.foreach(_.getContainer match {
    case machine: Machine => machine.addComponent(this)
    case _ =>
  })

  private def removeFrom(nodes: Iterable[ImmutableNode]) = nodes.foreach(_.getContainer match {
    case machine: Machine => machine.removeComponent(this)
    case _ =>
  })

  // ----------------------------------------------------------------------- //

  override def getMethods = callbacks.keySet

  override def getAnnotation(method: String) =
    callbacks.get(method) match {
      case Some(callback) => callbacks(method).annotation
      case _ => throw new NoSuchMethodException()
    }

  override def invoke(method: String, context: Context, arguments: AnyRef*): Array[AnyRef] = {
    callbacks.get(method) match {
      case Some(callback) => hosts(method) match {
        case Some(environment) => Registry.convert(callback(environment, context, new ArgumentsImpl(Seq(arguments: _*))))
        case _ => throw new NoSuchMethodException()
      }
      case _ => throw new NoSuchMethodException()
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey(NodeData.VisibilityTag)) {
      _visibility = Visibility.values()(nbt.getInteger(NodeData.VisibilityTag))
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setInteger(NodeData.VisibilityTag, _visibility.ordinal())
  }

  override def toString = super.toString + s"@$getName"
}