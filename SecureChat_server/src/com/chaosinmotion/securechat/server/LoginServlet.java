/*	SecureChat: A secure chat system which permits secure communications 
 *  between iOS devices and a back-end server.
 *
 *	Copyright © 2016 by William Edward Woody
 *
 *	This program is free software: you can redistribute it and/or modify it 
 *	under the terms of the GNU General Public License as published by the 
 *	Free Software Foundation, either version 3 of the License, or (at your 
 *	option) any later version.
 *
 *	This program is distributed in the hope that it will be useful, but 
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *	for more details.
 *
 *	You should have received a copy of the GNU General Public License along 
 *	with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.chaosinmotion.securechat.server;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.chaosinmotion.securechat.server.commands.ChangePassword;
import com.chaosinmotion.securechat.server.commands.CreateAccount;
import com.chaosinmotion.securechat.server.commands.ForgotPassword;
import com.chaosinmotion.securechat.server.commands.Login;
import com.chaosinmotion.securechat.server.commands.UpdateForgottenPassword;
import com.chaosinmotion.securechat.server.json.ReturnResult;
import com.chaosinmotion.securechat.server.json.SimpleReturnResult;
import com.chaosinmotion.securechat.shared.Errors;

/**
 * The LoginServlet serves as the servlet which parses all requests for
 * logging in and out of the SecureChat server. This parses the incoming
 * requests as JSON (using the JSON parser library in org.json), and
 * determines what command needs to be handled.
 */
public class LoginServlet extends HttpServlet
{
	private static final long serialVersionUID = -4609610834080827293L;

	/*
	 * It's all the rage with kids nowadays to specify each command as its
	 * own URI, but without understanding the limitations imposed by the
	 * HTTP protocol.
	 * 
	 * Get commands are commands that require no arguments, and that return
	 * a value. Downstream caching servers are permitted to cache the value
	 * and re-send the value without hitting our server if the URL matches and
	 * the cache hasn't timed out. Think of it as a file read operation with
	 * the URL being the file read.
	 * 
	 * Put commands are commands which save the contents of a file. Think of
	 * it as a 'write' command with the URL specifying the file being written.
	 * Downstream caching servers may re-send the contents of this file in
	 * response to a GET command with the same URL as the PUT URL just saved.
	 * 
	 * Post commands are similar to executing the program at the URL specified
	 * with the input file provided, and writing the output returned. 
	 * 
	 * We prefer POST over GET and PUT because this accurately represents what
	 * we are doing; we are running commands. Even our random token get
	 * command is a command; we do not want to rely on a downstream caching
	 * server caching the randomly returned token.
	 */

	/**
	 * Handle POST commands. This uses the cookie mechanism for Java
	 * servlets to track users for security reasons, and handles the
	 * commands for getting a token, for verifying server status, and for
	 * logging in and changing a password.
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		ReturnResult retVal = null;
		
		/*
		 * Step 1: determine the path element after the api/1/ URL. This
		 * determines the command
		 */

		String path = req.getPathInfo();
		if (path == null) {
			resp.sendError(404);
			return;
		}
		if (path.startsWith("/")) path = path.substring(1);
		
		/*
		 * Now handle commands that do not require the user to be logged in
		 */
		
		try {
			HttpSession session = req.getSession();
			
			if (path.equalsIgnoreCase("status")) {
				// Requests server status. This simply returns success.
				retVal = new ReturnResult();
				
			} else if (path.equalsIgnoreCase("token")) {
				// Generates a token used to hash the password sent from
				// the client. We use the token as a 'salt' to salt the
				// sha256-hashed password, in order to avoid a replay
				// attack on the passwords.
				//
				// We generate the random token by generating a UUID.
				// Unlike the client code (where we coded our own UUID
				// system), the server is at a known location, so there
				// is little point to hide the server hardware.
				
				String token = UUID.randomUUID().toString();
				session.setAttribute("token", token);
				retVal = new SimpleReturnResult("token",token);

			} else if (path.equalsIgnoreCase("login")) {
				// Handles the login request. This pulls the request
				// contents as a JSON object, and calls into our
				// utility class to handle the login logic. We use
				// the same pattern throughout.
				
				JSONTokener tokener = new JSONTokener(req.getInputStream());
				JSONObject requestParams = new JSONObject(tokener);
				String token = (String)session.getAttribute("token");
				Login.UserInfo uinfo = Login.processRequest(requestParams,token);
				if (uinfo == null) {
					retVal = new ReturnResult(Errors.ERROR_LOGIN,"Login error");
				} else {
					retVal = new ReturnResult();
					session.setAttribute("userinfo", uinfo);
				}
				
			} else if (path.equalsIgnoreCase("forgotpassword")) {
				// Handle a forgot password request. This is sent when a user
				// is attempting to add a new device but cannot remember his
				// password. This pulls the username, and calls into the
				// forgot password logic, which then sends out a message to
				// the various other devices.
				
				JSONTokener tokener = new JSONTokener(req.getInputStream());
				JSONObject requestParams = new JSONObject(tokener);
				ForgotPassword.processRequest(requestParams);
				retVal = new ReturnResult();
				
			} else if (path.equalsIgnoreCase("createaccount")) {
				// Handle an onboarding request to create a new account.
				// This processes the onboarding request with a new
				// account.

				JSONTokener tokener = new JSONTokener(req.getInputStream());
				JSONObject requestParams = new JSONObject(tokener);
				Login.UserInfo uinfo = CreateAccount.processRequest(requestParams);
				if (uinfo == null) {
					retVal = new ReturnResult(Errors.ERROR_DUPLICATEUSER,"Internal Error");
				} else {
					retVal = new ReturnResult();
					session.setAttribute("userinfo", uinfo);
				}
			} else {
				/*
				 * 	All the commands past this point require a user account.
				 */
				
				Login.UserInfo userinfo = (Login.UserInfo)session.getAttribute("userinfo");
				if (userinfo == null) {
					retVal = new ReturnResult(Errors.ERROR_UNAUTHORIZED,"Not authorized");
				} else {
					/*
					 * Now handle the various command requests
					 */
					
					if (path.equalsIgnoreCase("updateforgotpassword")) {
						// Handle a forgotten password. We have an interesting
						// irony here that the user needs to be logged in 
						// in order to reset his password. However, this
						// is handled by the fact that each device stores the
						// password in an encrypted format as a token on
						// each device. 
						//
						// This also implies there is no way for a user to
						// reset his password if he doesn't have a device
						// already associated with is account. This is a
						// deliberate design decision.
						//
						// If we were to associate each account with an e-mail
						// account, then the reset pathway would be different
						// and would involve the user arriving at a web site
						// URL with a random token in the URI.
						
						JSONTokener tokener = new JSONTokener(req.getInputStream());
						JSONObject requestParams = new JSONObject(tokener);
						if (UpdateForgottenPassword.processRequest(userinfo,requestParams)) {
							retVal = new ReturnResult();
						} else {
							retVal = new ReturnResult(Errors.ERROR_INTERNAL,"Internal error");
						}
						
					} else if (path.equalsIgnoreCase("changepassword")) {
						// Handle a change password request. This requries both
						// the existing password and the new password. 
						//
						// Yes, the user has his device, but by asking for an
						// existing password this assures that the phone wasn't
						// for example picked up by someone else while in an
						// unlocked state.
						
						JSONTokener tokener = new JSONTokener(req.getInputStream());
						JSONObject requestParams = new JSONObject(tokener);
						String token = (String)session.getAttribute("token");
						if (ChangePassword.processRequest(userinfo,requestParams,token)) {
							retVal = new ReturnResult();
						} else {
							retVal = new ReturnResult(Errors.ERROR_LOGIN,"Internal error");
						}
					}
				}
			}

		}
		catch (Throwable th) {
			retVal = new ReturnResult(th);
		}
		
		/*
		 * If we get here and we still haven't initialized return value,
		 * set to a 404 error. We assume this reaches here with a null
		 * value because the path doesn't exist.
		 */
		
		if (retVal == null) {
			resp.sendError(404);
			
		} else {
			/*
			 * We now have a return result. Formulate the response
			 */
			
			ServletOutputStream stream = resp.getOutputStream();
			resp.setContentType("application/json");
			stream.print(retVal.toString());
		}
	}
}
