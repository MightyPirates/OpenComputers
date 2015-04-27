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
import net.minecraftforge.common.util.ForgeDirection

object ModVanilla extends ModProxy with RedstoneProvider {
  def getMod = Mods.Minecraft

  def initialize() {
    Driver.add(new DriverBeacon)
    Driver.add(new DriverBrewingStand)
    Driver.add(new DriverComparator)
    Driver.add(new DriverFurnace)
    Driver.add(new DriverMobSpawner)
    Driver.add(new DriverNoteBlock)
    Driver.add(new DriverRecordPlayer)

    if (Settings.get.enableInventoryDriver) {
      Driver.add(new DriverInventory)
    }
    if (Settings.get.enableTankDriver) {
      Driver.add(new DriverFluidHandler)
      Driver.add(new DriverFluidTank)
    }
    if (Settings.get.enableCommandBlockDriver) {
      Driver.add(new DriverCommandBlock)
    }

    Driver.add(ConverterFluidStack)
    Driver.add(ConverterFluidTankInfo)
    Driver.add(ConverterItemStack)
    Driver.add(ConverterNBT)
    Driver.add(ConverterWorld)
    Driver.add(ConverterWorldProvider)

    RecipeHandler.init()

    BundledRedstone.addProvider(this)
  }

  override def computeInput(pos: BlockPosition, side: ForgeDirection): Int = {
    val world = pos.world.get
    // See BlockRedstoneLogic.getInputStrength() for reference.
    math.max(world.getIndirectPowerLevelTo(pos, side),
      if (world.getBlock(pos) == Blocks.redstone_wire) world.getBlockMetadata(pos) else 0)
  }

  override def computeBundledInput(pos: BlockPosition, side: ForgeDirection): Array[Int] = null
}
