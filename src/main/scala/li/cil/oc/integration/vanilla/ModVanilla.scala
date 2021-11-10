package li.cil.oc.integration.vanilla

import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.init.Blocks
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection

object ModVanilla extends ModProxy with RedstoneProvider {
  def getMod = Mods.Minecraft

  def initialize() {
    Driver.add(DriverBeacon)
    Driver.add(DriverBrewingStand)
    Driver.add(DriverComparator)
    Driver.add(DriverFurnace)
    Driver.add(DriverMobSpawner)
    Driver.add(DriverNoteBlock)
    Driver.add(DriverRecordPlayer)

    Driver.add(DriverBeacon.Provider)
    Driver.add(DriverBrewingStand.Provider)
    Driver.add(DriverComparator.Provider)
    Driver.add(DriverFurnace.Provider)
    Driver.add(DriverMobSpawner.Provider)
    Driver.add(DriverNoteBlock.Provider)
    Driver.add(DriverRecordPlayer.Provider)

    if (Settings.get.enableInventoryDriver) {
      Driver.add(new DriverInventory)
    }
    if (Settings.get.enableTankDriver) {
      Driver.add(new DriverFluidHandler)
      Driver.add(new DriverFluidTank)
    }
    if (Settings.get.enableCommandBlockDriver) {
      Driver.add(DriverCommandBlock)
    }

    Driver.add(ConverterFluidStack)
    Driver.add(ConverterFluidTankInfo)
    Driver.add(ConverterFluidContainerItem)
    Driver.add(ConverterItemStack)
    Driver.add(ConverterNBT)
    Driver.add(ConverterWorld)
    Driver.add(ConverterWorldProvider)

    RecipeHandler.init()

    BundledRedstone.addProvider(this)

    MinecraftForge.EVENT_BUS.register(EventHandlerVanilla)
  }

  override def computeInput(pos: BlockPosition, side: ForgeDirection): Int = {
    val world = pos.world.get
    math.max(world.computeRedstoneSignal(pos, side),
      if (world.getBlock(pos.offset(side)) == Blocks.redstone_wire) world.getBlockMetadata(pos.offset(side)) else 0)
  }

  override def computeBundledInput(pos: BlockPosition, side: ForgeDirection): Array[Int] = null
}
