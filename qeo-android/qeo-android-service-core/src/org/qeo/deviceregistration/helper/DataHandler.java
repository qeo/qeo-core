/*
 * Copyright (c) 2015 - Qeo LLC
 *
 * The source code form of this Qeo Open Source Project component is subject
 * to the terms of the Clear BSD license.
 *
 * You can redistribute it and/or modify it under the terms of the Clear BSD
 * License (http://directory.fsf.org/wiki/License:ClearBSD). See LICENSE file
 * for more details.
 *
 * The Qeo Open Source Project also includes third party Open Source Software.
 * See LICENSE file for more details.
 */

package org.qeo.deviceregistration.helper;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.qeo.android.service.QeoDefaults;
import org.qeo.android.service.ServiceApplication;
import org.qeo.android.service.exception.InvalidTokenException;
import org.qeo.deviceregistration.DeviceRegPref;
import org.qeo.deviceregistration.QeoManagementApp;

import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;

/**
 * Class to store some global data.
 */
public final class DataHandler
{
    private static String sAccessToken;

    private static long sTokenExpireTime = 0;
    private static long sTokenLoadTime = 0;
    private static long sNativeRealmId = 0;
    private static boolean sRefreshTokenInvalid = false;

    private static final Logger LOG = Logger.getLogger("DataHandler");

    private DataHandler()
    {
    }

    /**
     * Set the expiration time of the oauth token.
     * 
     * @param expireTime The expiration time in seconds.
     */
    private static void setTokenExpireTime(long expireTime)
    {
        sTokenExpireTime = expireTime;
    }

    /**
     * Set the time the token was last loaded.
     * 
     * @param lastLoadTime The time in epoch format
     */
    private static void setTokenLastLoadTime(long lastLoadTime)
    {
        sTokenLoadTime = lastLoadTime;
    }

    /**
     * Expire time (relative) for token in milliseconds.
     * 
     * @return Expire time (relative) for token in milliseconds.
     */
    public static long getExpiryInMilliSec()
    {
        return sTokenExpireTime * 1000;
    }

    /**
     * Expire time for token in milliseconds.
     * 
     * @return Expire time for token in milliseconds.
     */
    public static long getTokenLoadTime()
    {
        return sTokenLoadTime;
    }

    /**
     * Sets the access token received after OAuth success.
     * 
     * @param accessToken Oauth access token.
     */
    private static void setAccessToken(String accessToken)
    {
        sAccessToken = accessToken;
    }

    /**
     * Get the access token.<br>
     * It will fetch a new token if the current one is not valid. So this call may be blocking.
     * 
     * @return access token string.
     */
    public static synchronized String getAccessToken()
    {
        try {
            return getAccessTokenWithException();
        }
        catch (TokenResponseException e) {
            LOG.log(Level.WARNING, "Error fetching oauth token", e);
            return null;
        }
        catch (InvalidTokenException e) {
            LOG.fine(e.getMessage());
            return null;
        }
    }

    /**
     * Get the access token.<br>
     * It will fetch a new token if the current one is not valid. So this call may be blocking.<br>
     * It will throw an exception if there's something wrong.
     * 
     * @return access token string.
     * @throws com.google.api.client.auth.oauth2.TokenResponseException If the token can't be fetched.
     * @throws org.qeo.android.service.exception.InvalidTokenException  If there is something wrong with the
     *                                                                  refreshtoken.
     */
    public static synchronized String getAccessTokenWithException() throws TokenResponseException, InvalidTokenException
    {
        if (!isTokenValid()) {
            LOG.fine("AccessToken not valid");
            String mRefreshToken = DeviceRegPref.getRefreshToken();
            if (mRefreshToken == null) {
                throw new InvalidTokenException("No RefreshToken");
            }
            if (sRefreshTokenInvalid) {
                throw new InvalidTokenException("RefreshToken is invalid");
            }

            // Request a new Access Token using the Refresh Token.
            GenericUrl url = new GenericUrl(DeviceRegPref.getOauthServerURL());
            RefreshTokenRequest refreshTokenRequest =
                new RefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(), url, mRefreshToken);
            try {
                fetchTokens(refreshTokenRequest);
            }
            catch (TokenResponseException e) {
                sRefreshTokenInvalid = true;
                throw e;
            }
        }
        else {
            LOG.fine("AccessToken is valid");
        }
        if (sAccessToken == null) {
            throw new InvalidTokenException("AccessToken is invalid");
        }
        return sAccessToken;
    }

    /**
     * Status of the access token.
     * 
     * @return True if is still valid, false otherwise.
     */
    public static boolean isTokenValid()
    {
        LOG.fine("CHECK >> [LoadTime]:" + getTokenLoadTime() + "[ExpireTime]:" + getExpiryInMilliSec() + " < "
            + "[Current: ]" + System.currentTimeMillis());
        if (getTokenLoadTime() + getExpiryInMilliSec() < System.currentTimeMillis()) {
            LOG.fine("Token is invalid");
            return false;
        }
        LOG.fine("Token is valid");
        return true;
    }

    /**
     * Invalidate accesstoken.
     */
    public static void invalidateAccessToken()
    {
        sAccessToken = null;
        sTokenExpireTime = 0;
        sTokenLoadTime = 0;
    }

    /**
     * Fetch new OAuth Access/Refresh tokens.
     * 
     * @param request The request. Either RefreshTokenRequest or AuthorizationCodeTokenRequest.
     * @return true if the tokens were correctly fetched.
     * @throws TokenResponseException Thrown if fetching of the token fails.
     */
    public static synchronized boolean fetchTokens(TokenRequest request)
        throws TokenResponseException
    {
        TokenResponse tokenResponse = null;
        try {

            request.set("client_id", ServiceApplication.getMetaData(ServiceApplication.META_DATA_QEO_CLIENT_ID));
            request.set("client_secret",
                ServiceApplication.getMetaData(ServiceApplication.META_DATA_QEO_CLIENT_SECRET));
            request.set("redirect_uri", QeoDefaults.getOpenIdRedirectUrl());

            // Execute the request.
            LOG.fine("Token Request to " + request.getTokenServerUrl() + ": " + request.toString());
            try {
                tokenResponse = request.execute();
            }
            catch (TokenResponseException e) {
                if (e.getStatusCode() == 500) {
                    //there's an issue on the SMS where the connection to google fails often if it has been idle for
                    //a long time. To prevent this from ending up in the client we retry once on a http-500
                    LOG.warning("Got error 500 from oauth server, retry request");
                    tokenResponse = request.execute();
                }
                else {
                    throw e;
                }
            }
            LOG.fine("Token Response: " + tokenResponse.toString());

            // store the refresh token. It will change on every call!
            DeviceRegPref.edit().setRefreshToken(tokenResponse.getRefreshToken()).apply();

            // store the access token and the corresponding timeouts
            setAccessToken(tokenResponse.getAccessToken());
            setTokenExpireTime(tokenResponse.getExpiresInSeconds() / QeoManagementApp.TIME_DIV);
            setTokenLastLoadTime(System.currentTimeMillis());
            sRefreshTokenInvalid = false;
            return true;
        }
        catch (TokenResponseException e) {
            // RefreshToken is not valid, so clear it.
            DeviceRegPref.edit().setRefreshToken(null).apply();
            throw e;
        }
        catch (IOException e) {
            LOG.log(Level.WARNING, "Error getting OAuth tokens", e);
        }
        return false;
    }

    /**
     * Get the native realm id.
     * 
     * @return the native realm id.
     */
    public static long getNativeRealmId()
    {
        return sNativeRealmId;
    }

    /**
     * Set the native realm id.
     * 
     * @param nativeRealmId the id.
     */
    public static void setNativeRealmId(long nativeRealmId)
    {
        sNativeRealmId = nativeRealmId;
    }

}
