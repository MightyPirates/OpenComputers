package li.cil.oc.util

import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable

object RotationHelper {
  def fromYaw(yaw: Float) = {
    (yaw / 360 * 4).round & 3 match {
      case 0 => ForgeDirection.SOUTH
      case 1 => ForgeDirection.WEST
      case 2 => ForgeDirection.NORTH
      case 3 => ForgeDirection.EAST
    }
  }

  def toLocal(pitch: ForgeDirection, yaw: ForgeDirection, value: ForgeDirection) =
    translationFor(pitch, yaw)(value.ordinal)

  def toGlobal(pitch: ForgeDirection, yaw: ForgeDirection, value: ForgeDirection) =
    inverseTranslationFor(pitch, yaw)(value.ordinal)

  def translationFor(pitch: ForgeDirection, yaw: ForgeDirection) =
    translationCache.synchronized(translationCache.
      getOrElseUpdate(pitch, mutable.Map.empty).
      getOrElseUpdate(yaw, translations(pitch.ordinal)(yaw.ordinal - 2)))

  def inverseTranslationFor(pitch: ForgeDirection, yaw: ForgeDirection) =
    inverseTranslationCache.synchronized(inverseTranslationCache.
      getOrElseUpdate(pitch, mutable.Map.empty).
      getOrElseUpdate(yaw, {
      val t = translationFor(pitch, yaw)
      t.indices.
        map(ForgeDirection.getOrientation).
        map(t.indexOf).
        map(ForgeDirection.getOrientation).
        toArray
    }))

  // ----------------------------------------------------------------------- //

  private val translationCache = mutable.Map.empty[ForgeDirection, mutable.Map[ForgeDirection, Array[ForgeDirection]]]
  private val inverseTranslationCache = mutable.Map.empty[ForgeDirection, mutable.Map[ForgeDirection, Array[ForgeDirection]]]

  /**
   * Translates forge directions based on the block's pitch and yaw. The base
   * forward direction is facing south with no pitch. The outer array is for
   * the three different pitch states, the inner for the four different yaw
   * states.
   */
  private val translations = Array(
    // Pitch = Down
    Array(
      // Yaw = North
      Array(D.south, D.north, D.up, D.down, D.east, D.west, D.unknown),
      // Yaw = South
      Array(D.south, D.north, D.down, D.up, D.west, D.east, D.unknown),
      // Yaw = West
      Array(D.south, D.north, D.west, D.east, D.up, D.down, D.unknown),
      // Yaw = East
      Array(D.south, D.north, D.east, D.west, D.down, D.up, D.unknown)),
    // Pitch = Up
    Array(
      // Yaw = North
      Array(D.north, D.south, D.down, D.up, D.east, D.west, D.unknown),
      // Yaw = South
      Array(D.north, D.south, D.up, D.down, D.west, D.east, D.unknown),
      // Yaw = West
      Array(D.north, D.south, D.west, D.east, D.down, D.up, D.unknown),
      // Yaw = East
      Array(D.north, D.south, D.east, D.west, D.up, D.down, D.unknown)),
    // Pitch = Forward (North|East|South|West)
    Array(
      // Yaw = North
      Array(D.down, D.up, D.south, D.north, D.east, D.west, D.unknown),
      // Yaw = South
      Array(D.down, D.up, D.north, D.south, D.west, D.east, D.unknown),
      // Yaw = West
      Array(D.down, D.up, D.west, D.east, D.south, D.north, D.unknown),
      // Yaw = East
      Array(D.down, D.up, D.east, D.west, D.north, D.south, D.unknown)))

  /** Shortcuts for forge directions to make the above more readable. */
  private object D {
    val down = ForgeDirection.DOWN
    val up = ForgeDirection.UP
    val north = ForgeDirection.NORTH
    val south = ForgeDirection.SOUTH
    val west = ForgeDirection.WEST
    val east = ForgeDirection.EAST
    val unknown = ForgeDirection.UNKNOWN
  }

}
