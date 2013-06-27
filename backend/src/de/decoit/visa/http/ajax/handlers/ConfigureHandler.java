package de.decoit.visa.http.ajax.handlers;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.TEBackend;
import de.decoit.visa.http.QueryString;
import de.decoit.visa.http.ajax.AJAXServer;
import de.decoit.visa.ioconnector.IOToolException;


/**
 * The ConfigureHandler handles the /ajax/configure request. It receives
 * preferences from the frontend and stores them for later use in the main class
 * {@link TEBackend}. Additionally it closes any existing connection to the
 * IO-Tool since the call of this handler indicates a restart of the editor
 * frontend. This is done to prevent unused connections and problems connecting
 * to the IO-Tool.<br>
 * <br>
 * On success, a JSON object will be returned containing a JSON object of the
 * available VSA templates (key: vsatemplates).<br>
 * <br>
 * The stored preferences are:<br>
 * - Document root of the webserver the frontend is running on<br>
 * - Import and export directories for RDF/XML files<br>
 * - Dimensions of the editor grid (rows and cols)<br>
 * - Size of a single grid cell<br>
 * - The size of the margin of obstacle cells around each component<br>
 * <br>
 * Possible return messages of this handler are:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxException (exception caught but recovered)<br>
 * - ajaxMissing (missing arguments)
 *
 * @author Thomas Rix
 * @see AJAXServer
 * @see DefaultHandler
 */
public class ConfigureHandler extends DefaultHandler {
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
				// Close any existing connection to IO-Tool, this handler is
				// called on reload of the frontend
				try {
					TEBackend.closeIOConnector(false);
				}
				catch(IOToolException iote) {
					log.warn("Cannot close connection to the IO-Tool, using connection from old session");
				}

				// Process document root parameter
				String docRoot = queryParameters.get("documentRoot").get();

				// Set export directory
				StringBuilder sbExportPath = new StringBuilder(docRoot);
				if(!docRoot.endsWith("/")) {
					sbExportPath.append("/");
				}
				sbExportPath.append("export/");

				TEBackend.setExportPath(sbExportPath.toString());

				// Set import directory
				StringBuilder sbImportPath = new StringBuilder(docRoot);
				if(!docRoot.endsWith("/")) {
					sbImportPath.append("/");
				}
				sbImportPath.append("import/");

				TEBackend.setImportPath(sbImportPath.toString());

				// Process rows and cols parameter
				int rows = Integer.parseInt(queryParameters.get("rows").get());
				int cols = Integer.parseInt(queryParameters.get("cols").get());

				TEBackend.setGridDimensions(cols, rows);
				TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName("0.0.0.0").setSubgridDimensions(cols, rows);

				// Process cell size parameter
				int cSize = Integer.parseInt(queryParameters.get("csize").get());

				TEBackend.setCellSize(cSize);

				// Process component margin parameter
				int compMargin = Integer.parseInt(queryParameters.get("compMargin").get());

				TEBackend.setComponentMargin(compMargin);

				// Return success response
				JSONObject rv = new JSONObject();
				rv.put("status", AJAXServer.AJAX_SUCCESS);
				rv.put("vsatemplates", TEBackend.RDF_MANAGER.vsaTemplatesToJSON());
				response = rv.toString();
			}
			catch(Throwable ex) {
				TEBackend.logException(ex, log);

				// Exception was thrown during configuration
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

		// The documentRoot key must be present
		if(rv && (!pQueryString.containsKey("documentRoot") || pQueryString.get("documentRoot").get().isEmpty())) {
			rv = false;
		}

		// Vertical grid size must be present
		if(rv && (!pQueryString.containsKey("rows") || !pQueryString.get("rows").get().matches("\\d+"))) {
			rv = false;
		}

		// Vertical grid size must be present
		if(rv && (!pQueryString.containsKey("cols") || !pQueryString.get("cols").get().matches("\\d+"))) {
			rv = false;
		}

		// Cell size property must be present
		if(rv && (!pQueryString.containsKey("csize") || !pQueryString.get("csize").get().matches("\\d+"))) {
			rv = false;
		}

		// Component margin property must be present
		if(rv && (!pQueryString.containsKey("compMargin") || !pQueryString.get("compMargin").get().matches("\\d+"))) {
			rv = false;
		}

		return rv;
	}
}
