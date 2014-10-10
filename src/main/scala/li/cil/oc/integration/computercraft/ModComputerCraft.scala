package li.cil.oc.integration.computercraft

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.peripheral.IPeripheralProvider
import li.cil.oc.api.Driver
import li.cil.oc.common.tileentity.Switch
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraft.world.World

import scala.collection.mutable

object ModComputerCraft extends ModProxy {
  override def getMod = Mods.ComputerCraft

  override def initialize() {
    ComputerCraftAPI.registerPeripheralProvider(new IPeripheralProvider {
      override def getPeripheral(world: World, x: Int, y: Int, z: Int, side: Int) = world.getTileEntity(x, y, z) match {
        case switch: Switch => new SwitchPeripheral(switch)
        case _ => null
      }
    })

    Driver.add(DriverComputerCraftMedia)

    try {
      val driver: DriverPeripheral = new DriverPeripheral
      if (driver.isValid) {
        Driver.add(new ConverterLuaObject)
        Driver.add(driver)
      }
    }
    catch {
      case ignored: Throwable =>
    }
  }

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

}