package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.common.event.BlockChangeHandler
import li.cil.oc.common.event.BlockChangeHandler.ChangeListener
import li.cil.oc.server.network
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DriverBlock
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.network.AbstractManagedNodeContainer
import li.cil.oc.api.util.Location
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d

import scala.collection.convert.WrapAsJava._

/**
  * Mostly stolen from {@link li.cil.oc.common.tileentity.Adapter}
  *
  * @author Sangar, Vexatos
  */
class UpgradeMF(val host: Location, val coord: BlockPosition, val dir: EnumFacing) extends AbstractManagedNodeContainer with ChangeListener with DeviceInfo {
  override val getNode = api.Network.newNode(this, Visibility.NONE).
    withConnector().
    create()

  private var otherEnv: Option[NodeContainer] = None
  private var otherDrv: Option[(NodeContainerItem, DriverBlock)] = None
  private var blockData: Option[BlockData] = None

  override val canUpdate = true

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Bus,
    DeviceAttribute.Description -> "Remote Adapter",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.Scummtech,
    DeviceAttribute.Product -> "ERR NAME NOT FOUND"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  private def otherNode(tile: TileEntity, f: (Node) => Unit) {
    network.Network.getNetworkNode(tile, dir) match {
      case Some(otherNode) => f(otherNode)
      case _ => // Nothing to do here
    }
  }

  private def updateBoundState() {
    if (getNode != null && getNode.getNetwork != null && coord.world.exists(_.provider.getDimension == host.getWorld.provider.getDimension)
      && coord.toVec3.distanceTo(new Vec3d(host.xPosition, host.yPosition, host.zPosition)) <= Settings.get.mfuRange) {
      host.getWorld.getTileEntity(coord) match {
        case env: TileEntity with NodeContainer =>
          otherEnv match {
            case Some(environment: TileEntity) =>
              otherNode(environment, getNode.disconnect)
              otherEnv = None
            case _ => // Nothing to do here.
          }
          otherEnv = Some(env)
          // Remove any driver that might be there.
          otherDrv match {
            case Some((environment, driver)) =>
              getNode.disconnect(environment.getNode)
              environment.save(blockData.get.data)
              Option(environment.getNode).foreach(_.remove())
              otherDrv = None
            case _ => // Nothing to do here.
          }
          otherNode(env, getNode.connect)
        case _ =>
          // Remove any environment that might have been there.
          otherEnv match {
            case Some(environment: TileEntity) =>
              otherNode(environment, getNode.disconnect)
              otherEnv = None
            case _ => // Nothing to do here.
          }
          val world = coord.world.get
          Option(api.Driver.driverFor(world, coord.toBlockPos, dir)) match {
            case Some(newDriver) =>
              otherDrv match {
                case Some((oldEnvironment, driver)) =>
                  if (newDriver != driver) {
                    // This is... odd. Maybe moved by some other mod? First, clean up.
                    otherDrv = None
                    blockData = None
                    getNode.disconnect(oldEnvironment.getNode)

                    // Then rebuild - if we have something.
                    val environment = newDriver.createEnvironment(world, coord.toBlockPos, dir)
                    if (environment != null) {
                      otherDrv = Some((environment, newDriver))
                      blockData = Some(new BlockData(environment.getClass.getName, new NBTTagCompound()))
                      getNode.connect(environment.getNode)
                    }
                  } // else: the more things change, the more they stay the same.
                case _ =>
                  // A challenger appears. Maybe.
                  val environment = newDriver.createEnvironment(world, coord.toBlockPos, dir)
                  if (environment != null) {
                    otherDrv = Some((environment, newDriver))
                    blockData match {
                      case Some(data) if data.name == environment.getClass.getName =>
                        environment.load(data.data)
                      case _ =>
                    }
                    blockData = Some(new BlockData(environment.getClass.getName, new NBTTagCompound()))
                    getNode.connect(environment.getNode)
                  }
              }
            case _ => otherDrv match {
              case Some((environment, driver)) =>
                // We had something there, but it's gone now...
                getNode.disconnect(environment.getNode)
                environment.save(blockData.get.data)
                Option(environment.getNode).foreach(_.remove())
                otherDrv = None
              case _ => // Nothing before, nothing now.
            }
          }
      }
    }
  }

  private def disconnect() {
    otherEnv match {
      case Some(environment: TileEntity) =>
        otherNode(environment, getNode.disconnect)
        otherEnv = None
      case _ => // Nothing to do here.
    }
    otherDrv match {
      case Some((environment, driver)) =>
        getNode.disconnect(environment.getNode)
        environment.save(blockData.get.data)
        Option(environment.getNode).foreach(_.remove())
        otherDrv = None
      case _ => // Nothing to do here.
    }
  }

  override def onBlockChanged() = updateBoundState()

  override def update() {
    super.update()
    otherDrv match {
      case Some((env, drv)) if env.canUpdate => env.update()
      case _ => // No driver
    }
    if (host.getWorld.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      if (!getNode.tryChangeEnergy(-Settings.get.mfuCost * Settings.get.tickFrequency
        * coord.toVec3.distanceTo(new Vec3d(host.xPosition, host.yPosition, host.zPosition)))) {
        disconnect()
      }
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.getNode) {
      // Not checking for range yet because host may be a moving adapter, who knows?
      BlockChangeHandler.addListener(this, coord)

      updateBoundState()
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    otherEnv match {
      case Some(env: TileEntity) => otherNode(env, (otherNode) => if (node == otherNode) otherEnv = None)
      case _ => // No environment
    }
    otherDrv match {
      case Some((env, drv)) if node == env.getNode => otherDrv = None
      case _ => // No driver
    }
    if (node == this.getNode) {
      BlockChangeHandler.removeListener(this)
    }
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    Option(nbt.getCompoundTag(Constants.namespace + "adapter.block")) match {
      case Some(blockNbt: NBTTagCompound) =>
        if (blockNbt.hasKey("name") && blockNbt.hasKey("data")) {
          blockData = Some(new BlockData(blockNbt.getString("name"), blockNbt.getCompoundTag("data")))
        }
      case _ => // Invalid tag
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    val blockNbt = new NBTTagCompound()
    blockData.foreach({ data =>
      otherDrv.foreach(_._1.save(data.data))
      blockNbt.setString("name", data.name)
      blockNbt.setTag("data", data.data)
    })
    nbt.setTag(Constants.namespace + "adapter.block", blockNbt)
  }

  // ----------------------------------------------------------------------- //

  private class BlockData(val name: String, val data: NBTTagCompound)

}
