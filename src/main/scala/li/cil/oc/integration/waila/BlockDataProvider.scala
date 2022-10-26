package li.cil.oc.integration.waila

import java.util

import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.traits.NotAnalyzable
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.StackOption.SomeStack
import mcp.mobius.waila.api._
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.StringNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT

@WailaPlugin
object BlockDataProvider extends IWailaPlugin with IServerDataProvider[TileEntity] with IComponentProvider {
  val ConfigAddress = new ResourceLocation(OpenComputers.ID, "oc.address")
  val ConfigEnergy = new ResourceLocation(OpenComputers.ID, "oc.energy")
  val ConfigComponentName = new ResourceLocation(OpenComputers.ID, "oc.componentname")

  def register(registrar: IRegistrar) {
    registrar.registerComponentProvider(this, TooltipPosition.BODY, classOf[SimpleBlock])

    registrar.registerBlockDataProvider(this, classOf[li.cil.oc.api.network.Environment])
    registrar.registerBlockDataProvider(this, classOf[li.cil.oc.api.network.SidedEnvironment])

    registrar.addConfig(ConfigAddress, true)
    registrar.addConfig(ConfigEnergy, true)
    registrar.addConfig(ConfigComponentName, true)
  }

  override def appendServerData(tag: CompoundNBT, player: ServerPlayerEntity, world: World, tileEntity: TileEntity): Unit = {
    def writeNode(node: Node, tag: CompoundNBT) = {
      if (node != null && node.reachability != Visibility.None && !tileEntity.isInstanceOf[NotAnalyzable]) {
        if (node.address != null) {
          tag.putString("address", node.address)
        }
        node match {
          case connector: Connector =>
            tag.putInt("buffer", connector.localBuffer.toInt)
            tag.putInt("bufferSize", connector.localBufferSize.toInt)
          case _ =>
        }
        node match {
          case component: Component =>
            tag.putString("componentName", component.name)
          case _ =>
        }
      }
      tag
    }

    tileEntity match {
      case te: li.cil.oc.api.network.SidedEnvironment =>
        tag.setNewTagList("nodes", Direction.values.
          map(te.sidedNode).
          map(writeNode(_, new CompoundNBT())))
      case te: li.cil.oc.api.network.Environment =>
        writeNode(te.node, tag)
      case _ =>
    }

    // Override sided info (show info on all sides).
    def ignoreSidedness(node: Node): Unit = {
      tag.remove("nodes")
      val nodeTag = writeNode(node, new CompoundNBT())
      tag.setNewTagList("nodes", Direction.values.map(_ => nodeTag))
    }

    tileEntity match {
      case te: tileentity.Relay =>
        tag.putDouble("signalStrength", te.strength)
        // this might be called by waila before the components have finished loading, thus the addresses may be null
        tag.setNewTagList("addresses", stringIterableToNbt(te.componentNodes.collect {
          case n if n.address != null => n
        }.map(_.address)))
      case te: tileentity.Assembler =>
        ignoreSidedness(te.node)
        if (te.isAssembling) {
          tag.putDouble("progress", te.progress)
          tag.putInt("timeRemaining", te.timeRemaining)
          te.output match {
            case SomeStack(output) => tag.putString("output", output.getDescriptionId)
            case _ => // Huh...
          }
        }
      case te: tileentity.Charger =>
        tag.putDouble("chargeSpeed", te.chargeSpeed)
      case te: tileentity.DiskDrive =>
        // Override address with file system address.
        tag.remove("address")
        te.filesystemNode.foreach(writeNode(_, tag))
      case te: tileentity.Hologram => ignoreSidedness(te.node)
      case te: tileentity.Keyboard => ignoreSidedness(te.node)
      case te: tileentity.Screen => ignoreSidedness(te.node)
      case te: tileentity.Rack =>
        tag.remove("nodes")
        //tag.setNewTagList("servers", stringIterableToNbt(te.servers.map(_.fold("")(_.node.address))))
        //tag.putByteArray("sideIndexes", Direction.values.map(side => te.sides.indexWhere(_.contains(side))).map(_.toByte))
        // TODO
      case _ =>
    }
  }

  override def appendBody(tooltip: util.List[ITextComponent], accessor: IDataAccessor, config: IPluginConfig): Unit = {
    val tag = accessor.getServerData
    if (tag == null || tag.isEmpty) return

    accessor.getTileEntity match {
      case _: tileentity.Relay =>
        val address = tag.getList("addresses", NBT.TAG_STRING).getString(accessor.getSide.ordinal)
        val signalStrength = tag.getDouble("signalStrength")
        if (config.get(ConfigAddress)) {
          tooltip.add(Localization.Analyzer.Address(address))
        }
        tooltip.add(Localization.Analyzer.WirelessStrength(signalStrength))
      case _: tileentity.Assembler =>
        if (tag.contains("progress")) {
          val progress = tag.getDouble("progress")
          val timeRemaining = formatTime(tag.getInt("timeRemaining"))
          tooltip.add(new StringTextComponent(Localization.Assembler.Progress(progress, timeRemaining)))
          if (tag.contains("output")) {
            val output = tag.getString("output")
            tooltip.add(new StringTextComponent("Building: " + Localization.localizeImmediately(output)))
          }
        }
      case _: tileentity.Charger =>
        val chargeSpeed = tag.getDouble("chargeSpeed")
        tooltip.add(Localization.Analyzer.ChargerSpeed(chargeSpeed))
      case te: tileentity.Rack =>
//        val servers = tag.getList("servers", NBT.TAG_STRING).map((t: StringNBT) => t.getAsString).toArray
//        val hitPos = accessor.getMOP.getLocation
//        val address = te.slotAt(accessor.getSide, (hitPos.xCoord - accessor.getMOP.getBlockPos.getX).toFloat, (hitPos.yCoord - accessor.getMOP.getBlockPos.getY).toFloat, (hitPos.zCoord - accessor.getMOP.getBlockPos.getZ).toFloat) match {
//          case Some(slot) => servers(slot)
//          case _ => tag.getByteArray("sideIndexes").map(index => if (index >= 0) servers(index) else "").apply(te.toLocal(accessor.getSide).ordinal)
//        }
//        if (address.nonEmpty && config.get(ConfigAddress)) {
//          tooltip.add(Localization.Analyzer.Address(address))
//        }
        // TODO
      case _ =>
    }

    def readNode(tag: CompoundNBT) = {
      if (config.get(ConfigAddress) && tag.contains("address")) {
        val address = tag.getString("address")
        if (address.nonEmpty) {
          tooltip.add(Localization.Analyzer.Address(address))
        }
      }
      if (config.get(ConfigEnergy) && tag.contains("buffer") && tag.contains("bufferSize")) {
        val buffer = tag.getInt("buffer")
        val bufferSize = tag.getInt("bufferSize")
        if (bufferSize > 0) {
          tooltip.add(Localization.Analyzer.StoredEnergy(s"$buffer/$bufferSize"))
        }
      }
      if (config.get(ConfigComponentName) && tag.contains("componentName")) {
        val componentName = tag.getString("componentName")
        if (componentName.nonEmpty) {
          tooltip.add(Localization.Analyzer.ComponentName(componentName))
        }
      }
    }

    accessor.getTileEntity match {
      case te: li.cil.oc.api.network.SidedEnvironment =>
        readNode(tag.getList("nodes", NBT.TAG_COMPOUND).getCompound(accessor.getSide.ordinal))
      case te: li.cil.oc.api.network.Environment =>
        readNode(tag)
      case _ =>
    }
  }

  override def getStack(accessor: IDataAccessor, config: IPluginConfig) = accessor.getStack

  private def formatTime(seconds: Int) = {
    // Assembly times should not / rarely exceed one hour, so this is good enough.
    if (seconds < 60) f"0:$seconds%02d"
    else f"${seconds / 60}:${seconds % 60}%02d"
  }
}
