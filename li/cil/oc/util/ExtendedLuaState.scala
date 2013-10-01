package li.cil.oc.util

import com.naef.jnlua.{JavaFunction, LuaState}
import scala.language.implicitConversions

object ExtendedLuaState {

  implicit def extendLuaState(state: LuaState) = new ExtendedLuaState(state)

  class ExtendedLuaState(state: LuaState) {
    def pushScalaFunction(f: (LuaState) => Int) = state.pushJavaFunction(new JavaFunction {
      override def invoke(state: LuaState) = f(state)
    })
  }

}