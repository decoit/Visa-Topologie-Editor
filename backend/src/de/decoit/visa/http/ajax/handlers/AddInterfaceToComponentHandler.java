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
import de.decoit.visa.enums.PortOrientation;
import de.decoit.visa.http.QueryString;
import de.decoit.visa.http.ajax.AJAXServer;
import de.decoit.visa.topology.NetworkComponent.Interface;


/**
 * This HttpHandler implementation handles requests to add an additional
 * {@link de.decoit.visa.topology.NetworkComponent.Interface Interface} to an
 * existing {@link de.decoit.visa.topology.NetworkComponent NetworkComponent}.<br>
 * <br>
 * On success, a JSON object will be returned containing the JSON object of the
 * new interface (key: iface) and the current topology (key: topology)<br>
 * <br>
 * Any exception thrown during the processing of the request will cause the
 * request to fail. One attempt to recover the topology from the exception is
 * made. If that fails, the topology and RDF model will be cleared.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxException (exception caught but recovered)<br>
 * - ajaxMissing (missing arguments)
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class AddInterfaceToComponentHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(AddInterfaceToComponentHandler.class.getName());


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

				Interface iface = TEBackend.TOPOLOGY_STORAGE.getComponent(queryParameters.get("compID").get()).getConfig().createInterface(PortOrientation.valueOf(queryParameters.get("ori").get()));

				rv.put("status", AJAXServer.AJAX_SUCCESS);
				rv.put("iface", iface.toJSON());
				rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());

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

		if(rv && (!pQueryString.containsKey("compID") || pQueryString.get("compID").get().isEmpty())) {
			rv = false;
		}

		if(rv && (!pQueryString.containsKey("ori") || pQueryString.get("ori").get().isEmpty())) {
			rv = false;
		}

		return rv;
	}
}
