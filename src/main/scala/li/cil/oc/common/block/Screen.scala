package li.cil.oc.common.block

import java.util
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.util.mods.BuildCraft
import li.cil.oc.util.{PackedColor, Tooltip}
import li.cil.oc.{Settings, OpenComputers}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import scala.Array

abstract class Screen(val parent: SimpleDelegator) extends RedstoneAware with SimpleDelegate {
  val unlocalizedName = "Screen" + tier

  def tier: Int

  override def rarity = Array(EnumRarity.common, EnumRarity.uncommon, EnumRarity.rare).apply(tier)

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    val (w, h) = Settings.screenResolutionsByTier(tier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(tier))
    tooltip.addAll(Tooltip.get("Screen", w, h, depth))
  }

  object Icons {
    var b, b2, bbl, bbl2, bbm, bbm2, bbr, bbr2, bhb, bhb2, bhm, bhm2, bht, bht2, bml, bmm, bmr, btl, btm, btr, bvb, bvb2, bvm, bvt, f, f2, fbl, fbl2, fbm, fbm2, fbr, fbr2, fhb, fhb2, fhm, fhm2, fht, fht2, fml, fmm, fmr, ftl, ftm, ftr, fvb, fvb2, fvm, fvt = null: IIcon

    lazy val fh = Array(fht, fhm, fhb)
    lazy val fv = Array(fvt, fvm, fvb)
    lazy val bh = Array(bht, bhm, bhb)
    lazy val bv = Array(bvt, bvm, bvb)

    lazy val fth = Array(ftl, ftm, ftr)
    lazy val fmh = Array(fml, fmm, fmr)
    lazy val fbh = Array(fbl, fbm, fbr)
    lazy val bth = Array(btl, btm, btr)
    lazy val bmh = Array(bml, bmm, bmr)
    lazy val bbh = Array(bbl, bbm, bbr)

    lazy val ftv = Array(ftl, fml, fbl)
    lazy val fmv = Array(ftm, fmm, fbm)
    lazy val fbv = Array(ftr, fmr, fbr)
    lazy val btv = Array(btl, bml, bbl)
    lazy val bmv = Array(btm, bmm, bbm)
    lazy val bbv = Array(btr, bmr, bbr)

    lazy val fh2 = Array(fht2, fhm2, fhb2)
    lazy val fv2 = Array(fvt, fvm, fvb2)
    lazy val bh2 = Array(bht2, bhm2, bhb2)
    lazy val bv2 = Array(bvt, bvm, bvb2)
    lazy val fbh2 = Array(fbl2, fbm2, fbr2)
    lazy val bbh2 = Array(bbl2, bbm2, bbr2)

    lazy val fud = Icons.fh2 ++ Icons.fv2 ++ Icons.fth ++ Icons.fmh ++ Icons.fbh2
    lazy val bud = Icons.bh2.reverse ++ Icons.bv2 ++ Icons.bth.reverse ++ Icons.bmh.reverse ++ Icons.bbh2.reverse
    lazy val fsn = Icons.fh ++ Icons.fv ++ Icons.fth ++ Icons.fmh ++ Icons.fbh
    lazy val few = Icons.fv ++ Icons.fh ++ Icons.ftv ++ Icons.fmv ++ Icons.fbv
    lazy val bsn = Icons.bh ++ Icons.bv ++ Icons.bth ++ Icons.bmh ++ Icons.bbh
    lazy val bew = Icons.bv ++ Icons.bh ++ Icons.btv ++ Icons.bmv ++ Icons.bbv

    lazy val sud = Array(Icons.bvt, Icons.bvm, Icons.bvb2)
    lazy val sse = Array(Icons.bhb2, Icons.bhm2, Icons.bht2)
    lazy val snw = Array(Icons.bht2, Icons.bhm2, Icons.bhb2)

    lazy val th = Array(Icons.bhb, Icons.bhm, Icons.bht)
    lazy val tv = Array(Icons.bvb, Icons.bvm, Icons.bvt)
  }

  // This an ugly monstrosity, but it's still better than having to manually
  // compute ambient occlusion in a custom block renderer to keep the lighting
  // pretty... which would be even more grotesque.
  override def icon(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case screen: tileentity.Screen if screen.width > 1 || screen.height > 1 =>
        val right = screen.width - 1
        val bottom = screen.height - 1
        val (px, py) = screen.localPosition
        val (lx, ly) = screen.pitch match {
          case ForgeDirection.NORTH => (px, py)
          case ForgeDirection.UP => screen.yaw match {
            case ForgeDirection.SOUTH =>
              (px, py)
            case ForgeDirection.NORTH =>
              (right - px, bottom - py)
            case ForgeDirection.EAST =>
              (right - px, py)
            case ForgeDirection.WEST =>
              (px, bottom - py)
            case _ => throw new AssertionError("yaw has invalid value")
          }
          case ForgeDirection.DOWN => screen.yaw match {
            case ForgeDirection.SOUTH =>
              (px, bottom - py)
            case ForgeDirection.NORTH =>
              (right - px, py)
            case ForgeDirection.EAST =>
              (right - px, bottom - py)
            case ForgeDirection.WEST =>
              (px, py)
            case _ => throw new AssertionError("yaw has invalid value")
          }
          case _ => throw new AssertionError("pitch has invalid value")
        }
        // See which face we're rendering. We can pretty much treat front and
        // back the same, except with a different texture set. Same goes for
        // left and right sides, as well as top and bottom sides.
        localSide match {
          case ForgeDirection.SOUTH | ForgeDirection.NORTH =>
            val (ud, sn, ew) =
              if (localSide == ForgeDirection.SOUTH) (Icons.fud, Icons.fsn, Icons.few)
              else (Icons.bud, Icons.bsn, Icons.bew)
            val Array(ht, hm, hb, vt, vm, vb, tl, tm, tr, ml, mm, mr, bl, bm, br) = screen.pitch match {
              case ForgeDirection.NORTH => ud
              case _ => screen.yaw match {
                case ForgeDirection.SOUTH | ForgeDirection.NORTH => sn
                case ForgeDirection.EAST | ForgeDirection.WEST => ew
                case _ => throw new AssertionError("yaw has invalid value")
              }
            }
            if (screen.height == 1) {
              if (lx == 0) Some(ht)
              else if (lx == right) Some(hb)
              else Some(hm)
            }
            else if (screen.width == 1) {
              if (ly == 0) Some(vb)
              else if (ly == bottom) Some(vt)
              else Some(vm)
            }
            else {
              if (lx == 0) {
                if (ly == 0) Some(bl)
                else if (ly == bottom) Some(tl)
                else Some(ml)
              }
              else if (lx == right) {
                if (ly == 0) Some(br)
                else if (ly == bottom) Some(tr)
                else Some(mr)
              }
              else {
                if (ly == 0) Some(bm)
                else if (ly == bottom) Some(tm)
                else Some(mm)
              }
            }
          case ForgeDirection.EAST | ForgeDirection.WEST =>
            val (ud, sn, ew) =
              if (localSide == ForgeDirection.EAST) (Icons.sud, Icons.sse, Icons.snw)
              else (Icons.sud, Icons.snw, Icons.sse)
            val Array(t, m, b) = screen.pitch match {
              case ForgeDirection.NORTH => ud
              case _ => screen.yaw match {
                case ForgeDirection.SOUTH | ForgeDirection.EAST => sn
                case ForgeDirection.NORTH | ForgeDirection.WEST => ew
                case _ => throw new AssertionError("yaw has invalid value")
              }
            }
            if (screen.height == 1) {
              Some(Icons.b2)
            }
            else {
              if (ly == 0) Some(b)
              else if (ly == bottom) Some(t)
              else Some(m)
            }
          case ForgeDirection.UP | ForgeDirection.DOWN =>
            val (sn, ew) =
              if (localSide == ForgeDirection.UP ^ screen.pitch == ForgeDirection.DOWN) (Icons.snw, Icons.sse)
              else (Icons.sse, Icons.snw)
            val Array(t, m, b) = screen.pitch match {
              case ForgeDirection.NORTH => screen.yaw match {
                case ForgeDirection.SOUTH => Icons.th
                case ForgeDirection.NORTH => Icons.bh
                case ForgeDirection.EAST => Icons.bv
                case ForgeDirection.WEST => Icons.tv
                case _ => throw new AssertionError("yaw has invalid value")
              }
              case _ => screen.yaw match {
                case ForgeDirection.SOUTH | ForgeDirection.WEST => sn
                case ForgeDirection.NORTH | ForgeDirection.EAST => ew
                case _ => throw new AssertionError("yaw has invalid value")
              }
            }
            if (screen.width == 1) {
              if (screen.pitch == ForgeDirection.NORTH) Some(Icons.b)
              else Some(Icons.b2)
            }
            else {
              if (lx == 0) Some(b)
              else if (lx == right) Some(t)
              else Some(m)
            }
          case _ => None
        }
      case screen: tileentity.Screen =>
        val (f, b, t, s) = screen.pitch match {
          case ForgeDirection.NORTH => (Icons.f2, Icons.b2, Icons.b, Icons.b2)
          case _ => (Icons.f, Icons.b, Icons.b2, Icons.b2)
        }
        localSide match {
          case ForgeDirection.SOUTH => Some(f)
          case ForgeDirection.NORTH => Some(b)
          case ForgeDirection.DOWN | ForgeDirection.UP => Some(t)
          case _ => Some(s)
        }
      case _ => icon(localSide)
    }

  override def icon(side: ForgeDirection) =
    Some(side match {
      case ForgeDirection.SOUTH => Icons.f2
      case ForgeDirection.DOWN | ForgeDirection.UP => Icons.b
      case _ => Icons.b2
    })

  override def registerIcons(iconRegister: IIconRegister) = {
    Icons.b = iconRegister.registerIcon(Settings.resourceDomain + ":screen/b")
    Icons.b2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/b2")
    Icons.bbl = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bbl")
    Icons.bbl2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bbl2")
    Icons.bbm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bbm")
    Icons.bbm2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bbm2")
    Icons.bbr = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bbr")
    Icons.bbr2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bbr2")
    Icons.bhb = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bhb")
    Icons.bhb2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bhb2")
    Icons.bhm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bhm")
    Icons.bhm2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bhm2")
    Icons.bht = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bht")
    Icons.bht2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bht2")
    Icons.bml = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bml")
    Icons.bmm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bmm")
    Icons.bmr = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bmr")
    Icons.btl = iconRegister.registerIcon(Settings.resourceDomain + ":screen/btl")
    Icons.btm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/btm")
    Icons.btr = iconRegister.registerIcon(Settings.resourceDomain + ":screen/btr")
    Icons.bvb = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bvb")
    Icons.bvb2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bvb2")
    Icons.bvm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bvm")
    Icons.bvt = iconRegister.registerIcon(Settings.resourceDomain + ":screen/bvt")
    Icons.f = iconRegister.registerIcon(Settings.resourceDomain + ":screen/f")
    Icons.f2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/f2")
    Icons.fbl = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fbl")
    Icons.fbl2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fbl2")
    Icons.fbm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fbm")
    Icons.fbm2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fbm2")
    Icons.fbr = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fbr")
    Icons.fbr2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fbr2")
    Icons.fhb = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fhb")
    Icons.fhb2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fhb2")
    Icons.fhm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fhm")
    Icons.fhm2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fhm2")
    Icons.fht = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fht")
    Icons.fht2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fht2")
    Icons.fml = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fml")
    Icons.fmm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fmm")
    Icons.fmr = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fmr")
    Icons.ftl = iconRegister.registerIcon(Settings.resourceDomain + ":screen/ftl")
    Icons.ftm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/ftm")
    Icons.ftr = iconRegister.registerIcon(Settings.resourceDomain + ":screen/ftr")
    Icons.fvb = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fvb")
    Icons.fvb2 = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fvb2")
    Icons.fvm = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fvm")
    Icons.fvt = iconRegister.registerIcon(Settings.resourceDomain + ":screen/fvt")
  }

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 5

  // ----------------------------------------------------------------------- //

  override def drops(world: World, x: Int, y: Int, z: Int, fortune: Int) = {
    // Always drop the new screen block (with proper redstone support).
    val list = new java.util.ArrayList[ItemStack]()
    list.add(createItemStack())
    Some(list)
  }

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Screen(tier))

  // ----------------------------------------------------------------------- //

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                          side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) =
    if (!player.isSneaking && !BuildCraft.holdsApplicableWrench(player, x, y, z)) {
      world.getTileEntity(x, y, z) match {
        case screen: tileentity.Screen if screen.hasKeyboard =>
          // Yep, this GUI is actually purely client side. We could skip this
          // if, but it is clearer this way (to trigger it from the server we
          // would have to give screens a "container", which we do not want).
          if (world.isRemote) {
            player.openGui(OpenComputers, GuiType.Screen.id, world, x, y, z)
          }
          true
        case screen: tileentity.Screen if screen.tier > 0 && side == screen.facing =>
          screen.click(player, hitX, hitY, hitZ)
        case _ => false
      }
    }
    else false

  override def walk(world: World, x: Int, y: Int, z: Int, entity: Entity) =
    if (!world.isRemote) world.getTileEntity(x, y, z) match {
      case screen: tileentity.Screen if screen.tier > 0 && screen.facing == ForgeDirection.UP => screen.walk(entity)
      case _ =>
    }

  override def collide(world: World, x: Int, y: Int, z: Int, entity: Entity) =
    if (!world.isRemote) (entity, world.getTileEntity(x, y, z)) match {
      case (arrow: EntityArrow, screen: tileentity.Screen) if screen.tier > 0 =>
        val hitX = math.max(0, math.min(1, arrow.posX - x))
        val hitY = math.max(0, math.min(1, arrow.posY - y))
        val hitZ = math.max(0, math.min(1, arrow.posZ - z))
        val absX = math.abs(hitX - 0.5)
        val absY = math.abs(hitY - 0.5)
        val absZ = math.abs(hitZ - 0.5)
        val side = if (absX > absY && absX > absZ) {
          if (hitX < 0.5) ForgeDirection.WEST
          else ForgeDirection.EAST
        }
        else if (absY > absZ) {
          if (hitY < 0.5) ForgeDirection.DOWN
          else ForgeDirection.UP
        }
        else {
          if (hitZ < 0.5) ForgeDirection.NORTH
          else ForgeDirection.SOUTH
        }
        if (side == screen.facing) {
          screen.shot(arrow, hitX, hitY, hitZ)
        }
      case _ =>
    }

  // ----------------------------------------------------------------------- //

  override def validRotations(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case screen: tileentity.Screen =>
        if (screen.facing == ForgeDirection.UP || screen.facing == ForgeDirection.DOWN) ForgeDirection.VALID_DIRECTIONS
        else ForgeDirection.VALID_DIRECTIONS.filter {
          d => d != screen.facing && d != screen.facing.getOpposite
        }
      case _ => super.validRotations(world, x, y, z)
    }
}

object Screen {

  class Tier1(parent: SimpleDelegator) extends Screen(parent) {
    def tier = 0

    override def color = 0x7F7F7F
  }

  class Tier2(parent: SimpleDelegator) extends Screen(parent) {
    def tier = 1

    override def color = 0xFFFF66
  }

  class Tier3(parent: SimpleDelegator) extends Screen(parent) {
    def tier = 2

    override def color = 0x66FFFF
  }

}
