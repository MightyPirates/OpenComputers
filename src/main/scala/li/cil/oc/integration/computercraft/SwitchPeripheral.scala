package li.cil.oc.integration.computercraft

import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import li.cil.oc.common.tileentity.Switch

import scala.collection.mutable

class SwitchPeripheral(val switch: Switch) extends IPeripheral {
  override def getType = switch.getType

  override def attach(computer: IComputerAccess) {
    switch.computers += computer
    switch.openPorts += computer -> mutable.Set.empty
  }

  override def detach(computer: IComputerAccess) {
    switch.computers -= computer
    switch.openPorts -= computer
  }

  override def getMethodNames = switch.getMethodNames

  override def callMethod(computer: IComputerAccess, context: ILuaContext, method: Int, arguments: Array[AnyRef]) =
    switch.callMethod(computer, context, method, arguments)

  override def equals(other: IPeripheral) = other match {
    case peripheral: SwitchPeripheral => peripheral.switch == switch
    case _ => false
  }
}
