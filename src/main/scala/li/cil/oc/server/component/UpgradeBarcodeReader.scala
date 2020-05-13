package li.cil.oc.server.component

import java.util

import li.cil.oc.{Constants, OpenComputers, api}
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.internal
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.EnumFacing
import net.minecraft.world.WorldServer

import scala.collection.convert.WrapAsJava._

class UpgradeBarcodeReader(val host: EnvironmentHost) extends AbstractManagedEnvironment with DeviceInfo {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("barcode_reader").
    withConnector().
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Barcode reader upgrade",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Readerizer Deluxe"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  override def onMessage(message: Message): Unit = {
    super.onMessage(message)
    if (message.name == "tablet.use") message.source.host match {
      case machine: api.machine.Machine => (machine.host, message.data) match {
        case (tablet: internal.Tablet, Array(nbt: NBTTagCompound, stack: ItemStack, player: EntityPlayer, blockPos: BlockPosition, side: EnumFacing, hitX: java.lang.Float, hitY: java.lang.Float, hitZ: java.lang.Float)) =>
          host.world.getTileEntity(blockPos) match {
            case analyzable: Analyzable =>
              processNodes(analyzable.onAnalyze(player, side, hitX.toFloat, hitY.toFloat, hitZ.toFloat), nbt)
            case host: SidedEnvironment =>
              processNodes(Array(host.sidedNode(side)), nbt)
            case host: Environment =>
              processNodes(Array(host.node), nbt)
            case _ => // Ignore
          }
          case _ => // Ignore
      }
      case _ => // Ignore
    }
  }

  private def processNodes(nodes: Array[Node], nbt: NBTTagCompound): Unit = if (nodes != null) {
    val readerNBT = new NBTTagList()

    for (node <- nodes if node != null) {
      val nodeNBT = new NBTTagCompound()
      node match {
        case component: Component =>
          nodeNBT.setString("type", component.name)
        case _ =>
      }

      val address = node.address()
      if (address != null && !address.isEmpty) {
        nodeNBT.setString("address", node.address())
      }

      readerNBT.appendTag(nodeNBT)
    }

    nbt.setTag("analyzed", readerNBT)
  }
}
