/*
 *  Copyright (C) 2013, DECOIT GmbH
 *
 *	This file is part of VISA Topology-Editor.
 *
 *	VISA Topology-Editor is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by the
 *	Free Software Foundation, either version 3 of the License, or (at your option)
 *	any later version.
 *
 *	VISA Topology-Editor is distributed in the hope that it will be useful, but
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *	more details.
 *
 *	You should have received a copy of the GNU General Public License along with
 *	VISA Topology-Editor. If not, see <http://www.gnu.org/licenses/>.
 */

package de.decoit.visa.topology;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.Dimension2D;
import de.decoit.visa.Position2D;


/**
 * This class represents a virtual machine in the topology.
 *
 * @author Thomas Rix
 */
public class NCVM extends NetworkComponent {
	/**
	 * Type string used to build the local name for new components
	 */
	public static final String TYPE = "vm";


	/**
	 * Construct a new NCVM object from information received from the
	 * frontend.
	 *
	 * @param pID ID number
	 * @param pPortInfo Interface configuration
	 * @param pName Human readable name
	 * @param pCompDimensions Component dimensions
	 * @param pGridLoc Position of the component on the editor grid
	 */
	NCVM(int pID, ArrayList<String> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc) {
		super(pID, NCVM.TYPE, pName, pPortInfo, pCompDimensions, pGridLoc);

		if(log.isTraceEnabled()) {
			log.trace("NCVM created");
		}
	}


	/**
	 * Construct a new NCVM object from RDF information.
	 *
	 * @param pID ID number
	 * @param pPortInfo Interface configuration
	 * @param pName Human readable name
	 * @param pCompDimensions Component dimensions
	 * @param pGridLoc Position of the component on the editor grid
	 * @param pLocName RDF local name
	 */
	NCVM(int pID, ArrayList<HashMap<String, String>> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc, String pLocName) {
		super(pID, pName, pPortInfo, pCompDimensions, pGridLoc, pLocName);

		if(log.isTraceEnabled()) {
			log.trace("NCVM created from RDF information");
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

		rv.put("isVirtual", true);

		return rv;
	}
}
