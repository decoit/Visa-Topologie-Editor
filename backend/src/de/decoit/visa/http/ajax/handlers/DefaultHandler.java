package de.decoit.visa.http.ajax.handlers;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.decoit.visa.http.ajax.AJAXServer;


/**
 * This HttpHandler implementation handles AJAX requests with an unknown action.
 * It acts as a super class for all other request handlers.<br>
 * <br>
 * All requests handled by this class are answered with the return message
 * 'ajaxUnknown'.
 *
 * @author Thomas Rix
 * @see AJAXServer
 */
public class DefaultHandler implements HttpHandler {
	private static Logger log = Logger.getLogger(DefaultHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.warn("Unknown AJAX request: " + he.getRequestURI().toString());

		// Fill the response with the error code
		JSONObject rv = new JSONObject();
		try {
			rv.put("status", AJAXServer.AJAX_ERROR_UNKNOWN_REQ);
		}
		catch(JSONException ex) {
			/* Ignore */
		}

		String response = rv.toString();

		// Send the response
		sendResponse(he, response);
	}


	/**
	 * Send a reponse for the provided HTTP exchange with the content provided
	 * in pReponse. Content-Type header will be set to "text/html"
	 *
	 * @param pHE Object of the HTTP exchange
	 * @param pResponse Response content
	 * @throws IOException
	 */
	protected void sendResponse(HttpExchange pHE, String pResponse) throws IOException {
		StringBuilder sb = new StringBuilder("Sending response: ");
		sb.append(pResponse);
		log.info(sb.toString());

		// Set the content type of the request
		pHE.getResponseHeaders().add("Content-type", "text/html");

		// Send the response headers
		pHE.sendResponseHeaders(200, pResponse.length());

		// Write the actual response to the HttpExchange object
		OutputStream os = pHE.getResponseBody();
		os.write(pResponse.getBytes());
		os.close();
	}
}
