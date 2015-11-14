package li.cil.oc.server.component

import java.util.UUID

import li.cil.oc.OpenComputers
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.common.EventHandler
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.mutable

class UpgradeLeash(val host: Entity) extends prefab.ManagedEnvironment with traits.WorldAware {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("leash").
    create()

  val leashedEntities = mutable.Set.empty[UUID]

  override def position = BlockPosition(host)

  @Callback(doc = """function(side:number):boolean -- Tries to put an entity on the specified side of the device onto a leash.""")
  def leash(context: Context, args: Arguments): Array[AnyRef] = {
    if (leashedEntities.size >= 8) return result(Unit, "too many leashed entities")
    val side = args.checkSideAny(0)
    val nearBounds = position.bounds
    val farBounds = nearBounds.offset(side.offsetX * 2.0, side.offsetY * 2.0, side.offsetZ * 2.0)
    val bounds = nearBounds.func_111270_a(farBounds)
    entitiesInBounds[EntityLiving](bounds).find(_.allowLeashing()) match {
      case Some(entity) =>
        entity.setLeashedToEntity(host, true)
        leashedEntities += entity.getUniqueID
        context.pause(0.1)
        result(true)
      case _ => result(Unit, "no unleashed entity")
    }
  }

  @Callback(doc = """function() -- Unleashes all currently leashed entities.""")
  def unleash(context: Context, args: Arguments): Array[AnyRef] = {
    unleashAll()
    null
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      unleashAll()
    }
  }

  private def unleashAll() {
    entitiesInBounds[EntityLiving](position.bounds.expand(5, 5, 5)).foreach(entity => {
      if (leashedEntities.contains(entity.getUniqueID) && entity.getLeashedToEntity == host) {
        entity.clearLeashed(true, false)
      }
    })
    leashedEntities.clear()
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    leashedEntities ++= nbt.getTagList("leashedEntities", NBT.TAG_STRING).
      map((s: NBTTagString) => UUID.fromString(s.func_150285_a_()))
    // Re-acquire leashed entities. Need to do this manually because leashed
    // entities only remember their leashee if it's an EntityLivingBase...
    EventHandler.scheduleServer(() => {
      val foundEntities = mutable.Set.empty[UUID]
      entitiesInBounds[EntityLiving](position.bounds.expand(5, 5, 5)).foreach(entity => {
        if (leashedEntities.contains(entity.getUniqueID)) {
          entity.setLeashedToEntity(host, true)
          foundEntities += entity.getUniqueID
        }
      })
      val missing = leashedEntities.diff(foundEntities)
      if (missing.size > 0) {
        OpenComputers.log.info(s"Could not find ${missing.size} leashed entities after loading!")
        leashedEntities --= missing
      }
    })
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setNewTagList("leashedEntities", leashedEntities.map(_.toString))
  }
}
