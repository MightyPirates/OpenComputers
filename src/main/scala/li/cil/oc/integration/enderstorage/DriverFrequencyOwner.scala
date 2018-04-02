package li.cil.oc.integration.enderstorage

import codechicken.enderstorage.tile.{TileEnderChest, TileEnderTank, TileFrequencyOwner}
import codechicken.lib.colour.EnumColour
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.{Arguments, Callback, Context, Machine}
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

final class DriverFrequencyOwner extends DriverSidedTileEntity {
  override def getTileEntityClass = classOf[TileFrequencyOwner]
  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing) =
    new DriverFrequencyOwner.Environment(world.getTileEntity(pos).asInstanceOf[TileFrequencyOwner])
}

object DriverFrequencyOwner {
  private def name(enderDevice: TileFrequencyOwner) = enderDevice match {
    case _: TileEnderTank => "ender_tank"
    case _: TileEnderChest => "ender_chest"
    case _ => "ender_storage"
  }

  private lazy val frequencyColors = EnumColour.values().map(_.getMinecraftName)

  final class Environment(val enderDevice: TileFrequencyOwner)
    extends ManagedTileEntityEnvironment[TileFrequencyOwner](enderDevice, name(enderDevice)) with NamedBlock {

    override val preferredName = name(enderDevice)
    override val priority = 0

    @Callback(doc = "function():table -- Get the currently set frequency. {left, middle, right}")
    def getFrequency(context: Context, args: Arguments): Array[AnyRef] = Array[AnyRef](tileEntity.frequency.toArray)

    @Callback(doc = "function(left:number, middle:number, right:number) -- Set the frequency. Range 0-15 (inclusive).")
    def setFrequency(context: Context, args: Arguments): Array[AnyRef] = {
      val Seq(left, middle, right) =
        if (args.count == 1) {
          val freq = args.checkInteger(0)
          if ((freq & 0xFFF) != freq) throw new IllegalArgumentException("invalid frequency")
          (8 to 0 by -4).map(s => (freq >> s) & 0xF)
        } else {
          val colors = (0 to 2).map(args.checkInteger)
          if (colors.exists(c => (c & 0xF) != c)) throw new IllegalArgumentException("invalid frequency")
          colors
        }

      import li.cil.oc.Settings
      import Settings.EnderStorageFrequencyPolicy._
      val error = (enderDevice.frequency.owner, Settings.get.enderStorageFrequencyPolicy) match {
        case (_, Always) | (null | "" | "global", _) => None
        case (owner, DeviceOwnerIsSingleOwner) => context match {
          case machine: Machine if machine.users sameElements Array(owner) => None
          case _ => Some("the owner of the Ender device must be the single computer owner")
        }
        case _ => Some("cannot change frequency of owned storage")
      }
      error match {
        case None =>
          enderDevice.setFreq(enderDevice.frequency.setFrequency(left, middle, right))
          null
        case Some(msg) => Array[AnyRef](null, msg)
      }
    }

    @Callback(doc = "function():string -- Get the name of the owner, which is usually a player's name or 'global'.")
    def getOwner(context: Context, args: Arguments): Array[AnyRef] = Array[AnyRef](enderDevice.frequency.owner)

    @Callback(doc = "function():table -- Get the currently set frequency as a table of color names.")
    def getFrequencyColors(context: Context, args: Arguments): Array[AnyRef] = Array[AnyRef](tileEntity.frequency.getColours)

    @Callback(doc = "function():table -- Get a table with the mapping of colours (as Minecraft names) to Frequency numbers. NB: Frequencies are zero based!")
    def getColors(context: Context, args: Arguments): Array[AnyRef] = Array[AnyRef](frequencyColors)
  }
}
