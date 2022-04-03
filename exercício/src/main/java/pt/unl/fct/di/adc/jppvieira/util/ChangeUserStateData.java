package pt.unl.fct.di.adc.jppvieira.util;

public class ChangeUserStateData {
	public String userToChange, newState, tokenID;
	
	public ChangeUserStateData() {}
	
	public ChangeUserStateData(String userToChange, String newState, String tokenID) {
		this.userToChange = userToChange;
		this.newState = newState;
		this.tokenID = tokenID;
	}
}
