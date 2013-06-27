package de.decoit.visa.http.ajax.handlers;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.TEBackend;
import de.decoit.visa.http.ajax.AJAXServer;


/**
 * This handler tells the {@link de.decoit.visa.topology.TopologyStorage} to generate a JSON object of
 * the current topology.<br>
 * <br>
 * On success, the generated JSON object will be returned (key: topology).<br>
 * <br>
 * Any exception thrown during the processing of the request will cause the
 * request to fail. <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxException (exception caught and no recovery attempt made)<br>
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class RequestTopologyHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(RequestTopologyHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		String response;
		try {
			JSONObject rv = new JSONObject();
			rv.put("status", AJAXServer.AJAX_SUCCESS);
			rv.put("topology", TEBackend.TOPOLOGY_STORAGE.genTopologyJSON());
			response = rv.toString();
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

		// Send the response headers
		sendResponse(he, response);
	}
}
