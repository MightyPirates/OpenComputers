package li.cil.oc.common;

import li.cil.oc.common.container.ContainerAssembler;
import li.cil.oc.common.container.ContainerCase;
import li.cil.oc.common.container.ContainerCharger;
import li.cil.oc.common.container.ContainerDrone;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.common.tileentity.TileEntityAssembler;
import li.cil.oc.common.tileentity.TileEntityCase;
import li.cil.oc.common.tileentity.TileEntityCharger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public abstract class AbstractGuiHandler implements IGuiHandler {
    @Nullable
    @Override
    public Object getServerGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        final GuiType guiType = GuiType.VALUES[id];
        switch (guiType.category) {
            case BLOCK: {
                final TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
                switch (guiType) {
                    case Assembler:
                        if (tileEntity instanceof TileEntityAssembler) {
                            return new ContainerAssembler(player.inventory, (TileEntityAssembler) tileEntity);
                        }
                        break;
                    case Case:
                        if (tileEntity instanceof TileEntityCase) {
                            return new ContainerCase(player.inventory, (TileEntityCase) tileEntity);
                        }
                        break;
                    case Charger:
                        if (tileEntity instanceof TileEntityCharger) {
                            return new ContainerCharger(player.inventory, (TileEntityCharger) tileEntity);
                        }
                        break;
                    case Printer:
//                        if (tileEntity instanceof TileEntityPrinter) {
//                            return new ContainerPrinter(player.inventory, (TileEntityPrinter) tileEntity);
//                        }
                        break;
                    case Rack:
//                        if (tileEntity instanceof TileEntityRack) {
//                            return new ContainerRack(player.inventory, (TileEntityRack) tileEntity);
//                        }
                        break;
                    case Raid:
//                        if (tileEntity instanceof TileEntityRaid) {
//                            return new ContainerRaid(player.inventory, (TileEntityRaid) tileEntity);
//                        }
                        break;
                    case Relay:
//                        if (tileEntity instanceof TileEntityRelay) {
//                            return new ContainerRelay(player.inventory, (TileEntityRelay) tileEntity);
//                        }
                        break;
                    case Robot:
//                        if (tileEntity instanceof TileEntityRobot) {
//                            return new ContainerRobot(player.inventory, (TileEntityRobot) tileEntity);
//                        }
                        break;
                    case ServerInRack:
//                        if (tileEntity instanceof TileEntityRack) {
//                            final TileEntityRack rack = (TileEntityRack) tileEntity;
//                            final int slot = GuiType.extractSlot(y);
//                            final Server server = rack.getMountable(slot);
//                            return new ContainerAssembler(player.inventory, (TileEntityRack) tileEntity, server);
//                        }
                        break;
                }
                break;
            }
            case ENTITY: {
                final Entity entity = world.getEntityByID(x);
                switch (guiType) {
                    case Drone: {
                        if (entity instanceof Drone) {
                            return new ContainerDrone(player.inventory, (Drone) entity);
                        }
                        break;
                    }
                }
                break;
            }
            case ITEM: {
                final ItemStack stack = player.getHeldItem(player.getActiveHand());
                switch (guiType) {
                    case Database: {
//                        if (Delegator$.MODULE$.subItem(stack) instanceof UpgradeDatabase) {
//            new container.Database(player.inventory, new DatabaseInventory {
//              override def container = player.getHeldItemMainhand
//
//              override def isUsableByPlayer(player: EntityPlayer) = player == player
//            })
//                        }
                        break;
                    }
                    case Server: {
//          case Some(server: item.Server) if id == GuiType.Server.id =>
//            new container.Server(player.inventory, new ServerInventory {
//              override def container = player.getHeldItemMainhand
//
//              override def isUsableByPlayer(player: EntityPlayer) = player == player
//            })
                        break;
                    }
                    case Tablet: {
//          case Some(tablet: item.Tablet) if id == GuiType.TabletInner.id =>
//            val stack = player.getHeldItemMainhand
//            if (stack.hasTagCompound)
//              new container.Tablet(player.inventory, item.Tablet.get(stack, player))
//            else
//              null
                        break;
                    }
                }
                break;
            }
        }
        return null;
    }
}
