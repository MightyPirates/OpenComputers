package li.cil.oc.server.component

import net.minecraft.tileentity.TileEntity
import net.minecraft.entity.Entity
import net.minecraft.world.World

trait Container {
  def tileEntity: Option[TileEntity] = None

  def entity: Option[Entity] = None

  def world: World

  def x: Double

  def y: Double

  def z: Double

  def markChanged() {}
}

object Container {

  case class TileEntityContainer(container: TileEntity) extends Container {
    override def tileEntity = Option(container)

    override def world = container.getWorldObj

    override def x = container.xCoord + 0.5

    override def y = container.yCoord + 0.5

    override def z = container.zCoord + 0.5

    override def markChanged() = container.onInventoryChanged()
  }

  case class EntityContainer(container: Entity) extends Container {
    override def entity = Option(container)

    override def world = container.worldObj

    override def x = container.posX

    override def y = container.posY

    override def z = container.posZ
  }

}
