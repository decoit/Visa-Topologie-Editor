package de.decoit.visa.protocol;

import java.util.HashMap;


/**
 * Allowed commands for the {@link Request} class of the VISA protocol. All
 * available commands are listed and described below. Response descriptions use
 * the following notation:<br>
 * ReturnCode:Data:Message<br>
 * Data is given as a map: {:key1 =&gt; value1, :key2 =&gt; value2}<br>
 * Text in &lt;&gt; is a placeholder describing the content<br>
 * <br>
 * <strong>GET_TOPOLOGY_LIST:</strong><br>
 * This command returns a list of all topology IDs and the corresoponding
 * descriptions known to the server.<br>
 * <i>Command string:</i> get_topo_list<br>
 * <i>Arguments:</i> none<br>
 * <i>Response:</i><br>
 * - on success: 0:{:&lt;topology&gt; =&gt; &lt;description&gt;}:<br>
 * <br>
 * <strong>GET_TOPOLOGY:</strong><br>
 * Returns the RDF/XML data of the topology &lt;topology&gt;<br>
 * <i>Command string:</i> get_topo<br>
 * <i>Arguments:</i><br>
 * - topology (ID of the topology)<br>
 * <i>Response:</i><br>
 * - on success: 0:{:&lt;topology&gt; =&gt; &lt;RDF-Data&gt;}:<br>
 * - on failure: 100::"Unknown topology '&lt;topology&gt;'"<br>
 * <br>
 * <strong>WRITE_TOPOLOGY:</strong><br>
 * Stores the given RDF data in a topology with ID &lt;topology&gt;. Stores the
 * description for this topology if given. Updates the description for existing
 * topologies if given. Updates the RDF data for existing topologies. An update
 * may be realized as delete and store, so URI MAY change.<br>
 * <i>Command string:</i> write_topo<br>
 * <i>Arguments:</i><br>
 * - topology (ID of the topology)<br>
 * - data (RDF/XML data)<br>
 * - description (human readable name of the topology)<br>
 * <i>Response:</i><br>
 * - on success: 0::<br>
 * - on failure: 102::<br>
 * "Topology &lt;topology&gt; could not be parsed properly; please check your RDF syntax"<br>
 * - on failure: 103:{:report =&gt; "&lt;first invalid entry&gt;(\n&lt;another invalid entry&gt;)*"}:"Topology &lt;topology&gt; contains invalid data; see report for details"<br>
 * - on failure: 106::"Topology &lt;topology&gt; is write protected"<br>
 * <br>
 * <strong>DROP_TOPOLOGY:</strong><br>
 * Drop the given topology. Drop is even executed successfully, if the data
 * structure of the topology &lt;topology&gt; is invalid and therefore removed
 * from known topology list, accessible with 'GET_TOPOLOGY_LIST'. Trying to
 * delete a delete protected topology will fail.<br>
 * <i>Command string:</i> drop<br>
 * <i>Arguments:</i><br>
 * - topology (ID of the topology)<br>
 * <i>Response:</i><br>
 * - on success: 0::<br>
 * - on failure: 100::"Unknown topology '&lt;topology&gt;'"<br>
 * - on failure: 107::"Could not delete protected topology &lt;topology&gt;"<br>
 * <br>
 * <strong>REPLICATE:</strong><br>
 * Starts the replication for the environment configured in the server. Uses
 * given topology &lt;topology&gt;. Running CLEANUP is recommended. Replication
 * to a non empty environment is not supported; the user has to take care for
 * collision free topologies.<br>
 * <i>Command string:</i> replicate<br>
 * <i>Arguments:</i><br>
 * - topology (ID of the topology)<br>
 * <i>Response:</i><br>
 * - on wait: 254::<br>
 * - on success: 0::<br>
 * - on failure: 100::"Unknown topology '&lt;topology&gt;'<br>
 * - on failure: 104::"Environment connection error"<br>
 * - on failure: 105::"Command execution error"<br>
 * <br>
 * <strong>COLLECT:</strong><br>
 * Starts the collector for the environment configured in the server. Stores
 * collected data in the topology &lt;topology&gt;.<br>
 * <i>Command string:</i> collect<br>
 * <i>Arguments:</i><br>
 * - topology (ID of the topology)<br>
 * <i>Response:</i><br>
 * - on wait: 254::<br>
 * - on success: 0::<br>
 * - on failure: 104::"Environment connection error"<br>
 * - on failure: 105::"Command execution error"<br>
 * - on failure: 106::"Topology &lt;topology&gt; is write protected"<br>
 * <br>
 * <strong>CLEANUP:</strong><br>
 * Cleans up replication environment. Deletes VM images and networks.<br>
 * <i>Command string:</i> cleanup<br>
 * <i>Arguments:</i> none<br>
 * <i>Response:</i><br>
 * - on wait: 254::<br>
 * - on success: 0::<br>
 * - on failure: 104::"Environment connection error"<br>
 * - on failure: 105::"Command execution error"<br>
 * <br>
 * <strong>RESET:</strong><br>
 * Will not delete write or delete protected topologies (returnCode 1). Calls
 * cleanup and runs drop for all known topologies and will manually clean up the
 * topology storage directory. In case of an error reset continues with the next
 * step of the reset process.<br>
 * <i>Command string:</i> reset<br>
 * <i>Arguments:</i> none<br>
 * <i>Response:</i><br>
 * - on wait: 254::<br>
 * - on success: 0:{:report =&gt; &lt;text report of executed steps&gt;}:<br>
 * - on success: 1:{:report =&gt; &lt;text report of executed steps&gt;, :errors =&gt; &lt;text report of intended errors&gt;}:<br>
 * - on failure: 104::"Environment connection error"<br>
 * - on failure: 105::"Command execution error"
 *
 * @author Thomas Rix
 */
public enum Commands {
	/**
	 * Request the list of topogies from the IO-Tool
	 */
	GET_TOPOLOGY_LIST("get_topo_list"),

	/**
	 * Request a specific topology from the IO-Tool
	 */
	GET_TOPOLOGY("get_topo"),

	/**
	 * Write a specific topology into the IO-Tool
	 */
	WRITE_TOPOLOGY("write_topo"),

	/**
	 * Start replication of a specific topology
	 */
	REPLICATE("replicate"),

	/**
	 * Start collection of a specific topology
	 */
	COLLECT("collect"),

	/**
	 * Clean up the replication environment
	 */
	CLEANUP("cleanup"),

	/**
	 * Call CLEANUP and remove all unprotected topologies afterwards
	 */
	RESET("reset"),

	/**
	 * Delete a specific topology
	 */
	DROP_TOPOLOGY("drop");

	/** Command string which can be interpreted by the IO-Tool */
	private String str;


	private Commands(String s) {
		str = s;
	}


	@Override
	public String toString() {
		return str;
	}


	/**
	 * Get the command represented by a specific string.
	 *
	 * @param s String representation of the command
	 * @return The command object, null if an invalid string was provided
	 */
	public static Commands getCommand(String s) {
		switch(s) {
			case "get_topo_list":
				return GET_TOPOLOGY_LIST;
			case "get_topo":
				return GET_TOPOLOGY;
			case "write_topo":
				return WRITE_TOPOLOGY;
			case "replicate":
				return REPLICATE;
			case "collect":
				return COLLECT;
			case "cleanup":
				return CLEANUP;
			case "reset":
				return RESET;
			case "drop":
				return DROP_TOPOLOGY;
			default:
				return null;
		}
	}


	/**
	 * Get an empty argument map for the provided command. The map will have all
	 * required keys, the values are set to empty strings. It is recommended to
	 * request this map for a specific command, fill in the values and use it
	 * for the creation of a Request object.
	 *
	 * @param c The command the map is requested for
	 * @return The empty argument map
	 */
	public static HashMap<String, String> getEmptyArgumentMap(Commands c) {
		HashMap<String, String> rv = new HashMap<>();

		switch(c) {
			case GET_TOPOLOGY:
			case REPLICATE:
			case COLLECT:
			case DROP_TOPOLOGY:
				rv.put("topology", "");
				break;
			case WRITE_TOPOLOGY:
				rv.put("topology", "");
				rv.put("data", "");
				rv.put("description", "");
				break;
			default:
				break;
		}

		return rv;
	}
}
