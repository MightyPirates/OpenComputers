package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.{Config, OpenComputers}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Icon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection
import sun.plugin.dom.exception.InvalidStateException

class ScreenTier1(parent: SimpleDelegator) extends Screen(parent) {
  val unlocalizedName = "ScreenBasic"

  GameRegistry.registerTileEntity(classOf[tileentity.ScreenTier1], "oc.screen0")

  override def getRenderColor = 0x7F7F7F

  override def createTileEntity(world: World) = Some(new tileentity.ScreenTier1)
}

class ScreenTier2(parent: SimpleDelegator) extends Screen(parent) {
  val unlocalizedName = "ScreenAdvanced"

  GameRegistry.registerTileEntity(classOf[tileentity.ScreenTier2], "oc.screen1")

  override def getRenderColor = 0xFFFF66

  override def createTileEntity(world: World) = Some(new tileentity.ScreenTier2)
}

class ScreenTier3(parent: SimpleDelegator) extends Screen(parent) {
  val unlocalizedName = "ScreenProfessional"

  GameRegistry.registerTileEntity(classOf[tileentity.ScreenTier3], "oc.screen2")

  override def getRenderColor = 0x66FFFF

  override def createTileEntity(world: World) = Some(new tileentity.ScreenTier3)
}

abstract class Screen(val parent: SimpleDelegator) extends SimpleDelegate {

  // ----------------------------------------------------------------------- //
  // Rendering stuff
  // ----------------------------------------------------------------------- //

  object Icons {
    var b, b2, bbl, bbl2, bbm, bbm2, bbr, bbr2, bhb, bhb2, bhm, bhm2, bht, bht2, bml, bmm, bmr, btl, btm, btr, bvb, bvb2, bvm, bvt, f, f2, fbl, fbl2, fbm, fbm2, fbr, fbr2, fhb, fhb2, fhm, fhm2, fht, fht2, fml, fmm, fmr, ftl, ftm, ftr, fvb, fvb2, fvm, fvt = null: Icon

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
  override def getBlockTextureFromSide(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection) =
    world.getBlockTileEntity(x, y, z) match {
      case screen: tileentity.Screen if screen.width > 1 || screen.height > 1 =>
        val (w, h) = (screen.width, screen.height)
        // Don't ask. Seriously. Just... don't.
        val (lx, ly) =
          screen.pitch match {
            case ForgeDirection.NORTH => screen.localPosition
            case ForgeDirection.UP => screen.yaw match {
              case ForgeDirection.SOUTH =>
                screen.localPosition
              case ForgeDirection.NORTH =>
                val (x, y) = screen.localPosition
                (w - x - 1, h - y - 1)
              case ForgeDirection.EAST =>
                val (x, y) = screen.localPosition
                (w - x - 1, y)
              case ForgeDirection.WEST =>
                val (x, y) = screen.localPosition
                (x, h - y - 1)
              case _ => throw new InvalidStateException("yaw has invalid value")
            }
            case ForgeDirection.DOWN => screen.yaw match {
              case ForgeDirection.SOUTH =>
                val (x, y) = screen.localPosition
                (x, h - y - 1)
              case ForgeDirection.NORTH =>
                val (x, y) = screen.localPosition
                (w - x - 1, y)
              case ForgeDirection.EAST =>
                val (x, y) = screen.localPosition
                (w - x - 1, h - y - 1)
              case ForgeDirection.WEST =>
                screen.localPosition
              case _ => throw new InvalidStateException("yaw has invalid value")
            }
            case _ => throw new InvalidStateException("pitch has invalid value")
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
                case _ => throw new InvalidStateException("yaw has invalid value")
              }
            }
            if (h == 1) {
              if (lx == 0) Some(ht)
              else if (lx == w - 1) Some(hb)
              else Some(hm)
            }
            else if (w == 1) {
              if (ly == 0) Some(vb)
              else if (ly == h - 1) Some(vt)
              else Some(vm)
            }
            else {
              if (lx == 0) {
                if (ly == 0) Some(bl)
                else if (ly == h - 1) Some(tl)
                else Some(ml)
              }
              else if (lx == w - 1) {
                if (ly == 0) Some(br)
                else if (ly == h - 1) Some(tr)
                else Some(mr)
              }
              else {
                if (ly == 0) Some(bm)
                else if (ly == h - 1) Some(tm)
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
                case _ => throw new InvalidStateException("yaw has invalid value")
              }
            }
            if (h == 1) {
              Some(Icons.b2)
            }
            else {
              if (ly == 0) Some(b)
              else if (ly == h - 1) Some(t)
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
                case _ => throw new InvalidStateException("yaw has invalid value")
              }
              case _ => screen.yaw match {
                case ForgeDirection.SOUTH | ForgeDirection.WEST => sn
                case ForgeDirection.NORTH | ForgeDirection.EAST => ew
                case _ => throw new InvalidStateException("yaw has invalid value")
              }
            }
            if (w == 1) {
              if (screen.pitch == ForgeDirection.NORTH) Some(Icons.b)
              else Some(Icons.b2)
            }
            else {
              if (lx == 0) Some(b)
              else if (lx == w - 1) Some(t)
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

  override def registerIcons(iconRegister: IconRegister) = {
    Icons.b = iconRegister.registerIcon(Config.resourceDomain + ":screen/b")
    Icons.b2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/b2")
    Icons.bbl = iconRegister.registerIcon(Config.resourceDomain + ":screen/bbl")
    Icons.bbl2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/bbl2")
    Icons.bbm = iconRegister.registerIcon(Config.resourceDomain + ":screen/bbm")
    Icons.bbm2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/bbm2")
    Icons.bbr = iconRegister.registerIcon(Config.resourceDomain + ":screen/bbr")
    Icons.bbr2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/bbr2")
    Icons.bhb = iconRegister.registerIcon(Config.resourceDomain + ":screen/bhb")
    Icons.bhb2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/bhb2")
    Icons.bhm = iconRegister.registerIcon(Config.resourceDomain + ":screen/bhm")
    Icons.bhm2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/bhm2")
    Icons.bht = iconRegister.registerIcon(Config.resourceDomain + ":screen/bht")
    Icons.bht2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/bht2")
    Icons.bml = iconRegister.registerIcon(Config.resourceDomain + ":screen/bml")
    Icons.bmm = iconRegister.registerIcon(Config.resourceDomain + ":screen/bmm")
    Icons.bmr = iconRegister.registerIcon(Config.resourceDomain + ":screen/bmr")
    Icons.btl = iconRegister.registerIcon(Config.resourceDomain + ":screen/btl")
    Icons.btm = iconRegister.registerIcon(Config.resourceDomain + ":screen/btm")
    Icons.btr = iconRegister.registerIcon(Config.resourceDomain + ":screen/btr")
    Icons.bvb = iconRegister.registerIcon(Config.resourceDomain + ":screen/bvb")
    Icons.bvb2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/bvb2")
    Icons.bvm = iconRegister.registerIcon(Config.resourceDomain + ":screen/bvm")
    Icons.bvt = iconRegister.registerIcon(Config.resourceDomain + ":screen/bvt")
    Icons.f = iconRegister.registerIcon(Config.resourceDomain + ":screen/f")
    Icons.f2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/f2")
    Icons.fbl = iconRegister.registerIcon(Config.resourceDomain + ":screen/fbl")
    Icons.fbl2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/fbl2")
    Icons.fbm = iconRegister.registerIcon(Config.resourceDomain + ":screen/fbm")
    Icons.fbm2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/fbm2")
    Icons.fbr = iconRegister.registerIcon(Config.resourceDomain + ":screen/fbr")
    Icons.fbr2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/fbr2")
    Icons.fhb = iconRegister.registerIcon(Config.resourceDomain + ":screen/fhb")
    Icons.fhb2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/fhb2")
    Icons.fhm = iconRegister.registerIcon(Config.resourceDomain + ":screen/fhm")
    Icons.fhm2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/fhm2")
    Icons.fht = iconRegister.registerIcon(Config.resourceDomain + ":screen/fht")
    Icons.fht2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/fht2")
    Icons.fml = iconRegister.registerIcon(Config.resourceDomain + ":screen/fml")
    Icons.fmm = iconRegister.registerIcon(Config.resourceDomain + ":screen/fmm")
    Icons.fmr = iconRegister.registerIcon(Config.resourceDomain + ":screen/fmr")
    Icons.ftl = iconRegister.registerIcon(Config.resourceDomain + ":screen/ftl")
    Icons.ftm = iconRegister.registerIcon(Config.resourceDomain + ":screen/ftm")
    Icons.ftr = iconRegister.registerIcon(Config.resourceDomain + ":screen/ftr")
    Icons.fvb = iconRegister.registerIcon(Config.resourceDomain + ":screen/fvb")
    Icons.fvb2 = iconRegister.registerIcon(Config.resourceDomain + ":screen/fvb2")
    Icons.fvm = iconRegister.registerIcon(Config.resourceDomain + ":screen/fvm")
    Icons.fvt = iconRegister.registerIcon(Config.resourceDomain + ":screen/fvt")
  }

  override def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int) = 5

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  // ----------------------------------------------------------------------- //
  // Interaction
  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) =
    if (!player.isSneaking) {
      player.openGui(OpenComputers, GuiType.Screen.id, world, x, y, z)
      true
    }
    else false

  // ----------------------------------------------------------------------- //
  // Block rotation
  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) = validRotations

  private val validRotations = Array(
    ForgeDirection.DOWN,
    ForgeDirection.UP,
    ForgeDirection.SOUTH,
    ForgeDirection.WEST,
    ForgeDirection.NORTH,
    ForgeDirection.EAST)
}