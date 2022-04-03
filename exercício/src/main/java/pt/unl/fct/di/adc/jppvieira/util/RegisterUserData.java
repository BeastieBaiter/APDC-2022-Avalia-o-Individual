package pt.unl.fct.di.adc.jppvieira.util;

public class RegisterUserData {
	
	public String username, email, name, password, passwordConfirmation
					, profileType, landLine, mobile
					, adress, secondAdress, postalCode, nif, role, state;
	
	public RegisterUserData() {}
	
	public RegisterUserData(String username, String email, String name, String password,
							String passwordConfirmation, String profileType, String landline, 
							String mobile, String adress, String secondAdress, String postalCode, 
							String nif, String role, String state) {
		
		this.username = username;
		this.email = email;
		this.name = name;
		this.password = password;
		this.passwordConfirmation = passwordConfirmation;
		this.profileType = profileType;
		this.landLine = landline;
		this.mobile = mobile;
		this.adress = adress;
		this.secondAdress = secondAdress;
		this.postalCode = postalCode;
		this.nif = nif;
		this.role = role;
		this.state = state;
		
	}
}
