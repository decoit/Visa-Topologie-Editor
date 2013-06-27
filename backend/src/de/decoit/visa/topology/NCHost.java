package de.decoit.visa.topology;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.Dimension2D;
import de.decoit.visa.Position2D;


/**
 * This class represents a physical host machine in the topology.
 *
 * @author Thomas Rix
 */
class NCHost extends NetworkComponent {
	/**
	 * Type string used to build the local name for new components
	 */
	static final String TYPE = "host";


	/**
	 * Construct a new NCHost object from RDF information.
	 *
	 * @param pID ID number
	 * @param pPortInfo Interface configuration
	 * @param pName Human readable name
	 * @param pCompDimensions Component dimensions
	 * @param pGridLoc Position of the component on the editor grid
	 * @param pLocName RDF local name
	 */
	NCHost(int pID, ArrayList<HashMap<String, String>> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc, String pLocName) {
		super(pID, pName, pPortInfo, pCompDimensions, pGridLoc, pLocName);

		if(log.isTraceEnabled()) {
			log.trace("NCHost created from RDF information");
		}
	}


	// Inherited from IRemovableComponent
	@Override
	public void removeFromTopology() {
		super.removeFromTopology();
	}


	@Override
	public JSONObject toJSON() throws JSONException {
		// Get the JSONObject of the NetworkComponent
		JSONObject rv = super.toJSON();

		rv.put("isVirtual", false);

		return rv;
	}
}
