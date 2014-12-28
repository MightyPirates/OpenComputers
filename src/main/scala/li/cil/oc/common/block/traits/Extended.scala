package li.cil.oc.common.block.traits

import com.google.common.base.Optional
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.BlockState.StateImplementation
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.IUnlistedProperty

import scala.collection.mutable
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object Extended {
  // Patch in custom hash codes for extended block states.
  def wrap(state: IBlockState): StateImplementation = state match {
    case wrapped: ExtendedBlockStateWithHashCode => wrapped
    case extended: StateImplementation with IExtendedBlockState => new ExtendedBlockStateWithHashCode(extended)
    case simple: StateImplementation => simple
  }

  class ExtendedBlockStateWithHashCode(val inner: StateImplementation with IExtendedBlockState) extends StateImplementation(inner.getBlock, inner.getProperties) with IExtendedBlockState {
    override def getPropertyNames = inner.getPropertyNames

    override def getValue(property: IProperty) = inner.getValue(property)

    override def withProperty(property: IProperty, value: Comparable[_]): IBlockState = wrap(inner.withProperty(property, value))

    override def cycleProperty(property: IProperty) = wrap(inner.cycleProperty(property))

    override def getProperties = inner.getProperties

    override def getBlock = inner.getBlock

    override def getUnlistedNames = inner.getUnlistedNames

    override def getValue[V](property: IUnlistedProperty[V]) = inner.getValue(property)

    override def withProperty[V](property: IUnlistedProperty[V], value: V) = wrap(inner.withProperty(property, value)).asInstanceOf[IExtendedBlockState]

    override def getUnlistedProperties = inner.getUnlistedProperties

    override def equals(obj: scala.Any) = inner.equals(obj)

    override def hashCode() = (inner.hashCode() * 31) ^ inner.getUnlistedProperties.collect {
      case (property: IUnlistedProperty[AnyRef]@unchecked, value: Optional[AnyRef]@unchecked) if value.isPresent => property.getName + "=" + property.valueToString(value.get)
    }.toArray.sorted.mkString(",").hashCode
  }

}

trait Extended extends Block {
  setDefaultExtendedState(Extended.wrap(getBlockState.getBaseState))

  // Gnaah, implementation limitations :-/
  protected def setDefaultExtendedState(state: IBlockState): Unit

  override def getBlockState = super.getBlockState

  override def getActualState(state: IBlockState, worldIn: IBlockAccess, pos: BlockPos) = getExtendedState(state, worldIn, pos)

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos) =
    addExtendedState(getDefaultState.asInstanceOf[IExtendedBlockState], world, pos).
      getOrElse(super.getExtendedState(state, world, pos))

  override def createBlockState() = {
    val (listed, unlisted) = collectProperties()
    new ExtendedBlockState(this, listed.toArray, unlisted.toArray) {
      private lazy val validStates = ImmutableList.copyOf(super.getValidStates.map {
        case state: IBlockState => Extended.wrap(state)
      }.toArray)
      override def createState(block: Block, properties: ImmutableMap[_, _], unlistedProperties: ImmutableMap[_, _]) = Extended.wrap(super.createState(block, properties, unlistedProperties))

      override def getValidStates = validStates
    }
  }

  final def collectProperties() = {
    val listed = mutable.ArrayBuffer.empty[IProperty]
    val unlisted = mutable.ArrayBuffer.empty[IUnlistedProperty[_]]
    addExtendedProperties(listed, unlisted)
    (listed, unlisted)
  }

  final def collectRawProperties() = {
    val unlistedRaw = mutable.Map.empty[IUnlistedProperty[_], IProperty]
    addExtendedRawProperties(unlistedRaw)
    unlistedRaw
  }

  protected def addExtendedState(state: IExtendedBlockState, world: IBlockAccess, pos: BlockPos): Option[IExtendedBlockState] = Some(state)

  protected def addExtendedProperties(listed: mutable.ArrayBuffer[IProperty], unlisted: mutable.ArrayBuffer[IUnlistedProperty[_]]): Unit = {}

  protected def addExtendedRawProperties(unlisted: mutable.Map[IUnlistedProperty[_], IProperty]): Unit = {}
}
