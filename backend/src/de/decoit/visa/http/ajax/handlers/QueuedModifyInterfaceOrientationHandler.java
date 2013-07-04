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
import de.decoit.visa.enums.ModificationTarget;
import de.decoit.visa.enums.ModificationTargetAttribute;
import de.decoit.visa.http.QueryString;
import de.decoit.visa.http.ajax.AJAXServer;
import de.decoit.visa.http.ajax.ModificationQueue;


/**
 * This HttpHandler implementation queues a request to change the orientation of
 * an interface. The change is not executed, it is queued in a
 * {@link de.decoit.visa.http.ajax.ModificationQueue ModificationQueue} for
 * later use.<br>
 * <br>
 * On success, a JSON object will be returned which only contains a status
 * message<br>
 * <br>
 * Any exception thrown during the processing of the request will cause the
 * request to fail.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxException (exception caught and no recovery attempt made)<br>
 * - ajaxMissing (missing arguments)
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class QueuedModifyInterfaceOrientationHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(QueuedModifyInterfaceOrientationHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Get the URI of the request and extract the query string from it
		QueryString queryParameters = new QueryString(he.getRequestURI());

		// Create StringBuilder for the response
		String response = null;

		// Check if the query parameters are valid for this handler
		if(this.checkQueryParameters(queryParameters)) {
			// Any exception thrown during object creation will cause
			// failure of the AJAX request
			try {
				JSONObject rv = new JSONObject();

				ModificationQueue mq = TEBackend.getModificationQueue(queryParameters.get("queueID").get());
				mq.addModification(ModificationTarget.INTERFACE, queryParameters.get("ifID").get(), ModificationTargetAttribute.INTERFACE_ORIENTATION, queryParameters.get("ori").get());

				rv.put("status", AJAXServer.AJAX_SUCCESS);

				response = rv.toString();
			}
			catch(Throwable ex) {
				TEBackend.logException(ex, log);

				try {
					JSONObject rv = new JSONObject();
					rv.put("status", AJAXServer.AJAX_ERROR_EXCEPTION);
					rv.put("type", ex.getClass().getSimpleName());
					rv.put("message", ex.getMessage());
					response = rv.toString();
				}
				catch(JSONException e) {
					/* Ignore */
				}
			}

		}
		else {
			// Missing or malformed query string, set response to error code
			JSONObject rv = new JSONObject();
			try {
				rv.put("status", AJAXServer.AJAX_ERROR_MISSING_ARGS);
			}
			catch(JSONException exc) {
				/* Ignore */
			}

			response = rv.toString();
		}

		// Send the response
		sendResponse(he, response);
	}


	/**
	 * Check if the provided QueryString object contains all keys required by
	 * this handler and if their values are valid.
	 *
	 * @param pQueryString A QueryString object with all query parameters of the
	 *            request
	 * @return true if no problems were found, false if something is missing or
	 *         malformed
	 */
	private boolean checkQueryParameters(QueryString pQueryString) {
		boolean rv = true;

		if(rv && (!pQueryString.containsKey("ifID") || pQueryString.get("ifID").get().isEmpty())) {
			rv = false;
		}

		if(rv && (!pQueryString.containsKey("queueID") || pQueryString.get("queueID").get().isEmpty())) {
			rv = false;
		}

		if(rv && (!pQueryString.containsKey("ori") || pQueryString.get("ori").get().isEmpty())) {
			rv = false;
		}

		return rv;
	}
}
