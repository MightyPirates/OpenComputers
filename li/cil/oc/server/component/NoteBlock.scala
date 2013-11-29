package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network.{LuaCallback, Context, Arguments, Visibility}
import net.minecraft.block.material.Material
import net.minecraft.tileentity.TileEntityNote

class NoteBlock(entity: TileEntityNote) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("note_block").
    create()

  // ----------------------------------------------------------------------- //

  @LuaCallback("getPitch")
  def getPitch(context: Context, args: Arguments): Array[AnyRef] = result(entity.note)

  @LuaCallback("setPitch")
  def setPitch(context: Context, args: Arguments): Array[AnyRef] = {
    val value = args.checkInteger(0)
    if (value < 0 || value > 24) throw new IllegalArgumentException("invalid pitch")
    entity.note = value.toByte
    entity.onInventoryChanged()
    result(true)
  }

  @LuaCallback("trigger")
  def trigger(context: Context, args: Arguments): Array[AnyRef] = {
    val world = entity.getWorldObj
    val (x, y, z) = (entity.xCoord, entity.yCoord, entity.zCoord)

    entity.triggerNote(world, x, y, z)
    result(world.getBlockMaterial(x, y + 1, z) == Material.air)
  }
}
