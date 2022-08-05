package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import net.minecraft.block.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.model._
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.math.vector.Vector3f

trait SmartBlockModelBase extends IBakedModel {
  override def getOverrides: ItemOverrideList = ItemOverrideList.EMPTY

  @Deprecated
  override def getQuads(state: BlockState, side: Direction, rand: util.Random): util.List[BakedQuad] = Collections.emptyList()

  override def useAmbientOcclusion = true

  override def isGui3d = true

  override def usesBlockLight = true

  override def isCustomRenderer = false

  // Note: we don't care about the actual texture here, we just need the block
  // texture atlas. So any of our textures we know is loaded into it will do.
  @Deprecated
  override def getParticleIcon = Textures.getSprite(Textures.Block.GenericTop)

  @Deprecated
  override def getTransforms = DefaultBlockCameraTransforms

  @Deprecated
  protected final val DefaultBlockCameraTransforms = {
    val gui = new ItemTransformVec3f(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f))
    val ground = new ItemTransformVec3f(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0), new Vector3f(0.25f, 0.25f, 0.25f))
    val fixed = new ItemTransformVec3f(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f))
    val thirdperson_righthand = new ItemTransformVec3f(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0), new Vector3f(0.375f, 0.375f, 0.375f))
    val firstperson_righthand = new ItemTransformVec3f(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.40f, 0.40f, 0.40f))
    val firstperson_lefthand = new ItemTransformVec3f(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.40f, 0.40f, 0.40f))

    // scale(0.0625f): see ItemTransformVec3f.Deserializer.deserialize.
    gui.translation.mul(0.0625f)
    ground.translation.mul(0.0625f)
    fixed.translation.mul(0.0625f)
    thirdperson_righthand.translation.mul(0.0625f)
    firstperson_righthand.translation.mul(0.0625f)
    firstperson_lefthand.translation.mul(0.0625f)

    new ItemCameraTransforms(
      ItemTransformVec3f.NO_TRANSFORM,
      thirdperson_righthand,
      firstperson_lefthand,
      firstperson_righthand,
      ItemTransformVec3f.NO_TRANSFORM,
      gui,
      ground,
      fixed)
  }

  protected def missingModel = Minecraft.getInstance.getModelManager.getMissingModel

  // Standard faces for a unit cube.
  protected final val UnitCube = Array(
    Array(new Vector3d(0, 0, 1), new Vector3d(0, 0, 0), new Vector3d(1, 0, 0), new Vector3d(1, 0, 1)),
    Array(new Vector3d(0, 1, 0), new Vector3d(0, 1, 1), new Vector3d(1, 1, 1), new Vector3d(1, 1, 0)),
    Array(new Vector3d(1, 1, 0), new Vector3d(1, 0, 0), new Vector3d(0, 0, 0), new Vector3d(0, 1, 0)),
    Array(new Vector3d(0, 1, 1), new Vector3d(0, 0, 1), new Vector3d(1, 0, 1), new Vector3d(1, 1, 1)),
    Array(new Vector3d(0, 1, 0), new Vector3d(0, 0, 0), new Vector3d(0, 0, 1), new Vector3d(0, 1, 1)),
    Array(new Vector3d(1, 1, 1), new Vector3d(1, 0, 1), new Vector3d(1, 0, 0), new Vector3d(1, 1, 0))
  )

  // Planes perpendicular to facings. Negative values mean we mirror along that,
  // axis which is done to mirror back faces and the y axis (because up is
  // positive but for our texture coordinates down is positive).
  protected final val Planes = Array(
    (new Vector3d(1, 0, 0), new Vector3d(0, 0, -1)),
    (new Vector3d(1, 0, 0), new Vector3d(0, 0, 1)),
    (new Vector3d(-1, 0, 0), new Vector3d(0, -1, 0)),
    (new Vector3d(1, 0, 0), new Vector3d(0, -1, 0)),
    (new Vector3d(0, 0, 1), new Vector3d(0, -1, 0)),
    (new Vector3d(0, 0, -1), new Vector3d(0, -1, 0))
  )

  protected final val White = 0xFFFFFF

  /**
    * Generates a list of arrays, each containing the four vertices making up a
    * face of the box with the specified size.
    */
  protected def makeBox(from: Vector3d, to: Vector3d) = {
    val minX = math.min(from.x, to.x)
    val minY = math.min(from.y, to.y)
    val minZ = math.min(from.z, to.z)
    val maxX = math.max(from.x, to.x)
    val maxY = math.max(from.y, to.y)
    val maxZ = math.max(from.z, to.z)
    UnitCube.map(face => face.map(vertex => new Vector3d(
      math.max(minX, math.min(maxX, vertex.x)),
      math.max(minY, math.min(maxY, vertex.y)),
      math.max(minZ, math.min(maxZ, vertex.z)))))
  }

  protected def rotateVector(v: Vector3d, angle: Double, axis: Vector3d) = {
    // vrot = v * cos(angle) + (axis x v) * sin(angle) + axis * (axis dot v)(1 - cos(angle))
    def scale(v: Vector3d, s: Double) = v.scale(s)
    val cosAngle = math.cos(angle)
    val sinAngle = math.sin(angle)
    scale(v, cosAngle).
      add(scale(axis.cross(v), sinAngle)).
      add(scale(axis, axis.dot(v) * (1 - cosAngle)))
  }

  protected def rotateFace(face: Array[Vector3d], angle: Double, axis: Vector3d, around: Vector3d = new Vector3d(0.5, 0.5, 0.5)) = {
    face.map(v => rotateVector(v.subtract(around), angle, axis).add(around))
  }

  protected def rotateBox(box: Array[Array[Vector3d]], angle: Double, axis: Vector3d = new Vector3d(0, 1, 0), around: Vector3d = new Vector3d(0.5, 0.5, 0.5)) = {
    box.map(face => rotateFace(face, angle, axis, around))
  }

  /**
    * Create the BakedQuads for a set of quads defined by the specified vertices.
    * <p/>
    * Usually used to generate the quads for a cube previously generated using makeBox().
    */
  protected def bakeQuads(box: Array[Array[Vector3d]], texture: Array[TextureAtlasSprite], color: Option[Int]): Array[BakedQuad] = {
    val colorRGB = color.getOrElse(White)
    bakeQuads(box, texture, colorRGB)
  }

  /**
    * Create the BakedQuads for a set of quads defined by the specified vertices.
    * <p/>
    * Usually used to generate the quads for a cube previously generated using makeBox().
    */
  protected def bakeQuads(box: Array[Array[Vector3d]], texture: Array[TextureAtlasSprite], colorRGB: Int): Array[BakedQuad] = {
    Direction.values.map(side => {
      val vertices = box(side.get3DDataValue)
      val data = quadData(vertices, side, texture(side.get3DDataValue), colorRGB, 0)
      new BakedQuad(data, -1, side, texture(side.get3DDataValue), true)
    })
  }

  /**
    * Create a single BakedQuad of a unit cube's specified side.
    */
  protected def bakeQuad(side: Direction, texture: TextureAtlasSprite, color: Option[Int], rotation: Int) = {
    val colorRGB = color.getOrElse(White)
    val vertices = UnitCube(side.get3DDataValue)
    val data = quadData(vertices, side, texture, colorRGB, rotation)
    new BakedQuad(data, -1, side, texture, true)
  }

  // Generate raw data used for a BakedQuad based on the specified facing, vertices, texture and rotation.
  // The UV coordinates are generated from the positions of the vertices, i.e. they are simply cube-
  // mapped. This is good enough for us.
  protected def quadData(vertices: Array[Vector3d], facing: Direction, texture: TextureAtlasSprite, colorRGB: Int, rotation: Int): Array[Int] = {
    val (uAxis, vAxis) = Planes(facing.get3DDataValue)
    val rot = (rotation + 4) % 4
    vertices.flatMap(vertex => {
      var u = vertex.dot(uAxis)
      var v = vertex.dot(vAxis)
      if (uAxis.x + uAxis.y + uAxis.z < 0) u = 1 + u
      if (vAxis.x + vAxis.y + vAxis.z < 0) v = 1 + v
      for (i <- 0 until rot) {
        // (u, v) = (v, -u)
        val tmp = u
        u = v
        v = (-(tmp - 0.5)) + 0.5
      }
      rawData(vertex.x, vertex.y, vertex.z, facing, texture, texture.getU(u * 16), texture.getV(v * 16), colorRGB)
    })
  }

  // See FaceBakery#storeVertexData.
  protected def rawData(x: Double, y: Double, z: Double, face: Direction, texture: TextureAtlasSprite, u: Float, v: Float, colorRGB: Int) = {
    val vx = (face.getStepX * 127) & 0xFF
    val vy = (face.getStepY * 127) & 0xFF
    val vz = (face.getStepZ * 127) & 0xFF

    Array(
      java.lang.Float.floatToRawIntBits(x.toFloat),
      java.lang.Float.floatToRawIntBits(y.toFloat),
      java.lang.Float.floatToRawIntBits(z.toFloat),
      getFaceShadeColor(face, colorRGB),
      java.lang.Float.floatToRawIntBits(u),
      java.lang.Float.floatToRawIntBits(v),
      0, vx | (vy << 0x08) | (vz << 0x10)
    )
  }

  // See FaceBakery.
  protected def getFaceShadeColor(face: Direction, colorRGB: Int): Int = {
    val brightness = getFaceBrightness(face)
    val b = (colorRGB >> 16) & 0xFF
    val g = (colorRGB >> 8) & 0xFF
    val r = colorRGB & 0xFF
    0xFF000000 | shade(r, brightness) << 16 | shade(g, brightness) << 8 | shade(b, brightness)
  }

  private def shade(value: Int, brightness: Float) = (brightness * value).toInt max 0 min 255

  protected def getFaceBrightness(face: Direction): Float = {
    face match {
      case Direction.DOWN => 0.5f
      case Direction.UP => 1.0f
      case Direction.NORTH | Direction.SOUTH => 0.8f
      case Direction.WEST | Direction.EAST => 0.6f
    }
  }
}
