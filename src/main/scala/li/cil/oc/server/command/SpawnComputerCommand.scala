package li.cil.oc.server.command

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.command.SimpleCommand
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.capabilities.RotatableImpl
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import net.minecraft.command.ICommandSender
import net.minecraft.command.WrongUsageException
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.util.text.TextComponentString

object SpawnComputerCommand extends SimpleCommand("oc_spawnComputer") {
  aliases += "oc_sc"

  final val MaxDistance = 16

  override def getUsage(source: ICommandSender): String = name

  override def execute(server: MinecraftServer, source: ICommandSender, command: Array[String]): Unit = {
    source match {
      case player: EntityPlayer =>
        val world = player.getEntityWorld
        val origin = new Vec3d(player.posX, player.posY + player.getEyeHeight, player.posZ)
        val direction = player.getLookVec
        val lookAt = origin.addVector(direction.xCoord * MaxDistance, direction.yCoord * MaxDistance, direction.zCoord * MaxDistance)
        world.rayTraceBlocks(origin, lookAt) match {
          case hit: RayTraceResult if hit.typeOfHit == RayTraceResult.Type.BLOCK =>
            val hitPos = BlockPosition(hit.getBlockPos, world)
            val casePos = hitPos.offset(hit.sideHit)
            val screenPos = casePos.offset(EnumFacing.UP)
            val keyboardPos = screenPos.offset(EnumFacing.UP)

            if (!world.isAirBlock(casePos) || !world.isAirBlock(screenPos) || !world.isAirBlock(keyboardPos)) {
              player.sendMessage(new TextComponentString("Target position obstructed."))
              return
            }

            def rotateProperly(pos: BlockPosition):RotatableImpl = {
              world.getTileEntity(pos) match {
                case rotatable: RotatableImpl =>
                  rotatable.setFromEntityPitchAndYaw(player)
                  if (!rotatable.validFacings.contains(rotatable.pitch)) {
                    rotatable.pitch = rotatable.validFacings.headOption.getOrElse(EnumFacing.NORTH)
                  }
                  rotatable.invertRotation()
                  rotatable
                case _ => null // not rotatable
              }
            }

            world.setBlock(casePos, api.Items.get(Constants.BlockName.CaseCreative).block())
            rotateProperly(casePos)
            world.setBlock(screenPos, api.Items.get(Constants.BlockName.ScreenTier2).block())
            rotateProperly(screenPos) match {
              case rotatable: RotatableImpl => rotatable.pitch match {
                case EnumFacing.UP | EnumFacing.DOWN =>
                  rotatable.pitch = EnumFacing.NORTH
                case _ => // nothing to do here, pitch is fine
              }
              case _ => // ???
            }
            world.setBlock(keyboardPos, api.Items.get(Constants.BlockName.Keyboard).block())
            world.getTileEntity(keyboardPos) match {
              case t: RotatableImpl =>
                t.setFromEntityPitchAndYaw(player)
                t.setFromFacing(EnumFacing.UP)
              case _ => // ???
            }

            api.Network.joinOrCreateNetwork(world.getTileEntity(casePos))

            InventoryUtils.insertIntoInventoryAt(api.Items.get(Constants.ItemName.APUCreative).createItemStack(1), casePos)
            InventoryUtils.insertIntoInventoryAt(api.Items.get(Constants.ItemName.RAMTier6).createItemStack(2), casePos)
            InventoryUtils.insertIntoInventoryAt(api.Items.get(Constants.ItemName.HDDTier3).createItemStack(1), casePos)
            InventoryUtils.insertIntoInventoryAt(api.Items.get(Constants.ItemName.LuaBios).createItemStack(1), casePos)
            InventoryUtils.insertIntoInventoryAt(api.Items.get(Constants.ItemName.OpenOS).createItemStack(1), casePos)
          case _ => player.sendMessage(new TextComponentString("You need to be looking at a nearby block."))
        }
      case _ => throw new WrongUsageException("Can only be used by players.")
    }
  }

  // OP levels for reference:
  // 1 - Ops can bypass spawn protection.
  // 2 - Ops can use /clear, /difficulty, /effect, /gamemode, /gamerule, /give, /summon, /setblock and /tp, and can edit command blocks.
  // 3 - Ops can use /ban, /deop, /kick, and /op.
  // 4 - Ops can use /stop.

  override def getRequiredPermissionLevel = 2
}
