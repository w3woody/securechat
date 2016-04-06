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
import com.chaosinmotion.securechat.server.config.Config;
import com.chaosinmotion.securechat.server.database.Database;
import com.chaosinmotion.securechat.server.messages.NotificationService;

/**
 * This class helps with installation by verifying we can access the database
 * and providing us a means of knowing what the path is of our installed
 * server.
 * @author woody
 */
public class ValidateServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		ServletOutputStream out = resp.getOutputStream();

		resp.setContentType("text/html");

		out.println("<html><head><title>SecureChat</title></head>");
		out.println("<body>");
		
		out.println("<h1>Validating</h1>");
		
		/*
		 * Run our validation steps: make sure we can open the database.
		 */
		
		String str = Config.get().getProperty("dburl");
		out.print("<p>DB: " + str + " -- ");
		
		try {
			int v = Database.validate();
			out.println("<b>Success:</b> schema version " + v + ".</p>");
		}
		catch (Throwable e) {
			out.println("<b>Failure:</b> " + e.getLocalizedMessage() + ".</p>");
		}
		
		/*
		 * Now get the server URL the client would use
		 */
		
		String host = req.getRequestURL().toString();
		int index = host.indexOf("/ss/v");
		host = host.substring(0, index);
		out.println("<p>Client path: <u>" + host + "</u></p>");
		out.println("<p>(Client path is what the user uses when prompted for a server URL.)</p>");
		
		/*
		 *  Test the notification
		 */
		
		NotificationService n = NotificationService.getShared();
		if (n.isRunning()) {
			out.print("<p>Notification service ");
			if (n.getSSLFlag()) out.print("<b>is secure</b> ");
			out.print("running on " + n.getServerAddress() + ":" + n.getServerPort() + "</p>");
		} else {
			Throwable ex = n.getStartException();
			out.println("<p>Notification service is not running.</p>");
			while (ex != null) {
				out.println("<p>Exception: " + ex.getMessage() + "</p>");
				out.println("<blockquote>");
				StackTraceElement[] stack = ex.getStackTrace();
				for (StackTraceElement el: stack) {
					out.println("<p>" + el.toString() + "</p>");
				}
				out.println("</blockquote>");
				
				ex = ex.getCause();
			}
		}
		
		out.println("<p><i>SecureChat Copyright &copy; 2016 William Edward Woody.</i></p>");
		out.println("</body></html>");
	}
}
