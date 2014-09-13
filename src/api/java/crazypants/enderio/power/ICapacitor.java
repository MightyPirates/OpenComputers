package crazypants.enderio.power;

public interface ICapacitor {

  int getMinEnergyReceived();

  int getMaxEnergyReceived();

  int getMaxEnergyStored();

  int getMinActivationEnergy();

  int getPowerLoss();

  int getPowerLossRegularity();

  int getMaxEnergyExtracted();

}
