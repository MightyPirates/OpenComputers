package li.cil.oc.util

import java.util

import com.naef.jnlua.{JavaFunction, LuaState, LuaType}
import li.cil.oc.api.machine.Value
import li.cil.oc.{OpenComputers, Settings}

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

    def pushValue(value: Any, memo: util.IdentityHashMap[Any, Int] = new util.IdentityHashMap()) {
      if (memo.containsKey(value)) {
        lua.pushValue(memo.get(value))
      }
      else {
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
          case value: Array[_] => pushList(value, value.zipWithIndex.iterator, memo)
          case value: Value if Settings.get.allowUserdata => lua.pushJavaObjectRaw(value)
          case value: Product => pushList(value, value.productIterator.zipWithIndex, memo)
          case value: Seq[_] => pushList(value, value.zipWithIndex.iterator, memo)
          case value: java.util.Map[_, _] => pushTable(value, value.toMap, memo)
          case value: Map[_, _] => pushTable(value, value, memo)
          case value: mutable.Map[_, _] => pushTable(value, value.toMap, memo)
          case _ =>
            OpenComputers.log.warn("Tried to push an unsupported value of type to Lua: " + value.getClass.getName + ".")
            lua.pushNil()
        }
      }
    }

    def pushList(obj: AnyRef, list: Iterator[(Any, Int)], memo: util.IdentityHashMap[Any, Int]) {
      lua.newTable()
      memo += obj -> lua.getTop
      var count = 0
      list.foreach {
        case (value, index) =>
          pushValue(value, memo)
          lua.rawSet(-2, index + 1)
          count = count + 1
      }
      lua.pushString("n")
      lua.pushInteger(count)
      lua.rawSet(-3)
    }

    def pushTable(obj: AnyRef, map: Map[_, _], memo: util.IdentityHashMap[Any, Int]) {
      lua.newTable(0, map.size)
      memo += obj -> lua.getTop
      for ((key: AnyRef, value: AnyRef) <- map) {
        if (key != null && key != Unit && !key.isInstanceOf[BoxedUnit]) {
          pushValue(key, memo)
          pushValue(value, memo)
          lua.setTable(-3)
        }
      }
    }

    def toSimpleJavaObject(index: Int): AnyRef = lua.`type`(index) match {
      case LuaType.BOOLEAN => Boolean.box(lua.toBoolean(index))
      case LuaType.NUMBER => Double.box(lua.toNumber(index))
      case LuaType.STRING => lua.toByteArray(index)
      case LuaType.TABLE => lua.toJavaObject(index, classOf[java.util.Map[_, _]])
      case LuaType.USERDATA => lua.toJavaObjectRaw(index)
      case _ => null
    }

    def toSimpleJavaObjects(start: Int) =
      for (index <- start to lua.getTop) yield toSimpleJavaObject(index)
  }

}