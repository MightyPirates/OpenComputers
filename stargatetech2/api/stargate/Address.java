package stargatetech2.api.stargate;

public class Address {
	private Symbol[] symbols;
	
	public static Address create(Symbol[] symbols){
		try{
			boolean used[] = new boolean[40];
			if(symbols.length < 7) throw new Exception("Address too short: " + symbols.length);
			if(symbols.length > 9) throw new Exception("Address too long: " + symbols.length);
			for(int i = 0; i < used.length; i++){
				used[i] = (i == 0);
			}
			for(Symbol symbol : symbols){
				if(symbol == null || symbol == Symbol.VOID){
					throw new Exception("Invalid Symbol.");
				}
				if(used[symbol.ordinal()]){
					throw new Exception("Repeated Symbol.");
				}
				used[symbol.ordinal()] = true;
			}
			return new Address(symbols);
		}catch(Exception e){
			return null;
		}
	}
	
	private Address(Symbol[] symbols){
		this.symbols = symbols;
	}
	
	public int length(){
		return symbols.length;
	}
	
	public Symbol getSymbol(int symbol){
		if(symbol >= 0 && symbol < length()){
			return symbols[symbol];
		}else{
			return Symbol.VOID;
		}
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getSymbol(0).toString());
		sb.append(getSymbol(1).toString().toLowerCase());
		sb.append(getSymbol(2).toString().toLowerCase());
		sb.append(" ");
		sb.append(getSymbol(3).toString());
		sb.append(getSymbol(4).toString().toLowerCase());
		sb.append(getSymbol(5).toString().toLowerCase());
		sb.append(" ");
		sb.append(getSymbol(6).toString());
		sb.append(getSymbol(7).toString().toLowerCase());
		sb.append(getSymbol(8).toString().toLowerCase());
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof Address){
			Address a = (Address) o;
			if(a.length() == length()){
				for(int i = 0; i < length(); i++){
					if(symbols[i] != a.symbols[i]){
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return length();
	}
}