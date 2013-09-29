package li.cil.oc.util

import com.naef.jnlua.{JavaFunction, LuaState}

class ExtendedLuaState(val state: LuaState) {
  def pushScalaFunction(f: (LuaState) => Int) = state.pushJavaFunction(new ExtendedLuaState.ScalaFunction(f))
}

object ExtendedLuaState {

  implicit def extendLuaState(lua: LuaState) = new ExtendedLuaState(lua)

  private class ScalaFunction(val f: (LuaState) => Int) extends JavaFunction {
    override def invoke(state: LuaState) = f(state)
  }

}