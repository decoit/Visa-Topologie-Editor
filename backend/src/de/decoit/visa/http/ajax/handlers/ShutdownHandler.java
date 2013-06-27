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
 * This implementation of the HttpHandler tells the backend to shutdown all
 * services and stop itself.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxShutdown (backend shutdown successful)
 *
 * @author Thomas Rix
 *
 */
public class ShutdownHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(ShutdownHandler.class.getName());

	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Tell the frontend what is happening
		JSONObject rv = new JSONObject();
		try {
			rv.put("status", AJAXServer.AJAX_SHUTDOWN);
		}
		catch(JSONException ex) {
			/* Ignore */
		}
		String response = rv.toString();

		// Send the response
		sendResponse(he, response);

		try {
			// Stop the backend
			TEBackend.stopBackend();
		}
		catch(IOToolException iote) {
			/* Ignore, should not occur */
		}
	}
}
