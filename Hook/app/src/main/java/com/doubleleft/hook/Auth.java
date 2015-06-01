package com.doubleleft.hook;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.loopj.android.http.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by glaet on 2/28/14.
 */
public class Auth {
	protected static String AUTH_TOKEN_KEY = "hook-auth-token";
	protected static String AUTH_DATA_KEY = "hook-auth-data";

	protected SharedPreferences localStorage;
	protected RequestParams _currentUser;

	protected Client client;

	public Auth(Client client) {

		this.client = client;

		localStorage = Client.context.getSharedPreferences("hook-localStorage-" + client.getAppId(), Context.MODE_PRIVATE);

		if (localStorage != null) {
			String currentUser = localStorage.getString(client.getAppId() + "-" + AUTH_DATA_KEY, null);
			if (currentUser != null) {
				try {
					JSONObject user = (JSONObject) new JSONTokener(currentUser).nextValue();
					setCurrentUser(user);

				} catch (JSONException e) {
					Log.d("hook", "error on Auth module " + e.toString());
				}
			}
		}
	}

	public void register(RequestParams data, Responder responder) {
		final Responder clientResponder = responder;

		client.post("auth/email", data, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
				// If the response is JSONObject instead of expected JSONArray
			}

			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
				// Pull out the first event on the public timeline
				JSONObject firstEvent = timeline.get(0);
				String tweetText = firstEvent.getString("text");

				// Do something with the response
				System.out.println(tweetText);
			}
		});

		client.post("auth/email", data, new Responder() {
			@Override
			public void onSuccess(Response response) {
				registerToken(response.object);
				clientResponder.onSuccess(response);
			}

			@Override
			public void onError(Response response) {
				clientResponder.onError(response);
			}
		});
	}

	public void login(RequestParams data, Responder responder) {
		final Responder clientResponder = responder;

		client.post("auth/email/login", data, new Responder() {
			@Override
			public void onSuccess(Response response) {
				registerToken(response.object);
				clientResponder.onSuccess(response);
			}

			@Override
			public void onError(Response response) {
				clientResponder.onError(response);
			}
		});
	}

	public void forgotPassword(RequestParams data, Responder responder) {
		client.post("auth/email/forgotPassword", data, responder);
	}

	public void resetPassword(RequestParams data, Responder responder) {
		client.post("auth/email/resetPassword", data, responder);
	}

	public void logout() {
		setCurrentUser(null);
	}

	public boolean hasAuthToken() {
		return getAuthToken() != null;
	}

	public String getAuthToken() {
		return localStorage != null ? localStorage.getString(client.getAppId() + "-" + AUTH_TOKEN_KEY, null) : null;
	}

	protected void setCurrentUser(RequestParams data) {
		_currentUser = data;

		if (localStorage != null) {
			SharedPreferences.Editor editor = localStorage.edit();
			if (_currentUser == null) {
				editor.remove(client.getAppId() + "-" + AUTH_TOKEN_KEY);
				editor.remove(client.getAppId() + "-" + AUTH_DATA_KEY);
			} else {
				editor.putString(client.getAppId() + "-" + AUTH_DATA_KEY, _currentUser.toString());
			}
			editor.commit();
		}
	}

	public RequestParams getCurrentUser() {
		return _currentUser;
	}

	protected void registerToken(RequestParams data) {
		RequestParams tokenObject = data.optJSONObject("token");
		if (tokenObject != null) {
			if (localStorage != null) {
				SharedPreferences.Editor editor = localStorage.edit();
				editor.putString(client.getAppId() + "-" + AUTH_TOKEN_KEY, tokenObject.optString("token"));
				editor.commit();
			}
			setCurrentUser(data);
		}
	}

}