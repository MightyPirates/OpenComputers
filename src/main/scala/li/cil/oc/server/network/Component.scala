package li.cil.oc.server.network

import li.cil.oc.api.network
import li.cil.oc.api.network.{Node => ImmutableNode, _}
import li.cil.oc.server.component.machine.Machine
import li.cil.oc.server.driver.{CompoundBlockEnvironment, Registry}
import li.cil.oc.server.network.Callbacks.{ComponentCallback, PeripheralCallback}
import li.cil.oc.util.SideTracker
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

trait Component extends network.Component with Node {
  val name: String

  def visibility = _visibility

  private lazy val callbacks = Callbacks(host)

  private lazy val hosts = host match {
    case multi: CompoundBlockEnvironment =>
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
              case (_, environment: ManagedPeripheral) => environment.methods.contains(peripheral.name)
              case _ => false
            } match {
              case Some((_, environment)) => method -> Some(environment)
              case _ => method -> None
            }
        }
      }
    case _ => callbacks.map {
      case (method, callback) => method -> Some(host)
    }
  }

  private var _visibility = Visibility.None

  def setVisibility(value: Visibility) = {
    if (value.ordinal() > reachability.ordinal()) {
      throw new IllegalArgumentException("Trying to set computer visibility to '" + value + "' on a '" + name +
        "' node with reachability '" + reachability + "'. It will be limited to the node's reachability.")
    }
    if (SideTracker.isServer) {
      if (network != null) _visibility match {
        case Visibility.Neighbors => value match {
          case Visibility.Network => addTo(reachableNodes)
          case Visibility.None => removeFrom(neighbors)
          case _ =>
        }
        case Visibility.Network => value match {
          case Visibility.Neighbors =>
            val neighborSet = neighbors.toSet
            removeFrom(reachableNodes.filterNot(neighborSet.contains))
          case Visibility.None => removeFrom(reachableNodes)
          case _ =>
        }
        case Visibility.None => value match {
          case Visibility.Neighbors => addTo(neighbors)
          case Visibility.Network => addTo(reachableNodes)
          case _ =>
        }
      }
      _visibility = value
    }
  }

  def canBeSeenFrom(other: ImmutableNode) = visibility match {
    case Visibility.None => false
    case Visibility.Network => canBeReachedFrom(other)
    case Visibility.Neighbors => isNeighborOf(other)
  }

  private def addTo(nodes: Iterable[ImmutableNode]) = nodes.foreach(_.host match {
    case machine: Machine => machine.addComponent(this)
    case _ =>
  })

  private def removeFrom(nodes: Iterable[ImmutableNode]) = nodes.foreach(_.host match {
    case machine: Machine => machine.removeComponent(this)
    case _ =>
  })

  // ----------------------------------------------------------------------- //

  def methods = callbacks.keySet

  def doc(name: String) = callbacks.get(name) match {
    case Some(callback) => callback.doc
    case _ => throw new NoSuchMethodException()
  }

  def invoke(method: String, context: Context, arguments: AnyRef*) =
    callbacks.get(method) match {
      case Some(callback) => hosts(method) match {
        case Some(environment) => Registry.convert(callback(environment, context, new ArgumentsImpl(Seq(arguments: _*))))
        case _ => throw new NoSuchMethodException()
      }
      case _ => throw new NoSuchMethodException()
    }

  def isDirect(method: String) =
    callbacks.get(method) match {
      case Some(callback) => callbacks(method).direct
      case _ => throw new NoSuchMethodException()
    }

  def limit(method: String) =
    callbacks.get(method) match {
      case Some(callback) => callbacks(method).limit
      case _ => throw new NoSuchMethodException()
    }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("visibility")) {
      _visibility = Visibility.values()(nbt.getInteger("visibility"))
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setInteger("visibility", _visibility.ordinal())
  }
}