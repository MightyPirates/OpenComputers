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
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import mcp.mobius.waila.api.IWailaDataProvider
import mcp.mobius.waila.api.IWailaRegistrar
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

object BlockDataProvider extends IWailaDataProvider {
  val ConfigAddress = "oc.address"
  val ConfigEnergy = "oc.energy"
  val ConfigComponentName = "oc.componentName"

  def init(registrar: IWailaRegistrar) {
    registrar.registerBodyProvider(BlockDataProvider, classOf[SimpleBlock])

    registrar.registerNBTProvider(this, classOf[li.cil.oc.api.network.Environment])
    registrar.registerNBTProvider(this, classOf[li.cil.oc.api.network.SidedEnvironment])

    registrar.addConfig(OpenComputers.Name, ConfigAddress)
    registrar.addConfig(OpenComputers.Name, ConfigEnergy)
    registrar.addConfig(OpenComputers.Name, ConfigComponentName)
  }

  override def getNBTData(player: EntityPlayerMP, tileEntity: TileEntity, tag: NBTTagCompound, world: World, x: Int, y: Int, z: Int) = {
    def writeNode(node: Node, tag: NBTTagCompound) = {
      if (node != null && node.reachability != Visibility.None && !tileEntity.isInstanceOf[NotAnalyzable]) {
        if (node.address != null) {
          tag.setString("address", node.address)
        }
        node match {
          case connector: Connector =>
            tag.setInteger("buffer", connector.localBuffer.toInt)
            tag.setInteger("bufferSize", connector.localBufferSize.toInt)
          case _ =>
        }
        node match {
          case component: Component =>
            tag.setString("componentName", component.name)
          case _ =>
        }
      }
      tag
    }

    tileEntity match {
      case te: li.cil.oc.api.network.SidedEnvironment =>
        tag.setNewTagList("nodes", ForgeDirection.VALID_DIRECTIONS.
          map(te.sidedNode).
          map(writeNode(_, new NBTTagCompound())))
      case te: li.cil.oc.api.network.Environment =>
        writeNode(te.node, tag)
      case _ =>
    }

    // Override sided info (show info on all sides).
    def ignoreSidedness(node: Node): Unit = {
      tag.removeTag("nodes")
      val nodeTag = writeNode(node, new NBTTagCompound())
      tag.setNewTagList("nodes", ForgeDirection.VALID_DIRECTIONS.map(_ => nodeTag))
    }

    tileEntity match {
      case te: tileentity.AccessPoint =>
        tag.setDouble("signalStrength", te.strength)
        tag.setNewTagList("addresses", stringIterableToNbt(te.componentNodes.map(_.address)))
      case te: tileentity.Assembler =>
        ignoreSidedness(te.node)
        if (te.isAssembling) {
          tag.setDouble("progress", te.progress)
          tag.setInteger("timeRemaining", te.timeRemaining)
          te.output match {
            case Some(output) => tag.setString("output", output.getUnlocalizedName)
            case _ => // Huh...
          }
        }
      case te: tileentity.Charger =>
        tag.setDouble("chargeSpeed", te.chargeSpeed)
      case te: tileentity.DiskDrive =>
        // Override address with file system address.
        tag.removeTag("address")
        te.filesystemNode.foreach(writeNode(_, tag))
      case te: tileentity.Hologram => ignoreSidedness(te.node)
      case te: tileentity.Keyboard => ignoreSidedness(te.node)
      case te: tileentity.Screen => ignoreSidedness(te.node)
      case te: tileentity.Rack =>
        tag.removeTag("nodes")
//        tag.setNewTagList("servers", stringIterableToNbt(te.servers.map(_.fold("")(_.node.address))))
//        tag.setByteArray("sideIndexes", ForgeDirection.VALID_DIRECTIONS.map(side => te.sides.indexWhere(_.contains(side))).map(_.toByte))
        // TODO
      case _ =>
    }

    tag
  }

  override def getWailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler): util.List[String] = {
    val tag = accessor.getNBTData
    if (tag == null || tag.hasNoTags) return tooltip

    accessor.getTileEntity match {
      case _: tileentity.AccessPoint =>
        val address = tag.getTagList("addresses", NBT.TAG_STRING).getStringTagAt(accessor.getSide.ordinal)
        val signalStrength = tag.getDouble("signalStrength")
        if (config.getConfig(ConfigAddress)) {
          tooltip.add(Localization.Analyzer.Address(address).getUnformattedText)
        }
        tooltip.add(Localization.Analyzer.WirelessStrength(signalStrength).getUnformattedText)
      case _: tileentity.Assembler =>
        if (tag.hasKey("progress")) {
          val progress = tag.getDouble("progress")
          val timeRemaining = formatTime(tag.getInteger("timeRemaining"))
          tooltip.add(Localization.Assembler.Progress(progress, timeRemaining))
          if (tag.hasKey("output")) {
            val output = tag.getString("output")
            tooltip.add("Building: " + Localization.localizeImmediately(output))
          }
        }
      case _: tileentity.Charger =>
        val chargeSpeed = tag.getDouble("chargeSpeed")
        tooltip.add(Localization.Analyzer.ChargerSpeed(chargeSpeed).getUnformattedText)
      case te: tileentity.Rack =>
//        val servers = tag.getTagList("servers", NBT.TAG_STRING).map((t: NBTTagString) => t.func_150285_a_()).toArray
//        val hitPos = accessor.getPosition.hitVec
//        val address = te.slotAt(accessor.getSide, (hitPos.xCoord - accessor.getPosition.blockX).toFloat, (hitPos.yCoord - accessor.getPosition.blockY).toFloat, (hitPos.zCoord - accessor.getPosition.blockZ).toFloat) match {
//          case Some(slot) => servers(slot)
//          case _ => tag.getByteArray("sideIndexes").map(index => if (index >= 0) servers(index) else "").apply(te.toLocal(accessor.getSide).ordinal)
//        }
//        if (address.nonEmpty && config.getConfig(ConfigAddress)) {
//            tooltip.add(Localization.Analyzer.Address(address).getUnformattedText)
//        }
        // TODO
      case _ =>
    }

    def readNode(tag: NBTTagCompound) = {
      if (config.getConfig(ConfigAddress) && tag.hasKey("address")) {
        val address = tag.getString("address")
        if (address.nonEmpty) {
          tooltip.add(Localization.Analyzer.Address(address).getUnformattedText)
        }
      }
      if (config.getConfig(ConfigEnergy) && tag.hasKey("buffer") && tag.hasKey("bufferSize")) {
        val buffer = tag.getInteger("buffer")
        val bufferSize = tag.getInteger("bufferSize")
        if (bufferSize > 0) {
          tooltip.add(Localization.Analyzer.StoredEnergy(s"$buffer/$bufferSize").getUnformattedText)
        }
      }
      if (config.getConfig(ConfigComponentName) && tag.hasKey("componentName")) {
        val componentName = tag.getString("componentName")
        if (componentName.nonEmpty) {
          tooltip.add(Localization.Analyzer.ComponentName(componentName).getUnformattedText)
        }
      }
    }

    accessor.getTileEntity match {
      case te: li.cil.oc.api.network.SidedEnvironment =>
        readNode(tag.getTagList("nodes", NBT.TAG_COMPOUND).getCompoundTagAt(accessor.getSide.ordinal))
      case te: li.cil.oc.api.network.Environment =>
        readNode(tag)
      case _ =>
    }

    tooltip
  }

  override def getWailaStack(accessor: IWailaDataAccessor, config: IWailaConfigHandler) = accessor.getStack

  override def getWailaHead(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = tooltip

  override def getWailaTail(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = tooltip

  private def formatTime(seconds: Int) = {
    // Assembly times should not / rarely exceed one hour, so this is good enough.
    if (seconds < 60) f"0:$seconds%02d"
    else f"${seconds / 60}:${seconds % 60}%02d"
  }
}