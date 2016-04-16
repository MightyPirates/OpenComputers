package li.cil.oc.integration.util

import li.cil.oc.Settings

object Power {
  // Applied Energistics 2

  def fromAE(value: Double) = value * Settings.get.ratioAppliedEnergistics2

  def toAE(value: Double): Double = value / Settings.get.ratioAppliedEnergistics2

  // Factorization

  def fromCharge(value: Double) = value * Settings.get.ratioFactorization

  def toCharge(value: Double): Double = value / Settings.get.ratioFactorization

  // Galacticraft

  def fromGC(value: Double) = value * Settings.get.ratioGalacticraft

  def toGC(value: Double): Float = (value / Settings.get.ratioGalacticraft).toFloat

  // IndustrialCraft 2

  def fromEU(value: Double) = value * Settings.get.ratioIndustrialCraft2

  def toEU(value: Double): Double = value / Settings.get.ratioIndustrialCraft2

  // Mekanism

  def fromJoules(value: Double) = value * Settings.get.ratioMekanism

  def toJoules(value: Double): Double = value / Settings.get.ratioMekanism

  // Redstone Flux

  def fromRF(value: Double) = value * Settings.get.ratioRedstoneFlux

  def toRF(value: Double): Int = (value / Settings.get.ratioRedstoneFlux).toInt

  // RotaryCraft

  def fromWA(value: Double) = value * Settings.get.ratioRotaryCraft

  def toWA(value: Double): Long = (value / Settings.get.ratioRotaryCraft).toLong
}
