package li.cil.oc.server.network

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import java.lang.reflect.{Modifier, Method, InvocationTargetException}
import li.cil.oc.OpenComputers
import li.cil.oc.api.network
import li.cil.oc.api.network.{Node => ImmutableNode, _}
import li.cil.oc.server.component.machine.Machine
import li.cil.oc.server.driver.CompoundBlockEnvironment
import li.cil.oc.server.network.Component.{PeripheralCallback, ComponentCallback}
import net.minecraft.nbt.NBTTagCompound
import scala.Some
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.{immutable, mutable}

trait Component extends network.Component with Node {
  val name: String

  def visibility = _visibility

  private lazy val callbacks = Component.callbacks(host)

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
    if (FMLCommonHandler.instance.getEffectiveSide == Side.SERVER) {
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
        case Some(environment) => callback(environment, context, new Component.VarArgs(Seq(arguments: _*)))
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

object Component {
  private val cache = mutable.Map.empty[Class[_], immutable.Map[String, Callback]]

  def callbacks(host: Environment) = host match {
    case multi: CompoundBlockEnvironment => analyze(host)
    case peripheral: ManagedPeripheral => analyze(host)
    case _ => cache.getOrElseUpdate(host.getClass, analyze(host))
  }

  private def analyze(host: Environment) = {
    val callbacks = mutable.Map.empty[String, Callback]
    val seeds = host match {
      case multi: CompoundBlockEnvironment => multi.environments.map {
        case (_, environment) => environment.getClass: Class[_]
      }
      case _ => Seq(host.getClass: Class[_])
    }
    for (seed <- seeds) {
      var c: Class[_] = seed
      while (c != classOf[Object]) {
        val ms = c.getDeclaredMethods

        ms.filter(_.isAnnotationPresent(classOf[network.Callback])).foreach(m =>
          if (m.getParameterTypes.size != 2 ||
            (m.getParameterTypes()(0) != classOf[Context] && m.getParameterTypes()(0) != classOf[RobotContext]) ||
            m.getParameterTypes()(1) != classOf[Arguments]) {
            OpenComputers.log.severe("Invalid use of Callback annotation on %s.%s: invalid argument types or count.".format(m.getDeclaringClass.getName, m.getName))
          }
          else if (m.getReturnType != classOf[Array[AnyRef]]) {
            OpenComputers.log.severe("Invalid use of Callback annotation on %s.%s: invalid return type.".format(m.getDeclaringClass.getName, m.getName))
          }
          else if (!Modifier.isPublic(m.getModifiers)) {
            OpenComputers.log.severe("Invalid use of Callback annotation on %s.%s: method must be public.".format(m.getDeclaringClass.getName, m.getName))
          }
          else {
            val a = m.getAnnotation[network.Callback](classOf[network.Callback])
            val name = if (a.value != null && a.value.trim != "") a.value else m.getName
            if (!callbacks.contains(name)) {
              callbacks += name -> new ComponentCallback(m, a.direct, a.limit, a.doc)
            }
          }
        )

        c = c.getSuperclass
      }
    }
    host match {
      case multi: CompoundBlockEnvironment => multi.environments.map {
        case (_, environment) => environment match {
          case peripheral: ManagedPeripheral => for (name <- peripheral.methods() if !callbacks.contains(name)) {
            callbacks += name -> new PeripheralCallback(name)
          }
          case _ =>
        }
      }
      case peripheral: ManagedPeripheral =>
        for (name <- peripheral.methods() if !callbacks.contains(name)) {
          callbacks += name -> new PeripheralCallback(name)
        }
      case _ =>
    }
    callbacks.toMap
  }

  // ----------------------------------------------------------------------- //

  abstract class Callback(val direct: Boolean, val limit: Int, val doc: String = "") {
    def apply(instance: Environment, context: Context, args: Arguments): Array[AnyRef]
  }

  class ComponentCallback(val method: Method, direct: Boolean, limit: Int, doc: String) extends Callback(direct, limit, doc) {
    override def apply(instance: Environment, context: Context, args: Arguments) = try {
      method.invoke(instance, context, args).asInstanceOf[Array[AnyRef]]
    } catch {
      case e: InvocationTargetException => throw e.getCause
    }
  }

  class PeripheralCallback(val name: String) extends Callback(true, 100) {
    override def apply(instance: Environment, context: Context, args: Arguments) =
      instance match {
        case peripheral: ManagedPeripheral => peripheral.invoke(name, context, args)
        case _ => throw new NoSuchMethodException()
      }
  }

  class VarArgs(val args: Seq[AnyRef]) extends Arguments {
    def iterator() = args.iterator

    def count() = args.length

    def checkAny(index: Int) = {
      checkIndex(index, "value")
      args(index) match {
        case Unit | None => null
        case arg => arg
      }
    }

    def checkBoolean(index: Int) = {
      checkIndex(index, "boolean")
      args(index) match {
        case value: java.lang.Boolean => value
        case value => throw typeError(index, value, "boolean")
      }
    }

    def checkDouble(index: Int) = {
      checkIndex(index, "number")
      args(index) match {
        case value: java.lang.Double => value
        case value => throw typeError(index, value, "number")
      }
    }

    def checkInteger(index: Int) = {
      checkIndex(index, "number")
      args(index) match {
        case value: java.lang.Double => value.intValue
        case value => throw typeError(index, value, "number")
      }
    }

    def checkString(index: Int) = {
      checkIndex(index, "string")
      args(index) match {
        case value: java.lang.String => value
        case value: Array[Byte] => new String(value, "UTF-8")
        case value => throw typeError(index, value, "string")
      }
    }

    def checkByteArray(index: Int) = {
      checkIndex(index, "string")
      args(index) match {
        case value: java.lang.String => value.getBytes("UTF-8")
        case value: Array[Byte] => value
        case value => throw typeError(index, value, "string")
      }
    }

    def checkTable(index: Int) = {
      checkIndex(index, "table")
      args(index) match {
        case value: java.util.Map[_, _] => value
        case value: Map[_, _] => value
        case value: mutable.Map[_, _] => value
        case value => throw typeError(index, value, "table")
      }
    }

    def isBoolean(index: Int) =
      index >= 0 && index < count && (args(index) match {
        case value: java.lang.Boolean => true
        case _ => false
      })

    def isDouble(index: Int) =
      index >= 0 && index < count && (args(index) match {
        case value: java.lang.Double => true
        case _ => false
      })

    def isInteger(index: Int) =
      index >= 0 && index < count && (args(index) match {
        case value: java.lang.Integer => true
        case value: java.lang.Double => true
        case _ => false
      })

    def isString(index: Int) =
      index >= 0 && index < count && (args(index) match {
        case value: java.lang.String => true
        case value: Array[Byte] => true
        case _ => false
      })

    def isByteArray(index: Int) =
      index >= 0 && index < count && (args(index) match {
        case value: java.lang.String => true
        case value: Array[Byte] => true
        case _ => false
      })

    def isTable(index: Int) =
      index >= 0 && index < count && (args(index) match {
        case value: java.util.Map[_, _] => true
        case value: Map[_, _] => true
        case value: mutable.Map[_, _] => true
        case _ => false
      })

    private def checkIndex(index: Int, name: String) =
      if (index < 0) throw new IndexOutOfBoundsException()
      else if (args.length <= index) throw new IllegalArgumentException(
        "bad arguments #%d (%s expected, got no value)".
          format(index + 1, name))

    private def typeError(index: Int, have: AnyRef, want: String) =
      new IllegalArgumentException(
        "bad argument #%d (%s expected, got %s)".
          format(index + 1, want, typeName(have)))

    private def typeName(value: AnyRef): String = value match {
      case null | Unit | None => "nil"
      case _: java.lang.Boolean => "boolean"
      case _: java.lang.Double => "double"
      case _: java.lang.String => "string"
      case _: Array[Byte] => "string"
      case value: java.util.Map[_, _] => "table"
      case value: Map[_, _] => "table"
      case value: mutable.Map[_, _] => "table"
      case _ => value.getClass.getSimpleName
    }
  }

}