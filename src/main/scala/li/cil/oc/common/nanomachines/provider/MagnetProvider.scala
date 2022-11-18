package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.prefab.AbstractBehavior
import net.minecraft.entity.item.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.math.vector.Vector3d

import scala.collection.convert.ImplicitConversionsToScala._

object MagnetProvider extends ScalaProvider("9324d5ec-71f1-41c2-b51c-406e527668fc") {
  override def createScalaBehaviors(player: PlayerEntity) = Iterable(new MagnetBehavior(player))

  override def readBehaviorFromNBT(player: PlayerEntity, nbt: CompoundNBT) = new MagnetBehavior(player)

  class MagnetBehavior(player: PlayerEntity) extends AbstractBehavior(player) {
    override def getNameHint = "magnet"

    override def update(): Unit = {
      val world = player.level
      if (!world.isClientSide) {
        val actualRange = Settings.get.nanomachineMagnetRange * api.Nanomachines.getController(player).getInputCount(this)
        val items = world.getEntitiesOfClass(classOf[ItemEntity], player.getBoundingBox.inflate(actualRange, actualRange, actualRange))
        items.collect {
          case item: ItemEntity if !item.hasPickUpDelay && !item.getItem.isEmpty && player.inventory.items.exists(stack => stack.isEmpty || stack.getCount < stack.getMaxStackSize && stack.sameItem(item.getItem)) =>
            val dx = player.getX - item.getX
            val dy = player.getY - item.getY
            val dz = player.getZ - item.getZ
            val delta = new Vector3d(dx, dy, dz).normalize()
            item.push(delta.x * 0.1, delta.y * 0.1, delta.z * 0.1)
        }
      }
    }
  }

}
