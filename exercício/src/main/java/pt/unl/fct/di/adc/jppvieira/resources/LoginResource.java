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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.jppvieira.util.AuthToken;
import pt.unl.fct.di.adc.jppvieira.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {
	
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private final Gson gson = new Gson(); 
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public LoginResource() {}
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response doLogin(LoginData data) {
		LOG.fine("Attempt to login user: " + data.username);
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = datastore.get(userKey);
		
		if (user != null) {
			String hashedPWD = user.getString("user_pwd");
			if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
				AuthToken authToken = new AuthToken(data.username, user.getString("role"));
				Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(authToken.tokenID);
				Entity token = Entity.newBuilder(tokenKey)
								.set("tokenID", authToken.tokenID)
								.set("username", authToken.username)
								.set("role", authToken.role)
								.set("creationData", authToken.creationData)
								.set("expirationData", authToken.expirationData)
								.set("verifier", authToken.verifier).build();
				datastore.add(token);
				
				LOG.info("User '" + data.username + "' logged in sucessfully.");
				return Response.ok(gson.toJson(token)).build();
			}
			else {
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		else {
			//User does not exist
			LOG.warning("Failed to login attempt for username: " + data.username);
			return Response.status(Status.FORBIDDEN).build();
		}
	}

}
