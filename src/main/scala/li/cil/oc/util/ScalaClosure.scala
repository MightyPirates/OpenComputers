package li.cil.oc.util

import li.cil.oc.OpenComputers
import org.luaj.vm3.lib.VarArgFunction
import org.luaj.vm3.{LuaValue, Varargs}
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.math.ScalaNumber
import scala.runtime.BoxedUnit

class ScalaClosure(val f: (Varargs) => Varargs) extends VarArgFunction {
  override def invoke(args: Varargs) = f(args)
}

object ScalaClosure {
  implicit def wrapClosure(f: (Varargs) => LuaValue) = new ScalaClosure(args => f(args) match {
    case varargs: Varargs => varargs
    case LuaValue.NONE => LuaValue.NONE
    case result => LuaValue.varargsOf(Array(result))
  })

  implicit def wrapVarArgClosure(f: (Varargs) => Varargs) = new ScalaClosure(f)

  def toLuaValue(value: Any): LuaValue = {
    (value match {
      case number: ScalaNumber => number.underlying
      case reference: AnyRef => reference
      case null => null
      case primitive => primitive.asInstanceOf[AnyRef]
    }) match {
      case null | Unit | _: BoxedUnit => LuaValue.NIL
      case value: java.lang.Boolean => LuaValue.valueOf(value.booleanValue)
      case value: java.lang.Byte => LuaValue.valueOf(value.byteValue)
      case value: java.lang.Character => LuaValue.valueOf(String.valueOf(value))
      case value: java.lang.Short => LuaValue.valueOf(value.shortValue)
      case value: java.lang.Integer => LuaValue.valueOf(value.intValue)
      case value: java.lang.Long => LuaValue.valueOf(value.longValue)
      case value: java.lang.Float => LuaValue.valueOf(value.floatValue)
      case value: java.lang.Double => LuaValue.valueOf(value.doubleValue)
      case value: java.lang.String => LuaValue.valueOf(value)
      case value: Array[Byte] => LuaValue.valueOf(value)
      case value: Array[_] => toLuaList(value)
      case value: Product => toLuaList(value.productIterator.toIterable)
      case value: Seq[_] => toLuaList(value)
      case value: java.util.Map[_, _] => toLuaTable(value.toMap)
      case value: Map[_, _] => toLuaTable(value)
      case value: mutable.Map[_, _] => toLuaTable(value.toMap)
      case _ =>
        OpenComputers.log.warning("Tried to push an unsupported value of type to Lua: " + value.getClass.getName + ".")
        LuaValue.NIL
    }
  }

  def toLuaList(value: Iterable[Any]): LuaValue = {
    val table = LuaValue.listOf(value.map(toLuaValue).toArray)
    table.set("n", table.length())
    table
  }

  def toLuaTable(value: Map[_, _]): LuaValue = {
    LuaValue.tableOf(value.flatMap {
      case (k, v) => Seq(toLuaValue(k), toLuaValue(v))
    }.toArray)
  }

  def toSimpleJavaObject(value: LuaValue): AnyRef = value.`type`() match {
    case LuaValue.TBOOLEAN => Boolean.box(value.toboolean())
    case LuaValue.TNUMBER => Double.box(value.todouble())
    case LuaValue.TSTRING => value.tojstring()
    case LuaValue.TTABLE =>
      val table = value.checktable()
      table.keys.map(key => toSimpleJavaObject(key) -> toSimpleJavaObject(table.get(key))).toMap
    case _ => null
  }

  def toSimpleJavaObjects(args: Varargs, start: Int = 1) =
    for (index <- start to args.narg()) yield toSimpleJavaObject(args.arg(index))
}