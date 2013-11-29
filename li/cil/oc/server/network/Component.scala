package li.cil.oc.server.network

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import java.lang.reflect.{Method, InvocationTargetException}
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity
import li.cil.oc.util.Persistable
import net.minecraft.nbt.NBTTagCompound
import scala.Some
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.{immutable, mutable}

trait Component extends api.network.Component with Persistable {
  val name: String

  def visibility = _visibility

  private lazy val callbacks = Component.callbacks(host.getClass)

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

  def canBeSeenFrom(other: api.network.Node) = visibility match {
    case Visibility.None => false
    case Visibility.Network => canBeReachedFrom(other)
    case Visibility.Neighbors => isNeighborOf(other)
  }

  private def addTo(nodes: Iterable[api.network.Node]) = nodes.foreach(_.host match {
    case computer: tileentity.Computer => computer.computer.addComponent(this)
    case _ =>
  })

  private def removeFrom(nodes: Iterable[api.network.Node]) = nodes.foreach(_.host match {
    case computer: tileentity.Computer => computer.computer.removeComponent(this)
    case _ =>
  })

  // ----------------------------------------------------------------------- //

  def methods = callbacks.keySet

  def invoke(method: String, context: Context, arguments: AnyRef*) =
    callbacks.get(method) match {
      case Some(callback) => callback(host, context, new Component.VarArgs(Seq(arguments: _*)))
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

  def callbacks(clazz: Class[_]) = cache.getOrElseUpdate(clazz, analyze(clazz))

  private def analyze(clazz: Class[_]) = {
    val callbacks = mutable.Map.empty[String, Callback]
    var c = clazz
    while (c != classOf[Object]) {
      val ms = c.getDeclaredMethods

      ms.filter(_.isAnnotationPresent(classOf[LuaCallback])).foreach(m =>
        if (m.getParameterTypes.size != 2 ||
          (m.getParameterTypes()(0) != classOf[Context] && m.getParameterTypes()(0) != classOf[RobotContext]) ||
          m.getParameterTypes()(1) != classOf[Arguments]) {
          throw new IllegalArgumentException("Invalid use of LuaCallback annotation (invalid signature).")
        }
        else if (m.getReturnType != classOf[Array[AnyRef]]) {
          throw new IllegalArgumentException("Invalid use of LuaCallback annotation (invalid signature).")
        }
        else {
          val a = m.getAnnotation[LuaCallback](classOf[LuaCallback])
          if (a.value == null || a.value == "") {
            throw new IllegalArgumentException("Invalid use of LuaCallback annotation (name must not be null or empty).")
          }
          else if (!callbacks.contains(a.value)) {
            callbacks += a.value -> new Callback(m, a.direct, a.limit)
          }
        }
      )

      c = c.getSuperclass
    }
    callbacks.toMap
  }

  // ----------------------------------------------------------------------- //

  class Callback(val method: Method, val direct: Boolean, val limit: Int) {
    def apply(instance: AnyRef, context: Context, args: Arguments): Array[AnyRef] = try {
      method.invoke(instance, context, args).asInstanceOf[Array[AnyRef]]
    } catch {
      case e: InvocationTargetException => throw e.getCause
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
        case value: Array[Byte] => value
        case value => throw typeError(index, value, "string")
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
        case value: Array[Byte] => true
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
      case _ => value.getClass.getSimpleName
    }
  }

}