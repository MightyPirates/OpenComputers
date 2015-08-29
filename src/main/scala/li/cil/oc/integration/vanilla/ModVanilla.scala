package li.cil.oc.integration.vanilla

import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.{Crop, BundledRedstone}
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.integration.util.Crop.CropProvider
import li.cil.oc.server.component._
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block._
import net.minecraft.init.{Items, Blocks}
import net.minecraft.item.Item
import net.minecraftforge.common.util.ForgeDirection

object ModVanilla extends ModProxy with RedstoneProvider with CropProvider {
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
    Crop.addProvider(this)
  }

  override def computeInput(pos: BlockPosition, side: ForgeDirection): Int = {
    val world = pos.world.get
    math.max(world.computeRedstoneSignal(pos, side),
      if (world.getBlock(pos.offset(side)) == Blocks.redstone_wire) world.getBlockMetadata(pos.offset(side)) else 0)
  }

  override def computeBundledInput(pos: BlockPosition, side: ForgeDirection): Array[Int] = null

  override def getInformation(pos: BlockPosition): Array[AnyRef] = {
    val world = pos.world.get
    val target = world.getBlock(pos.x, pos.y, pos.z)
    target match {
      case crop: BlockBush => {
        val meta = world.getBlockMetadata(pos.x, pos.y, pos.z)
        var name = crop.getLocalizedName
        var modifier = 7
        crop match {

          case Blocks.wheat => {
            name = Item.itemRegistry.getNameForObject(Items.wheat)
          }
          case Blocks.melon_stem => {
            //Localize this?
            name = "Melon stem"
          }
          case Blocks.pumpkin_stem => {
            name = "Pumpkin stem"
          }
          case Blocks.nether_wart => {
            modifier = 3
          }
          case _ =>
        }
        result(name, meta * 100 / modifier)
      }
      case cocoa: BlockCocoa => {
        val meta = world.getBlockMetadata(pos.x, pos.y, pos.z)
        val value = meta * 100 / 2

        result(cocoa.getLocalizedName, Math.min(value, 100))
      }
      case _: BlockMelon | _: BlockPumpkin => {
        result(target.getLocalizedName, 100)
      }
      case _: BlockCactus | _: BlockReed => {
        val meta = world.getBlockMetadata(pos.x, pos.y, pos.z)
        result(target.getLocalizedName, meta)
      }
      case _ => result(Unit, "Not a crop")
    }
  }

  override def isValidFor(block: Block): Boolean = {
    block match {
      //has to be specified for crops otherwise overriding blocks of other mods might not get their own Provider
      case _: BlockStem | Blocks.wheat | Blocks.carrots | Blocks.potatoes | _: BlockCocoa | _: BlockMelon | _: BlockPumpkin | _: BlockCactus | _: BlockReed => true
      case _ => false
    }

  }
}
