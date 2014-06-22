package li.cil.oc.server.component.machine.luaj

import java.util.logging.Level

import li.cil.oc.OpenComputers
import li.cil.oc.api.machine.Value
import li.cil.oc.server.component.machine.LuaJLuaArchitecture
import li.cil.oc.server.network.{ArgumentsImpl, Callbacks}
import li.cil.oc.util.ScalaClosure._
import org.luaj.vm3.{LuaValue, Varargs}

class UserdataAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    val userdata = LuaValue.tableOf()

    userdata.set("apply", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value]).asInstanceOf[Value]
      val params = toSimpleJavaObjects(args, 2)
      owner.invoke(() => Array(value.apply(machine, new ArgumentsImpl(params))))
    })

    userdata.set("unapply", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value]).asInstanceOf[Value]
      val params = toSimpleJavaObjects(args, 2)
      owner.invoke(() => {
        value.unapply(machine, new ArgumentsImpl(params))
        null
      })
    })

    userdata.set("call", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value]).asInstanceOf[Value]
      val params = toSimpleJavaObjects(args, 2)
      owner.invoke(() => value.call(machine, new ArgumentsImpl(params)))
    })

    userdata.set("dispose", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value]).asInstanceOf[Value]
      try value.dispose(machine) catch {
        case t: Throwable => OpenComputers.log.log(Level.WARNING, "Error in dispose method of userdata of type " + value.getClass.getName, t)
      }
      LuaValue.NIL
    })

    userdata.set("methods", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value])
      LuaValue.tableOf(Callbacks(value).map(entry => Seq(LuaValue.valueOf(entry._1), LuaValue.valueOf(entry._2.direct))).flatten.toArray)
    })

    userdata.set("invoke", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value]).asInstanceOf[Value]
      val method = args.checkjstring(2)
      val params = toSimpleJavaObjects(args, 3)
      owner.invoke(() => machine.invoke(value, method, params.toArray))
    })

    userdata.set("doc", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value]).asInstanceOf[Value]
      val method = args.checkjstring(2)
      owner.documentation(() => machine.documentation(value, method))
    })

    lua.set("userdata", userdata)
  }
}
