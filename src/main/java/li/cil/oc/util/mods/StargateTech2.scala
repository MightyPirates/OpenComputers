package li.cil.oc.util.mods

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.versioning.{VersionParser, DefaultArtifactVersion}
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import stargatetech2.api.bus.BusEvent.{RemoveFromNetwork, AddToNetwork}

object StargateTech2 {
  def isAvailable = Loader.isModLoaded("StargateTech2") && (try {
    val mod = Loader.instance.getIndexedModList.get("StargateTech2")
    val have = new DefaultArtifactVersion(mod.getVersion)
    val want = VersionParser.parseRange("[0.6.0,)")

    want.containsVersion(have)
  } catch {
    case _: Throwable => false
  })
}

object StargateTech2API {
  def addDevice(world: World, x: Int, y: Int, z: Int) = MinecraftForge.EVENT_BUS.post(new AddToNetwork(world, x, y, z))

  def removeDevice(world: World, x: Int, y: Int, z: Int) = MinecraftForge.EVENT_BUS.post(new RemoveFromNetwork(world, x, y, z))
}