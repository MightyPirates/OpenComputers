package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network._
import li.cil.oc.{Settings, api}
import net.minecraftforge.common.ForgeDirection

class PowerConverter extends traits.PowerAcceptor with traits.Environment with traits.NotAnalyzable {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferConverter).
    create()

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = true

  override protected def connector(side: ForgeDirection) = Option(node)

  override def canUpdate = isServer
}
