package pt.unl.fct.di.adc.jppvieira.resources;

import java.util.ArrayList;
import java.util.List;
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
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import pt.unl.fct.di.adc.jppvieira.util.TokenData;
import pt.unl.fct.di.adc.jppvieira.util.ChangeRoleData;
import pt.unl.fct.di.adc.jppvieira.util.ChangeUserStateData;
import pt.unl.fct.di.adc.jppvieira.util.ModifyPasswordData;
import pt.unl.fct.di.adc.jppvieira.util.ModifyUserAtributesData;
import pt.unl.fct.di.adc.jppvieira.util.RemoveUserData;

@Path("/loggedIn")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoggedInResource {
	
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private final Gson gson = new Gson(); 
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public LoggedInResource() {}
	
	@POST
	@Path("/removeUser")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRemoveUser(RemoveUserData data) {
		LOG.fine("Attempt to remove user: " + data.userToRemove);
		
		Key userToRemoveKey = datastore.newKeyFactory().setKind("User").newKey(data.userToRemove);
		Entity userToRemove = datastore.get(userToRemoveKey);
		
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenID);
		Entity token = datastore.get(tokenKey);
		
		if (token == null) {
			LOG.warning("Token with id: '" + data.tokenID + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("Token does not exist").build();
		}
		
		Key loggedInUserKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("username"));
		Entity loggedInUser = datastore.get(loggedInUserKey);
		
		if (!validToken(token, loggedInUser)) {
			LOG.warning("Token with id: '" + data.tokenID + "' has expired.");
			return Response.status(Status.BAD_REQUEST).entity("Token has expired, please login again.").build();
		}
		
		if (userToRemove == null) {
			LOG.warning("User with id: '" + data.userToRemove + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("User with id: '" 
																+ data.userToRemove +  "' does not exist").build();
		}
		
		if (!verifyRoleToDelete(loggedInUser, userToRemove)) {
			LOG.warning("User with id: '" + loggedInUser.getKey().toString() 
			+ "' does not have permissions to remove user with id: '" + data.userToRemove + "'.");
			return Response.status(Status.BAD_REQUEST).entity("You do not have permissions to remove user with id: '" 
																+ data.userToRemove + "'.").build();
		}
		
		datastore.delete(userToRemoveKey);
		LOG.info("User with id: '" + data.userToRemove + "' was sucessfully removed");
		return Response.ok(data.userToRemove + " was sucessfully removed.").build();
	}
	
	@POST
	@Path("/listUsers")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response listUsers(TokenData data) {
		LOG.fine("Attempt to list users");
		
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenID);
		Entity token = datastore.get(tokenKey);
		
		if (token == null) {
			LOG.warning("Token with id: '" + data.tokenID + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("Token does not exist").build();
		}
		
		Key loggedInUserKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("username"));
		Entity loggedInUser = datastore.get(loggedInUserKey);
		
		
		if (!validToken(token, loggedInUser)) {
			LOG.warning("Token with id: '" + data.tokenID + "' has expired.");
			return Response.status(Status.BAD_REQUEST).entity("Token has expired, please login again.").build();
		}
		
		Query<Entity> query;
		QueryResults<Entity> results;
		String loggedInUserRole = loggedInUser.getString("role");
		
		
		if (loggedInUserRole.equals("USER")) {
				query = Query.newEntityQueryBuilder().setKind("User")
						.setFilter(CompositeFilter
						.and(PropertyFilter.eq("profileType", "PUBLIC"), PropertyFilter.eq("role", "USER"), 
							PropertyFilter.eq("state", "ENABLED"))).build();
				
				results = datastore.run(query);
				if (results == null) {
					LOG.warning("No available users to list.");
					return Response.status(Status.NO_CONTENT).build();
				}
				else {
					List<Entity> usersList = new ArrayList<>();
					while (results.hasNext()) {
						Entity user = results.next();
						Entity auxUser = Entity.newBuilder(user.getKey())
								.set("email", user.getString("email"))
								.set("name", user.getString("name")).build(); 
						usersList.add(auxUser);
					}
					
					LOG.info("Users were sucessfully listed");
					return Response.ok(gson.toJson(usersList)).build();
				}
				
				
		}
		else if (loggedInUserRole.equals("GBO")) {
			query = Query.newEntityQueryBuilder().setKind("User")
					.setFilter(PropertyFilter.eq("role", "USER")).build();
			
			results = datastore.run(query);
			if (results == null) {
				LOG.warning("No available users to list.");
				return Response.status(Status.NO_CONTENT).build();
			}
			else {
				List<Entity> usersList = new ArrayList<>();
				while (results.hasNext()) {
					usersList.add(results.next());
				}
				
				LOG.info("Users were sucessfully listed");
				return Response.ok(gson.toJson(usersList)).build();
			}
		}
		else if (loggedInUserRole.equals("GS")) {
			query = Query.newEntityQueryBuilder().setKind("User")
					.setFilter(CompositeFilter
					.and(PropertyFilter.eq("role", "GBO"), PropertyFilter.eq("role", "USER"))).build();
			
			results = datastore.run(query);
			if (results == null) {
				LOG.warning("No available users to list.");
				return Response.status(Status.NO_CONTENT).build();
			}
			else {
				List<Entity> usersList = new ArrayList<>();
				while (results.hasNext()) {
					usersList.add(results.next());
				}
				
				LOG.info("Users were sucessfully listed");
				return Response.ok(gson.toJson(usersList)).build();
			}
		}
		else {
			query = Query.newEntityQueryBuilder().setKind("User").build();
			
			results = datastore.run(query);
			if (results == null) {
				LOG.warning("No available users to list.");
				return Response.status(Status.NO_CONTENT).build();
			}
			else {
				List<Entity> usersList = new ArrayList<>();
				while (results.hasNext()) {
					usersList.add(results.next());
				}
				
				LOG.info("Users were sucessfully listed");
				return Response.ok(gson.toJson(usersList)).build();
			}
		}
	}
	
	@POST
	@Path("/modifyUserAtributes")
	@Consumes(MediaType.APPLICATION_JSON)
	public 	Response modifyUserAtributes(ModifyUserAtributesData data) {
		LOG.fine("Attempt to modify password");
		
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenID);
		Entity token = datastore.get(tokenKey);
		
		if (token == null) {
			LOG.warning("Token with id: '" + data.tokenID + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("Token does not exist").build();
		}
		
		Key loggedInUserKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("username"));
		Entity loggedInUser = datastore.get(loggedInUserKey);
		
		
		if (!validToken(token, loggedInUser)) {
			LOG.warning("Token with id: '" + data.tokenID + "' has expired.");
			return Response.status(Status.BAD_REQUEST).entity("Token has expired, please login again.").build();
		}
		
		Key userToChangeKey = datastore.newKeyFactory().setKind("User").newKey(data.userToChange);
		Entity userToChange = datastore.get(userToChangeKey);
		
		if (userToChange == null) {
			LOG.warning("User with id: '" + data.userToChange + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("User with id: '" 
																+ data.userToChange +  "' does not exist").build();
		}
		
		if (!verifyRoleToChangeAtributes(loggedInUser, userToChange)) {
			LOG.warning("User with id: '" + loggedInUser.getKey().toString() 
					+ "' does not have permissions to change user with id: '" + data.userToChange + "'.");
					return Response.status(Status.BAD_REQUEST).entity("You do not have permissions to change the atributes of user with id: '" 
																		+ data.userToChange + "'.").build();
		}
		
		if (loggedInUser.getString("role").equals("USER")) {
			userToChange = Entity.newBuilder(userToChangeKey)
					.set("email", userToChange.getString("email"))
					.set("name", userToChange.getString("name"))
					.set("user_pwd", userToChange.getString("user_pwd"))
					.set("profileType", data.profileType)
					.set("landline", data.landLine)
					.set("mobile", data.mobile)
					.set("adress", data.adress)
					.set("secondAdress", data.secondAdress)
					.set("postalCode", data.postalCode)
					.set("nif", data.nif)
					.set("role", userToChange.getString("role"))
					.set("state", userToChange.getString("state"))
					.set("user_creation_time", userToChange.getTimestamp("user_creation_time")).build();
		}
		else {
			userToChange = Entity.newBuilder(userToChangeKey)
					.set("email", data.email)
					.set("name", data.name)
					.set("user_pwd", userToChange.getString("user_pwd"))
					.set("profileType", data.profileType)
					.set("landline", data.landLine)
					.set("mobile", data.mobile)
					.set("adress", data.adress)
					.set("secondAdress", data.secondAdress)
					.set("postalCode", data.postalCode)
					.set("nif", data.nif)
					.set("role", userToChange.getString("role"))
					.set("state", userToChange.getString("state"))
					.set("user_creation_time", userToChange.getTimestamp("user_creation_time")).build();
		}
		
		datastore.put(userToChange);
		LOG.info("Atributes changed sucessfully");
		return Response.ok("Atributes changed sucessfully").build();
	}

	@POST
	@Path("/modifyPassword")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response modifyPassword(ModifyPasswordData data) {
		LOG.fine("Attempt to modify password");
		
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenID);
		Entity token = datastore.get(tokenKey);
		
		if (token == null) {
			LOG.warning("Token with id: '" + data.tokenID + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("Token does not exist").build();
		}
		
		Key loggedInUserKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("username"));
		Entity loggedInUser = datastore.get(loggedInUserKey);
		
		
		if (!validToken(token, loggedInUser)) {
			LOG.warning("Token with id: '" + data.tokenID + "' has expired.");
			return Response.status(Status.BAD_REQUEST).entity("Token has expired, please login again.").build();
		}
		
		if (loggedInUser.getString("state").equals("DISABLED")) {
			LOG.warning("User with id: '" + loggedInUser.getKey() + "' is in state DISABLED.");
			return Response.status(Status.BAD_REQUEST).entity("User with id: '" + loggedInUser.getKey() 
																+ "' is in state DISABLED.").build();
		}
		
		String hashedPWD = loggedInUser.getString("user_pwd");
		if (!hashedPWD.equals(DigestUtils.sha512Hex(data.oldPassword))) {
			LOG.warning("Incorrect old password");
			return Response.status(Status.BAD_REQUEST).entity("Incorrect old password").build();
		}
		
		if (!data.newPassword.equals(data.newPasswordConfirmation)) {				
			LOG.warning("Passwords do not match");
			return Response.status(Status.BAD_REQUEST).entity("Passwords do not match").build();
		}
			
		loggedInUser = Entity.newBuilder(loggedInUserKey)
				.set("email", loggedInUser.getString("email"))
				.set("name", loggedInUser.getString("name"))
				.set("user_pwd", DigestUtils.sha512Hex(data.newPassword))
				.set("profileType", loggedInUser.getString("profileType"))
				.set("landline", loggedInUser.getString("landline"))
				.set("mobile", loggedInUser.getString("mobile"))
				.set("adress", loggedInUser.getString("adress"))
				.set("secondAdress", loggedInUser.getString("secondAdress"))
				.set("postalCode", loggedInUser.getString("postalCode"))
				.set("nif", loggedInUser.getString("nif"))
				.set("role", loggedInUser.getString("role"))
				.set("state", loggedInUser.getString("state"))
				.set("user_creation_time", loggedInUser.getTimestamp("user_creation_time")).build();
		
		datastore.put(loggedInUser);
		LOG.info("Password changed sucessfully");
		return Response.ok("Password changed sucessfully").build();
	}
	
	@POST
	@Path("/logout")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogOut(TokenData data) {
		LOG.fine("Attempt to log out");
		
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenID);
		Entity token = datastore.get(tokenKey);
		
		if (token == null) {
			return Response.status(Status.BAD_REQUEST).entity("Token does not exist.").build();
		}
		
		String username = token.getString("username");
		
		LOG.info("User with id: '" + username + "' was sucessfully logged out");
		datastore.delete(tokenKey);
		return Response.ok(username + " has sucessfully logged out.").build();

	}
	
	@POST
	@Path("/changeUserRole")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changeUserRole(ChangeRoleData data) {
		LOG.fine("Attempt to change role of user with id: '" + data.userToChange + "'.");
		
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenID);
		Entity token = datastore.get(tokenKey);
		
		if (token == null) {
			LOG.warning("Token with id: '" + data.tokenID + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("Token does not exist").build();
		}
		
		Key loggedInUserKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("username"));
		Entity loggedInUser = datastore.get(loggedInUserKey);
		
		
		if (!validToken(token, loggedInUser)) {
			LOG.warning("Token with id: '" + data.tokenID + "' has expired.");
			return Response.status(Status.BAD_REQUEST).entity("Token has expired, please login again.").build();
		}
		
		Key userToChangeKey = datastore.newKeyFactory().setKind("User").newKey(data.userToChange);
		Entity userToChange = datastore.get(userToChangeKey);
		
		if (userToChange == null) {
			LOG.warning("User with id: '" + data.userToChange + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("User with id: '" 
																+ data.userToChange +  "' does not exist").build();
		}
		
		if (loggedInUser.getString("state").equals("DISABLED")) {
			LOG.warning("User with id: '" + loggedInUser.getKey() + "' is in state DISABLED.");
			return Response.status(Status.BAD_REQUEST).entity("User with id: '" + loggedInUser.getKey() + "' is in state DISABLED.").build();
		}
		
		if (!verifyChangeRole(loggedInUser, userToChange)) {
			LOG.warning("User with id: '" + loggedInUser.getKey().toString() 
					+ "' does not have permissions to change the role of user with id: '" + data.userToChange + "'.");
					return Response.status(Status.BAD_REQUEST).entity("You do not have permissions to change the role of user with id: '" 
																		+ data.userToChange + "'.").build();
		}
		
		userToChange = Entity.newBuilder(userToChangeKey)
				.set("email", userToChange.getString("email"))
				.set("name", userToChange.getString("name"))
				.set("user_pwd", userToChange.getString("user_pwd"))
				.set("profileType", userToChange.getString("profileType"))
				.set("landline", userToChange.getString("landline"))
				.set("mobile", userToChange.getString("mobile"))
				.set("adress", userToChange.getString("adress"))
				.set("secondAdress", userToChange.getString("secondAdress"))
				.set("postalCode", userToChange.getString("postalCode"))
				.set("nif", userToChange.getString("nif"))
				.set("role", data.newRole)
				.set("state", userToChange.getString("state"))
				.set("user_creation_time", userToChange.getTimestamp("user_creation_time")).build();
	
		datastore.put(userToChange);
		LOG.info("Role changed sucessfully");
		return Response.ok("Role changed sucessfully").build();
	}
	
	@POST
	@Path("/changeUserState")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changeUserState(ChangeUserStateData data) {
		LOG.fine("Attempt to change state of user with id: '" + data.userToChange + "'.");
		
		Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenID);
		Entity token = datastore.get(tokenKey);
		
		if (token == null) {
			LOG.warning("Token with id: '" + data.tokenID + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("Token does not exist").build();
		}
		
		Key loggedInUserKey = datastore.newKeyFactory().setKind("User").newKey(token.getString("username"));
		Entity loggedInUser = datastore.get(loggedInUserKey);
		
		
		if (!validToken(token, loggedInUser)) {
			LOG.warning("Token with id: '" + data.tokenID + "' has expired.");
			return Response.status(Status.BAD_REQUEST).entity("Token has expired, please login again.").build();
		}
		
		Key userToChangeKey = datastore.newKeyFactory().setKind("User").newKey(data.userToChange);
		Entity userToChange = datastore.get(userToChangeKey);
		
		if (userToChange == null) {
			LOG.warning("User with id: '" + data.userToChange + "' does not exist.");
			return Response.status(Status.BAD_REQUEST).entity("User with id: '" 
																+ data.userToChange +  "' does not exist").build();
		}
		
		if (!verifyChangeState(loggedInUser, userToChange)) {
			LOG.warning("User with id: '" + loggedInUser.getKey().toString() 
					+ "' does not have permissions to change the state of user with id: '" + data.userToChange + "'.");
					return Response.status(Status.BAD_REQUEST).entity("You do not have permissions to change the state of user with id: '" 
																		+ data.userToChange + "'.").build();
		}
		
		userToChange = Entity.newBuilder(userToChangeKey)
				.set("email", userToChange.getString("email"))
				.set("name", userToChange.getString("name"))
				.set("user_pwd", userToChange.getString("user_pwd"))
				.set("profileType", userToChange.getString("profileType"))
				.set("landline", userToChange.getString("landline"))
				.set("mobile", userToChange.getString("mobile"))
				.set("adress", userToChange.getString("adress"))
				.set("secondAdress", userToChange.getString("secondAdress"))
				.set("postalCode", userToChange.getString("postalCode"))
				.set("nif", userToChange.getString("nif"))
				.set("role", userToChange.getString("role"))
				.set("state", data.newState)
				.set("user_creation_time", userToChange.getTimestamp("user_creation_time")).build();
	
		datastore.put(userToChange);
		LOG.info("State changed sucessfully");
		return Response.ok("State changed sucessfully").build();
	}

	private boolean verifyRoleToDelete(Entity loggedInUser, Entity userToRemove) {
		String loggedInUserRole = loggedInUser.getString("role");
		String userToRemoveRole = userToRemove.getString("role");
		
		if (loggedInUserRole.equals("USER") && loggedInUser.getKey().equals(userToRemove.getKey())) {
			return true;
		}
		if (loggedInUserRole.equals("GBO") 
				&& (userToRemoveRole.equals("USER") || loggedInUser.getKey().equals(userToRemove.getKey()))) {
			return true;
		}
		if (loggedInUserRole.equals("GS") 
				&& (userToRemoveRole.equals("USER") || userToRemoveRole.equals("GBO") 
						||loggedInUser.getKey().equals(userToRemove.getKey()))) {
			return true;
		}
		if (loggedInUserRole.equals("SU") && !userToRemoveRole.equals("SU")) {
			return true;
		}
		return false;
	}

	private boolean validToken(Entity token, Entity loggedInUser) {
		if (loggedInUser != null) {
			if (token.getString("verifier").equals(DigestUtils.sha1Hex(loggedInUser.getKey().toString()))); {
				if (token.getLong("expirationData") > System.currentTimeMillis()) {
					return true;
				}
			}
		}
		datastore.delete(token.getKey());
		return false;
	}
	
	private boolean verifyRoleToChangeAtributes(Entity loggedInUser, Entity userToChange) {
		String loggedInUserRole = loggedInUser.getString("role");
		String userToChangeRole = userToChange.getString("role");
		
		if (loggedInUserRole.equals("USER") && loggedInUser.getKey().equals(userToChange.getKey())) {
			return true;
		}
		if (loggedInUserRole.equals("GBO") 
				&& (userToChangeRole.equals("USER") || loggedInUser.getKey().equals(userToChange.getKey()))) {
			return true;
		}
		if (loggedInUserRole.equals("GS") 
				&& (userToChangeRole.equals("USER") || userToChangeRole.equals("GBO") 
						||loggedInUser.getKey().equals(userToChange.getKey()))) {
			return true;
		}
		if (loggedInUserRole.equals("SU") && 
				(!userToChangeRole.equals("SU") || loggedInUser.getKey().equals(userToChange.getKey()))) {
			return true;
		}
		return false;
	}
	
	private boolean verifyChangeRole(Entity loggedInUser, Entity userToChange) {
		String loggedInRole = loggedInUser.getString("role");
		String toChangeRole = userToChange.getString("role"); 
		
		if (loggedInRole.equals("SU")) {
			return true;
		}
		if (loggedInRole.equals("GS") && 
				(toChangeRole.equals("USER") || toChangeRole.equals("GBO") || loggedInUser.getKey().equals(userToChange.getKey()))) {
			return true;
		}
		return false;
	}

	private boolean verifyChangeState(Entity loggedInUser, Entity userToChange) {
		String loggedInRole = loggedInUser.getString("role");
		String toChangeRole = userToChange.getString("role"); 
		
		if (loggedInRole.equals("SU")) {
			return true;
		}
		if (loggedInRole.equals("GS") && 
				(toChangeRole.equals("GBO") || loggedInUser.getKey().equals(userToChange.getKey()))) {
			return true;
		}
		if (loggedInRole.equals("GBO") && 
				(toChangeRole.equals("USER") || loggedInUser.getKey().equals(userToChange.getKey()))) {
			return true;
		}
		if (loggedInRole.equals("USER") && loggedInUser.getKey().equals(userToChange.getKey())) {
			return true;
		}
		return false;
	}
}
