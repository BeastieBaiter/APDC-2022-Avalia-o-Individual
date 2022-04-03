package pt.unl.fct.di.adc.jppvieira.util;

public class ModifyPasswordData {
	
	public String oldPassword, newPassword, newPasswordConfirmation, tokenID;
	
	public ModifyPasswordData() {}
	
	public ModifyPasswordData(String oldPassword, String newPasssword, String newPasswordConfirmation, String tokenID) {
		this.oldPassword = oldPassword;
		this.newPassword = newPasssword;
		this.newPasswordConfirmation = newPasswordConfirmation;
		this.tokenID = tokenID;
	}
	
}
