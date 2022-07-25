package li.cil.oc

import li.cil.oc.common.IMC
import li.cil.oc.common.Proxy
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.forgespi.Environment
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle._
import net.minecraftforge.fml.event.server._
import net.minecraftforge.fml.network.simple.SimpleChannel
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import scala.collection.convert.WrapAsScala._

@Mod(OpenComputers.ID)
object OpenComputers {
  final val ID = "opencomputers"

  final val Name = "OpenComputers"

  val Version = ModLoadingContext.get.getActiveContainer.getModInfo.getVersion

  final val log: Logger = LogManager.getLogger(Name)

  lazy val proxy: Proxy = {
    val cls = Environment.get.getDist match {
      case Dist.CLIENT => Class.forName("li.cil.oc.client.Proxy")
      case _ => Class.forName("li.cil.oc.common.Proxy")
    }
    cls.getConstructor().newInstance().asInstanceOf[Proxy]
  }

  val modContainer: ModContainer = ModLoadingContext.get.getActiveContainer

  var channel: SimpleChannel = null

  MinecraftForge.EVENT_BUS.register(this)

  @Deprecated
  def openGui(player: PlayerEntity, guiId: Int, world: World, x: Int, y: Int, z: Int): Unit = proxy.openGui(player, guiId, world, x, y, z)

  @SubscribeEvent
  def commonInit(e: FMLCommonSetupEvent): Unit = {
    proxy.preInit(e)
    proxy.init(e)
  }

  @SubscribeEvent
  def serverStart(e: FMLServerStartingEvent): Unit = {
    ThreadPoolFactory.safePools.foreach(_.newThreadPool())
  }

  @SubscribeEvent
  def serverStop(e: FMLServerStoppedEvent): Unit = {
    ThreadPoolFactory.safePools.foreach(_.waitForCompletion())
  }

  @SubscribeEvent
  def imc(e: InterModProcessEvent): Unit = InterModComms.getMessages(OpenComputers.ID).sequential.iterator.foreach(IMC.handleMessage)

  @SubscribeEvent
  def loadComplete(e: FMLLoadCompleteEvent): Unit = proxy.postInit(e)
}
