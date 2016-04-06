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
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.chaosinmotion.securechat.server.commands.AddDevice;
import com.chaosinmotion.securechat.server.commands.Devices;
import com.chaosinmotion.securechat.server.commands.Login;
import com.chaosinmotion.securechat.server.commands.RemoveDevice;
import com.chaosinmotion.securechat.server.json.ReturnResult;
import com.chaosinmotion.securechat.shared.Errors;

/**
 * The LoginServlet serves as the servlet which parses all requests for
 * logging in and out of the SecureChat server. This parses the incoming
 * requests as JSON (using the JSON parser library in org.json), and
 * determines what command needs to be handled.
 */
public class DeviceServlet extends HttpServlet
{
	private static final long serialVersionUID = 7239914374051520533L;

	/*
	 * Determine the commmand we process. See our notes with LoginServlet as
	 * to why we use POST exclusively.
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

		try {
			/*
			 * All commands require authentication. This determines if we have
			 * it or not.
			 */
			HttpSession session = req.getSession();
			Login.UserInfo userinfo = (Login.UserInfo)session.getAttribute("userinfo");
			if (userinfo == null) {
				retVal = new ReturnResult(Errors.ERROR_UNAUTHORIZED,"Not authorized");
			} else {
				
				if (path.equalsIgnoreCase("adddevice")) {
					/*
					 * Process the adddevice request. This adds a new device
					 * to this user's list of device. We are basically a 
					 * key escrow system with each user associated with a 
					 * list of devices (by id) and their public keys.
					 * 
					 * This is a potential weak point in the protocol in that
					 * a bad guy could theoretically add his own device to
					 * this account. But to do so he'd have to have the
					 * username and password for this user, and the fact that
					 * there were multiple devices associated with this
					 * account would be effectively broadcast to all senders.
					 */
					
					JSONTokener tokener = new JSONTokener(req.getInputStream());
					JSONObject requestParams = new JSONObject(tokener);
					if (AddDevice.processRequest(userinfo,requestParams)) {
						retVal = new ReturnResult();
					} else {
						retVal = new ReturnResult(Errors.ERROR_INTERNAL,"Internal error");
					}
				
				} else if (path.equalsIgnoreCase("removedevice")) {
					/*
					 * Process the removedevice request. This removes the
					 * device (by its device ID) from the list of devices
					 * associated with the user.
					 */
					
					JSONTokener tokener = new JSONTokener(req.getInputStream());
					JSONObject requestParams = new JSONObject(tokener);
					if (RemoveDevice.processRequest(userinfo,requestParams)) {
						retVal = new ReturnResult();
					} else {
						retVal = new ReturnResult(Errors.ERROR_INTERNAL,"Internal error");
					}

				} else if (path.equalsIgnoreCase("devices")) {
					/*
					 * Process the devices request, which, for a given username
					 * returns an array of devices (device ID and public key)
					 * associated with the user. This allows each sender to
					 * know the devices to encrypt messages for.
					 */
					
					JSONTokener tokener = new JSONTokener(req.getInputStream());
					JSONObject requestParams = new JSONObject(tokener);
					retVal = Devices.processRequest(userinfo, requestParams);
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
