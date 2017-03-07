package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.event.SignChangeEvent
import li.cil.oc.api.internal
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Message
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.network.AbstractManagedEnvironment
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntitySign
import net.minecraft.util.EnumFacing
import net.minecraft.util.text.TextComponentString
import net.minecraft.world.WorldServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fml.common.eventhandler.Event

import scala.collection.convert.WrapAsJava._

abstract class UpgradeSign extends AbstractManagedEnvironment with DeviceInfo {
  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Sign upgrade",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Labelizer Deluxe"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  def host: EnvironmentHost

  protected def getValue(tileEntity: Option[TileEntitySign]): Array[AnyRef] = {
    tileEntity match {
      case Some(sign) => result(sign.signText.map(_.getUnformattedText).mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }

  protected def setValue(tileEntity: Option[TileEntitySign], text: String): Array[AnyRef] = {
    tileEntity match {
      case Some(sign) =>
        val player = host match {
          case robot: internal.Robot => robot.player
          case _ => FakePlayerFactory.get(host.getWorld.asInstanceOf[WorldServer], Settings.get.fakePlayerProfile)
        }

        val lines = text.lines.padTo(4, "").map(line => if (line.length > 15) line.substring(0, 15) else line).toArray

        if (!canChangeSign(player, sign, lines)) {
          return result(Unit, "not allowed")
        }

        lines.map(line => new TextComponentString(line)).copyToArray(sign.signText)
        host.getWorld.notifyBlockUpdate(sign.getPos)

        MinecraftForge.EVENT_BUS.post(new SignChangeEvent.Post(sign, lines))

        result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }

  protected def findSign(side: EnumFacing) = {
    val hostPos = BlockPosition(host)
    host.getWorld.getTileEntity(hostPos) match {
      case sign: TileEntitySign => Option(sign)
      case _ => host.getWorld.getTileEntity(hostPos.offset(side)) match {
        case sign: TileEntitySign => Option(sign)
        case _ => None
      }
    }
  }

  private def canChangeSign(player: EntityPlayer, tileEntity: TileEntitySign, lines: Array[String]): Boolean = {
    if (!host.getWorld.isBlockModifiable(player, tileEntity.getPos)) {
      return false
    }
    val event = new BlockEvent.BreakEvent(host.getWorld, tileEntity.getPos, tileEntity.getWorld.getBlockState(tileEntity.getPos), player)
    MinecraftForge.EVENT_BUS.post(event)
    if (event.isCanceled || event.getResult == Event.Result.DENY) {
      return false
    }

    val signEvent = new SignChangeEvent.Pre(tileEntity, lines)
    MinecraftForge.EVENT_BUS.post(signEvent)
    !(signEvent.isCanceled || signEvent.getResult == Event.Result.DENY)
  }

  override def onMessage(message: Message): Unit = {
    super.onMessage(message)
    if (message.getName == "tablet.use") message.getSource.getEnvironment match {
      case machine: api.machine.Machine => (machine.host, message.getData) match {
        case (tablet: internal.Tablet, Array(nbt: NBTTagCompound, stack: ItemStack, player: EntityPlayer, blockPos: BlockPosition, side: EnumFacing, hitX: java.lang.Float, hitY: java.lang.Float, hitZ: java.lang.Float)) =>
          host.getWorld.getTileEntity(blockPos) match {
            case sign: TileEntitySign =>
              nbt.setString("signText", sign.signText.map(_.getUnformattedText).mkString("\n"))
            case _ =>
          }
        case _ => // Ignore.
      }
      case _ => // Ignore.
    }
  }
}
