/*
 *  Copyright (C) 2013, DECOIT GmbH
 *
 *	This file is part of VISA Topology-Editor.
 *
 *	VISA Topology-Editor is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by the
 *	Free Software Foundation, either version 3 of the License, or (at your option)
 *	any later version.
 *
 *	VISA Topology-Editor is distributed in the hope that it will be useful, but
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *	more details.
 *
 *	You should have received a copy of the GNU General Public License along with
 *	VISA Topology-Editor. If not, see <http://www.gnu.org/licenses/>.
 */

package de.decoit.visa.http.ajax.handlers;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.TEBackend;
import de.decoit.visa.http.ajax.AJAXServer;
import de.decoit.visa.ioconnector.IOToolException;


/**
 * This implementation of the HttpHandler tells the backend to shutdown all
 * services and stop itself.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxShutdown (backend shutdown successful)
 *
 * @author Thomas Rix
 *
 */
public class ShutdownHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(ShutdownHandler.class.getName());

	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Tell the frontend what is happening
		JSONObject rv = new JSONObject();
		try {
			rv.put("status", AJAXServer.AJAX_SHUTDOWN);
		}
		catch(JSONException ex) {
			/* Ignore */
		}
		String response = rv.toString();

		// Send the response
		sendResponse(he, response);

		try {
			// Stop the backend
			TEBackend.stopBackend();
		}
		catch(IOToolException iote) {
			/* Ignore, should not occur */
		}
	}
}
