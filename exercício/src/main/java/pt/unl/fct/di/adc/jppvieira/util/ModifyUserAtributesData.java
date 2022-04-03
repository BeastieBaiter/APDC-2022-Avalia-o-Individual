package pt.unl.fct.di.adc.jppvieira.util;

public class ModifyUserAtributesData {
	
	public String userToChange, email, name, profileType, landLine, mobile, 
					adress, secondAdress, postalCode, nif, tokenID;
	
	public ModifyUserAtributesData() {}
	
	public ModifyUserAtributesData(String userTochange, String email, String name, String profileType, 
									String landLine, String mobile, String adress, String secondAdress, 
									String postalCode, String nif, String tokenID) {
		this.userToChange = userTochange;
		this.email = email;
		this.name = name;
		this.profileType = profileType;
		this.landLine = landLine;
		this.mobile = mobile;
		this.adress = adress;
		this.secondAdress = secondAdress;
		this.postalCode = postalCode;
		this.nif = nif;
		this.tokenID = tokenID;
	}
}
