package pt.unl.fct.di.adc.jppvieira.util;

public class ChangeRoleData {
	public String userToChange, newRole, tokenID;
	
	public ChangeRoleData() {}
	
	public ChangeRoleData(String userToChange, String newRole, String tokenID) {
		this.userToChange = userToChange;
		this.newRole = newRole;
		this.tokenID = tokenID;
	}
}
