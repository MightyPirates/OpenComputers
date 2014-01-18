package li.cil.oc.util

import com.naef.jnlua
import java.io.InputStream
import org.luaj
import com.naef.jnlua.LuaState.GcAction
import com.naef.jnlua.LuaType

trait LuaState {
  def getTop: Int

  def setTop(n: Int)

  def pop(n: Int)

  def `type`(index: Int): jnlua.LuaType

  def isNil(index: Int): Boolean

  def isBoolean(index: Int): Boolean

  def isNumber(index: Int): Boolean

  def isString(index: Int): Boolean

  def isTable(index: Int): Boolean

  def isFunction(index: Int): Boolean

  def isThread(index: Int): Boolean

  def checkType(index: Int, what: jnlua.LuaType)

  def checkArg(index: Int, condition: Boolean, message: String)

  def checkInteger(index: Int): Int

  def checkString(index: Int): String

  def toBoolean(index: Int): Boolean

  def toNumber(index: Int): Double

  def toByteArray(index: Int): Array[Byte]

  def toString(index: Int): String

  def toMap(index: Int): java.util.Map[_, _]

  def insert(index: Int)

  def pushNil()

  def pushBoolean(value: Boolean)

  def pushInteger(value: Int)

  def pushNumber(value: Double)

  def pushByteArray(value: Array[Byte])

  def pushString(value: String)

  def pushValue(index: Int)

  def pushScalaFunction(f: LuaState => Int)

  def load(stream: InputStream, name: String)

  def call(index: Int, argCount: Int)

  def resume(index: Int, argCount: Int): Int

  def status(index: Int): Int

  def gc(what: com.naef.jnlua.LuaState.GcAction, arg: Int)

  def newTable(arrayCount: Int = 0, recordCount: Int = 0)

  def newThread()

  def getTable(index: Int)

  def setTable(index: Int)

  def setField(index: Int, key: String)

  def getField(index: Int, key: String)

  def rawSet(index: Int)

  def rawSet(index: Int, key: Int)

  def getGlobal(key: String)

  def setGlobal(key: String)

  def next(index: Int): Boolean

  def getFreeMemory: Int

  def getTotalMemory: Int

  def setTotalMemory(value: Int)

  def close()
}

object LuaState {
  implicit def wrap(lua: jnlua.LuaState): LuaState = new LuaState {
    def getTop = lua.getTop

    def setTop(n: Int) = lua.setTop(n)

    def pop(n: Int) = lua.pop(n)

    def `type`(index: Int) = lua.`type`(index)

    def isNil(index: Int) = lua.isNil(index)

    def isBoolean(index: Int) = lua.isBoolean(index)

    def isNumber(index: Int) = lua.isNumber(index)

    def isString(index: Int) = lua.isString(index)

    def isTable(index: Int) = lua.isTable(index)

    def isFunction(index: Int) = lua.isFunction(index)

    def isThread(index: Int) = lua.isThread(index)

    def checkType(index: Int, what: jnlua.LuaType) = lua.checkType(index, what)

    def checkArg(index: Int, condition: Boolean, message: String) = lua.checkArg(index, condition, message)

    def checkInteger(index: Int) = lua.checkInteger(index)

    def checkString(index: Int) = lua.checkString(index)

    def toBoolean(index: Int) = lua.toBoolean(index)

    def toNumber(index: Int) = lua.toNumber(index)

    def toByteArray(index: Int) = lua.toByteArray(index)

    def toString(index: Int) = lua.toString(index)

    def toMap(index: Int) = lua.toJavaObject(index, classOf[java.util.Map[_, _]])

    def insert(index: Int) = lua.insert(index)

    def pushNil() = lua.pushNil()

    def pushBoolean(value: Boolean) = lua.pushBoolean(value)

    def pushInteger(value: Int) = lua.pushInteger(value)

    def pushNumber(value: Double) = lua.pushNumber(value)

    def pushByteArray(value: Array[Byte]) = lua.pushByteArray(value)

    def pushString(value: String) = lua.pushString(value)

    def pushValue(index: Int) = lua.pushValue(index)

    def pushScalaFunction(f: (LuaState) => Int) = lua.pushJavaFunction(new jnlua.JavaFunction {
      def invoke(state: jnlua.LuaState) = f(LuaState.wrap(state))
    })

    def load(stream: InputStream, name: String) = lua.load(stream, name, "t")

    def call(index: Int, argCount: Int) = lua.call(index, argCount)

    def resume(index: Int, argCount: Int) = lua.resume(index, argCount)

    def status(index: Int) = lua.status(index)

    def gc(what: jnlua.LuaState.GcAction, arg: Int) = lua.gc(what, arg)

    def newTable(arrayCount: Int, recordCount: Int) = lua.newTable(arrayCount, recordCount)

    def newThread() = lua.newThread()

    def getTable(index: Int) = lua.getTable(index)

    def setTable(index: Int) = lua.setTable(index)

    def setField(index: Int, key: String) = lua.setField(index, key)

    def getField(index: Int, key: String) = lua.getField(index, key)

    def rawSet(index: Int) = lua.rawSet(index)

    def rawSet(index: Int, key: Int) = lua.rawSet(index, key)

    def getGlobal(key: String) = lua.getGlobal(key)

    def setGlobal(key: String) = lua.setGlobal(key)

    def next(index: Int) = lua.next(index)

    def getFreeMemory = lua.getFreeMemory

    def getTotalMemory = lua.getTotalMemory

    def setTotalMemory(value: Int) = lua.setTotalMemory(value)

    def close() = lua.close()
  }

  implicit def wrap(lua: luaj.vm2.Globals): LuaState = new LuaState {
    def getTop = ???

    def setTop(n: Int) = ???

    def pop(n: Int) = ???

    def `type`(index: Int) = ???

    def isNil(index: Int) = ???

    def isBoolean(index: Int) = ???

    def isNumber(index: Int) = ???

    def isString(index: Int) = ???

    def isTable(index: Int) = ???

    def isFunction(index: Int) = ???

    def isThread(index: Int) = ???

    def checkType(index: Int, what: LuaType) = ???

    def checkArg(index: Int, condition: Boolean, message: String) = ???

    def checkInteger(index: Int) = ???

    def checkString(index: Int) = ???

    def toBoolean(index: Int) = ???

    def toNumber(index: Int) = ???

    def toByteArray(index: Int) = ???

    def toString(index: Int) = ???

    def toMap(index: Int) = ???

    def insert(index: Int) = ???

    def pushNil() = ???

    def pushBoolean(value: Boolean) = ???

    def pushInteger(value: Int) = ???

    def pushNumber(value: Double) = ???

    def pushByteArray(value: Array[Byte]) = ???

    def pushString(value: String) = ???

    def pushValue(index: Int) = ???

    def pushScalaFunction(f: (LuaState) => Int) = ???

    def load(stream: InputStream, name: String) = ???

    def call(index: Int, argCount: Int) = ???

    def resume(index: Int, argCount: Int) = ???

    def status(index: Int) = ???

    def gc(what: GcAction, arg: Int) = ???

    def newTable(arrayCount: Int, recordCount: Int) = ???

    def newThread() = ???

    def getTable(index: Int) = ???

    def setTable(index: Int) = ???

    def setField(index: Int, key: String) = ???

    def getField(index: Int, key: String) = ???

    def rawSet(index: Int) = ???

    def rawSet(index: Int, key: Int) = ???

    def getGlobal(key: String) = ???

    def setGlobal(key: String) = ???

    def next(index: Int) = ???

    def getFreeMemory = ???

    def getTotalMemory = ???

    def setTotalMemory(value: Int) = ???

    def close() = ???
  }
}