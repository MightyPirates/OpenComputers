package forestry.api.mail;

public enum EnumPostage {
	P_0(0), P_1(1), P_2(2), P_5(5), P_10(10), P_20(20), P_50(50), P_100(100), P_200(200);

	private final int value;

	private EnumPostage(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}
}
