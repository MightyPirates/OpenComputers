package stargatetech2.api.stargate;

public enum Symbol {
	VOID(""),
	AT	("At"),		//  1
	AL	("Al"),		//  2
	CLA	("Cla"),	//  3
	UR	("Ur"),		//  4
	ON	("On"),		//  5
	DEH	("Deh"),	//  6
	EC	("Ec"),		//  7
	MIG	("Mig"),	//  8
	AM	("Am"),		//  9
	RUM	("Rum"),	// 10
	AR	("Ar"),		// 11
	VA	("Va"),		// 12
	COR	("Cor"),	// 13
	PRA	("Pra"),	// 14
	OM	("Om"),		// 15
	ET	("Et"),		// 16
	AS	("As"),		// 17
	US	("Us"),		// 18
	GON	("Gon"),	// 19
	ORM	("Orm"),	// 20
	EM	("Em"),		// 21
	AC	("Ac"),		// 22
	OTH	("Oth"),	// 23
	LOS	("Los"),	// 24
	LAN	("Lan"),	// 25
	EST	("Est"),	// 26
	CRO	("Cro"),	// 27
	SIL	("Sil"),	// 28
	TA	("Ta"),		// 29
	BREI("Brei"),	// 30
	RUSH("Rush"),	// 31
	ERP	("Erp"),	// 32
	SET	("Set"),	// 33
	ULF	("Ulf"),	// 34
	PRO	("Pro"),	// 35
	SAL	("Sal"),	// 36
	TIS	("Tis"),	// 37
	MAC	("Mac"),	// 38
	IRT	("Irt");	// 39
	
	private String name;
	
	private Symbol(String name){
		this.name = name;
	}
	
	public static Symbol get(int s){
		if(s >= 0 && s <= 39){
			return values()[s];
		}else{
			return VOID;
		}
	}
	
	@Override
	public String toString(){
		return name;
	}
}