package pt.unl.fct.di.adc.jppvieira.util;

public class RemoveUserData {
	
	public String userToRemove, tokenID;
	
	public RemoveUserData() {}
	
	public RemoveUserData(String userToRemove, String tokenID) {
		this.userToRemove = userToRemove;
		this.tokenID = tokenID;
	}

}
