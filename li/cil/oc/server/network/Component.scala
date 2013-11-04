package li.cil.oc.server.network

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import java.lang.reflect.InvocationTargetException
import li.cil.oc.api
import li.cil.oc.api.network.{LuaCallback, Arguments, Context, Visibility}
import li.cil.oc.server.component.Computer
import li.cil.oc.util.Persistable
import net.minecraft.nbt.NBTTagCompound
import scala.Some
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.{immutable, mutable}

trait Component extends api.network.Component with Persistable {
  val name: String

  def visibility = visibility_

  private lazy val luaCallbacks = Component.callbacks(host.getClass)

  private var visibility_ = Visibility.None

  def setVisibility(value: Visibility) = {
    if (value.ordinal() > reachability.ordinal()) {
      throw new IllegalArgumentException("Trying to set computer visibility to '" + value + "' on a '" + name +
        "' node with reachability '" + reachability + "'. It will be limited to the node's reachability.")
    }
    if (FMLCommonHandler.instance.getEffectiveSide == Side.SERVER) {
      if (network != null) visibility_ match {
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
      visibility_ = value
    }
  }

  def canBeSeenFrom(other: api.network.Node) = visibility match {
    case Visibility.None => false
    case Visibility.Network => canBeReachedFrom(other)
    case Visibility.Neighbors => isNeighborOf(other)
  }

  private def addTo(nodes: Iterable[api.network.Node]) = nodes.foreach(_.host match {
    case computer: Computer.Environment => computer.instance.addComponent(this)
    case _ =>
  })

  private def removeFrom(nodes: Iterable[api.network.Node]) = nodes.foreach(_.host match {
    case computer: Computer.Environment => computer.instance.removeComponent(this)
    case _ =>
  })

  // ----------------------------------------------------------------------- //

  def methods() = luaCallbacks.keySet

  def invoke(method: String, context: Context, arguments: AnyRef*) = {
    luaCallbacks.get(method) match {
      case Some((_, callback)) => callback(host, context, new Component.VarArgs(Seq(arguments: _*)))
      case _ => throw new NoSuchMethodException()
    }
  }

  def isAsynchronous(method: String) = luaCallbacks(method)._1

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("oc.component.visibility"))
      visibility_ = Visibility.values()(nbt.getInteger("oc.component.visibility"))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setInteger("oc.component.visibility", visibility_.ordinal())
  }
}

object Component {
  private val cache = mutable.Map.empty[Class[_], immutable.Map[String, (Boolean, (Object, Context, Arguments) => Array[Object])]]

  def callbacks(clazz: Class[_]) = cache.getOrElseUpdate(clazz, analyze(clazz))

  private def analyze(clazz: Class[_]) = {
    val callbacks = mutable.Map.empty[String, (Boolean, (Object, Context, Arguments) => Array[Object])]
    var c = clazz
    while (c != classOf[Object]) {
      val ms = c.getDeclaredMethods

      ms.filter(_.isAnnotationPresent(classOf[LuaCallback])).foreach(m =>
        if (m.getParameterTypes.size != 2 ||
          m.getParameterTypes()(0) != classOf[Context] ||
          m.getParameterTypes()(1) != classOf[Arguments]) {
          throw new IllegalArgumentException("Invalid use of LuaCallback annotation (invalid signature).")
        }
        else if (m.getReturnType != classOf[Array[Object]]) {
          throw new IllegalArgumentException("Invalid use of LuaCallback annotation (invalid signature).")
        }
        else {
          val a = m.getAnnotation[LuaCallback](classOf[LuaCallback])
          if (a.value == null || a.value == "") {
            throw new IllegalArgumentException("Invalid use of LuaCallback annotation (name must not be null or empty).")
          }
          else if (!callbacks.contains(a.value)) {
            callbacks += a.value ->(a.asynchronous(), (o: Object, c: Context, a: Arguments) => try {
              m.invoke(o, c, a).asInstanceOf[Array[Object]]
            } catch {
              case e: InvocationTargetException => throw e.getCause
            })
          }
        }
      )

      c = c.getSuperclass
    }
    callbacks.toMap
  }

  // ----------------------------------------------------------------------- //

  class VarArgs(val args: Seq[AnyRef]) extends Arguments {
    def iterator() = args.iterator

    def count() = args.length

    def checkAny(index: Int) = {
      checkIndex(index, "value")
      args(index)
    }

    def checkBoolean(index: Int): Boolean = {
      checkIndex(index, "boolean")
      args(index) match {
        case value: java.lang.Boolean => value
        case value => throw typeError(index, value, "boolean")
      }
    }

    def checkDouble(index: Int): Double = {
      checkIndex(index, "number")
      args(index) match {
        case value: java.lang.Double => value
        case value => throw typeError(index, value, "number")
      }
    }

    def checkInteger(index: Int): Int = {
      checkIndex(index, "number")
      args(index) match {
        case value: java.lang.Double => value.intValue
        case value => throw typeError(index, value, "number")
      }
    }

    def checkString(index: Int) =
      new String(checkByteArray(index), "UTF-8")

    def checkByteArray(index: Int): Array[Byte] = {
      checkIndex(index, "string")
      args(index) match {
        case value: Array[Byte] => value
        case value => throw typeError(index, value, "string")
      }
    }

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
      case null => "nil"
      case _: java.lang.Boolean => "boolean"
      case _: java.lang.Double => "double"
      case _: java.lang.String => "string"
      case _: Array[Byte] => "string"
      case _ => value.getClass.getSimpleName
    }
  }

}