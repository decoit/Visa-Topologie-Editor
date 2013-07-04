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
 * This HttpHandler implementation handles requests to disconnect from the
 * IO-Tool<br>
 * <br>
 * Any exception thrown during the processing of the request will cause the
 * request to fail.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxException (exception caught and no recovery attempt made)<br>
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class IOToolDisconnectHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(IOToolDisconnectHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Create String for the response
		String response = null;

		// Any exception thrown during object creation will
		// cause failure of the AJAX request
		try {
			TEBackend.closeIOConnector(false);

			JSONObject rv = new JSONObject();
			rv.put("status", AJAXServer.AJAX_SUCCESS);
			response = rv.toString();
		}
		catch(IOToolException iote) {
			JSONObject rv = new JSONObject();
			try {
				rv.put("status", AJAXServer.AJAX_ERROR_IOTOOL_BUSY);
				response = rv.toString();
			}
			catch(JSONException exc) {
				/* Ignore */
			}
		}
		catch(Throwable ex) {
			TEBackend.logException(ex, log);

			JSONObject rv = new JSONObject();
			try {
				rv.put("status", AJAXServer.AJAX_ERROR_EXCEPTION);
				rv.put("type", ex.getClass().getSimpleName());
				rv.put("message", ex.getMessage());
			}
			catch(JSONException exc) {
				/* Ignore */
			}

			response = rv.toString();
		}

		// Send the response
		sendResponse(he, response);
	}
}
