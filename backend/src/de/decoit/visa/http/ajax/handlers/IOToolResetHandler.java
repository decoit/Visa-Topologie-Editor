package de.decoit.visa.http.ajax.handlers;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.TEBackend;
import de.decoit.visa.enums.IOToolRequestStatus;
import de.decoit.visa.http.ajax.AJAXServer;


/**
 * This HttpHandler implementation handles requests to reset the IO-Tool.<br>
 * <br>
 * On success, a JSON object will be returned containing the return code and
 * message of the IO-Tool reponse (keys: returncode and message). The return
 * code and message keys will also be present in an 'ajaxGeneral' reply.<br>
 * <br>
 * Any exception thrown during the processing of the request will cause the
 * request to fail.<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxIOWait (wait for completion)<br>
 * - ajaxIOBusy (IO-Tool is busy)<br>
 * - ajaxGeneral (general error, see message key for details)<br>
 * - ajaxException (exception caught and no recovery attempt made)
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class IOToolResetHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(IOToolResetHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Create String for the response
		String response = null;

		// Any exception thrown during object creation will
		// cause failure of the AJAX request
		try {
			// Execute the request to the IO-Tool
			IOToolRequestStatus status = TEBackend.getIOConnector().resetIOTool();

			JSONObject rv = new JSONObject();
			if(status == IOToolRequestStatus.SUCCESS) {
				rv.put("status", AJAXServer.AJAX_SUCCESS);
			}
			else if(status == IOToolRequestStatus.WAIT) {
				rv.put("status", AJAXServer.AJAX_IOTOOL_WAIT);
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