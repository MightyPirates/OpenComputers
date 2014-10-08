package cofh.api.transport;

interface IEnderAttuned {

	public String getChannelString();

	public int getFrequency();

	public boolean setFrequency(int frequency);

	public boolean clearFrequency();

}
