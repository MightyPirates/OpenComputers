package li.cil.oc.util

import com.naef.jnlua.{JavaFunction, LuaState}

object ExtendedLuaState {
  implicit def extendLuaState(state: LuaState) = new {
    def pushScalaFunction(f: (LuaState) => Int) = state.pushJavaFunction(new JavaFunction {
      override def invoke(state: LuaState) = f(state)
    })
  }
}