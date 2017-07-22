package li.cil.oc.server.machine.luaj

import li.cil.oc.OpenComputers
import li.cil.oc.api.machine.Value
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.machine.ArgumentsImpl
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.Varargs

import scala.collection.convert.WrapAsScala._

class UserdataAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    val userdata = LuaValue.tableOf()

    userdata.set("apply", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value]).asInstanceOf[Value]
      val params = toSimpleJavaObjects(args, 2)
      owner.invoke(() => Registry.convert(Array(value.apply(machine, new ArgumentsImpl(params)))))
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
      owner.invoke(() => Registry.convert(value.call(machine, new ArgumentsImpl(params))))
    })

    userdata.set("dispose", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value]).asInstanceOf[Value]
      try value.dispose(machine) catch {
        case t: Throwable => OpenComputers.log.warn("Error in dispose method of userdata of type " + value.getClass.getName, t)
      }
      LuaValue.NIL
    })

    userdata.set("methods", (args: Varargs) => {
      val value = args.checkuserdata(1, classOf[Value])
      LuaValue.tableOf(machine.methods(value).map(entry => {
        val (name, annotation) = entry
        Seq(LuaValue.valueOf(name), LuaValue.valueOf(annotation.direct))
      }).flatten.toArray)
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
      owner.documentation(() => machine.methods(value)(method).doc)
    })

    lua.set("userdata", userdata)
  }
}
