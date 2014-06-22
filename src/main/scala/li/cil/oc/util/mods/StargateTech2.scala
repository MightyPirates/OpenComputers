package li.cil.oc.util.mods

import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import stargatetech2.api.bus.BusEvent.{AddToNetwork, RemoveFromNetwork}

object StargateTech2 {
  def addDevice(world: World, x: Int, y: Int, z: Int) = MinecraftForge.EVENT_BUS.post(new AddToNetwork(world, x, y, z))

  def removeDevice(world: World, x: Int, y: Int, z: Int) = MinecraftForge.EVENT_BUS.post(new RemoveFromNetwork(world, x, y, z))
}