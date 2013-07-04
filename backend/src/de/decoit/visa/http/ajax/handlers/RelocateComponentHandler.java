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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.TEBackend;
import de.decoit.visa.http.QueryString;
import de.decoit.visa.http.ajax.AJAXServer;
import de.decoit.visa.topology.NetworkComponent;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup;


/**
 * This HttpHandler implementation handles requests to relocate an existing
 * {@link NetworkComponent} to a new position on the editor grid.<br>
 * <br>
 * On success, a JSON object will be returned containing the JSON object of the
 * relocated component (key: component) and the current topology (key: topology)<br>
 * <br>
 * Any exception thrown during the processing of the request will cause the
 * request to fail. One attempt to recover the topology from the exception is
 * made. If that fails, the topology and RDF model will be cleared.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxInvalid (invalid object type)<br>
 * - ajaxException (exception caught but recovered)<br>
 * - ajaxExceptionUnresolved (exception caught and recovery not possible)<br>
 * - ajaxMissing (missing arguments)
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class RelocateComponentHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(RelocateComponentHandler.class.getName());

	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Get the URI of the request and extract the query string from it
		QueryString queryParameters = new QueryString(he.getRequestURI());

		// Create StringBuilder for the response
		String response = null;

		// Check if the query parameters are valid for this handler
		if(this.checkQueryParameters(queryParameters)) {
			// Any exception thrown during object relocation will cause
			// failure of the AJAX request
			try {
				// Get the required object from topology storage
				NetworkComponent nc = TEBackend.TOPOLOGY_STORAGE.getComponent(queryParameters.get("oid").get());

				if(nc != null) {
					nc.getConfig().setGridLocation(Integer.parseInt(queryParameters.get("glocX").get()), Integer.parseInt(queryParameters.get("glocY").get()));

					JSONObject rv = new JSONObject();
					rv.put("status", AJAXServer.AJAX_SUCCESS);
					rv.put("component", nc.toJSON());
					rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());

					response = rv.toString();
				}
				else {
					if(queryParameters.get("oid").get().startsWith("gSwitch_")) {
						Pattern p = Pattern.compile("^gSwitch_(cgroup\\d+?)_([0-9a-zA-Z_\\-]+)$");
						Matcher m = p.matcher(queryParameters.get("oid").get());

						if(m.matches()) {
							ComponentGroup cg = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByID(m.group(1));
							cg.getGroupSwitch(m.group(2)).setGridLocation(Integer.parseInt(queryParameters.get("glocX").get()), Integer.parseInt(queryParameters.get("glocY").get()));

							JSONObject rv = new JSONObject();
							rv.put("status", AJAXServer.AJAX_SUCCESS);
							rv.put("component", cg.getGroupSwitch(m.group(2)).toJSON());
							rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());

							response = rv.toString();
						}
						else {
							log.debug("Regex did not match");
							JSONObject rv = new JSONObject();
							rv.put("status", AJAXServer.AJAX_ERROR_INVALID_OBJ);
							response = rv.toString();
						}
					}
					else {
						String cgID = queryParameters.get("oid").get();

						if(TEBackend.TOPOLOGY_STORAGE.hasComponentGroup(cgID)) {
							ComponentGroup cg = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByID(cgID);

							cg.setGridLocation(Integer.parseInt(queryParameters.get("glocX").get()), Integer.parseInt(queryParameters.get("glocY").get()));

							JSONObject rv = new JSONObject();
							rv.put("status", AJAXServer.AJAX_SUCCESS);
							rv.put("component", cg.toJSON());
							rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());

							response = rv.toString();
						}
						else {
							JSONObject rv = new JSONObject();
							rv.put("status", AJAXServer.AJAX_ERROR_INVALID_OBJ);
							response = rv.toString();
						}
					}
				}
			}
			catch(Throwable ex) {
				TEBackend.logException(ex, log);

				try {
					// Synchronize the topology with the RDF model to resolve
					// any errors caused by the caught exception
					TEBackend.RDF_MANAGER.syncTopologyToRDF();

					JSONObject rv = new JSONObject();
					rv.put("status", AJAXServer.AJAX_ERROR_EXCEPTION);
					rv.put("type", ex.getClass().getSimpleName());
					rv.put("message", ex.getMessage());
					rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());
					response = rv.toString();
				}
				catch(Throwable e) {
					// Exception during synchronization, the model may have been
					// corrupted so the whole backend was cleared
					JSONObject rv = new JSONObject();
					try {
						rv.put("status", AJAXServer.AJAX_ERROR_EXCEPTION_UNRESOLVED);
						rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());
					}
					catch(JSONException exc) {
						/* Ignore */
					}

					response = rv.toString();
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

		// The oid key must be present
		if(rv && (!pQueryString.containsKey("oid") || pQueryString.get("oid").get().isEmpty())) {
			rv = false;
		}

		// Horizontal coordinate for grid location must be present
		if(rv && (!pQueryString.containsKey("glocX") || pQueryString.get("glocX").get().isEmpty())) {
			rv = false;
		}

		// Vertical coordinate for grid location must be present
		if(rv && (!pQueryString.containsKey("glocY") || pQueryString.get("glocY").get().isEmpty())) {
			rv = false;
		}

		return rv;
	}
}
