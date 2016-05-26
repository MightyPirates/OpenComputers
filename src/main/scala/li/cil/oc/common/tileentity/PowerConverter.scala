package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network._
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class PowerConverter extends traits.PowerAcceptor with traits.Environment with traits.NotAnalyzable {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferConverter).
    create()

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing) = true

  override protected def connector(side: EnumFacing) = Option(node)

  override def energyThroughput = Settings.get.powerConverterRate

  override def updateEntity(): Unit = {
    super[PowerAcceptor].updateEntity()
    super[Environment].updateEntity()
  }
}
