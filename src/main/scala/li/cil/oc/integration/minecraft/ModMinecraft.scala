package li.cil.oc.integration.minecraft

import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.BlockRedstoneWire
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.MinecraftForge

object ModMinecraft extends ModProxy with RedstoneProvider {
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
    Driver.add(ConverterItemStack)
    Driver.add(ConverterNBT)
    Driver.add(ConverterWorld)
    Driver.add(ConverterWorldProvider)

    RecipeHandler.init()

    BundledRedstone.addProvider(this)

    MinecraftForge.EVENT_BUS.register(EventHandlerVanilla)
  }

  override def computeInput(pos: BlockPosition, side: EnumFacing): Int = {
    val world = pos.world.get
    math.max(world.computeRedstoneSignal(pos, side),
      if (world.getBlock(pos.offset(side)) == Blocks.REDSTONE_WIRE) world.getBlockMetadata(pos.offset(side)).getValue(BlockRedstoneWire.POWER).intValue() else 0)
  }

  override def computeBundledInput(pos: BlockPosition, side: EnumFacing): Array[Int] = null
}
