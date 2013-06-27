package de.decoit.visa.http.ajax.handlers;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.TEBackend;
import de.decoit.visa.http.QueryString;
import de.decoit.visa.http.ajax.AJAXServer;


/**
 * This HttpHandler implementation handles requests to remove an existing
 * {@link de.decoit.visa.topology.NetworkCable} from the topology. The cable will be removed from the
 * topology storage and the RDF model.<br>
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
public class RemoveCableHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(RemoveCableHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Get the URI of the request and extract the query string from it
		QueryString queryParameters = new QueryString(he.getRequestURI());

		// Create StringBuilder for the response
		String response = null;

		// Check if the query parameters are valid for this handler
		if(this.checkQueryParameters(queryParameters)) {
			// Any exception thrown during object removal will cause failure
			// of the AJAX request
			try {
				// Get the required object from topology storage
				TEBackend.TOPOLOGY_STORAGE.removeCable(queryParameters.get("oid").get());

				JSONObject rv = new JSONObject();
				rv.put("status", AJAXServer.AJAX_SUCCESS);
				rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());

				response = rv.toString();
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

		// The oid key must be present
		if(rv && (!pQueryString.containsKey("oid") || pQueryString.get("oid").get().isEmpty())) {
			rv = false;
		}

		return rv;
	}
}
