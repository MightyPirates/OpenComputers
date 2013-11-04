package li.cil.oc.common

import cpw.mods.fml.common.event._
import li.cil.oc._
import li.cil.oc.server.component.Computer
import li.cil.oc.server.driver
import li.cil.oc.server.fs
import li.cil.oc.server.network
import net.minecraftforge.common.MinecraftForge

class Proxy {
  def preInit(e: FMLPreInitializationEvent): Unit = {
    Config.load(e.getSuggestedConfigurationFile)

    api.Driver.instance = driver.Registry
    api.FileSystem.instance = fs.FileSystem
    api.Network.instance = network.Network
  }

  def init(e: FMLInitializationEvent): Unit = {
    Blocks.init()
    Items.init()

    api.Driver.add(driver.FileSystem)
    api.Driver.add(driver.GraphicsCard)
    api.Driver.add(driver.Memory)
    api.Driver.add(driver.NetworkCard)
    api.Driver.add(driver.RedstoneCard)
    api.Driver.add(driver.CommandBlock)
    api.Driver.add(driver.PowerSupply)
    api.Driver.add(driver.Peripheral)

    MinecraftForge.EVENT_BUS.register(Computer)
    MinecraftForge.EVENT_BUS.register(network.Network)
  }

  def postInit(e: FMLPostInitializationEvent): Unit = {
    // Lock the driver registry to avoid drivers being added after computers
    // may have already started up. This makes sure the driver API won't change
    // over the course of a game, since that could lead to weird effects.
    driver.Registry.locked = true
  }
}