package li.cil.oc.util

import scala.collection.mutable
import scala.reflect.ClassTag

class RTree[Data](val maxEntries: Int)(implicit val coordinate: Data => (Double, Double, Double)) {

  if (maxEntries < 1) throw new IllegalArgumentException("maxEntries must be larger or equal to 1.")

  private val minEntries = maxEntries / 2

  private var root: Node = new Leaf()

  def add(value: Data) = root.add(new Entry(new Point(value), value)) match {
    case node1 if node1 != root =>
      val node2 = root
      val newRoot = new NonLeaf()
      newRoot.nodes += node1
      newRoot.nodes += node2
      root = newRoot
    case _ =>
  }

  def remove(value: Data) = root.remove(value) match {
    case Some(result) if result => root = new Leaf()
    case _ =>
  }

  def query(from: (Double, Double, Double), to: (Double, Double, Double)) = root.query(new Point(from), new Point(to))

  private class Entry(val point: Point, val data: Data) extends Splittable {
    def min = point

    def max = point
  }

  private abstract class Node extends Splittable {
    var min: Point = Point.PositiveInfinity

    var max: Point = Point.NegativeInfinity

    def add(entry: Entry): Node

    def remove(value: Data): Option[Boolean]

    def query(from: Point, to: Point): Iterable[Data]

    protected def split[Child <: Splittable : ClassTag](list: mutable.Set[Child], value: Child, constructor: (Iterable[Child]) => Node): Node = {
      list += value
      if (list.size > maxEntries) {
        val (set1, min1, max1, set2, min2, max2) = Splittable.split(list.toArray)

        list.clear()
        list ++= set1
        min = min1
        max = max1

        val newNode = constructor(set2)
        newNode.min = min2
        newNode.max = max2
        newNode
      } else {
        min = min min value.min
        max = max max value.max
        this
      }
    }
  }

  private class NonLeaf extends Node {
    val nodes = mutable.Set.empty[Node]

    def add(entry: Entry): Node = {
      var bestChild: Node = null
      var bestGrowth = Double.PositiveInfinity
      var bestVolume = Double.PositiveInfinity
      for (child <- nodes) {
        val oldVolume = Point.volume(child.min, child.max)
        val volume = Point.volume(child.min min entry.min, child.max max entry.max)
        val growth = volume - oldVolume
        if (growth < bestGrowth || (growth == bestGrowth && volume < bestVolume)) {
          bestChild = child
          bestGrowth = growth
          bestVolume = volume
        }
      }

      split[Node](nodes, bestChild.add(entry), (set) => new NonLeaf() {
        nodes ++= set
      })
    }

    def remove(value: Data): Option[Boolean] = {
      for (node <- nodes) {
        node.remove(value) match {
          case Some(result) =>
            return if (result) {
              nodes.remove(node)
              Some(nodes.size == 0)
            }
            else None
          case _ =>
        }
      }
      None
    }

    def query(from: Point, to: Point) =
      if (from.x <= max.x && from.y <= max.y && from.z <= max.z && to.x >= min.x && to.y >= min.y && to.z >= min.z)
        nodes.foldRight(Iterable.empty[Data])((child, result) => result ++ child.query(from, to))
      else Iterable.empty[Data]
  }

  private class Leaf extends Node {
    val entries: mutable.Set[Entry] = mutable.Set.empty

    def add(entry: Entry): Node = {
      remove(entry.data) // Avoid duplicates.
      split[Entry](entries, entry, (set) => new Leaf() {
        entries ++= set
      })
    }

    def remove(value: Data) =
      entries.find(_.data == value) match {
        case Some(entry) =>
          entries.remove(entry)
          Some(entries.size == 0)
        case _ => None
      }

    def query(from: Point, to: Point) = entries.filter(entry => from.x <= entry.max.x && from.y <= entry.max.y && from.z <= entry.max.z && to.x >= entry.min.x && to.y >= entry.min.y && to.z >= entry.min.z).map(_.data)
  }

  private class Point(val x: Double, val y: Double, val z: Double) {
    def this(p: (Double, Double, Double)) = this(p._1, p._2, p._3)

    def min(other: Point) = new Point(x min other.x, y min other.y, z min other.z)

    def max(other: Point) = new Point(x max other.x, y max other.y, z max other.z)
  }

  private object Point {
    val NegativeInfinity = new Point(Double.NegativeInfinity, Double.NegativeInfinity, Double.NegativeInfinity)
    val PositiveInfinity = new Point(Double.PositiveInfinity, Double.PositiveInfinity, Double.PositiveInfinity)

    def volume(p1: Point, p2: Point) = {
      val sx = (p2.x - p1.x).abs
      val sy = (p2.y - p1.y).abs
      val sz = (p2.z - p1.z).abs
      sx * sy * sz
    }
  }

  private trait Splittable {
    def min: Point

    def max: Point
  }

  private object Splittable {
    def split[T <: Splittable](values: Array[T]) = {
      var seed1: Option[T] = None
      var seed2: Option[T] = None
      var worst = Double.NegativeInfinity
      for (i <- 0 until values.length) {
        val si = values(i)
        for (j <- 0 until values.length) {
          val sj = values(j)
          val vol1 = Point.volume(si.min, si.max)
          val vol2 = Point.volume(sj.min, sj.max)
          val vol = Point.volume(si.min min sj.min, si.max max sj.max)
          val d = vol - vol1 - vol2
          if (d > worst) {
            seed1 = Some(si)
            seed2 = Some(sj)
            worst = d
          }
        }
      }
      (seed1, seed2) match {
        case (Some(s1), Some(s2)) =>
          val r1 = new SplitResult(mutable.Set(s1), s1.min, s1.max)
          val r2 = new SplitResult(mutable.Set(s2), s2.min, s2.max)

          val list = mutable.Set.empty ++ values
          list -= s1
          list -= s2
          while (!list.isEmpty) {
            if (minEntries - r1.set.size >= list.size) {
              r1.set ++= list
              list.clear()
            }
            else if (minEntries - r2.set.size >= list.size) {
              r2.set ++= list
              list.clear()
            }
            else {
              var bestValue: Option[T] = None
              var r = r1
              var best = Double.NegativeInfinity
              for (value <- list) {
                val newVol1 = Point.volume(r1.min min value.min, r1.max max value.max)
                val newVol2 = Point.volume(r2.min min value.min, r2.max max value.max)
                val growth1 = newVol1 - r1.volume
                val growth2 = newVol2 - r2.volume
                val d = (growth2 - growth1).abs
                if (d > best) {
                  bestValue = Some(value)
                  r = if (growth1 < growth2 || (growth1 == growth2 && newVol1 < newVol2)) r1 else r2
                  best = d
                }
              }
              bestValue match {
                case Some(value) =>
                  list -= value
                  r.set += value
                  r.min = r.min min value.min
                  r.max = r.max max value.max
                  r.volume = Point.volume(r.min, r.max)
                case _ => throw new AssertionError()
              }
            }
          }

          (r1.set, r1.min, r1.max, r2.set, r2.min, r2.max)
        case _ => throw new AssertionError()
      }
    }
  }

  private class SplitResult[T](val set: mutable.Set[T], var min: Point, var max: Point) {
    var volume: Double = Point.volume(min, max)
  }

}
