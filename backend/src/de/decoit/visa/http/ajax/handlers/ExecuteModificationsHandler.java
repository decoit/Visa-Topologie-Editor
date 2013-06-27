package de.decoit.visa.http.ajax.handlers;

import java.io.IOException;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import de.decoit.visa.TEBackend;
import de.decoit.visa.enums.ModificationTarget;
import de.decoit.visa.enums.ModificationTargetAttribute;
import de.decoit.visa.enums.PortOrientation;
import de.decoit.visa.http.QueryString;
import de.decoit.visa.http.ajax.AJAXServer;
import de.decoit.visa.http.ajax.ModificationQueue;
import de.decoit.visa.http.ajax.ModificationQueue.Modification;
import de.decoit.visa.net.IPAddress;
import de.decoit.visa.net.IPNetwork;
import de.decoit.visa.topology.NetworkComponent;
import de.decoit.visa.topology.NetworkComponent.Interface;
import de.decoit.visa.topology.NetworkComponent.Interface.IPConfig;


/**
 * This HttpHandler implementation executes all
 * {@link de.decoit.visa.http.ajax.ModificationQueue.Modification Modifications}
 * stored in the specified {@link de.decoit.visa.http.ajax.ModificationQueue
 * ModificationQueue}.<br>
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
public class ExecuteModificationsHandler extends DefaultHandler {
	private static Logger log = Logger.getLogger(ExecuteModificationsHandler.class.getName());


	@Override
	public void handle(HttpExchange he) throws IOException {
		log.info(he.getRequestURI().toString());

		// Get the URI of the request and extract the query string from it
		QueryString queryParameters = new QueryString(he.getRequestURI());

		// Create StringBuilder for the response
		String response = null;

		// Check if the query parameters are valid for this handler
		if(this.checkQueryParameters(queryParameters)) {
			// Any exception thrown during object creation will cause
			// failure of the AJAX request
			try {
				JSONObject rv = new JSONObject();

				// Get the modification queue which will be processed
				ModificationQueue mq = TEBackend.getModificationQueue(queryParameters.get("queueID").get());

				for(Modification mod : mq.getModifications()) {
					switch(mod.getTargetType()) {
						case COMPONENT:
							// Get the target object
							NetworkComponent nc = TEBackend.TOPOLOGY_STORAGE.getComponent(mod.getTargetLocalName());

							if(mod.getTargetAttribute().equals(ModificationTargetAttribute.COMPONENT_NAME)) {
								nc.setName(mod.getTargetAttributeValue());
							}
							else {
								throw new IllegalArgumentException("Unsupported target attribute for target object COMPONENT found");
							}
							break;
						case INTERFACE:
							// Get the target object
							Interface iface = TEBackend.TOPOLOGY_STORAGE.getInterface(mod.getTargetLocalName());

							if(mod.getTargetAttribute().equals(ModificationTargetAttribute.INTERFACE_NETWORK)) {
								List<IPConfig> ipList = iface.getNonLinkLocalIPConfig();
								if(ipList.size() > 1) {
									throw new UnsupportedOperationException("Changing network value on interfaces with multiple non-link-local addresses is not supported");
								}
								else if(ipList.size() == 1) {
									iface.removeIPConfiguration(ipList.get(0));
								}

								if(!mod.getTargetAttributeValue().equals("none")) {
									IPNetwork network = TEBackend.TOPOLOGY_STORAGE.getNetwork(mod.getTargetAttributeValue());
									IPAddress dummyAddr = network.getFreeIPAddress();

									iface.configureIP(dummyAddr, network);
								}
							}
							else if(mod.getTargetAttribute().equals(ModificationTargetAttribute.INTERFACE_ORIENTATION)) {
								PortOrientation ori = PortOrientation.valueOf(mod.getTargetAttributeValue());

								iface.setOrientation(ori);
							}
							else {
								throw new IllegalArgumentException("Unsupported target attribute for target object INTERFACE found");
							}
							break;
						default:
							throw new EnumConstantNotPresentException(ModificationTarget.class, mod.getTargetType().toString());
					}
				}

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

		if(rv && (!pQueryString.containsKey("queueID") || pQueryString.get("queueID").get().isEmpty())) {
			rv = false;
		}

		return rv;
	}
}
