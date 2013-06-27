package de.decoit.visa.http.ajax.handlers;

import java.io.IOException;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.TEBackend;
import de.decoit.visa.enums.IOToolRequestStatus;
import de.decoit.visa.http.ajax.AJAXServer;


/**
 * This HttpHandler implementation handles requests to load the list of
 * available topologies from the IO-Tool.<br>
 * <br>
 * On success, a JSON object will be returned containing a JSON object that
 * contains the data received by the IO-Tool response (key: data) and the return
 * code and message of that reponse (keys: returncode and message). The return
 * code and message keys will also be present in an 'ajaxGeneral' reply.<br>
 * <br>
 * Any exception thrown during the processing of the request will cause the
 * request to fail.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxGeneral (general error, see message key for details)<br>
 * - ajaxException (exception caught and no recovery attempt made)<br>
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class IOToolTopoListHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(IOToolTopoListHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Create String for the response
		String response = null;

		// Any exception thrown during object creation will
		// cause failure of the AJAX request
		try {
			IOToolRequestStatus status = TEBackend.getIOConnector().requestTopologyList();

			JSONObject rv = new JSONObject();

			if(status == IOToolRequestStatus.SUCCESS) {
				rv.put("status", AJAXServer.AJAX_SUCCESS);

				Map<String, String> data = TEBackend.getIOConnector().getLastReturnData();

				JSONObject dataJSON = new JSONObject();
				if(data != null) {
					for(Map.Entry<String, String> topoEntry : data.entrySet()) {
						dataJSON.put(topoEntry.getKey(), topoEntry.getValue());
					}
				}
				rv.put("data", dataJSON);
			}
			else if(status == IOToolRequestStatus.IOTOOL_BUSY) {
				rv.put("status", AJAXServer.AJAX_ERROR_IOTOOL_BUSY);
			}
			else {
				rv.put("status", AJAXServer.AJAX_ERROR_GENERAL);
			}

			rv.put("returncode", TEBackend.getIOConnector().getLastReturnCode());
			rv.put("message", TEBackend.getIOConnector().getLastReturnMsg());

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

		// Send the response
		sendResponse(he, response);
	}
}
