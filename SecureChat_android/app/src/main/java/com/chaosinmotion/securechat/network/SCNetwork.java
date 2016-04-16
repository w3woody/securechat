/*
 * Copyright (c) 2016. William Edward Woody
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>
 *
 */

package com.chaosinmotion.securechat.network;

import android.text.TextUtils;
import android.util.Log;

import com.chaosinmotion.securechat.utils.ThreadPool;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Class wraper for handling network requests
 *
 * Created by woody on 4/9/16.
 */
public class SCNetwork
{
	private static final int LOGIN_SUCCESS = 0;
	private static final int LOGIN_FAILURE = 1;
	private static final int LOGIN_SERVERERROR = 2;

	/**
	 * Response interface used by callers to get a response from our
	 * asynchronous request process
	 */
	public interface ResponseInterface
	{
		void responseResult(Response response);
	}

	/**
	 * Interface that must be called once the login dialog completes.
	 */
	public interface LoginResponse
	{
		/**
		 * Pass in true if the login dialog succeeded, and false if the
		 * operation was canceled.
		 * @param success True if logged in, false if canceled
		 */
		void didLogin(boolean success);
	}

	public interface Delegate
	{
		void startWaitSpinner();
		void stopWaitSpinner();
		void showServerError(Response response);
		SCNetworkCredentials credentials();
		void requestLoginDialog(LoginResponse response);
	}

	/**
	 * Interface which represents the results of attempting to log in.
	 */
	public interface LoginCallback
	{
		void loginResult(int reason);
	}

	private static class Request
	{
		String requestURI;
		JSONObject params;
		boolean skipErrors;
		boolean backgroundFlag;
		Object caller;
		long enqueueTime;
		ResponseInterface callback;

		Future<?> taskFuture;       // generated if in progress call for cancel

		Response lastError;
		boolean waitFlag;
	}

	public static class Response
	{
		private int serverCode;
		private boolean success;
		private int error;
		private String errorMessage;
		private JSONArray exceptionStack;
		private JSONObject data;

		public JSONObject getData()
		{
			return data;
		}

		public int getError()
		{
			return error;
		}

		public String getErrorMessage()
		{
			return errorMessage;
		}

		public JSONArray getExceptionStack()
		{
			return exceptionStack;
		}

		public int getServerCode()
		{
			return serverCode;
		}

		public boolean isSuccess()
		{
			return success;
		}

		public boolean isServerError()
		{
			return serverCode != 200;
		}

		public boolean isAuthenticationError()
		{
			return !success && (error == 4);
		}
	}

	/*
	 *  State
	 */
	private static SCNetwork shared;

	private CookieManager cookies;
	private ArrayList<Request> callQueue;
	private ArrayList<Request> retryQueue;
	private String server;
	private boolean inLogin;
	private boolean networkError;
	private Delegate delegate;

	/**
	 * Internal initialization
	 */
	private SCNetwork()
	{
		cookies = new CookieManager();
		callQueue = new ArrayList<Request>();
		retryQueue = new ArrayList<Request>();
	}

	/**
	 * Obtain the network object for network requests
	 * @return
	 */
	public static synchronized SCNetwork get()
	{
		if (shared == null) shared = new SCNetwork();
		return shared;
	}

	/**
	 * Set the network delegate
	 */

	public void setNetworkDelegate(Delegate del)
	{
		delegate = del;
	}

	/************************************************************************/
	/*																		*/
	/*	Login Requests														*/
	/*																		*/
	/************************************************************************/

	/**
	 * Attempt to log in. Returns the results in a callback run asynchronously
	 * @param creds
	 * @param callback
	 */
	public void doLogin(final SCNetworkCredentials creds, final LoginCallback callback)
	{
		// Enqueue the login request
		request("login/token", null, this, new ResponseInterface()
		{
			@Override
			public void responseResult(Response response)
			{
				if (response.isSuccess()) {
					try {
						String token = response.getData().optString("token");

						JSONObject params = new JSONObject();
						params.putOpt("username", creds.getUsername());
						params.putOpt("password", creds.hashPasswordWithToken(token));

						request("login/login", params, this, new ResponseInterface()
						{
							@Override
							public void responseResult(Response response)
							{
								if (response.isSuccess()) {
									callback.loginResult(LOGIN_SUCCESS);
								} else if (response.getError() == 2) {
									callback.loginResult(LOGIN_FAILURE);
								} else {
									callback.loginResult(LOGIN_SERVERERROR);
								}
							}
						});
					} catch (JSONException e) {
						// I don't know a better way to handle this.
						callback.loginResult(LOGIN_SERVERERROR);
					}
				} else {
					showError(response);
					callback.loginResult(LOGIN_SERVERERROR);
				}
			}
		});
	}

	/**
	 *	We failed to login. Cancel all failed operations. Call on main thread
	 */

	private void failedLogin()
	{
		inLogin = false;

		for (Request req: retryQueue) {
			req.callback.responseResult(req.lastError);
		}
		retryQueue.clear();
	}

	/**
	 *	Succeeded logging in; retry all the requests that need to be retried
	 */

	private void successfulLogin()
	{
		inLogin = false;

		ArrayList<Request> tmp = new ArrayList<Request>(retryQueue);
		retryQueue.clear();;

		for (Request req: tmp) {
			sendRequest(req);
		}
	}

	/**
	 *	Login request
	 */

	private void loginRequest()
	{
		if (inLogin) return;
		inLogin = true;

		// Perform login request if we have credentials. Otherwise run dialog
		SCNetworkCredentials creds = delegate.credentials();
		if (creds != null) {
			doLogin(creds, new LoginCallback()
			{
				@Override
				public void loginResult(int reason)
				{
					if (reason == LOGIN_SUCCESS) {
						successfulLogin();
					} else if (reason == LOGIN_FAILURE) {
						runLoginDialog();
					} else {
						failedLogin();
					}
				}
			});
		} else {
			runLoginDialog();
		}
	}

	/**
	 *	Run the login dialog. The login dialog will handle the login process
	 *	itself. We presume the dialog will actually handle the login
	 *  process by calling doLogin, and if successful this will return
	 *  true. We also presume the login dialog will set the credentials
	 *  returned by our delegate.
	 */

	private void runLoginDialog()
	{
		delegate.requestLoginDialog(new LoginResponse()
		{
			@Override
			public void didLogin(boolean success)
			{
				if (success) {
					successfulLogin();
				} else {
					failedLogin();
				}
			}
		});
	}

	/************************************************************************/
	/*																		*/
	/*	Request																*/
	/*																		*/
	/************************************************************************/

	/**
	 *	Iterate through all queued calls forcing them to be canceled
	 */

	public void cancelRequestsWithCaller(Object caller)
	{
		ArrayList<Request> remove = new ArrayList<Request>();
		for (Request req: callQueue) {
			if (req.caller == caller) {
				if (req.taskFuture != null) {
					req.taskFuture.cancel(true);
				}
				remove.add(req);

				if (req.waitFlag) {
					delegate.stopWaitSpinner();
					req.waitFlag = false;
				}
			}
		}
		callQueue.removeAll(remove);

		remove.clear();
		for (Request req: retryQueue) {
			if (req.caller == caller) {
				if (req.taskFuture != null) {
					req.taskFuture.cancel(true);
				}
				remove.add(req);

				if (req.waitFlag) {
					delegate.stopWaitSpinner();
					req.waitFlag = false;
				}
			}
		}
		retryQueue.removeAll(remove);
	}

	/*
	 *	Set the server prefix
	 */

	public void setServerPrefix(String prefix)
	{
		if ((prefix == null) || (prefix.length() == 0)) return; // empty

		if (prefix.charAt(prefix.length()-1) == '/') {
			prefix = prefix.substring(0,prefix.length()-1); // trim off trailing '/'
		}

		int index = prefix.indexOf("://");
		if (index == -1) {
			prefix = "https://" + prefix;       // prepend https:// if not otherwise specified
		}
		server = prefix + "/ss/api/1/";         // full url prefix
	}

	/*
	 *	Convert network request to a request URL
	 */

	private HttpURLConnection requestWith(Request req) throws IOException
	{
		String path = server + req.requestURI;
		URL url = new URL(path);

		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");

		if (cookies.getCookieStore().getCookies().size() > 0) {
			conn.setRequestProperty("Cookie", TextUtils.join(";",cookies.getCookieStore().getCookies()));
		}

		conn.setDoInput(true);

		if (req.params != null) {
			String jsonString = req.params.toString();
			conn.setDoOutput(true);
			OutputStream os = conn.getOutputStream();
			os.write(jsonString.getBytes("UTF-8"));
			os.close();
		}

		return conn;
	}

	/**
	 * Internal method to parse result from server
	 * @param is Input stream from server
	 * @return JSON parsed result
	 */
	private static JSONObject parseResult(InputStream is) throws IOException, JSONException
	{
		ByteArrayOutputStream bais = new ByteArrayOutputStream();
		byte[] buffer = new byte[512];
		int len;

		while (0 < (len = is.read(buffer))) {
			bais.write(buffer,0,len);
		}
		is.close();

		byte[] data = bais.toByteArray();
		if (data.length < 2) return new JSONObject();   // no return result
		String json = new String(data,"UTF-8");
		JSONTokener tokener = new JSONTokener(json);
		return (JSONObject)tokener.nextValue();
	}

	/*
	 *	Internal process request. This enqueues the request and parses the
	 *	response.
	 */

	private synchronized void sendRequest(final Request request)
	{
		callQueue.add(request);

		// If not in background, spin the spinner
		if (!request.backgroundFlag) {
			delegate.startWaitSpinner();
			request.waitFlag = true;
		}

		request.taskFuture = ThreadPool.get().enqueueAsync(new Runnable()
		{
			@Override
			public void run()
			{
				try {
					HttpURLConnection conn = requestWith(request);

					conn.connect();
					Map<String,List<String>> headers = conn.getHeaderFields();
					List<String> clist = headers.get("Set-Cookie");
					if (clist != null) {
						for (String cookie: clist) {
							cookies.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
						}
					}

					InputStream is = conn.getInputStream();
					JSONObject d = parseResult(is);
					conn.disconnect();

					final Response response = new Response();
					response.serverCode = conn.getResponseCode();
					if (d != null) {
						response.success = d.optBoolean("success");
						response.error = d.optInt("error");
						response.errorMessage = d.optString("message");
						response.exceptionStack = d.optJSONArray("exception");
						response.data = d.optJSONObject("data");
					}

					ThreadPool.get().enqueueMain(new Runnable()
					{
						@Override
						public void run()
						{
							if (request.waitFlag) {
								delegate.stopWaitSpinner();
								request.waitFlag = false;
							}
							callQueue.remove(request);
							handleResponse(response,request);
						}
					});
				}
				catch (Exception ex) {
					/*
					 *  This happens if there is a connection error.
					 */
					ThreadPool.get().enqueueMain(new Runnable()
					{
						@Override
						public void run()
						{
							if (request.waitFlag) {
								delegate.stopWaitSpinner();
								request.waitFlag = false;
							}
							callQueue.remove(request);
							handleIOError(request);
						}
					});
				}
			}
		});
	}

	/*
	 *	Handle errors
	 */

	private void showError(Response response)
	{
		if (response.isServerError()) {
			if (networkError) return;
			networkError = true;        // if server error, do only once
		}
		delegate.showServerError(response);
	}

	/*
	 *	Handle the request
	 */

	private void handleResponse(Response response, Request request)
	{
		if (response.isAuthenticationError()) {
			/*
			 *  If authentication error, queue for retry and try to log in
			 */

			retryQueue.add(request);
			request.lastError = response;
			loginRequest();
			return;
		}

		/*
		 *  Process error notifications
		 */

		if (!request.skipErrors) {
			/*
			 *  Handle errors via delegate. If we are skipping error processing
			 *  we simply pass the results up
			 */

			if (response.isServerError()) {
				showError(response);
			} else if (!response.success) {
				/*
				 *  For random errors, display them. Skip login errors.
				 */

				if (response.error != 2) {
					showError(response);
				}
			}
		}

		/*
		 *  Reset our network error flag if the request succeeded.
		 */

		if (!response.isServerError()) {
			networkError = false;
		}

		/*
		 *  Now pass the response upwards.
		 */
		request.callback.responseResult(response);
	}

	/**
	 * This shouldn't happen normally, as it occurs while we are
	 * constructing the request. But handle it anyway.
	 * @param request
	 */
	private void handleIOError(Request request)
	{
		Response response = new Response();
		response.serverCode = 0;
		response.success = false;
		response.error = 3; // internal error?
		response.errorMessage = "Client internal error";
		response.exceptionStack = null;
		response.data = null;
		handleResponse(response,request);
	}

	/*
	 *	Enqueue request
	 */

	public void request(String requestUri, JSONObject params, Object caller,
	                    ResponseInterface callback)
	{
		request(requestUri,params,false,false,caller,callback);
	}

	public void request(String requestUri, JSONObject params,
	                    boolean inBackground, boolean skipErrors,
	                    Object caller, ResponseInterface callback)
	{
		Request request = new Request();

		request.requestURI = requestUri;
		request.params = params;
		request.backgroundFlag = inBackground;
		request.skipErrors = skipErrors;
		request.caller = caller;
		request.callback = callback;
		request.enqueueTime = System.currentTimeMillis();

		sendRequest(request);
	}
}
