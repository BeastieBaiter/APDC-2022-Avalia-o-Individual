package pt.unl.fct.di.adc.jppvieira.util;

import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

public class AuthToken {
	public static final long EXPIRATION_TIME = 1000 * 60 * 60 * 2; // 2h

	public String username, role, tokenID, verifier;
	public long creationData;
	public long expirationData;

	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.tokenID = UUID.randomUUID().toString();
		this.creationData = System.currentTimeMillis();
		this.expirationData = this.creationData + AuthToken.EXPIRATION_TIME;
		this.verifier = DigestUtils.sha1Hex(username);

	}
}
