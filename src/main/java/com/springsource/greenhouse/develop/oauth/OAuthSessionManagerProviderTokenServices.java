package com.springsource.greenhouse.develop.oauth;

import javax.inject.Inject;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth.provider.token.InvalidOAuthTokenException;
import org.springframework.security.oauth.provider.token.OAuthAccessProviderToken;
import org.springframework.security.oauth.provider.token.OAuthProviderToken;
import org.springframework.security.oauth.provider.token.OAuthProviderTokenServices;

import com.springsource.greenhouse.account.Account;
import com.springsource.greenhouse.account.AccountRepository;
import com.springsource.greenhouse.account.InvalidAccessTokenException;
import com.springsource.greenhouse.develop.AppConnection;
import com.springsource.greenhouse.develop.AppRepository;

public class OAuthSessionManagerProviderTokenServices implements OAuthProviderTokenServices {
	
	private OAuthSessionManager sessionManager;

	private AccountRepository accountRepository;
	
	private AppRepository appRepository;
	
	@Inject
	public OAuthSessionManagerProviderTokenServices(OAuthSessionManager sessionManager,
			AccountRepository accountRepository, AppRepository appRepository) {
		this.sessionManager = sessionManager;
		this.accountRepository = accountRepository;
		this.appRepository = appRepository;
	}
	
	public OAuthProviderToken createUnauthorizedRequestToken(String consumerKey, String callbackUrl) {
		return providerTokenFor(sessionManager.newOAuthSession(consumerKey, callbackUrl));
	}

	public void authorizeRequestToken(String requestToken, String verifier, Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof Account)) {
			throw new IllegalArgumentException("Authenticated user principal is not of expected Account type");
		}
		try {
			Long authorizingAccountId = ((Account) authentication.getPrincipal()).getId();			
			sessionManager.authorize(requestToken, authorizingAccountId, verifier);
		} catch (InvalidRequestTokenException e) {
			throw new InvalidOAuthTokenException(e.getMessage());
		}
	}
	
	public OAuthAccessProviderToken createAccessToken(String requestToken) {
		try {
			return providerTokenFor(sessionManager.grantAccess(requestToken));
		} catch (InvalidRequestTokenException e) {
			throw new InvalidOAuthTokenException(e.getMessage());
		}
	}

	public OAuthProviderToken getToken(String tokenValue) {
		try {
			return providerTokenFor(sessionManager.getSession(tokenValue));
		} catch (InvalidRequestTokenException e) {
			try {
				return providerTokenFor(appRepository.findAppConnection(tokenValue));
			} catch (InvalidAccessTokenException ex) {
				throw new InvalidOAuthTokenException("Could not find OAuthSession or AppConnection for provided OAuth request token " + tokenValue);
			}
		}
	}
	
	// internal helpers
	
	private OAuthProviderToken providerTokenFor(OAuthSession session) {
		return new OAuthSessionProviderToken(session);
	}
	
	private OAuthAccessProviderToken providerTokenFor(AppConnection connection) {
		return new AppConnectionProviderToken(connection, accountRepository);
	}

}