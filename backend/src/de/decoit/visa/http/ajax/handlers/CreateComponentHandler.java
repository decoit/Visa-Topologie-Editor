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
import de.decoit.visa.Dimension2D;
import de.decoit.visa.TEBackend;
import de.decoit.visa.http.QueryString;
import de.decoit.visa.http.ajax.AJAXServer;
import de.decoit.visa.topology.NCSwitch;
import de.decoit.visa.topology.NCVM;
import de.decoit.visa.topology.NetworkComponent;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup;


/**
 * This HttpHandler implementation handles requests to create a new
 * {@link NetworkComponent}. It will create a new object which will be added to
 * the topology storage.<br>
 * <br>
 * On success, a JSON object will be returned containing the JSON object of the
 * new created component (key: newComponent), an indicator if the created
 * component was a switch (key: switch) and the current topology (key: topology)<br>
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
public class CreateComponentHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(CreateComponentHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Get the URI of the request and extract the query string from it
		QueryString queryParameters = new QueryString(he.getRequestURI());

		// Create StringBuilder for the response
		String response = null;

		// Check if the query parameters are valid for this handler
		if(this.checkQueryParameters(queryParameters)) {
			// Check oid scheme
			Pattern p = Pattern.compile("^new_(.+?)$");
			Matcher m = p.matcher(queryParameters.get("oid").get());
			if(m.matches()) {
				// Extract component type
				String compType = m.group(1);
				NetworkComponent newNC = null;

				// Any exception thrown during object creation will cause
				// failure of the AJAX request
				try {
					String name = queryParameters.get("name").get();
					Dimension2D dim = new Dimension2D(Integer.parseInt(queryParameters.get("dimX").get()), Integer.parseInt(queryParameters.get("dimY").get()));

					ComponentGroup cg = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByID(queryParameters.get("group").get());

					JSONObject responseJSON = new JSONObject();

					// Create the new object according to the component type
					// extracted above
					switch(compType) {
						case NCSwitch.TYPE:
							newNC = TEBackend.TOPOLOGY_STORAGE.createSwitch(queryParameters.get("ifori").toList(), name, dim, null);
							newNC.getConfig().setComponentGroup(cg.getName());
							((NCSwitch) newNC).createGroupSwitches();

							responseJSON.put("newComponent", ((NCSwitch) newNC).toJSON());
							responseJSON.put("switch", true);
							responseJSON.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());

							break;
						case NCVM.TYPE:
							newNC = TEBackend.TOPOLOGY_STORAGE.createVM(queryParameters.get("ifori").toList(), name, dim, null);
							newNC.getConfig().setComponentGroup(cg.getName());

							responseJSON.put("newComponent", newNC.toJSON());
							responseJSON.put("switch", false);
							responseJSON.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());

							break;
						default:
							responseJSON = null;
					}

					responseJSON.put("status", AJAXServer.AJAX_SUCCESS);

					// Set the HTTP response to the identifier string of the new
					// component
					response = responseJSON.toString();
				}
				catch(Throwable ex) {
					TEBackend.logException(ex, log);

					try {
						// Synchronize the topology with the RDF model to
						// resolve
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
						// Exception during synchronization, the model may have
						// been
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
				// Received object ID did not match the required scheme
				JSONObject rv = new JSONObject();
				try {
					// Missing or malformed query string, set response to error
					// code
					rv.put("status", AJAXServer.AJAX_ERROR_MISSING_ARGS);
				}
				catch(JSONException exc) {
					/* Ignore */
				}

				response = rv.toString();
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

		// The oid key must contain an object ID that starts with "new_"
		if(rv && (!pQueryString.containsKey("oid") || !pQueryString.get("oid").get().startsWith("new_"))) {
			rv = false;
		}

		if(rv && (!pQueryString.containsKey("name") || pQueryString.get("name").get().isEmpty())) {
			rv = false;
		}

		if(rv && (!pQueryString.containsKey("group") || pQueryString.get("group").get().isEmpty())) {
			rv = false;
		}

		if(rv && (!pQueryString.containsKey("dimX") || !pQueryString.get("dimX").get().matches("\\d+"))) {
			rv = false;
		}

		if(rv && (!pQueryString.containsKey("dimY") || !pQueryString.get("dimY").get().matches("\\d+"))) {
			rv = false;
		}

		if(rv && (!pQueryString.containsKey("ifori") || pQueryString.get("ifori").size() != Integer.parseInt(pQueryString.get("ifnum").get()))) {
			rv = false;
		}

		return rv;
	}
}
