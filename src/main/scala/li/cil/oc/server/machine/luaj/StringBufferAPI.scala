package li.cil.oc.server.machine.luaj

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.item.MutableProcessor
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.machine.Value
import li.cil.oc.api.network.Connector
import li.cil.oc.server.machine.StringBufferValue
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.Varargs

class StringBufferAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize(): Unit = {
    val strbuf = LuaValue.tableOf()

    strbuf.set("new", (args: Varargs) => {
      def size = args.checkint(1)
      if (size < 0 || size > 2 * 1024 * 1024) {
        throw new IllegalArgumentException("size is out of range")
      }
      LuaValue.userdataOf(new StringBufferValue(size).asInstanceOf[Value])
    })

    lua.set("strbuf", strbuf)
  }
}
