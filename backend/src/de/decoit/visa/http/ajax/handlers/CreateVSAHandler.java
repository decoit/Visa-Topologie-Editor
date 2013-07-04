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
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.TEBackend;
import de.decoit.visa.http.QueryString;
import de.decoit.visa.http.ajax.AJAXServer;


/**
 * This HttpHandler implementation handles requests to import a VSA template
 * into the existing topology.<br>
 * <br>
 * On success, a JSON object will be returned containing the JSON object of the
 * current topology (key: topology)<br>
 * <br>
 * Any exception thrown during the processing of the request will cause the
 * request to fail. One attempt to recover the topology from the exception is
 * made. If that fails, the topology and RDF model will be cleared.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxException (exception caught but recovered)<br>
 * - ajaxExceptionUnresolved (exception caught and recovery not possible)<br>
 * - ajaxMissing (missing arguments)
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class CreateVSAHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(ConfigureHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Get the URI of the request and extract the query string from it
		QueryString queryParameters = new QueryString(he.getRequestURI());

		// Create StringBuilder for the response
		String response = null;

		// Check if the query parameters are valid for this handler
		if(this.checkQueryParameters(queryParameters)) {
			try {
				HashMap<String, String> connTargets = new HashMap<>();
				HashMap<String, String> connVLANs = new HashMap<>();

				boolean connParamsPresent = true;

				Document tpl = TEBackend.RDF_MANAGER.getVSATemplate(Integer.parseInt(queryParameters.get("tplid").get()));

				// Verify that all connections are present in the query
				// parameters
				// Get all elements of type CONNECTION and iterate over the list
				NodeList nl = tpl.getElementsByTagName("CONNECTION");
				for(int i = 0; i < nl.getLength(); i++) {
					Node n = nl.item(i);

					// Check if the node is an Element. Should be the case, just
					// for security reasons
					if(n.getNodeType() == Node.ELEMENT_NODE) {
						// Cast the node to Element to access the attributes
						Element e = (Element) n;

						String id = e.getAttribute("id");

						if(!queryParameters.containsKey(id)) {
							// A connection is missing, set the response to the
							// missing arguments message and break the loop
							connParamsPresent = false;

							JSONObject rv = new JSONObject();
							rv.put("status", AJAXServer.AJAX_ERROR_MISSING_ARGS);
							response = rv.toString();

							break;
						}
						else {
							connTargets.put(id, queryParameters.get(id).get());

							StringBuilder sbVLAN = new StringBuilder(id);
							sbVLAN.append("_vlan");

							if(queryParameters.containsKey(sbVLAN.toString())) {
								connVLANs.put(id, queryParameters.get(sbVLAN.toString()).get());
							}
						}
					}
				}

				if(connParamsPresent) {
					TEBackend.RDF_MANAGER.importRDFTemplate(Integer.parseInt(queryParameters.get("tplid").get()), queryParameters.get("name").get(), connTargets, connVLANs);

					// Return success response
					JSONObject rv = new JSONObject();
					rv.put("status", AJAXServer.AJAX_SUCCESS);
					rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());
					response = rv.toString();
				}
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
			// Missing or malformed query string, set response to error code
			JSONObject rv = new JSONObject();
			try {
				// Missing or malformed query string, set response to error code
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

		if(rv && (!pQueryString.containsKey("tplid") || !pQueryString.get("tplid").get().matches("\\d+"))) {
			rv = false;
		}

		if(rv && (!pQueryString.containsKey("name") || pQueryString.get("name").get().isEmpty())) {
			rv = false;
		}

		return rv;
	}
}
