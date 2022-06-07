package li.cil.oc.util

import net.minecraft.util.EnumFacing

import scala.collection.mutable

object RotationHelper {
  def fromYaw(yaw: Float) = {
    (yaw / 360 * 4).round & 3 match {
      case 0 => EnumFacing.SOUTH
      case 1 => EnumFacing.WEST
      case 2 => EnumFacing.NORTH
      case 3 => EnumFacing.EAST
    }
  }

  def toLocal(pitch: EnumFacing, yaw: EnumFacing, value: EnumFacing) =
    translationFor(pitch, yaw)(value.ordinal)

  def toGlobal(pitch: EnumFacing, yaw: EnumFacing, value: EnumFacing) =
    inverseTranslationFor(pitch, yaw)(value.ordinal)

  def translationFor(pitch: EnumFacing, yaw: EnumFacing) =
    translationCache.synchronized(translationCache.
      getOrElseUpdate(pitch, mutable.Map.empty).
      getOrElseUpdate(yaw, translations(pitch.ordinal)(yaw.ordinal - 2)))

  def inverseTranslationFor(pitch: EnumFacing, yaw: EnumFacing) =
    inverseTranslationCache.synchronized(inverseTranslationCache.
      getOrElseUpdate(pitch, mutable.Map.empty).
      getOrElseUpdate(yaw, {
      val t = translationFor(pitch, yaw)
      t.indices.
        map(EnumFacing.byIndex).
        map(t.indexOf).
        map(EnumFacing.byIndex).
        toArray
    }))

  // ----------------------------------------------------------------------- //

  private val translationCache = mutable.Map.empty[EnumFacing, mutable.Map[EnumFacing, Array[EnumFacing]]]
  private val inverseTranslationCache = mutable.Map.empty[EnumFacing, mutable.Map[EnumFacing, Array[EnumFacing]]]

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
      Array(D.south, D.north, D.up, D.down, D.east, D.west),
      // Yaw = South
      Array(D.south, D.north, D.down, D.up, D.west, D.east),
      // Yaw = West
      Array(D.south, D.north, D.west, D.east, D.up, D.down),
      // Yaw = East
      Array(D.south, D.north, D.east, D.west, D.down, D.up)),
    // Pitch = Up
    Array(
      // Yaw = North
      Array(D.north, D.south, D.down, D.up, D.east, D.west),
      // Yaw = South
      Array(D.north, D.south, D.up, D.down, D.west, D.east),
      // Yaw = West
      Array(D.north, D.south, D.west, D.east, D.down, D.up),
      // Yaw = East
      Array(D.north, D.south, D.east, D.west, D.up, D.down)),
    // Pitch = Forward (North|East|South|West)
    Array(
      // Yaw = North
      Array(D.down, D.up, D.south, D.north, D.east, D.west),
      // Yaw = South
      Array(D.down, D.up, D.north, D.south, D.west, D.east),
      // Yaw = West
      Array(D.down, D.up, D.west, D.east, D.south, D.north),
      // Yaw = East
      Array(D.down, D.up, D.east, D.west, D.north, D.south)))

  /** Shortcuts for forge directions to make the above more readable. */
  private object D {
    val down = EnumFacing.DOWN
    val up = EnumFacing.UP
    val north = EnumFacing.NORTH
    val south = EnumFacing.SOUTH
    val west = EnumFacing.WEST
    val east = EnumFacing.EAST
  }

}
