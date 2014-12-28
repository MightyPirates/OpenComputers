package li.cil.oc.common.block

import java.util

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.Color
import li.cil.oc.util.PackedColor
import li.cil.oc.util.Tooltip
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Screen(val tier: Int) extends RedstoneAware with traits.OmniRotatable {
  setLightLevel(0.34f)

  override protected def setDefaultExtendedState(state: IBlockState) = setDefaultState(state)

  @SideOnly(Side.CLIENT)
  override def getRenderColor(state: IBlockState) = Color.rgbValues(Color.byTier(tier))

  // ----------------------------------------------------------------------- //

  override def rarity = Array(EnumRarity.COMMON, EnumRarity.UNCOMMON, EnumRarity.RARE).apply(tier)

  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    val (w, h) = Settings.screenResolutionsByTier(tier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(tier))
    tooltip.addAll(Tooltip.get(getClass.getSimpleName, w, h, depth))
  }

  // ----------------------------------------------------------------------- //

  // TODO remove
//  object Icons {
//    var b, b2, bbl, bbl2, bbm, bbm2, bbr, bbr2, bhb, bhb2, bhm, bhm2, bht, bht2, bml, bmm, bmr, btl, btm, btr, bvb, bvb2, bvm, bvt, f, f2, fbl, fbl2, fbm, fbm2, fbr, fbr2, fhb, fhb2, fhm, fhm2, fht, fht2, fml, fmm, fmr, ftl, ftm, ftr, fvb, fvb2, fvm, fvt = null: IIcon
//
//    def fh = Array(fht, fhm, fhb)
//
//    def fv = Array(fvt, fvm, fvb)
//
//    def bh = Array(bht, bhm, bhb)
//
//    def bv = Array(bvt, bvm, bvb)
//
//    def fth = Array(ftl, ftm, ftr)
//
//    def fmh = Array(fml, fmm, fmr)
//
//    def fbh = Array(fbl, fbm, fbr)
//
//    def bth = Array(btl, btm, btr)
//
//    def bmh = Array(bml, bmm, bmr)
//
//    def bbh = Array(bbl, bbm, bbr)
//
//    def ftv = Array(ftl, fml, fbl)
//
//    def fmv = Array(ftm, fmm, fbm)
//
//    def fbv = Array(ftr, fmr, fbr)
//
//    def btv = Array(btl, bml, bbl)
//
//    def bmv = Array(btm, bmm, bbm)
//
//    def bbv = Array(btr, bmr, bbr)
//
//    def fh2 = Array(fht2, fhm2, fhb2)
//
//    def fv2 = Array(fvt, fvm, fvb2)
//
//    def bh2 = Array(bht2, bhm2, bhb2)
//
//    def bv2 = Array(bvt, bvm, bvb2)
//
//    def fbh2 = Array(fbl2, fbm2, fbr2)
//
//    def bbh2 = Array(bbl2, bbm2, bbr2)
//
//    def fud = Icons.fh2 ++ Icons.fv2 ++ Icons.fth ++ Icons.fmh ++ Icons.fbh2
//
//    def bud = Icons.bh2.reverse ++ Icons.bv2 ++ Icons.bth.reverse ++ Icons.bmh.reverse ++ Icons.bbh2.reverse
//
//    def fsn = Icons.fh ++ Icons.fv ++ Icons.fth ++ Icons.fmh ++ Icons.fbh
//
//    def few = Icons.fv ++ Icons.fh ++ Icons.ftv ++ Icons.fmv ++ Icons.fbv
//
//    def bsn = Icons.bh ++ Icons.bv ++ Icons.bth ++ Icons.bmh ++ Icons.bbh
//
//    def bew = Icons.bv ++ Icons.bh ++ Icons.btv ++ Icons.bmv ++ Icons.bbv
//
//    def sud = Array(Icons.bvt, Icons.bvm, Icons.bvb2)
//
//    def sse = Array(Icons.bhb2, Icons.bhm2, Icons.bht2)
//
//    def snw = Array(Icons.bht2, Icons.bhm2, Icons.bhb2)
//
//    def th = Array(Icons.bhb, Icons.bhm, Icons.bht)
//
//    def tv = Array(Icons.bvb, Icons.bvm, Icons.bvt)
//  }
//
//  // This an ugly monstrosity, but it's still better than having to manually
//  // compute ambient occlusion in a custom block renderer to keep the lighting
//  // pretty... which would be even more grotesque.
//  override def getIcon(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: EnumFacing, localSide: EnumFacing) =
//    world.getTileEntity(x, y, z) match {
//      case screen: tileentity.Screen if screen.width > 1 || screen.height > 1 =>
//        val right = screen.width - 1
//        val bottom = screen.height - 1
//        val (px, py) = screen.localPosition
//        val (lx, ly) = screen.pitch match {
//          case EnumFacing.NORTH => (px, py)
//          case EnumFacing.UP => screen.yaw match {
//            case EnumFacing.SOUTH =>
//              (px, py)
//            case EnumFacing.NORTH =>
//              (right - px, bottom - py)
//            case EnumFacing.EAST =>
//              (right - px, py)
//            case EnumFacing.WEST =>
//              (px, bottom - py)
//            case _ => throw new AssertionError("yaw has invalid value")
//          }
//          case EnumFacing.DOWN => screen.yaw match {
//            case EnumFacing.SOUTH =>
//              (px, bottom - py)
//            case EnumFacing.NORTH =>
//              (right - px, py)
//            case EnumFacing.EAST =>
//              (right - px, bottom - py)
//            case EnumFacing.WEST =>
//              (px, py)
//            case _ => throw new AssertionError("yaw has invalid value")
//          }
//          case _ => throw new AssertionError("pitch has invalid value")
//        }
//        // See which face we're rendering. We can pretty much treat front and
//        // back the same, except with a different texture set. Same goes for
//        // left and right sides, as well as top and bottom sides.
//        localSide match {
//          case EnumFacing.SOUTH | EnumFacing.NORTH =>
//            val (ud, sn, ew) =
//              if (localSide == EnumFacing.SOUTH) (Icons.fud, Icons.fsn, Icons.few)
//              else (Icons.bud, Icons.bsn, Icons.bew)
//            val Array(ht, hm, hb, vt, vm, vb, tl, tm, tr, ml, mm, mr, bl, bm, br) = screen.pitch match {
//              case EnumFacing.NORTH => ud
//              case _ => screen.yaw match {
//                case EnumFacing.SOUTH | EnumFacing.NORTH => sn
//                case EnumFacing.EAST | EnumFacing.WEST => ew
//                case _ => throw new AssertionError("yaw has invalid value")
//              }
//            }
//            if (screen.height == 1) {
//              if (lx == 0) ht
//              else if (lx == right) hb
//              else hm
//            }
//            else if (screen.width == 1) {
//              if (ly == 0) vb
//              else if (ly == bottom) vt
//              else vm
//            }
//            else {
//              if (lx == 0) {
//                if (ly == 0) bl
//                else if (ly == bottom) tl
//                else ml
//              }
//              else if (lx == right) {
//                if (ly == 0) br
//                else if (ly == bottom) tr
//                else mr
//              }
//              else {
//                if (ly == 0) bm
//                else if (ly == bottom) tm
//                else mm
//              }
//            }
//          case EnumFacing.EAST | EnumFacing.WEST =>
//            val (ud, sn, ew) =
//              if (localSide == EnumFacing.EAST) (Icons.sud, Icons.sse, Icons.snw)
//              else (Icons.sud, Icons.snw, Icons.sse)
//            val Array(t, m, b) = screen.pitch match {
//              case EnumFacing.NORTH => ud
//              case _ => screen.yaw match {
//                case EnumFacing.SOUTH | EnumFacing.EAST => sn
//                case EnumFacing.NORTH | EnumFacing.WEST => ew
//                case _ => throw new AssertionError("yaw has invalid value")
//              }
//            }
//            if (screen.height == 1) {
//              Icons.b2
//            }
//            else {
//              if (ly == 0) b
//              else if (ly == bottom) t
//              else m
//            }
//          case EnumFacing.UP | EnumFacing.DOWN =>
//            val (sn, ew) =
//              if (localSide == EnumFacing.UP ^ screen.pitch == EnumFacing.DOWN) (Icons.snw, Icons.sse)
//              else (Icons.sse, Icons.snw)
//            val Array(t, m, b) = screen.pitch match {
//              case EnumFacing.NORTH => screen.yaw match {
//                case EnumFacing.SOUTH => Icons.th
//                case EnumFacing.NORTH => Icons.bh
//                case EnumFacing.EAST => Icons.bv
//                case EnumFacing.WEST => Icons.tv
//                case _ => throw new AssertionError("yaw has invalid value")
//              }
//              case _ => screen.yaw match {
//                case EnumFacing.SOUTH | EnumFacing.WEST => sn
//                case EnumFacing.NORTH | EnumFacing.EAST => ew
//                case _ => throw new AssertionError("yaw has invalid value")
//              }
//            }
//            if (screen.width == 1) {
//              if (screen.pitch == EnumFacing.NORTH) Icons.b
//              else Icons.b2
//            }
//            else {
//              if (lx == 0) b
//              else if (lx == right) t
//              else m
//            }
//          case _ => null
//        }
//      case screen: tileentity.Screen =>
//        val (f, b, t, s) = screen.pitch match {
//          case EnumFacing.NORTH => (Icons.f2, Icons.b2, Icons.b, Icons.b2)
//          case _ => (Icons.f, Icons.b, Icons.b2, Icons.b2)
//        }
//        localSide match {
//          case EnumFacing.SOUTH => f
//          case EnumFacing.NORTH => b
//          case EnumFacing.DOWN | EnumFacing.UP => t
//          case _ => s
//        }
//      case _ => getIcon(localSide, 0)
//    }
//
//  override def getIcon(side: EnumFacing, metadata: Int) =
//    side match {
//      case EnumFacing.SOUTH => Icons.f2
//      case EnumFacing.DOWN | EnumFacing.UP => Icons.b
//      case _ => Icons.b2
//    }
//
//  override def registerBlockIcons(iconRegister: IIconRegister) = {
//    Icons.b = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/b")
//    Icons.b2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/b2")
//    Icons.bbl = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bbl")
//    Icons.bbl2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bbl2")
//    Icons.bbm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bbm")
//    Icons.bbm2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bbm2")
//    Icons.bbr = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bbr")
//    Icons.bbr2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bbr2")
//    Icons.bhb = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bhb")
//    Icons.bhb2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bhb2")
//    Icons.bhm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bhm")
//    Icons.bhm2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bhm2")
//    Icons.bht = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bht")
//    Icons.bht2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bht2")
//    Icons.bml = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bml")
//    Icons.bmm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bmm")
//    Icons.bmr = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bmr")
//    Icons.btl = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/btl")
//    Icons.btm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/btm")
//    Icons.btr = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/btr")
//    Icons.bvb = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bvb")
//    Icons.bvb2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bvb2")
//    Icons.bvm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bvm")
//    Icons.bvt = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/bvt")
//    Icons.f = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/f")
//    Icons.f2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/f2")
//    Icons.fbl = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fbl")
//    Icons.fbl2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fbl2")
//    Icons.fbm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fbm")
//    Icons.fbm2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fbm2")
//    Icons.fbr = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fbr")
//    Icons.fbr2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fbr2")
//    Icons.fhb = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fhb")
//    Icons.fhb2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fhb2")
//    Icons.fhm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fhm")
//    Icons.fhm2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fhm2")
//    Icons.fht = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fht")
//    Icons.fht2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fht2")
//    Icons.fml = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fml")
//    Icons.fmm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fmm")
//    Icons.fmr = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fmr")
//    Icons.ftl = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/ftl")
//    Icons.ftm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/ftm")
//    Icons.ftr = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/ftr")
//    Icons.fvb = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fvb")
//    Icons.fvb2 = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fvb2")
//    Icons.fvm = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fvm")
//    Icons.fvt = iconRegister.getAtlasSprite(Settings.resourceDomain + ":screen/fvt")
//  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(state: IBlockState) = true

  override def createTileEntity(world: World, state: IBlockState) = new tileentity.Screen(tier)

  // ----------------------------------------------------------------------- //

  override def onBlockPlacedBy(world: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack)
    world.getTileEntity(pos) match {
      case screen: tileentity.Screen => screen.delayUntilCheckForMultiBlock = 0
      case _ =>
    }
  }

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = rightClick(world, pos, player, side, hitX, hitY, hitZ, force = false)

  def rightClick(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, force: Boolean) =
    if (Wrench.holdsApplicableWrench(player, pos)) false
    else world.getTileEntity(pos) match {
      case screen: tileentity.Screen if screen.hasKeyboard && (force || player.isSneaking == screen.invertTouchMode) =>
        // Yep, this GUI is actually purely client side. We could skip this
        // if, but it is clearer this way (to trigger it from the server we
        // would have to give screens a "container", which we do not want).
        if (world.isRemote) {
          player.openGui(OpenComputers, GuiType.Screen.id, world, pos.getX, pos.getY, pos.getZ)
        }
        true
      case screen: tileentity.Screen if screen.tier > 0 && side == screen.facing =>
        screen.click(player, hitX, hitY, hitZ)
      case _ => false
    }

  override def onEntityCollidedWithBlock(world: World, pos: BlockPos, entity: Entity) =
    if (world.isRemote) (entity, world.getTileEntity(pos)) match {
      case (arrow: EntityArrow, screen: tileentity.Screen) if screen.tier > 0 =>
        val hitX = math.max(0, math.min(1, arrow.posX - pos.getX))
        val hitY = math.max(0, math.min(1, arrow.posY - pos.getY))
        val hitZ = math.max(0, math.min(1, arrow.posZ - pos.getZ))
        val absX = math.abs(hitX - 0.5)
        val absY = math.abs(hitY - 0.5)
        val absZ = math.abs(hitZ - 0.5)
        val side = if (absX > absY && absX > absZ) {
          if (hitX < 0.5) EnumFacing.WEST
          else EnumFacing.EAST
        }
        else if (absY > absZ) {
          if (hitY < 0.5) EnumFacing.DOWN
          else EnumFacing.UP
        }
        else {
          if (hitZ < 0.5) EnumFacing.NORTH
          else EnumFacing.SOUTH
        }
        if (side == screen.facing) {
          screen.shot(arrow)
        }
      case _ =>
    }
  else world.getTileEntity(pos) match {
    case screen: tileentity.Screen if screen.tier > 0 && screen.facing == EnumFacing.UP => screen.walk(entity)
    case _ =>
  }

  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, pos: BlockPos) =
    world.getTileEntity(pos) match {
      case screen: tileentity.Screen =>
        if (screen.facing == EnumFacing.UP || screen.facing == EnumFacing.DOWN) EnumFacing.values
        else EnumFacing.values.filter {
          d => d != screen.facing && d != screen.facing.getOpposite
        }
      case _ => super.getValidRotations(world, pos)
    }
}
