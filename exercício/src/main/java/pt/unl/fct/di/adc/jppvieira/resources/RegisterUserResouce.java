package pt.unl.fct.di.adc.jppvieira.resources;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;

import pt.unl.fct.di.adc.jppvieira.util.RegisterUserData;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterUserResouce {
	
	private static final Logger LOG = Logger.getLogger(RegisterUserResouce.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public RegisterUserResouce() {}
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegisterUseResponse (RegisterUserData data) {
		LOG.fine("Attempt to register user: " + data.username);
		
		//Check input data
		if (!validRegistration(data)) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);
			if (user != null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User already exists").build();
			}
			else {
				String profileType;
				if (data.profileType.equals("PUBLIC") || data.profileType.equals("PRIVATE")) {
					profileType = data.profileType;
				}
				else {
					profileType = "";
				}
				
				String role;
				switch (data.role) {
					case "GBO":
						role = data.role;
						break;
						
					case "GS":
						role = data.role;
						break;
						
					case "SU":
						role = data.role;
						break;
			
					default:
						role = "USER";
						break;
				}
				
				String state;
				if(data.state.equals("ENABLED"))
					state = data.state;
				else
					state = "DISABLED";
				
				user = Entity.newBuilder(userKey)
						.set("email", data.email)
						.set("name", data.name)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("profileType", profileType)
						.set("landline", data.landLine)
						.set("mobile", data.mobile)
						.set("adress", data.adress)
						.set("secondAdress", data.secondAdress)
						.set("postalCode", data.postalCode)
						.set("nif", data.nif)
						.set("role", role)
						.set("state", state)
						.set("user_creation_time", Timestamp.now()).build();
				txn.add(user);
				LOG.info("User registered " + data.username);
				txn.commit();
				return Response.ok().build();
			}
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
		
	}

	private boolean validRegistration(RegisterUserData data) {
		String[] emailSplit = data.email.split("\\.");
		String[] postalCodeSplit = data.postalCode.split("-");
		
		if (!data.username.equals("") && !data.name.equals("")) {
			if (data.password.equals(data.passwordConfirmation)) {
				if (emailSplit[0].contains("@") && (emailSplit[emailSplit.length-1].length() == 2 
												|| emailSplit[emailSplit.length-1].length() == 3)) {
					if (data.landLine.equals("") || data.landLine.length() == 9) {
						if (data.mobile.equals("") || data.mobile.length() == 9) {
							if (data.postalCode.equals("") || (postalCodeSplit.length == 2 && 
									postalCodeSplit[0].length() == 4 && postalCodeSplit[1].length() == 3)) {
								return true;
							}
						}
						
					}
				}
			}
		}
		
		return false;
	}
}
