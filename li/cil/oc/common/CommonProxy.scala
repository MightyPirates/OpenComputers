package li.cil.oc.common

import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.registry.LanguageRegistry
import li.cil.oc.Blocks
import li.cil.oc.Config
import li.cil.oc.Items
import li.cil.oc.server.computer.Computer
import li.cil.oc.server.computer.Drivers

class CommonProxy {
  def preInit(e: FMLPreInitializationEvent): Unit = {

    val config = new net.minecraftforge.common.Configuration(e.getSuggestedConfigurationFile())

    Config.blockComputerId = config.getBlock("computer", Config.blockComputerId,
      "The block ID used for computers.").getInt(Config.blockComputerId)
    Config.blockMonitorId = config.getBlock("computer", Config.blockMonitorId,
      "The block ID used for monitors.").getInt(Config.blockMonitorId)
  }

  def init(e: FMLInitializationEvent): Unit = {
    Blocks.init()
    Items.init()

    // TODO Figure out how resource pack based localization works.
    LanguageRegistry.addName(Blocks.computer, "Computer")

    new Computer(null)
  }

  def postInit(e: FMLPostInitializationEvent): Unit = {
    // Lock the driver registry to avoid drivers being added after computers
    // may have already started up. This makes sure the driver API won't change
    // over the course of a game, since that could lead to weird effects.
    Drivers.locked = true
  }
}