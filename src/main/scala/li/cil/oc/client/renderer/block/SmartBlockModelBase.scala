package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model._
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import org.lwjgl.util.vector.Vector3f

trait SmartBlockModelBase extends IBakedModel {
  override def getOverrides: ItemOverrideList = ItemOverrideList.NONE

  override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] = Collections.emptyList()

  override def isAmbientOcclusion = true

  override def isGui3d = true

  override def isBuiltInRenderer = false

  // Note: we don't care about the actual texture here, we just need the block
  // texture atlas. So any of our textures we know is loaded into it will do.
  override def getParticleTexture = Textures.getSprite(Textures.Block.GenericTop)

  override def getItemCameraTransforms = DefaultBlockCameraTransforms

  protected final val DefaultBlockCameraTransforms = {
    val gui = new ItemTransformVec3f(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f))
    val ground = new ItemTransformVec3f(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0), new Vector3f(0.25f, 0.25f, 0.25f))
    val fixed = new ItemTransformVec3f(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f))
    val thirdperson_righthand = new ItemTransformVec3f(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f, 0.375f, 0.375f))
    val firstperson_righthand = new ItemTransformVec3f(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.40f, 0.40f, 0.40f))
    val firstperson_lefthand = new ItemTransformVec3f(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.40f, 0.40f, 0.40f))

    // scale(0.0625f): see ItemTransformVec3f.Deserializer.deserialize.
    gui.translation.scale(0.0625f)
    ground.translation.scale(0.0625f)
    fixed.translation.scale(0.0625f)
    thirdperson_righthand.translation.scale(0.0625f)
    firstperson_righthand.translation.scale(0.0625f)
    firstperson_lefthand.translation.scale(0.0625f)

    new ItemCameraTransforms(
      ItemTransformVec3f.DEFAULT,
      thirdperson_righthand,
      firstperson_lefthand,
      firstperson_righthand,
      ItemTransformVec3f.DEFAULT,
      gui,
      ground,
      fixed)
  }

  protected def missingModel = Minecraft.getMinecraft.getRenderItem.getItemModelMesher.getModelManager.getMissingModel

  // Standard faces for a unit cube.
  protected final val UnitCube = Array(
    Array(new Vec3d(0, 0, 1), new Vec3d(0, 0, 0), new Vec3d(1, 0, 0), new Vec3d(1, 0, 1)),
    Array(new Vec3d(0, 1, 0), new Vec3d(0, 1, 1), new Vec3d(1, 1, 1), new Vec3d(1, 1, 0)),
    Array(new Vec3d(1, 1, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 0), new Vec3d(0, 1, 0)),
    Array(new Vec3d(0, 1, 1), new Vec3d(0, 0, 1), new Vec3d(1, 0, 1), new Vec3d(1, 1, 1)),
    Array(new Vec3d(0, 1, 0), new Vec3d(0, 0, 0), new Vec3d(0, 0, 1), new Vec3d(0, 1, 1)),
    Array(new Vec3d(1, 1, 1), new Vec3d(1, 0, 1), new Vec3d(1, 0, 0), new Vec3d(1, 1, 0))
  )

  // Planes perpendicular to facings. Negative values mean we mirror along that,
  // axis which is done to mirror back faces and the y axis (because up is
  // positive but for our texture coordinates down is positive).
  protected final val Planes = Array(
    (new Vec3d(1, 0, 0), new Vec3d(0, 0, -1)),
    (new Vec3d(1, 0, 0), new Vec3d(0, 0, 1)),
    (new Vec3d(-1, 0, 0), new Vec3d(0, -1, 0)),
    (new Vec3d(1, 0, 0), new Vec3d(0, -1, 0)),
    (new Vec3d(0, 0, 1), new Vec3d(0, -1, 0)),
    (new Vec3d(0, 0, -1), new Vec3d(0, -1, 0))
  )

  protected final val White = 0xFFFFFF

  /**
    * Generates a list of arrays, each containing the four vertices making up a
    * face of the box with the specified size.
    */
  protected def makeBox(from: Vec3d, to: Vec3d) = {
    val minX = math.min(from.x, to.x)
    val minY = math.min(from.y, to.y)
    val minZ = math.min(from.z, to.z)
    val maxX = math.max(from.x, to.x)
    val maxY = math.max(from.y, to.y)
    val maxZ = math.max(from.z, to.z)
    UnitCube.map(face => face.map(vertex => new Vec3d(
      math.max(minX, math.min(maxX, vertex.x)),
      math.max(minY, math.min(maxY, vertex.y)),
      math.max(minZ, math.min(maxZ, vertex.z)))))
  }

  protected def rotateVector(v: Vec3d, angle: Double, axis: Vec3d) = {
    // vrot = v * cos(angle) + (axis x v) * sin(angle) + axis * (axis dot v)(1 - cos(angle))
    def scale(v: Vec3d, s: Double) = new Vec3d(v.x * s, v.y * s, v.z * s)
    val cosAngle = math.cos(angle)
    val sinAngle = math.sin(angle)
    scale(v, cosAngle).
      add(scale(axis.crossProduct(v), sinAngle)).
      add(scale(axis, axis.dotProduct(v) * (1 - cosAngle)))
  }

  protected def rotateFace(face: Array[Vec3d], angle: Double, axis: Vec3d, around: Vec3d = new Vec3d(0.5, 0.5, 0.5)) = {
    face.map(v => rotateVector(v.subtract(around), angle, axis).add(around))
  }

  protected def rotateBox(box: Array[Array[Vec3d]], angle: Double, axis: Vec3d = new Vec3d(0, 1, 0), around: Vec3d = new Vec3d(0.5, 0.5, 0.5)) = {
    box.map(face => rotateFace(face, angle, axis, around))
  }

  /**
    * Create the BakedQuads for a set of quads defined by the specified vertices.
    * <p/>
    * Usually used to generate the quads for a cube previously generated using makeBox().
    */
  protected def bakeQuads(box: Array[Array[Vec3d]], texture: Array[TextureAtlasSprite], color: Option[Int]): Array[BakedQuad] = {
    val colorRGB = color.getOrElse(White)
    bakeQuads(box, texture, colorRGB)
  }

  /**
    * Create the BakedQuads for a set of quads defined by the specified vertices.
    * <p/>
    * Usually used to generate the quads for a cube previously generated using makeBox().
    */
  protected def bakeQuads(box: Array[Array[Vec3d]], texture: Array[TextureAtlasSprite], colorRGB: Int): Array[BakedQuad] = {
    EnumFacing.values.map(side => {
      val vertices = box(side.getIndex)
      val data = quadData(vertices, side, texture(side.getIndex), colorRGB, 0)
      new BakedQuad(data, -1, side, texture(side.getIndex), true, DefaultVertexFormats.ITEM)
    })
  }

  /**
    * Create a single BakedQuad of a unit cube's specified side.
    */
  protected def bakeQuad(side: EnumFacing, texture: TextureAtlasSprite, color: Option[Int], rotation: Int) = {
    val colorRGB = color.getOrElse(White)
    val vertices = UnitCube(side.getIndex)
    val data = quadData(vertices, side, texture, colorRGB, rotation)
    new BakedQuad(data, -1, side, texture, true, DefaultVertexFormats.ITEM)
  }

  // Generate raw data used for a BakedQuad based on the specified facing, vertices, texture and rotation.
  // The UV coordinates are generated from the positions of the vertices, i.e. they are simply cube-
  // mapped. This is good enough for us.
  protected def quadData(vertices: Array[Vec3d], facing: EnumFacing, texture: TextureAtlasSprite, colorRGB: Int, rotation: Int): Array[Int] = {
    val (uAxis, vAxis) = Planes(facing.getIndex)
    val rot = (rotation + 4) % 4
    vertices.flatMap(vertex => {
      var u = vertex.dotProduct(uAxis)
      var v = vertex.dotProduct(vAxis)
      if (uAxis.x + uAxis.y + uAxis.z < 0) u = 1 + u
      if (vAxis.x + vAxis.y + vAxis.z < 0) v = 1 + v
      for (i <- 0 until rot) {
        // (u, v) = (v, -u)
        val tmp = u
        u = v
        v = (-(tmp - 0.5)) + 0.5
      }
      rawData(vertex.x, vertex.y, vertex.z, facing, texture, texture.getInterpolatedU(u * 16), texture.getInterpolatedV(v * 16), colorRGB)
    })
  }

  // See FaceBakery#storeVertexData.
  protected def rawData(x: Double, y: Double, z: Double, face: EnumFacing, texture: TextureAtlasSprite, u: Float, v: Float, colorRGB: Int) = {
    val vx = (face.getFrontOffsetX * 127) & 0xFF
    val vy = (face.getFrontOffsetY * 127) & 0xFF
    val vz = (face.getFrontOffsetZ * 127) & 0xFF

    Array(
      java.lang.Float.floatToRawIntBits(x.toFloat),
      java.lang.Float.floatToRawIntBits(y.toFloat),
      java.lang.Float.floatToRawIntBits(z.toFloat),
      getFaceShadeColor(face, colorRGB),
      java.lang.Float.floatToRawIntBits(u),
      java.lang.Float.floatToRawIntBits(v),
      vx | (vy << 0x08) | (vz << 0x10)
    )
  }

  // See FaceBakery.
  protected def getFaceShadeColor(face: EnumFacing, colorRGB: Int): Int = {
    val brightness = getFaceBrightness(face)
    val b = (colorRGB >> 16) & 0xFF
    val g = (colorRGB >> 8) & 0xFF
    val r = colorRGB & 0xFF
    0xFF000000 | shade(r, brightness) << 16 | shade(g, brightness) << 8 | shade(b, brightness)
  }

  private def shade(value: Int, brightness: Float) = (brightness * value).toInt max 0 min 255

  protected def getFaceBrightness(face: EnumFacing): Float = {
    /*face match {
      case EnumFacing.DOWN => 0.5f
      case EnumFacing.UP => 1.0f
      case EnumFacing.NORTH | EnumFacing.SOUTH => 0.8f
      case EnumFacing.WEST | EnumFacing.EAST => 0.6f
    }*/

    //minecraft already applies tint based on quad's facing, so there is no need to apply it second time
    1.0f
  }
}
