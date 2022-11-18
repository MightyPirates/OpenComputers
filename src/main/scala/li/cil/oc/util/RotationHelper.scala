package li.cil.oc.util

import net.minecraft.util.Direction

import scala.collection.mutable

object RotationHelper {
  private val DIRECTIONS = Direction.values

  def getNumDirections = DIRECTIONS.length

  def getFront(index: Int): Direction = DIRECTIONS(Math.floorMod(index, DIRECTIONS.length))

  def fromYaw(yaw: Float) = {
    (yaw / 360 * 4).round & 3 match {
      case 0 => Direction.SOUTH
      case 1 => Direction.WEST
      case 2 => Direction.NORTH
      case 3 => Direction.EAST
    }
  }

  def toLocal(pitch: Direction, yaw: Direction, value: Direction) =
    translationFor(pitch, yaw)(value.ordinal)

  def toGlobal(pitch: Direction, yaw: Direction, value: Direction) =
    inverseTranslationFor(pitch, yaw)(value.ordinal)

  def translationFor(pitch: Direction, yaw: Direction) =
    translationCache.synchronized(translationCache.
      getOrElseUpdate(pitch, mutable.Map.empty).
      getOrElseUpdate(yaw, translations(pitch.ordinal)(yaw.ordinal - 2)))

  def inverseTranslationFor(pitch: Direction, yaw: Direction) =
    inverseTranslationCache.synchronized(inverseTranslationCache.
      getOrElseUpdate(pitch, mutable.Map.empty).
      getOrElseUpdate(yaw, {
      val t = translationFor(pitch, yaw)
      t.indices.map(Direction.from3DDataValue).map(t.indexOf(_)).map(Direction.from3DDataValue).toArray
    }))

  // ----------------------------------------------------------------------- //

  private val translationCache = mutable.Map.empty[Direction, mutable.Map[Direction, Array[Direction]]]
  private val inverseTranslationCache = mutable.Map.empty[Direction, mutable.Map[Direction, Array[Direction]]]

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
    val down = Direction.DOWN
    val up = Direction.UP
    val north = Direction.NORTH
    val south = Direction.SOUTH
    val west = Direction.WEST
    val east = Direction.EAST
  }

}
