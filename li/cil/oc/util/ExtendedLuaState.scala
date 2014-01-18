package li.cil.oc.util

import com.naef.jnlua.{LuaType, JavaFunction, LuaState}
import li.cil.oc.OpenComputers
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.language.implicitConversions
import scala.math.ScalaNumber
import scala.runtime.BoxedUnit

object ExtendedLuaState {

  implicit def extendLuaState(state: LuaState) = new ExtendedLuaState(state)

  class ExtendedLuaState(val lua: LuaState) {
    def pushScalaFunction(f: (LuaState) => Int) = lua.pushJavaFunction(new JavaFunction {
      override def invoke(state: LuaState) = f(state)
    })

    def pushValue(value: Any) {
      (value match {
        case number: ScalaNumber => number.underlying
        case reference: AnyRef => reference
        case null => null
        case primitive => primitive.asInstanceOf[AnyRef]
      }) match {
        case null | Unit | _: BoxedUnit => lua.pushNil()
        case value: java.lang.Boolean => lua.pushBoolean(value.booleanValue)
        case value: java.lang.Byte => lua.pushNumber(value.byteValue)
        case value: java.lang.Character => lua.pushString(String.valueOf(value))
        case value: java.lang.Short => lua.pushNumber(value.shortValue)
        case value: java.lang.Integer => lua.pushNumber(value.intValue)
        case value: java.lang.Long => lua.pushNumber(value.longValue)
        case value: java.lang.Float => lua.pushNumber(value.floatValue)
        case value: java.lang.Double => lua.pushNumber(value.doubleValue)
        case value: java.lang.String => lua.pushString(value)
        case value: Array[Byte] => lua.pushByteArray(value)
        case value: Array[_] => pushList(value.zipWithIndex.iterator)
        case value: Product => pushList(value.productIterator.zipWithIndex)
        case value: Seq[_] => pushList(value.zipWithIndex.iterator)
        case value: java.util.Map[_, _] => pushTable(value.toMap)
        case value: Map[_, _] => pushTable(value)
        case value: mutable.Map[_, _] => pushTable(value.toMap)
        case _ =>
          OpenComputers.log.warning("Tried to push an unsupported value of type to Lua: " + value.getClass.getName + ".")
          lua.pushNil()
      }
    }

    def pushList(list: Iterator[(Any, Int)]) {
      lua.newTable()
      var count = 0
      list.foreach {
        case (value, index) =>
          pushValue(value)
          lua.rawSet(-2, index + 1)
          count = count + 1
      }
      lua.pushString("n")
      lua.pushInteger(count)
      lua.rawSet(-3)
    }

    def pushTable(map: Map[_, _]) {
      lua.newTable(0, map.size)
      for ((key: AnyRef, value: AnyRef) <- map) {
        if (key != null && key != Unit && !key.isInstanceOf[BoxedUnit]) {
          pushValue(key)
          pushValue(value)
          lua.setTable(-3)
        }
      }
    }

    def toSimpleJavaObject(index: Int): AnyRef = lua.`type`(index) match {
      case LuaType.BOOLEAN => Boolean.box(lua.toBoolean(index))
      case LuaType.NUMBER => Double.box(lua.toNumber(index))
      case LuaType.STRING => lua.toByteArray(index)
      case LuaType.TABLE => lua.toJavaObject(index, classOf[java.util.Map[_, _]])
      case _ => null
    }

    def toSimpleJavaObjects(start: Int) =
      for (index <- start to lua.getTop) yield toSimpleJavaObject(index)
  }

}