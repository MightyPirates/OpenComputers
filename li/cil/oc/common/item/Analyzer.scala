package li.cil.oc.common.item

import cpw.mods.fml.common.network.Player
import li.cil.oc.Config
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.{Analyzable, Connector, Environment}
import li.cil.oc.server.PacketSender
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class Analyzer(val parent: Delegator) extends Delegate {
  val unlocalizedName = "Analyzer"

  override def onItemUse(item: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getBlockTileEntity(x, y, z) match {
      case analyzable: Analyzable =>
        val stats = new NBTTagCompound()
        if (!world.isRemote) {
          analyzeNode(stats, analyzable.onAnalyze(stats, player, side, hitX, hitY, hitZ), player)
        }
        true
      case environment: Environment =>
        if (!world.isRemote) {
          analyzeNode(new NBTTagCompound(), environment, player)
        }
        true
      case _ => super.onItemUse(item, player, world, x, y, z, side, hitX, hitY, hitZ)
    }
  }

  private def analyzeNode(stats: NBTTagCompound, environment: Environment, player: EntityPlayer) = if (environment != null) {
    environment.node match {
      case connector: Connector if connector.localBufferSize > 0 => stats.setString(Config.namespace + "text.Analyzer.StoredEnergy", "%.2f/%.2f".format(connector.localBuffer, connector.localBufferSize))
      case _ =>
    }
    environment.node match {
      case component: Component => stats.setString(Config.namespace + "text.Analyzer.ComponentName", component.name)
      case _ =>
    }
    val address = environment.node.address()
    stats.setString(Config.namespace + "text.Analyzer.Address", address)
    PacketSender.sendAnalyze(stats, address, player.asInstanceOf[Player])
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":analyzer")
  }
}
