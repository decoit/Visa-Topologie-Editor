package de.decoit.visa.http.ajax;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import org.apache.log4j.Logger;
import com.sun.net.httpserver.HttpServer;
import de.decoit.visa.http.ajax.handlers.*;


/**
 * This class starts and stops the HttpServer instance which handles AJAX
 * requests from the frontend. It listens on the port which was provided to the
 * constructor. The action AJAX request is identified by the path of the URL:<br>
 * <br>
 * /ajax/configure (set configuration options for the backend)<br>
 * /ajax/updateTopology (update topology information, for example the name)<br>
 * /ajax/ioConnect (connect to the IO-Tool)<br>
 * /ajax/ioDisconnect (disconnect from the IO-Tool)<br>
 * /ajax/ioTopoList (request the topology list from the IO-Tool)<br>
 * /ajax/ioLoadTopo (load a specific topology from the IO-Tool)<br>
 * /ajax/ioWriteTopo (save a specific topology from the IO-Tool)<br>
 * /ajax/requestTopology (request JSON object of the current topology)<br>
 * /ajax/createComponent (create a new NetworkComponent)<br>
 * /ajax/relocateComponent (relocate existing NetworkComponent)<br>
 * /ajax/removeComponent (remove existing NetworkComponent)<br>
 * /ajax/queuedModifyInterfaceIP (queue modify interface network action)<br>
 * /ajax/queuedModifyInterfaceOri (queue modify interface orientation action)<br>
 * /ajax/queuedModifyComponentName (queue modify component name action)<br>
 * /ajax/createCable (create a new NetworkCable)<br>
 * /ajax/removeCable (remove a NetworkCable from the topology)<br>
 * /ajax/createVLAN (create a new VLAN)<br>
 * /ajax/modifyVLAN (modify an existing VLAN)<br>
 * /ajax/removeVLAN (remove an existing VLAN)<br>
 * /ajax/exportRDFXML (export the topology as RDF/XML file)<br>
 * /ajax/importRDFXML (import a RDF/XML file into the topology)<br>
 * /ajax/resetBackend (reset the backend a saved state)<br>
 * /ajax/shutdownBackend (shutdown the HttpServer and the backend)<br>
 * /ajax/discardModificationQueue (discard the specified modification queue)<br>
 * <br>
 * Each action will be redirected to a different request handler. The AJAXServer
 * defines several public string constants which act as error codes for the
 * frontend.<br>
 * <br>
 * This class defines the following return codes for the frontend:<br>
 * - ajaxSuccess (success)<br>
 * - ajaxShutdown (backend shutdown successful)<br>
 * - ajaxGeneral (general error, see message key for details)<br>
 * - ajaxInvalid (invalid object type)<br>
 * - ajaxException (exception caught but recovered or no recovery attempt made)<br>
 * - ajaxExceptionUnresolved (exception caught and recovery not possible)<br>
 * - ajaxMissing (missing arguments)<br>
 * - ajaxUnknown (unknown request type)<br>
 * <br>
 * The requests are always answered with a JSON object containing one of the
 * above return messages (key: status). Successful requests can contain
 * additional keys, which are defined by the handlers.<br>
 * If the status key contains the values 'ajaxGeneral' or 'ajaxException' an
 * error message is sent unter the key 'message'. The 'ajaxException' reply also
 * includes the type of exception using the key 'type'.<br>
 * Any handler that alters the topology structure returns a JSON object of the
 * current topology using the key 'topology'.
 *
 * @author Thomas Rix
 */
public class AJAXServer {
	private static Logger log = Logger.getLogger(AJAXServer.class.getName());

	/**
	 * Response message: Request successful
	 */
	public static final String AJAX_SUCCESS = "ajaxSuccess";

	/**
	 * Response message: Shutdown in progress
	 */
	public static final String AJAX_SHUTDOWN = "ajaxShutdown";

	/**
	 * Response message: IO-Tool needs time to process the command, please wait
	 */
	public static final String AJAX_IOTOOL_WAIT = "ajaxIOWait";

	/**
	 * Response message: Request failed with general error
	 */
	public static final String AJAX_ERROR_GENERAL = "ajaxGeneral";

	/**
	 * Response message: Missing arguments for the request
	 */
	public static final String AJAX_ERROR_MISSING_ARGS = "ajaxMissing";

	/**
	 * Response message: Unknown request
	 */
	public static final String AJAX_ERROR_UNKNOWN_REQ = "ajaxUnknown";

	/**
	 * Response message: Request failed with unhandled exception
	 */
	public static final String AJAX_ERROR_EXCEPTION = "ajaxException";

	/**
	 * Response message: Requesr failed with unhandled exception and rollback
	 * failed
	 */
	public static final String AJAX_ERROR_EXCEPTION_UNRESOLVED = "ajaxExceptionUnresolved";

	/**
	 * Response message: Invalid object type for the request
	 */
	public static final String AJAX_ERROR_INVALID_OBJ = "ajaxInvalid";

	/**
	 * Response message: IO-Tool is busy
	 */
	public static final String AJAX_ERROR_IOTOOL_BUSY = "ajaxIOBusy";

	private HttpServer ajaxServer;
	private int serverPort;
	private HashMap<String, ModificationQueue> modQueues;


	/**
	 * Construct a new AJAXServer object which creates a HttpServer instance
	 * that listens on port pPort.
	 *
	 * @param pPort The port on which the HttpServer will listen
	 * @throws IOException
	 */
	public AJAXServer(int pPort) throws IOException {
		serverPort = pPort;
		modQueues = new HashMap<>();

		ajaxServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
		ajaxServer.createContext("/", new DefaultHandler());
		ajaxServer.createContext("/ajax/configure", new ConfigureHandler());
		ajaxServer.createContext("/ajax/updateTopology", new UpdateTopologyHandler());
		ajaxServer.createContext("/ajax/ioConnect", new IOToolConnectHandler());
		ajaxServer.createContext("/ajax/ioDisconnect", new IOToolDisconnectHandler());
		ajaxServer.createContext("/ajax/ioTopoList", new IOToolTopoListHandler());
		ajaxServer.createContext("/ajax/ioLoadTopo", new IOToolLoadTopoHandler());
		ajaxServer.createContext("/ajax/ioWriteTopo", new IOToolWriteTopoHandler());
		ajaxServer.createContext("/ajax/ioDropTopo", new IOToolDropTopologyHandler());
		ajaxServer.createContext("/ajax/ioCollectTopo", new IOToolCollectHandler());
		ajaxServer.createContext("/ajax/ioReplicateTopo", new IOToolReplicateHandler());
		ajaxServer.createContext("/ajax/ioCleanup", new IOToolCleanupHandler());
		ajaxServer.createContext("/ajax/ioReset", new IOToolResetHandler());
		ajaxServer.createContext("/ajax/requestTopology", new RequestTopologyHandler());
		ajaxServer.createContext("/ajax/createComponent", new CreateComponentHandler());
		ajaxServer.createContext("/ajax/relocateComponent", new RelocateComponentHandler());
		ajaxServer.createContext("/ajax/addInterfaceToComp", new AddInterfaceToComponentHandler());
		ajaxServer.createContext("/ajax/modifyVLANAssignment", new ModifyVLANAssignmentHandler());
		ajaxServer.createContext("/ajax/removeComponent", new RemoveComponentHandler());
		ajaxServer.createContext("/ajax/queuedModifyInterfaceIP", new QueuedModifyInterfaceIPHandler());
		ajaxServer.createContext("/ajax/queuedModifyInterfaceOri", new QueuedModifyInterfaceOrientationHandler());
		ajaxServer.createContext("/ajax/queuedModifyComponentName", new QueuedModifyComponentNameHandler());
		ajaxServer.createContext("/ajax/createVSA", new CreateVSAHandler());
		ajaxServer.createContext("/ajax/createCable", new CreateCableHandler());
		ajaxServer.createContext("/ajax/removeCable", new RemoveCableHandler());
		ajaxServer.createContext("/ajax/createSubnet", new CreateNetworkHandler());
		ajaxServer.createContext("/ajax/createVLAN", new CreateVLANHandler());
		ajaxServer.createContext("/ajax/modifyVLAN", new ModifyVLANHandler());
		ajaxServer.createContext("/ajax/removeVLAN", new RemoveVLANHandler());
		ajaxServer.createContext("/ajax/exportRDFXML", new ExportRDFHandler());
		ajaxServer.createContext("/ajax/importRDFXML", new ImportRDFHandler());
		ajaxServer.createContext("/ajax/resetBackend", new ResetBackendHandler());
		ajaxServer.createContext("/ajax/shutdownBackend", new ShutdownHandler());
		ajaxServer.createContext("/ajax/executeModificationQueue", new ExecuteModificationsHandler());
		ajaxServer.createContext("/ajax/discardModificationQueue", new DiscardModificationsHandler());
	}


	/**
	 * Start the HttpServer instance
	 */
	public void run() {
		ajaxServer.start();

		StringBuilder sb = new StringBuilder("AJAX server started on port ");
		sb.append(serverPort);

		log.info(sb.toString());
	}


	/**
	 * Stop the HttpServer instance
	 */
	public void shutdown() {
		ajaxServer.stop(0);
		log.info("AJAX server terminated");
	}


	/**
	 * Return the modification queue stored under the provided target local
	 * name. If no such queue exists, a new one will be created and returned.
	 *
	 * @param pQueueID ID for this queue
	 * @return A modification queue using the provided values as target
	 */
	public ModificationQueue getModificationQueue(String pQueueID) {
		ModificationQueue rv = modQueues.get(pQueueID);

		// Create new queue if no queue with the specified ID was found
		if(rv == null) {
			rv = new ModificationQueue(pQueueID);
			modQueues.put(pQueueID, rv);
		}

		return rv;
	}
}
