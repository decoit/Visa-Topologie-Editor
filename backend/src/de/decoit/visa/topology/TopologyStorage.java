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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.Dimension2D;
import de.decoit.visa.Position2D;
import de.decoit.visa.TEBackend;
import de.decoit.visa.enums.IPVersion;
import de.decoit.visa.enums.PortOrientation;
import de.decoit.visa.gridlayout.GridLayout;
import de.decoit.visa.interfaces.IJSON;
import de.decoit.visa.interfaces.INetworkInterface;
import de.decoit.visa.interfaces.IRDFObject;
import de.decoit.visa.net.IPNetwork;
import de.decoit.visa.rdf.VISA;
import de.decoit.visa.topology.NCSwitch.GroupSwitch;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup.GroupInterface;


/**
 * This class stores an object structure of the current topology. It uses the
 * classes defined in the de.decoit.visa.topology package. Objects are stored
 * using ComponentGroups which can be accessed by their group name. Components
 * that are not assigned to a specific group are located in the global group
 * "0.0.0.0". Besides the ComponentGroups this class contains separate lists of
 * VLANs, cables and used local names for RDF objects.<br>
 * <br>
 * The class is designed as a singleton to make sure only one topology storage
 * is available in the backend. This is necessary to prevent multiple identical
 * ID numbers or local names.
 *
 * @author Thomas Rix
 */
public class TopologyStorage {
	private static TopologyStorage instance = null;
	private static Logger log = Logger.getLogger(TopologyStorage.class.getName());
	private static int nextGroupID = 0;

	private HashMap<String, String> groupNameIDMap;
	private HashMap<String, ComponentGroup> storage;
	private HashMap<String, IRDFObject> localNames;
	private HashMap<String, VLAN> vlans;
	private HashMap<String, NetworkCable> cables;
	private HashMap<String, IPNetwork> networks;
	private TreeSet<Integer> usedIDs;
	private String topologyID;
	private int lastVLANID;


	/**
	 * Return the singleton instance of the topology storage. If no instance
	 * exists a new will be created.
	 *
	 * @return The singleton instance of the topology storage
	 */
	public static TopologyStorage getInstance() {
		if(instance == null) {
			instance = new TopologyStorage();
		}

		return instance;
	}


	/**
	 * Generate a unique ID for a new topology. It is built by appending the
	 * current UNIX timestamp to the string "custom_topology_".
	 *
	 * @return A new unique topology ID
	 */
	private static String genNewTopologyID() {
		Date currDate = new Date();

		StringBuilder sb = new StringBuilder("custom_topology_");
		sb.append(currDate.getTime());

		return sb.toString();
	}


	/**
	 * Construct a new TopologyStorage
	 */
	private TopologyStorage() {
		groupNameIDMap = new HashMap<>();
		storage = new HashMap<>();

		ComponentGroup rootcg = new ComponentGroup("0.0.0.0");
		storage.put(rootcg.getIdentifier(), rootcg);
		groupNameIDMap.put(rootcg.getName(), rootcg.getIdentifier());

		localNames = new HashMap<>();
		vlans = new HashMap<>();
		cables = new HashMap<>();
		networks = new HashMap<>();

		usedIDs = new TreeSet<>();

		topologyID = genNewTopologyID();
		lastVLANID = 0;

		if(log.isTraceEnabled()) {
			log.trace("TopologyStorage created");
		}
	}


	/**
	 * Set a new topology ID for the current topology
	 *
	 * @param pID The new ID
	 */
	public void setTopologyID(String pID) {
		if(!pID.isEmpty()) {
			topologyID = pID;
		}
		else {
			throw new IllegalArgumentException("Empty string for topology ID provided");
		}
	}


	/**
	 * Set a new human readable name for the current topology
	 *
	 * @param pName The new topology name
	 */
	public void setTopologyName(String pName) {
		TEBackend.RDF_MANAGER.setRootNodeName(pName);
	}


	/**
	 * Create a new host from data extracted from a RDF model. Parameters for
	 * component dimensions and grid location are optional. If the dimensions
	 * parameter is set to null, a default size of 5x5 cells will be used.
	 * Setting the grid location parameter to null will force automatic
	 * placement of the component using the graphviz software collection.
	 *
	 * The new component will automatically be added to the topology storage.
	 * The local name of the RDF node will be used as identifier.
	 *
	 * @param pPortInfo A list containing the interface information extracted
	 *            from the RDF data stored in a <String, String> map.
	 * @param pName Name of the new component, cannot be null or empty
	 * @param pCompDimensions Size of the new component, can be set to null for
	 *            default size (5x5 cells)
	 * @param pGridLoc Grid location of the component's dragbox
	 * @param pLocName The local name of the RDF node representing this
	 *            component
	 * @return The new host object
	 */
	public NCHost createHost(ArrayList<HashMap<String, String>> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc, String pLocName) {
		if(!localNames.containsKey(pLocName)) {
			int newID = getNextComponentID(NCHost.TYPE);

			if(pCompDimensions == null) {
				pCompDimensions = new Dimension2D(5, 5);
			}

			NCHost rv = new NCHost(newID, pPortInfo, pName, pCompDimensions, pGridLoc, pLocName);
			addComponent(rv, false);

			return rv;
		}
		else {
			throw new IllegalArgumentException("Local name is already in use");
		}
	}


	/**
	 * Create a new switch from data received from the frontend. Parameters for
	 * component dimensions and grid location are optional. If the dimensions
	 * parameter is set to null, a default size of 5x5 cells will be used.
	 * Setting the grid location parameter to null will force automatic
	 * placement of the component using the graphviz software collection.
	 *
	 * The new component will automatically be added to the topology storage and
	 * RDF model. A unique local name for the RDF resource is generated during
	 * creation.
	 *
	 * @param pPortInfo A list containing the interface orientations for the new
	 *            interfaces, cannot be null or empty
	 * @param pName Name of the new component, cannot be null or empty
	 * @param pCompDimensions Size of the new component, can be set to null for
	 *            default size (5x5 cells)
	 * @param pGridLoc Grid location of the component's dragbox
	 * @return The new switch object
	 */
	public NCSwitch createSwitch(ArrayList<String> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc) {
		int newID = getNextComponentID(NCSwitch.TYPE);

		if(pCompDimensions == null) {
			pCompDimensions = new Dimension2D(5, 5);
		}

		NCSwitch rv = new NCSwitch(newID, pPortInfo, pName, pCompDimensions, pGridLoc);

		addComponent(rv, true);

		return rv;
	}


	/**
	 * Create a new switch from data extracted from a RDF model. Parameters for
	 * component dimensions and grid location are optional. If the dimensions
	 * parameter is set to null, a default size of 5x5 cells will be used.
	 * Setting the grid location parameter to null will force automatic
	 * placement of the component using the graphviz software collection.
	 *
	 * The new component will automatically be added to the topology storage.
	 * The local name of the RDF node will be used as identifier.
	 *
	 * @param pPortInfo A list containing the interface information extracted
	 *            from the RDF data stored in a <String, String> map.
	 * @param pName Name of the new component, cannot be null or empty
	 * @param pCompDimensions Size of the new component, can be set to null for
	 *            default size (5x5 cells)
	 * @param pGridLoc Grid location of the component's dragbox
	 * @param pLocName The local name of the RDF node representing this
	 *            component
	 * @return The new switch object
	 */
	public NCSwitch createSwitch(ArrayList<HashMap<String, String>> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc, String pLocName) {
		if(!localNames.containsKey(pLocName)) {
			int newID = getNextComponentID(NCSwitch.TYPE);

			if(pCompDimensions == null) {
				pCompDimensions = new Dimension2D(5, 5);
			}

			NCSwitch rv = new NCSwitch(newID, pPortInfo, pName, pCompDimensions, pGridLoc, pLocName);

			addComponent(rv, false);

			return rv;
		}
		else {
			throw new IllegalArgumentException("Local name is already in use");
		}
	}


	/**
	 * Create a new VM from data received from the frontend. Parameters for
	 * component dimensions and grid location are optional. If the dimensions
	 * parameter is set to null, a default size of 5x5 cells will be used.
	 * Setting the grid location parameter to null will force automatic
	 * placement of the component using the graphviz software collection.
	 *
	 * The new component will automatically be added to the topology storage and
	 * RDF model. A unique local name for the RDF resource is generated during
	 * creation.
	 *
	 * @param pPortInfo A list containing the interface orientations for the new
	 *            interfaces, cannot be null or empty
	 * @param pName Name of the new component, cannot be null or empty
	 * @param pCompDimensions Size of the new component, can be set to null for
	 *            default size (5x5 cells)
	 * @param pGridLoc Grid location of the component's dragbox
	 * @return The new VM object
	 */
	public NCVM createVM(ArrayList<String> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc) {
		int newID = getNextComponentID(NCVM.TYPE);

		if(pCompDimensions == null) {
			pCompDimensions = new Dimension2D(5, 5);
		}

		NCVM rv = new NCVM(newID, pPortInfo, pName, pCompDimensions, pGridLoc);

		addComponent(rv, true);

		return rv;
	}


	/**
	 * Create a new VM from data extracted from a RDF model. Parameters for
	 * component dimensions and grid location are optional. If the dimensions
	 * parameter is set to null, a default size of 5x5 cells will be used.
	 * Setting the grid location parameter to null will force automatic
	 * placement of the component using the graphviz software collection.
	 *
	 * The new component will automatically be added to the topology storage.
	 * The local name of the RDF node will be used as identifier.
	 *
	 * @param pPortInfo A list containing the interface information extracted
	 *            from the RDF data stored in a <String, String> map.
	 * @param pName Name of the new component, cannot be null or empty
	 * @param pCompDimensions Size of the new component, can be set to null for
	 *            default size (5x5 cells)
	 * @param pGridLoc Grid location of the component's dragbox
	 * @param pLocName The local name of the RDF node representing this
	 *            component
	 * @return The new VM object
	 */
	public NCVM createVM(ArrayList<HashMap<String, String>> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc, String pLocName) {
		if(!localNames.containsKey(pLocName)) {
			int newID = getNextComponentID(NCVM.TYPE);

			if(pCompDimensions == null) {
				pCompDimensions = new Dimension2D(5, 5);
			}

			NCVM rv = new NCVM(newID, pPortInfo, pName, pCompDimensions, pGridLoc, pLocName);

			addComponent(rv, false);

			return rv;
		}
		else {
			throw new IllegalArgumentException("Local name is already in use");
		}
	}


	/**
	 * Get an existing component object from the topology storage identified by
	 * the provided local name. If no object with that local name was found the
	 * method will return null. If the identified object is no instance of
	 * NetworkComponent an IllegalArgumentException will be thrown.
	 *
	 * @param pLocName Local name identifying the component object, must
	 *            identify a NetworkComponent object
	 * @return The component object identified by the local name, null if no
	 *         object was found
	 */
	public NetworkComponent getComponent(String pLocName) {
		IRDFObject rdfo = localNames.get(pLocName);

		if(rdfo instanceof NetworkComponent) {
			return (NetworkComponent) rdfo;
		}
		else if(rdfo == null) {
			return null;
		}
		else {
			throw new IllegalArgumentException("Object identified by the provided local name was no NetworkComponent instance");
		}
	}


	/**
	 * Remove an existing component, identified by the provided local name, from
	 * the topology storage and RDF model.
	 *
	 * @param pLocName Local name to identify the component, must identify an
	 *            existing NetworkComponent instance
	 */
	public void removeComponent(String pLocName) {
		IRDFObject rdfo = localNames.get(pLocName);

		if(rdfo instanceof NetworkComponent) {
			NetworkComponent nc = (NetworkComponent) rdfo;

			if(nc instanceof NCSwitch) {
				((NCSwitch) nc).removeFromTopology();
			}
			else if(nc instanceof NCHost) {
				((NCHost) nc).removeFromTopology();
			}
			else if(nc instanceof NCVM) {
				((NCVM) nc).removeFromTopology();
			}

			String group = nc.getConfig().getComponentGroup();
			getComponentGroupByName(group).removeComponent(nc);
			localNames.remove(pLocName);
		}
		else if(rdfo == null) {
			throw new IllegalArgumentException("No object with this local name found");
		}
		else {
			throw new IllegalArgumentException("Object identified by the provided local name was no NetworkComponent instance");
		}
	}


	/**
	 * Create a new interface and assign it to the specified component. This
	 * method is called for interfaces created when new components are added by
	 * the frontend or new interfaces are added to existing components.
	 *
	 * @param pOrientation Orientation of the new interface
	 * @param pComp Component which the interface will be assigned to
	 * @return The new created interface
	 */
	NetworkComponent.Interface createInterface(PortOrientation pOrientation, NetworkComponent pComp) {
		int newID = getNextComponentID(NetworkComponent.Interface.TYPE);

		NetworkComponent.Interface rv = new NetworkComponent.Interface(newID, pOrientation, pComp);

		TEBackend.RDF_MANAGER.addObject(rv);
		localNames.put(rv.getIdentifier(), rv);

		return rv;
	}


	/**
	 * Create a new interface and assign it to the specified component. This
	 * method is called for interfaces created from RDF information.
	 *
	 * @param pOrientation Orientation of the new interface
	 * @param pComp Component which the interface will be assigned to
	 * @param pLocName RDF local name of the interface
	 * @return The new created interface
	 */
	NetworkComponent.Interface createInterface(PortOrientation pOrientation, NetworkComponent pComp, String pLocName) {
		if(!localNames.containsKey(pLocName)) {
			NetworkComponent.Interface rv = new NetworkComponent.Interface(pOrientation, pComp, pLocName);

			localNames.put(rv.getIdentifier(), rv);

			return rv;
		}
		else {
			throw new IllegalArgumentException("Local name is already in use");
		}
	}


	/**
	 * Get the interface with the specified RDF local name from the storage.
	 *
	 * @param pLocName RDF local name of the interface
	 * @return The requested interface, null if no interface with that local
	 *         name was found
	 */
	public NetworkComponent.Interface getInterface(String pLocName) {
		IRDFObject rdfo = localNames.get(pLocName);

		if(rdfo instanceof NetworkComponent.Interface) {
			return (NetworkComponent.Interface) rdfo;
		}
		else if(rdfo == null) {
			return null;
		}
		else {
			throw new IllegalArgumentException("Object identified by the provided local name was no NetworkComponent.Interface instance");
		}
	}


	/**
	 * Remove the interface with the specified RDF local name from the topology.
	 * It will be removed from the component it is assigned to, the storage
	 * structures and the RDF model.
	 *
	 * @param pLocName RDF local name of the interface
	 */
	void removeInterface(String pLocName) {
		IRDFObject rdfo = localNames.get(pLocName);

		if(rdfo instanceof NetworkComponent.Interface) {
			NetworkComponent.Interface i = (NetworkComponent.Interface) rdfo;
			localNames.remove(pLocName);
			i.removeFromTopology();
		}
		else if(rdfo == null) {
			throw new IllegalArgumentException("No object with this local name found");
		}
		else {
			throw new IllegalArgumentException("Object identified by the provided local name was no NetworkComponent.Interface instance");
		}
	}


	/**
	 * Create a new connection between two interfaces using a
	 * {@link NetworkCable} object. An optional {@link GroupInterface
	 * GroupInterface}, which will be used as a gateway into a
	 * {@link ComponentGroup ComponentGroup}, can be specified.
	 *
	 * @param pLeft Interface at the left end of the cable
	 * @param pRight Interface at the right end of the cable
	 * @param pGroupGW Optional {@link GroupInterface GroupInterface} used to
	 *            connect interfaces through the border of a group. Can be set
	 *            to null if not required
	 * @return The new created cable object
	 */
	public NetworkCable createCable(NetworkComponent.Interface pLeft, NetworkComponent.Interface pRight, GroupInterface pGroupGW) {
		int newID = getNextCableID();

		NetworkCable rv = new NetworkCable(newID, pLeft, pRight, pGroupGW);

		cables.put(rv.getIdentifier(), rv);

		// Add the cable to its component group
		getComponentGroupByName(rv.getGroupName()).addCable(rv);

		return rv;
	}


	/**
	 * Remove the cable with the specified identifier string from the topology.
	 * It will be disconnected from the interfaces and removed from the storage.
	 * Also the 'connected' properties connecting the two interfaces in the RDF
	 * model are removed.
	 *
	 * @param pIdent Identifier string of the cable
	 */
	public void removeCable(String pIdent) {
		NetworkCable nc = cables.get(pIdent);

		if(nc != null) {
			nc.removeFromTopology();

			cables.remove(pIdent);
		}
		else {
			throw new IllegalArgumentException("No object with this local name found");
		}
	}


	/**
	 * Create a new VLAN object using the name and color received from the
	 * frontend.
	 *
	 * @param pName Human readable name of the VLAN
	 * @param pColor Color string of the VLAN, must be specified as a sharp
	 *            followed by a 6-digit hex code (#RRGGBB)
	 * @return The new created VLAN
	 */
	public VLAN createVLAN(String pName, String pColor) {
		int newID = getNextVLANID();

		VLAN rv = new VLAN(newID, null);
		rv.setName(pName);

		rv.setColor(pColor);

		vlans.put(rv.getRDFLocalName(), rv);

		return rv;
	}


	/**
	 * Create a new VLAN object using information extracted from the RDF model.
	 * A color will be selected using the
	 * {@link de.decoit.visa.topology.VLAN.ColorChooser ColorChooser} class. The
	 * ID number may be overridden by an automatically generated ID
	 * to prevent collisions with existing VLANs. Such changes will update the
	 * RDF model to represent the new situation.
	 *
	 * @param pLocName RDF local name of the VLAN
	 * @param pID ID number of the VLAN
	 * @return The new created VLAN
	 */
	public VLAN createVLAN(String pLocName, int pID) {
		int id;
		boolean updateRDF;

		if(pID <= lastVLANID) {
			id = getNextVLANID();
			updateRDF = true;
		}
		else {
			lastVLANID = pID;
			id = pID;
			updateRDF = false;
		}

		VLAN rv = new VLAN(id, pLocName);

		vlans.put(pLocName, rv);

		if(updateRDF) {
			TEBackend.RDF_MANAGER.updateProperty(rv, VISA.ID);
		}

		return rv;
	}


	/**
	 * Get a VLAN specified by the given RDF local name. If no object with that
	 * local name was found, a new VLAN object will be created using the local
	 * name and an automatically generated ID number. A color will be selected
	 * using the {@link de.decoit.visa.topology.VLAN.ColorChooser ColorChooser}
	 * class.
	 *
	 * @param pLocName RDF local name of the VLAN
	 * @return The requested VLAN
	 */
	public VLAN getVLAN(String pLocName) {
		if(vlans.containsKey(pLocName)) {
			return vlans.get(pLocName);
		}
		else {
			int newID = getNextVLANID();

			VLAN rv = new VLAN(newID, pLocName);

			vlans.put(pLocName, rv);

			return rv;
		}
	}


	/**
	 * Remove the VLAN with the specified RDF local name from the topology. It
	 * will be removed from the storage and the RDF model. This action can only
	 * be executed if the VLAN is <strong>not</strong> assigned to any
	 * interfaces.
	 *
	 * @param pLocName RDF local name of the VLAN
	 */
	public void removeVLAN(String pLocName) {
		VLAN vlan = vlans.get(pLocName);

		if(vlan != null) {
			vlan.removeFromTopology();

			vlans.remove(pLocName);
		}
		else {
			throw new IllegalArgumentException("No object with this local name found");
		}
	}


	/**
	 * Create a new network object. If a network with this address and subnet
	 * mask length already exists it will be returned instead. If an existing
	 * network with the provided address but different subent mask length is
	 * found, an exception will be thrown-
	 *
	 * @param pNetworkAddress String notation of the network IP address, must be
	 *            valid for the provided IP version
	 * @param pSubnetMaskLength Bit length of the subnet mask used for this
	 *            network
	 * @param pVersion Version of the Internet Protocol which will be used for
	 *            this network
	 * @return The newly created or existing network
	 */
	public IPNetwork createNetwork(String pNetworkAddress, int pSubnetMaskLength, IPVersion pVersion) {
		IPNetwork newNet = networks.get(pNetworkAddress);

		if(newNet == null) {
			newNet = new IPNetwork(pNetworkAddress, pSubnetMaskLength, pVersion);
			networks.put(newNet.getNetworkAddress().getAddressString(), newNet);
		}
		else if(newNet.getSubnetMaskLength() != pSubnetMaskLength) {
			throw new IllegalStateException("A network with this address but different subnet mask length already exists");
		}

		return newNet;
	}


	/**
	 * Return the network with the provided network address. If no such network
	 * exists, the method will return null.
	 *
	 * @param pNetworkAddress String notation of the network IP address
	 * @return The stored network, null if no network was found
	 */
	public IPNetwork getNetwork(String pNetworkAddress) {
		return networks.get(pNetworkAddress);
	}


	/**
	 * Get the {@link ComponentGroup ComponentGroup} with the specified
	 * identifier. If no such group exists the method will return null.
	 *
	 * @param pID Identifier of the group
	 * @return The requested group object, null if no matching group was found
	 */
	public ComponentGroup getComponentGroupByID(String pID) {
		return storage.get(pID);
	}


	/**
	 * Get the {@link ComponentGroup ComponentGroup} with the specified group
	 * name. If no group with that name exists, a new one will be created and
	 * added to the storage.
	 *
	 * @param pName Name of the group
	 * @return The requested group object
	 */
	public ComponentGroup getComponentGroupByName(String pName) {
		if(!pName.isEmpty()) {
			String cgID = groupNameIDMap.get(pName);
			ComponentGroup rv = storage.get(cgID);

			if(rv == null) {
				rv = new ComponentGroup(pName);

				storage.put(rv.getIdentifier(), rv);
				groupNameIDMap.put(rv.getName(), rv.getIdentifier());
			}

			return rv;
		}
		else {
			throw new IllegalArgumentException("Group name cannot be empty");
		}
	}


	/**
	 * Query if the storage contains a {@link ComponentGroup ComponentGroup}
	 * with the specified group ID. This method wraps around
	 * HashMap.containsKey().
	 *
	 * @param pID Identifier of the group
	 * @return true if a group with that name was found, false otherwise
	 */
	public boolean hasComponentGroup(String pID) {
		return storage.containsKey(pID);
	}


	/**
	 * Get the next usable group ID number
	 *
	 * @return ID number for the next group
	 */
	public int getNextComponentGroupID() {
		int rv = nextGroupID;
		nextGroupID++;

		return rv;
	}


	/**
	 * Get the object with the specified RDF local name. The returned object can
	 * be of any type that is stored in the RDF model. This includes, but is not
	 * limited to, {@link NetworkComponent},
	 * {@link de.decoit.visa.topology.NetworkComponent.Interface Interface},
	 * {@link NetworkCable} and {@link VLAN}.
	 *
	 * @param pLocName RDF local name of the requested object
	 * @return The requested object, null if no object was found
	 */
	public IRDFObject getRDFObject(String pLocName) {
		return localNames.get(pLocName);
	}


	/**
	 * Remove all objects stored in the topology storage. The storage, vlans,
	 * cables local names maps and set of used IDs will be cleared.
	 */
	public void clear() {
		groupNameIDMap.clear();
		storage.clear();
		vlans.clear();
		cables.clear();
		usedIDs.clear();
		localNames.clear();
		networks.clear();

		nextGroupID = 0;
		lastVLANID = 0;
		topologyID = genNewTopologyID();

		ComponentGroup rootcg = new ComponentGroup("0.0.0.0");
		storage.put(rootcg.getIdentifier(), rootcg);
		groupNameIDMap.put(rootcg.getName(), rootcg.getIdentifier());
	}


	/**
	 * Generate a JSON encoded JavaScript object with information about all
	 * objects that are currently present in the topology.
	 *
	 * @return JSONObject containing a structure with information about all
	 *         stored objects
	 * @throws JSONException if adding values to the JSONObject fails
	 */
	public JSONObject genTopologyJSON() throws JSONException {
		JSONObject json = new JSONObject();

		json.put("identifier", topologyID);
		json.put("name", StringEscapeUtils.escapeHtml4(TEBackend.RDF_MANAGER.getRootNodeName()));

		JSONObject groupJSON = new JSONObject();
		JSONObject ifJSON = new JSONObject();
		// Iterate through all stored types
		for(Map.Entry<String, ComponentGroup> groupEntry : storage.entrySet()) {
			groupJSON.put(groupEntry.getValue().identifier, groupEntry.getValue().toJSON());

			// Iterate through all stored objects and add their interfaces to
			// the interface list
			for(NetworkComponent nc : groupEntry.getValue().componentList) {
				for(Map.Entry<String, NetworkComponent.Interface> ifEntry : nc.getConfig().getPorts().entrySet()) {
					ifJSON.put(ifEntry.getKey(), ifEntry.getValue().toJSON());
				}
			}
		}
		json.put("groups", groupJSON);
		json.put("interfaces", ifJSON);

		JSONObject cableJSON = new JSONObject();
		for(Map.Entry<String, NetworkCable> cableEntry : cables.entrySet()) {
			cableJSON.put(cableEntry.getKey(), cableEntry.getValue().toJSON());
		}
		json.put("cables", cableJSON);

		JSONObject vlanJSON = new JSONObject();
		for(Map.Entry<String, VLAN> vlanEntry : vlans.entrySet()) {
			vlanJSON.put(vlanEntry.getKey(), vlanEntry.getValue().toJSON());
		}
		json.put("vlans", vlanJSON);

		JSONObject networkJSON = new JSONObject();
		for(Map.Entry<String, IPNetwork> networkEntry : networks.entrySet()) {
			networkJSON.put(networkEntry.getKey(), networkEntry.getValue().toJSON());
		}
		json.put("networks", networkJSON);

		json.put("importHistory", TEBackend.RDF_MANAGER.historyToJSON());

		return json;
	}


	/**
	 * Layout the topology by using the de.decoit.visa.gridlayout.GridLayout
	 * class. By default, the 'neato' executable will be used for layouting. If
	 * any nodes with fixed positions are detected, the 'fdp' executable will be
	 * used to get better results. All existing switches and VMs will be used as
	 * nodes, all cables as edges. Already positioned nodes will not be moved.
	 */
	public void layoutTopology() {
		try {
			// Layout the subgrids of all component groups

			HashSet<ComponentGroup> processedGroups = new HashSet<>();

			for(Map.Entry<String, ComponentGroup> groupEntry : storage.entrySet()) {
				// Process all groups except the global group 0.0.0.0
				if(!groupEntry.getValue().isGlobalGroup()) {
					// neato is the default layouter
					String command = "neato";

					// Create a new layouter
					GridLayout layout = new GridLayout(groupEntry.getValue().subGridDimensions);

					// Add all components of this group to the layouter
					for(NetworkComponent nc : groupEntry.getValue().componentList) {
						// Use fdp layouter if there are nodes with fixed
						// positions
						if(!layout.addComponent(nc)) {
							command = "fdp";
						}
					}

					for(NetworkCable nc : groupEntry.getValue().cables) {
						layout.addCable(nc);
					}

					for(Map.Entry<String, GroupSwitch> gsEntry : groupEntry.getValue().groupSwitches.entrySet()) {
						// Use fdp layouter if there are nodes with fixed
						// positions
						if(!layout.addGroupSwitch(gsEntry.getValue())) {
							command = "fdp";
						}
					}

					// Run the layouter
					layout.run(command);

					// Add the current group to the processed groups set
					processedGroups.add(groupEntry.getValue());
				}
			}

			// Layout the base layer group 0.0.0.0

			// neato is the default layouter
			String command = "neato";

			// Create a new layouter
			GridLayout layout = new GridLayout(TEBackend.getGridDimensions());

			// Add all components to the layouter
			for(NetworkComponent nc : getComponentGroupByName("0.0.0.0").componentList) {
				// Use fdp layouter if there are nodes with fixed positions
				if(!layout.addComponent(nc)) {
					command = "fdp";
				}
			}

			for(NetworkCable nc : getComponentGroupByName("0.0.0.0").cables) {
				if(nc.getLeft().getComponentGroup().equals(nc.getRight().getComponentGroup())) {
					layout.addCable(nc);
				}
			}

			// Add all group objects to the layouter
			for(ComponentGroup cg : processedGroups) {
				// Use fdp layouter if there are nodes with fixed positions
				if(!layout.addComponentGroup(cg)) {
					command = "fdp";
				}
			}

			// Run the layouter
			layout.run(command);
		}
		catch(IOException ex) {
			StringBuilder sb = new StringBuilder("Caught: [");
			sb.append(ex.getClass().getSimpleName());
			sb.append("] ");
			sb.append(ex.getMessage());
			log.error(sb.toString());

			if(log.isDebugEnabled()) {
				for(StackTraceElement ste : ex.getStackTrace()) {
					log.debug(ste.toString());
				}
			}
		}
	}


	/**
	 * Update the orientation on all connected interfaces of the specified
	 * components. The method will try to make the interfaces connected by a
	 * cable facing each other to prevent unneccessary cable crossings. The
	 * components are specified by their RDF local names. Local names that do
	 * not identify a component will be ignored.
	 *
	 * @param pLocalNames Set of local names of the components which will be
	 *            processed
	 */
	public void updateInterfaceOrientations(HashSet<String> pLocalNames) {
		for(String locName : pLocalNames) {
			IRDFObject rdfo = localNames.get(locName);
			if(rdfo instanceof NetworkComponent && !(rdfo instanceof NCSwitch)) {
				NetworkComponent nc = (NetworkComponent) rdfo;
				HashMap<String, NetworkComponent.Interface> ifmap = nc.getConfig().getPorts();

				int freeIfSpotsLeft = nc.getConfig().getComponentDimensions().getY();
				int freeIfSpotsRight = nc.getConfig().getComponentDimensions().getY();
				int freeIfSpotsTop = nc.getConfig().getComponentDimensions().getX();
				int freeIfSpotsBottom = nc.getConfig().getComponentDimensions().getX();

				for(Map.Entry<String, NetworkComponent.Interface> entry : ifmap.entrySet()) {
					if(entry.getValue().isConnected()) {
						NetworkComponent.Interface thisIf = entry.getValue();
						NetworkComponent.Interface otherIf = null;
						Position2D thisPos;
						Position2D otherPos;

						if(thisIf.getCable().getGroupGateway() != null) {
							ComponentGroup cg = thisIf.getCable().getGroupGateway().getOuterType();

							thisPos = thisIf.getComponent().getConfig().getGridLocation();

							otherPos = cg.getGridLocation();
						}
						else {
							switch(thisIf.getCableEnd()) {
								case LEFT:
									otherIf = thisIf.getCable().getRight();
									break;
								case RIGHT:
									otherIf = thisIf.getCable().getLeft();
									break;
							}

							thisPos = thisIf.getComponent().getConfig().getGridLocation();
							otherPos = otherIf.getComponent().getConfig().getGridLocation();
						}

						if(Math.abs(thisPos.getX() - otherPos.getX()) >= Math.abs(thisPos.getY() - otherPos.getY())) {
							if(thisPos.getX() - otherPos.getX() < 0) {
								// If there is space on the right side, place it
								// there. Otherwise try positioning it on the
								// other sides
								if(freeIfSpotsRight > 0) {
									thisIf.setOrientation(PortOrientation.RIGHT);
									freeIfSpotsRight--;
								}
								else if(freeIfSpotsLeft > 0) {
									thisIf.setOrientation(PortOrientation.LEFT);
									freeIfSpotsLeft--;
								}
								else if(freeIfSpotsTop > 0) {
									thisIf.setOrientation(PortOrientation.TOP);
									freeIfSpotsTop--;
								}
								else if(freeIfSpotsBottom > 0) {
									thisIf.setOrientation(PortOrientation.BOTTOM);
									freeIfSpotsBottom--;
								}
							}
							else {
								// If there is space on the right side, place it
								// there. Otherwise try positioning it on the
								// other sides
								if(freeIfSpotsLeft > 0) {
									thisIf.setOrientation(PortOrientation.LEFT);
									freeIfSpotsLeft--;
								}
								else if(freeIfSpotsRight > 0) {
									thisIf.setOrientation(PortOrientation.RIGHT);
									freeIfSpotsRight--;
								}
								else if(freeIfSpotsTop > 0) {
									thisIf.setOrientation(PortOrientation.TOP);
									freeIfSpotsTop--;
								}
								else if(freeIfSpotsBottom > 0) {
									thisIf.setOrientation(PortOrientation.BOTTOM);
									freeIfSpotsBottom--;
								}
							}
						}
						else {
							if(thisPos.getY() - otherPos.getY() < 0) {
								// If there is space on the top side, place it
								// there. Otherwise try positioning it on the
								// other sides
								if(freeIfSpotsBottom > 0) {
									thisIf.setOrientation(PortOrientation.BOTTOM);
									freeIfSpotsBottom--;
								}
								else if(freeIfSpotsTop > 0) {
									thisIf.setOrientation(PortOrientation.TOP);
									freeIfSpotsTop--;
								}
								else if(freeIfSpotsLeft > 0) {
									thisIf.setOrientation(PortOrientation.LEFT);
									freeIfSpotsLeft--;
								}
								else if(freeIfSpotsRight > 0) {
									thisIf.setOrientation(PortOrientation.RIGHT);
									freeIfSpotsRight--;
								}
							}
							else {
								// If there is space on the top side, place it
								// there. Otherwise try positioning it on the
								// other sides
								if(freeIfSpotsTop > 0) {
									thisIf.setOrientation(PortOrientation.TOP);
									freeIfSpotsTop--;
								}
								else if(freeIfSpotsBottom > 0) {
									thisIf.setOrientation(PortOrientation.BOTTOM);
									freeIfSpotsBottom--;
								}
								else if(freeIfSpotsLeft > 0) {
									thisIf.setOrientation(PortOrientation.LEFT);
									freeIfSpotsLeft--;
								}
								else if(freeIfSpotsRight > 0) {
									thisIf.setOrientation(PortOrientation.RIGHT);
									freeIfSpotsRight--;
								}
							}
						}
					}
					else {
						switch(entry.getValue().getOrientation()) {
							case LEFT:
								// If there is space on the left side, leave it
								// there. Otherwise try positioning it on the
								// other sides
								if(freeIfSpotsLeft > 0) {
									freeIfSpotsLeft--;
								}
								else if(freeIfSpotsRight > 0) {
									entry.getValue().setOrientation(PortOrientation.RIGHT);
									freeIfSpotsRight--;
								}
								else if(freeIfSpotsTop > 0) {
									entry.getValue().setOrientation(PortOrientation.TOP);
									freeIfSpotsTop--;
								}
								else if(freeIfSpotsBottom > 0) {
									entry.getValue().setOrientation(PortOrientation.BOTTOM);
									freeIfSpotsBottom--;
								}
								break;
							case RIGHT:
								// If there is space on the right side, leave it
								// there. Otherwise try positioning it on the
								// other sides
								if(freeIfSpotsRight > 0) {
									freeIfSpotsRight--;
								}
								else if(freeIfSpotsLeft > 0) {
									entry.getValue().setOrientation(PortOrientation.LEFT);
									freeIfSpotsLeft--;
								}
								else if(freeIfSpotsTop > 0) {
									entry.getValue().setOrientation(PortOrientation.TOP);
									freeIfSpotsTop--;
								}
								else if(freeIfSpotsBottom > 0) {
									entry.getValue().setOrientation(PortOrientation.BOTTOM);
									freeIfSpotsBottom--;
								}
								break;
							case TOP:
								// If there is space on the top side, leave it
								// there. Otherwise try positioning it on the
								// other sides
								if(freeIfSpotsTop > 0) {
									freeIfSpotsTop--;
								}
								else if(freeIfSpotsBottom > 0) {
									entry.getValue().setOrientation(PortOrientation.BOTTOM);
									freeIfSpotsBottom--;
								}
								else if(freeIfSpotsLeft > 0) {
									entry.getValue().setOrientation(PortOrientation.LEFT);
									freeIfSpotsLeft--;
								}
								else if(freeIfSpotsRight > 0) {
									entry.getValue().setOrientation(PortOrientation.RIGHT);
									freeIfSpotsRight--;
								}
								break;
							case BOTTOM:
								// If there is space on the bottom side, leave
								// it
								// there. Otherwise try positioning it on the
								// other sides
								if(freeIfSpotsBottom > 0) {
									freeIfSpotsBottom--;
								}
								else if(freeIfSpotsTop > 0) {
									entry.getValue().setOrientation(PortOrientation.TOP);
									freeIfSpotsTop--;
								}
								else if(freeIfSpotsLeft > 0) {
									entry.getValue().setOrientation(PortOrientation.LEFT);
									freeIfSpotsLeft--;
								}
								else if(freeIfSpotsRight > 0) {
									entry.getValue().setOrientation(PortOrientation.RIGHT);
									freeIfSpotsRight--;
								}
						}
					}
				}
			}
		}
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((localNames == null) ? 0 : localNames.hashCode());
		result = prime * result + ((storage == null) ? 0 : storage.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		TopologyStorage other = (TopologyStorage) obj;
		if(localNames == null) {
			if(other.localNames != null)
				return false;
		}
		else if(!localNames.equals(other.localNames))
			return false;
		if(storage == null) {
			if(other.storage != null)
				return false;
		}
		else if(!storage.equals(other.storage))
			return false;
		return true;
	}


	/**
	 * Find the first unused component ID. The method is counting up from 1
	 * until an unused ID is found. So if the used IDs are 1, 2, 4 it will
	 * return the 3, on the next call it will return 5. If an ID is not marked
	 * as used but a local name following the scheme pType_newID already exists,
	 * the ID will be marked as used and the search will continue.
	 *
	 * @param pType Value of the TYPE parameter of a class, cannot be null
	 * @return The next free component ID
	 */
	private int getNextComponentID(String pType) {
		int newID = 1;

		while(usedIDs.contains(newID)) {
			newID++;

			StringBuilder sb = new StringBuilder(pType);
			sb.append("_");
			sb.append(newID);

			if(localNames.containsKey(sb.toString())) {
				usedIDs.add(newID);
			}
		}

		usedIDs.add(newID);

		return newID;
	}


	/**
	 * Find the first unused cable ID. The method is counting up from 1
	 * until an unused ID is found. So if the used IDs are 1, 2, 4 it will
	 * return the 3, on the next call it will return 5. If an ID is not marked
	 * as used but a local name following the scheme ncable_newID already
	 * exists,
	 * the ID will be marked as used and the search will continue.
	 *
	 * @return The next free cable ID
	 */
	private int getNextCableID() {
		int newID = 1;

		StringBuilder sb = new StringBuilder(NetworkCable.TYPE);
		sb.append("_");
		sb.append(newID);

		while(cables.containsKey(sb.toString())) {
			newID++;

			sb = new StringBuilder(NetworkCable.TYPE);
			sb.append("_");
			sb.append(newID);
		}

		return newID;
	}


	/**
	 * Return the next unused VLAN ID. This is achieved by incrementing the last
	 * known ID by 1. The last known ID is updated when VLANs are created using
	 * RDF information, so there may be gabs in the ID list.
	 *
	 * @return The next VLAN ID
	 */
	private int getNextVLANID() {
		int newID = lastVLANID + 1;
		lastVLANID = newID;

		return newID;
	}


	/**
	 * Add the specified component to the storage structure and the RDF model if
	 * requested.
	 *
	 * @param pComponent Component which will be added
	 * @param pUpdateRDF true: Update the RDF model with the information stored
	 *            in the component object. false: Do not touch the RDF model.
	 */
	private void addComponent(NetworkComponent pComponent, final boolean pUpdateRDF) {
		if(log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Adding component to storage: ");
			sb.append(pComponent.getIdentifier());
			sb.append(", local name: ");
			sb.append(pComponent.getRDFLocalName());
			log.debug(sb.toString());
		}

		if(!localNames.containsKey(pComponent.getRDFLocalName())) {
			if(pUpdateRDF) {
				if(log.isDebugEnabled()) {
					StringBuilder sb = new StringBuilder("Adding object to RDF model: ");
					sb.append(pComponent.getRDFLocalName());
					log.debug(sb.toString());
				}

				// Add the object to the RDF model
				TEBackend.RDF_MANAGER.addObject(pComponent);
			}

			localNames.put(pComponent.getRDFLocalName(), pComponent);

			ComponentGroup group = getComponentGroupByName(pComponent.getConfig().getComponentGroup());
			group.addComponent(pComponent);
		}
		else {
			throw new IllegalStateException("The RDF local name of this object is already in use");
		}
	}


	/**
	 * This class represents a group of components that share a specific
	 * property. They can belong to the same subnet or the same VSA. The group
	 * name identifies the group and should be unique, using an already taken
	 * group name will overwrite the existing group.
	 *
	 * @author Thomas Rix
	 */
	public class ComponentGroup implements IJSON {
		private HashSet<NetworkComponent> componentList;
		private String groupName;
		private Position2D gridLocation;
		private Dimension2D groupDimensions;
		private Dimension2D dragboxDimensions;
		private Position2D componentInterfaceOffset;
		private Dimension2D subGridDimensions;
		private HashSet<GroupInterface> groupInterfaces;
		private HashMap<String, GroupSwitch> groupSwitches;
		private HashSet<NetworkCable> cables;
		private int nextGroupInterfaceID;
		private String identifier;


		/**
		 * Construct a new group using the specified name as identifier. The
		 * group element on the editor grid will have the dimensions 10x10 grid
		 * cells and the subgrid will have the dimensions 80x40 grid cells.
		 *
		 * @param pName Group name, cannot be empty or null
		 */
		private ComponentGroup(String pName) {
			nextGroupInterfaceID = 0;

			if(!pName.isEmpty()) {
				groupName = pName;
			}
			else {
				throw new IllegalArgumentException("Empty group name provided");
			}

			StringBuilder sbID = new StringBuilder("cgroup");
			sbID.append(nextGroupID);
			nextGroupID++;
			identifier = sbID.toString();

			gridLocation = null;
			groupDimensions = new Dimension2D(10, 10);
			subGridDimensions = new Dimension2D(175, 75);

			componentList = new HashSet<>();
			groupInterfaces = new HashSet<>();
			groupSwitches = new HashMap<>();
			cables = new HashSet<>();

			calcDragboxDimensions();
		}


		/**
		 * Return the identifier of this component group
		 *
		 * @return Identifier string
		 */
		public String getIdentifier() {
			return identifier;
		}


		/**
		 * Return the name of this group
		 *
		 * @return The name of this group
		 */
		public String getName() {
			return groupName;
		}


		/**
		 * Add a component to this group.
		 *
		 * @param pNC Component which will be added, cannot be null
		 * @return true if the component was added successfully, false otherwise
		 */
		boolean addComponent(NetworkComponent pNC) {
			if(pNC != null) {
				boolean rv = componentList.add(pNC);
				return rv;
			}
			else {
				throw new NullPointerException("NULL pointer for network component");
			}
		}


		/**
		 * Remove a component from this group.
		 *
		 * @param pNC Component which will be removed, cannot be null
		 * @return true if the component was removed successfully, false
		 *         otherwise
		 */
		boolean removeComponent(NetworkComponent pNC) {
			if(pNC != null) {
				boolean rv = componentList.remove(pNC);

				if(componentList.isEmpty()) {
					for(Map.Entry<String, GroupSwitch> gsEntry : groupSwitches.entrySet()) {
						gsEntry.getValue().removeFromGroup();
					}

					for(GroupInterface gIf : groupInterfaces) {
						gIf.removeFromGroup();
					}

					storage.remove(groupName);
				}

				return rv;
			}
			else {
				throw new NullPointerException("NULL pointer for network component");
			}
		}


		/**
		 * Set the grid location of this group to the specified coordinates
		 *
		 * @param pX Horizontal coordinate
		 * @param pY Vertical coordinate
		 */
		public void setGridLocation(int pX, int pY) {
			gridLocation = new Position2D(pX, pY, TEBackend.getGridDimensions());

			if(log.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder("ComponentGroup relocated to x=");
				sb.append(gridLocation.getX());
				sb.append(", y=");
				sb.append(gridLocation.getY());
				log.debug(sb.toString());
			}
		}


		/**
		 * Return the grid location of this group
		 *
		 * @return The grid location of this group
		 */
		public Position2D getGridLocation() {
			return gridLocation;
		}


		/**
		 * Set the size of the subgrid to the provided dimensions
		 *
		 * @param pX Horizontal size in grid cells
		 * @param pY Vertical size in grid cells
		 */
		public void setSubgridDimensions(int pX, int pY) {
			if(subGridDimensions == null) {
				subGridDimensions = new Dimension2D(pX, pY);
			}
			else {
				subGridDimensions.set(pX, pY);
			}
		}


		/**
		 * Return the dimensions of the subgrid
		 *
		 * @return The dimensions of the subgrid
		 */
		public Dimension2D getSubgridDimensions() {
			return subGridDimensions;
		}


		/**
		 * Return the dimensions of the dragbox containing this group
		 *
		 * @return The dimensions of the dragbox
		 */
		public Dimension2D getDragboxDimensions() {
			return dragboxDimensions;
		}


		/**
		 * Add a virtual group switch to this group.
		 *
		 * @param pGS Reference to the group switch
		 */
		void addGroupSwitch(GroupSwitch pGS) {
			if(pGS != null) {
				groupSwitches.put(pGS.getSwitchRDFLocalName(), pGS);
			}
			else {
				throw new NullPointerException("NULL pointer for group switch");
			}
		}


		/**
		 * Remove a virtual group switch from this group.
		 *
		 * @param pGS Reference to the group switch
		 */
		void removeGroupSwitch(GroupSwitch pGS) {
			if(pGS != null) {
				groupInterfaces.remove(pGS.getVirtualInterface().getConnectedTo());
				groupSwitches.remove(pGS.getSwitchRDFLocalName());
			}
			else {
				throw new NullPointerException("NULL pointer for group switch");
			}
		}


		/**
		 * Get the group switch with the specified RDF local name from this
		 * group.
		 *
		 * @param pSwitchLocName RDF local name of the group switch
		 * @return The requested object, null if no object was found
		 */
		public GroupSwitch getGroupSwitch(String pSwitchLocName) {
			return groupSwitches.get(pSwitchLocName);
		}


		/**
		 * Get a set containing all {@link GroupInterface GroupInterfaces} of
		 * this group.
		 *
		 * @return A set containing all {@link GroupInterface GroupInterfaces}
		 *         of this group
		 */
		public HashSet<GroupInterface> getGroupInterfaces() {
			return groupInterfaces;
		}


		/**
		 * Create a new connection between an interface inside the group and an
		 * interface outside the group. This will add a new GroupInterface to
		 * this group which will act as gateway through the group border to
		 * connect the inner and outer interfaces.
		 *
		 * @param pInner Interface of the component inside the group
		 * @param pOuter Interface of the component outside the group
		 * @return true if the group interface was added successfully, false
		 *         otherwise
		 */
		public GroupInterface createOuterConnection(INetworkInterface pInner, INetworkInterface pOuter) {
			GroupInterface newGI = new GroupInterface(pInner, pOuter);

			groupInterfaces.add(newGI);

			calcDragboxDimensions();

			return newGI;
		}


		/**
		 * Add a cable to this group
		 *
		 * @param pNC The cable to be added
		 * @return true if adding the cable was successful, false otherwise
		 */
		boolean addCable(NetworkCable pNC) {
			if(pNC != null) {
				return cables.add(pNC);
			}
			else {
				throw new NullPointerException("NULL pointer for cable provided");
			}
		}


		/**
		 * Remove a cable from this group
		 *
		 * @param pNC The cable to be removed
		 * @return true if removing the cable was successful, false otherwise
		 */
		boolean removeCable(NetworkCable pNC) {
			if(pNC != null) {
				return cables.remove(pNC);
			}
			else {
				throw new NullPointerException("NULL pointer for cable provided");
			}
		}


		/**
		 * Check if this is the global group 0.0.0.0
		 *
		 * @return true if the group name equals "0.0.0.0", false otherwise
		 */
		public boolean isGlobalGroup() {
			return groupName.equals("0.0.0.0");
		}


		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject rv = new JSONObject();

			rv.put("name", StringEscapeUtils.escapeHtml4(groupName));
			rv.put("identifier", identifier);
			rv.put("isGroup", true);

			JSONObject compsJSON = new JSONObject();
			for(NetworkComponent nc : componentList) {
				JSONObject ncJSON = null;
				if(nc instanceof NCSwitch) {
					ncJSON = ((NCSwitch) nc).toJSON();
				}
				else if(nc instanceof NCHost) {
					ncJSON = ((NCHost) nc).toJSON();
				}
				else if(nc instanceof NCVM) {
					ncJSON = ((NCVM) nc).toJSON();
				}

				if(ncJSON != null) {
					compsJSON.put(nc.getIdentifier(), ncJSON);
				}
			}
			rv.put("components", compsJSON);

			JSONObject cableJSON = new JSONObject();
			for(NetworkCable nc : cables) {
				cableJSON.put(nc.getIdentifier(), nc.toJSON());
			}
			rv.put("cables", cableJSON);

			JSONObject gsJSON = new JSONObject();
			for(Map.Entry<String, GroupSwitch> gsEntry : groupSwitches.entrySet()) {
				gsJSON.put(gsEntry.getValue().getRDFLocalName(), gsEntry.getValue().toJSON());
			}
			rv.put("groupSwitches", gsJSON);

			JSONObject confJSON = new JSONObject();

			if(gridLocation != null) {
				confJSON.put("gloc", gridLocation.toJSON());
			}

			confJSON.put("compOffset", componentInterfaceOffset.toJSON());
			confJSON.put("compDim", groupDimensions.toJSON());
			confJSON.put("dragDim", dragboxDimensions.toJSON());
			confJSON.put("gridDim", subGridDimensions.toJSON());

			JSONObject ifJSON = new JSONObject();
			for(GroupInterface gIf : groupInterfaces) {
				ifJSON.put(gIf.localName, gIf.toJSON());
			}
			confJSON.put("interfaces", ifJSON);

			rv.put("config", confJSON);

			return rv;
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			else if(obj instanceof ComponentGroup) {
				ComponentGroup ng = (ComponentGroup) obj;

				return ng.groupName.equals(groupName);
			}
			else {
				return false;
			}
		}


		/**
		 * Calculate the dimensions of the dragbox containing this group object
		 */
		private void calcDragboxDimensions() {
			if(groupDimensions != null) {
				int x = groupDimensions.getX() + TEBackend.getComponentMargin() * 2;
				int y = groupDimensions.getY() + TEBackend.getComponentMargin() * 2 + groupInterfaces.size();
				int ifOffsetX = TEBackend.getComponentMargin();
				int ifOffsetY = TEBackend.getComponentMargin();

				dragboxDimensions = new Dimension2D(x, y);
				componentInterfaceOffset = new Position2D(ifOffsetX, ifOffsetY);

				if(log.isDebugEnabled()) {
					StringBuilder sb = new StringBuilder("Dragbox resized to x=");
					sb.append(dragboxDimensions.getX());
					sb.append(", y=");
					sb.append(dragboxDimensions.getY());
					log.debug(sb.toString());
				}
			}
			else {
				throw new IllegalStateException("No component dimensions were set");
			}
		}


		/**
		 * An interface connecting two interfaces through the border of a
		 * ComponentGroup.
		 *
		 * @author Thomas Rix
		 */
		public class GroupInterface implements IRDFObject, IJSON {
			private INetworkInterface innerConnection;
			private INetworkInterface outerConnection;
			private PortOrientation orientation;
			private String localName;


			/**
			 * Construct a new group interface connecting the two specified
			 * interfaces.
			 *
			 * @param pInner Interface that is located inside the group, cannot
			 *            be null and must be part of a component inside the
			 *            group
			 * @param pOuter Interface that is located outside the group, cannot
			 *            be null and must be part of a component that belongs
			 *            to the global group 0.0.0.0
			 */
			private GroupInterface(INetworkInterface pInner, INetworkInterface pOuter) {
				if(pInner != null) {
					if(pInner.getComponentGroup().equals(getOuterType().groupName)) {
						innerConnection = pInner;
					}
					else {
						StringBuilder sb = new StringBuilder("Inner interface group mismatch: ");
						sb.append(pInner.getComponentGroup());
						sb.append(" -> ");
						sb.append(getOuterType().groupName);

						log.error(sb.toString());

						throw new IllegalArgumentException("Specified inner interface is not located in this group");
					}
				}
				else {
					throw new NullPointerException("NULL pointer for inner virtual interface");
				}

				if(pOuter != null) {
					if(pOuter.getComponentGroup().equals("0.0.0.0")) {
						outerConnection = pOuter;
					}
					else {
						StringBuilder sb = new StringBuilder("Outer interface group mismatch: ");
						sb.append(pOuter.getComponentGroup());
						sb.append(" -> ");
						sb.append("0.0.0.0");

						log.error(sb.toString());

						throw new IllegalArgumentException("Specified outer interface is not located in the global group 0.0.0.0");
					}
				}
				else {
					throw new NullPointerException("NULL pointer for outer virtual interface");
				}

				StringBuilder sbLocName = new StringBuilder("GroupInterface_");
				sbLocName.append(getOuterType().getIdentifier());
				sbLocName.append("_");
				sbLocName.append(getOuterType().nextGroupInterfaceID);

				localName = sbLocName.toString();

				getOuterType().nextGroupInterfaceID++;

				orientation = PortOrientation.BOTTOM;
			}


			/**
			 * Return the interface of the inner component
			 *
			 * @return The interface of the inner component
			 */
			public INetworkInterface getInnerConnection() {
				return innerConnection;
			}


			/**
			 * Return the interface of the outer component
			 *
			 * @return The interface of the outer component
			 */
			public INetworkInterface getOuterConnection() {
				return outerConnection;
			}


			/**
			 * Remove this group interface from the containing group
			 */
			void removeFromGroup() {
				groupInterfaces.remove(this);
			}


			/**
			 * Set the orientation of this interface.
			 *
			 * @param pOri The new orientation, cannot be null
			 */
			public void setOrientation(PortOrientation pOri) {
				if(pOri != null) {
					if(!pOri.toString().equals(orientation.toString())) {
						orientation = pOri;

						ComponentGroup.this.calcDragboxDimensions();

						if(log.isDebugEnabled()) {
							StringBuilder sb = new StringBuilder("Interface orientation of '");
							sb.append(localName);
							sb.append("' updated to: ");
							sb.append(orientation.toString());
							log.debug(sb.toString());
						}
					}
					else {
						if(log.isDebugEnabled()) {
							StringBuilder sb = new StringBuilder("Interface orientation of '");
							sb.append(localName);
							sb.append("' did not change. Old and new orientation: ");
							sb.append(orientation.toString());
							log.debug(sb.toString());
						}
					}
				}
				else {
					throw new NullPointerException("NULL pointer for interface orientation provided");
				}
			}


			/**
			 * Return the orientation of this port
			 *
			 * @return The orientation of this port
			 */
			public PortOrientation getOrientation() {
				return orientation;
			}


			@Override
			public String getRDFLocalName() {
				return localName;
			}


			@Override
			public JSONObject toJSON() throws JSONException {
				JSONObject rv = new JSONObject();

				rv.put("identifier", localName);
				rv.put("orientation", orientation.toString());
				rv.put("innerConn", innerConnection.toJSON());
				rv.put("outerConn", outerConnection.toJSON());

				return rv;
			}


			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + getOuterType().hashCode();
				result = prime * result + ((localName == null) ? 0 : localName.hashCode());
				result = prime * result + ((innerConnection == null) ? 0 : innerConnection.hashCode());
				result = prime * result + ((outerConnection == null) ? 0 : outerConnection.hashCode());
				return result;
			}


			@Override
			public boolean equals(Object obj) {
				if(this == obj) {
					return true;
				}
				else if(obj instanceof GroupInterface) {
					GroupInterface ng = (GroupInterface) obj;

					return (ng.getOuterType().equals(getOuterType()) && ng.localName.equals(localName) && ng.innerConnection.equals(innerConnection) && ng.outerConnection.equals(outerConnection));
				}
				else {
					return false;
				}
			}


			/**
			 * Return the enclosing ComponentGroup object
			 *
			 * @return The enclosing ComponentGroup object
			 */
			private ComponentGroup getOuterType() {
				return ComponentGroup.this;
			}
		}
	}
}
