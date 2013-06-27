package de.decoit.visa.http.ajax.handlers;

import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.Position2D;
import de.decoit.visa.TEBackend;
import de.decoit.visa.http.QueryString;
import de.decoit.visa.http.QueryString.QueryStringValue;
import de.decoit.visa.http.ajax.AJAXServer;
import de.decoit.visa.topology.NetworkCable;
import de.decoit.visa.topology.NetworkComponent;


/**
 * This HttpHandler implementation handles requests to create a new
 * {@link NetworkCable}. It will create a new object which will be added to the
 * topology storage.<br>
 * <br>
 * On success, a JSON object will be returned containing the JSON object of the
 * new created cable (key: newCable) and the current topology (key: topology)<br>
 * <br>
 * Any exception thrown during the processing of the request will cause the
 * request to fail. One attempt to recover the topology from the exception is
 * made. If that fails, the topology and RDF model will be cleared.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxGeneral (general error, see message key for details)<br>
 * - ajaxInvalid (invalid object type)<br>
 * - ajaxException (exception caught but recovered)<br>
 * - ajaxExceptionUnresolved (exception caught and recovery not possible)<br>
 * - ajaxMissing (missing arguments)
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class CreateCableHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(CreateCableHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Get the URI of the request and extract the query string from it
		QueryString queryParameters = new QueryString(he.getRequestURI());

		// Create String for the response
		String response = null;

		// Check if the query parameters are valid for this handler
		if(this.checkQueryParameters(queryParameters)) {
			// Any exception thrown during object creation will
			// cause failure of the AJAX request
			try {
				NetworkComponent.Interface startPort = TEBackend.TOPOLOGY_STORAGE.getInterface(queryParameters.get("oids").get());
				NetworkComponent.Interface endPort = TEBackend.TOPOLOGY_STORAGE.getInterface(queryParameters.get("oide").get());

				// Check if both ports are present
				if(startPort != null && endPort != null) {
					if(startPort.isConnected() || endPort.isConnected()) {
						// Error: One of the ports is already connected to a
						// cable!
						JSONObject rv = new JSONObject();
						rv.put("status", AJAXServer.AJAX_ERROR_GENERAL);
						rv.put("message", "One of the interfaces is already connected");
						response = rv.toString();
					}
					else {
						// Create the new NetworkCable
						// TODO no GroupInterface information from frontend
						NetworkCable newCable = TEBackend.TOPOLOGY_STORAGE.createCable(startPort, endPort, null);

						LinkedList<Position2D> corners = this.createCableCorners(queryParameters.get("path"));
						newCable.setPathCorners(corners);

						if(log.isDebugEnabled()) {
							StringBuilder sb = new StringBuilder("New Cable created: ");
							sb.append(newCable.getIdentifier());
							log.debug(sb.toString());
						}

						// Return the new cable and topology
						JSONObject rv = new JSONObject();
						rv.put("status", AJAXServer.AJAX_SUCCESS);
						rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());
						rv.put("newCable", newCable.toJSON());
						response = rv.toString();
					}
				}
				else {
					JSONObject rv = new JSONObject();
					rv.put("status", AJAXServer.AJAX_ERROR_INVALID_OBJ);
					response = rv.toString();
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

		// The oids key must be present and the value must not be empty
		if(rv && (!pQueryString.containsKey("oids") || pQueryString.get("oids").get().isEmpty())) {
			rv = false;
		}

		// The oide key must be present and the value must not be empty
		if(rv && (!pQueryString.containsKey("oide") || pQueryString.get("oide").get().isEmpty())) {
			rv = false;
		}

		// Path corners must be present at contain at least 2 elements
		if(rv && (!pQueryString.containsKey("path") || pQueryString.get("path").size() < 2)) {
			rv = false;
		}

		return rv;
	}


	/**
	 * Create a LinkedList\<Position2D\> from a QueryStringValue object
	 * containing the provided path corners of a NetworkCable.
	 *
	 * @param pCorners QueryStringValue object containing the provided path
	 *            corners, each value must match the scheme c_Y_X where Y and X
	 *            are integer numbers
	 * @return LinkedList containing cable corners as Position2D objects
	 * @throws IllegalArgumentException
	 */
	private LinkedList<Position2D> createCableCorners(QueryStringValue pCorners) throws IllegalArgumentException {
		LinkedList<Position2D> rv = new LinkedList<>();
		for(int i = 0; i < pCorners.size(); i++) {
			Pattern p = Pattern.compile("^c_(\\d+)_(\\d+)$");
			Matcher m = p.matcher(pCorners.get(i));

			if(m.matches()) {
				int x = Integer.valueOf(m.group(2));
				int y = Integer.valueOf(m.group(1));

				rv.add(new Position2D(x, y, TEBackend.getGridDimensions()));
			}
			else {
				throw new IllegalArgumentException("Malformed cell ID provided");
			}
		}

		return rv;
	}
}
