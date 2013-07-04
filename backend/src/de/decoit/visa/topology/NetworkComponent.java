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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.Dimension2D;
import de.decoit.visa.Position2D;
import de.decoit.visa.TEBackend;
import de.decoit.visa.enums.CableEnd;
import de.decoit.visa.enums.IPVersion;
import de.decoit.visa.enums.PortOrientation;
import de.decoit.visa.interfaces.IJSON;
import de.decoit.visa.interfaces.INetworkInterface;
import de.decoit.visa.interfaces.IRDFObject;
import de.decoit.visa.interfaces.IRemovableComponent;
import de.decoit.visa.interfaces.ITopologyElement;
import de.decoit.visa.net.IPAddress;
import de.decoit.visa.net.IPNetwork;
import de.decoit.visa.rdf.VISA;
import de.decoit.visa.rdf.VISABackup;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup;


/**
 * This class is an abstract base class for all physical network components,
 * e.g. servers, clients, routers etc. It is extended by the classes
 * {@link NCHost}, {@link NCVM} and {@link NCSwitch} to represent specific
 * component types.
 *
 * @author Thomas Rix
 */
abstract public class NetworkComponent implements ITopologyElement, IRemovableComponent, IRDFObject {
	protected static Logger log = Logger.getLogger(NetworkComponent.class.getName());

	protected String name;
	protected String localName;
	protected NetworkComponent.ComponentConfig config;
	private int id;
	private boolean objectBeingRemoved;


	/**
	 * Construct a new NetworkComponent object from information received from
	 * the frontend.
	 *
	 * @param pID ID number
	 * @param pClassType Type string used to build the local name string
	 * @param pName Human readable name
	 * @param pPortInfo Interface configuration
	 * @param pCompDimensions Component dimensions
	 * @param pGridLoc Position of the component on the editor grid
	 */
	NetworkComponent(int pID, String pClassType, String pName, List<String> pPortInfo, Dimension2D pCompDimensions, Position2D pGridLoc) {
		id = pID;
		objectBeingRemoved = false;

		// Construct the identifier from the type information and ID number
		StringBuilder sbIdentifier = new StringBuilder();
		sbIdentifier.append(pClassType);
		sbIdentifier.append("_");
		sbIdentifier.append(pID);
		localName = sbIdentifier.toString();

		// Set the name to the name prefix, this checks if pName is empty or
		// null
		this.setName(pName);

		config = new NetworkComponent.ComponentConfig(pPortInfo.size(), pPortInfo, pCompDimensions, pGridLoc);

		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.COMP_LOC_X);
		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.COMP_LOC_Y);
		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.COMP_DIM_X);
		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.COMP_DIM_Y);
	}


	/**
	 * Construct a new NetworkComponent object from RDF information.
	 *
	 * @param pID ID number
	 * @param pName Human readable name
	 * @param pPortInfo Interface configuration
	 * @param pCompDimensions Component dimensions
	 * @param pGridLoc Position of the component on the editor grid
	 * @param pLocalName RDF local name
	 */
	NetworkComponent(int pID, String pName, List<HashMap<String, String>> pPortInfo, Dimension2D pCompDimensions, Position2D pGridLoc, String pLocalName) {
		id = pID;

		// Local name is identical to identifier if not specified
		if(pLocalName != null && !pLocalName.isEmpty()) {
			localName = pLocalName;
		}
		else {
			throw new IllegalArgumentException("Local name cannot be empty");
		}

		// Set the name to the name prefix, this checks if pName is empty or
		// null
		this.setName(pName);

		config = new NetworkComponent.ComponentConfig(pPortInfo, pCompDimensions, pGridLoc);

		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.COMP_LOC_X);
		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.COMP_LOC_Y);
		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.COMP_DIM_X);
		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.COMP_DIM_Y);
	}


	/**
	 * Set a new name for this component
	 *
	 * @param pName The new name, cannot be empty or null
	 */
	public void setName(String pName) {
		if(pName != null) {
			if(!pName.isEmpty()) {
				name = pName.replaceAll("\\$ID\\$", Integer.toString(id));
				TEBackend.RDF_MANAGER.updateProperty(this, VISA.NAME);
			}
			else {
				throw new IllegalArgumentException("Empty name provided");
			}
		}
		else {
			throw new NullPointerException("NULL pointer for name provided");
		}
	}


	/**
	 * Return the current name of this component
	 *
	 * @return The current name of this component
	 */
	public String getName() {
		return name;
	}


	/**
	 * Return the configuration object of this component
	 *
	 * @return The configuration object
	 */
	public NetworkComponent.ComponentConfig getConfig() {
		return config;
	}


	@Override
	public String getRDFLocalName() {
		return localName;
	}


	@Override
	public String getIdentifier() {
		return getRDFLocalName();
	}


	@Override
	public void removeFromTopology() {
		objectBeingRemoved = true;

		// TEBackend.TOPOLOGY_STORAGE.getComponentGroup(config.getComponentGroup()).removeComponent(this);

		// Disconnect every NetworkPort and remove it from the topology storage
		for(Map.Entry<String, NetworkComponent.Interface> entry : config.ports.entrySet()) {
			TEBackend.TOPOLOGY_STORAGE.removeInterface(entry.getValue().getIdentifier());
		}

		// Remove this component from the RDF model
		TEBackend.RDF_MANAGER.removeObject(this);
	}


	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject rv = new JSONObject();

		rv.put("id", id);
		rv.put("identifier", localName);
		rv.put("name", StringEscapeUtils.escapeHtml4(name));
		rv.put("config", config.toJSON());

		if(this instanceof NCSwitch) {
			rv.put("isSwitch", true);
		}
		else {
			rv.put("isSwitch", false);
		}

		rv.put("isGroup", false);

		return rv;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((localName == null) ? 0 : localName.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		else if(obj instanceof NetworkComponent) {
			NetworkComponent nc = (NetworkComponent) obj;
			return (nc.id == id && nc.localName.equals(localName));
		}
		else {
			return false;
		}
	}


	/**
	 * Objects of this class store configuration information of NetworkComponent
	 * objects. This includes the interfaces and component group as well as
	 * frontend specific information like grid position and component size.
	 *
	 * @author Thomas Rix
	 */
	@SuppressWarnings("ucd")
	public class ComponentConfig implements IJSON {
		protected HashMap<String, NetworkComponent.Interface> ports;
		private int portcount;
		private Position2D gridLocation;
		private Position2D componentInterfaceOffset;
		private Dimension2D componentDimensions;
		private Dimension2D dragboxDimensions;
		private String componentGroup;


		/**
		 * Construct a new object from information received from the frontend.
		 *
		 * @param pPortCount Number of interfaces of the component
		 * @param pPortInfo Interface configuration
		 * @param pCompDimensions Component dimensions
		 * @param pGridLoc Position of the component on the editor grid
		 */
		private ComponentConfig(int pPortCount, List<String> pPortInfo, Dimension2D pCompDimensions, Position2D pGridLoc) {
			portcount = pPortCount;
			componentDimensions = pCompDimensions;
			dragboxDimensions = null;
			gridLocation = pGridLoc;
			ports = new HashMap<>();
			componentGroup = null;

			for(String orientation : pPortInfo) {
				NetworkComponent.Interface newPort = TEBackend.TOPOLOGY_STORAGE.createInterface(PortOrientation.valueOf(orientation), getOuterType());

				// Add the interface to this NetworkComponent
				ports.put(newPort.getIdentifier(), newPort);
			}

			// By default every component is grouped in 0.0.0.0
			componentGroup = "0.0.0.0";

			calcDragboxDimensions();
		}


		/**
		 * Construct a new object from RDF information.
		 *
		 * @param pPortInfo Interface configuration
		 * @param pCompDimensions Component dimensions
		 * @param pGridLoc Position of the component on the editor grid
		 */
		private ComponentConfig(List<HashMap<String, String>> pPortInfo, Dimension2D pCompDimensions, Position2D pGridLoc) {
			portcount = pPortInfo.size();
			componentDimensions = pCompDimensions;
			dragboxDimensions = null;
			gridLocation = pGridLoc;
			ports = new HashMap<>();
			componentGroup = null;

			Iterator<HashMap<String, String>> portInfoIt = pPortInfo.iterator();

			while(portInfoIt.hasNext()) {
				NetworkComponent.Interface newPort = null;

				HashMap<String, String> portInfoMap = portInfoIt.next();

				// Create a new interface with the provided information
				newPort = TEBackend.TOPOLOGY_STORAGE.createInterface(PortOrientation.valueOf(portInfoMap.get("orientation")), getOuterType(), portInfoMap.get("localName"));

				// If IP configuration information is present, set it
				if(portInfoMap.containsKey("IPAddress") && portInfoMap.containsKey("IPNetwork")) {
					IPNetwork newNet = TEBackend.TOPOLOGY_STORAGE.getNetwork(portInfoMap.get("IPNetwork"));

					try {
						newPort.configureIP(portInfoMap.get("IPAddress"), newNet);
					}
					catch(Throwable ex) {
						throw ex;
					}
				}

				// Assign interface to VLANs if the vlan key is present
				if(portInfoMap.containsKey("vlan")) {
					HashSet<VLAN> vlanSet = new HashSet<>();

					// Split the vlan information from RDF model
					String[] vlans = portInfoMap.get("vlan").split(";");
					for(String vlanLocName : vlans) {
						vlanSet.add(TEBackend.TOPOLOGY_STORAGE.getVLAN(vlanLocName));
					}

					newPort.setVLAN(vlanSet);
				}

				// Add the interface to this NetworkComponent
				ports.put(newPort.getIdentifier(), newPort);
			}

			// By default every component is grouped in 0.0.0.0
			componentGroup = "0.0.0.0";

			calcDragboxDimensions();
		}


		/**
		 * Set a new grid location for the component.
		 *
		 * @param pX Horizontal position
		 * @param pY Vertical position
		 */
		public void setGridLocation(int pX, int pY) {
			gridLocation = new Position2D(pX, pY, TEBackend.getGridDimensions());

			TEBackend.RDF_MANAGER.updateProperty(getOuterType(), VISABackup.COMP_LOC_X);
			TEBackend.RDF_MANAGER.updateProperty(getOuterType(), VISABackup.COMP_LOC_Y);

			if(log.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder("Component ");
				sb.append(localName);
				sb.append(" relocated to x=");
				sb.append(gridLocation.getX());
				sb.append(", y=");
				sb.append(gridLocation.getY());
				log.debug(sb.toString());
			}
		}


		/**
		 * Return the current grid location
		 *
		 * @return The current grid location
		 */
		public Position2D getGridLocation() {
			return gridLocation;
		}


		/**
		 * Resize the component to the specified dimensions.
		 *
		 * @param pX Horizontal dimension
		 * @param pY Vertical dimension
		 */
		public void setComponentDimensions(int pX, int pY) {
			if(componentDimensions == null) {
				componentDimensions = new Dimension2D(pX, pY);
			}
			else {
				componentDimensions.set(pX, pY);
			}

			TEBackend.RDF_MANAGER.updateProperty(getOuterType(), VISABackup.COMP_DIM_X);
			TEBackend.RDF_MANAGER.updateProperty(getOuterType(), VISABackup.COMP_DIM_Y);

			calcDragboxDimensions();
		}


		/**
		 * Return the current dimensions of the component
		 *
		 * @return The current dimensions
		 */
		public Dimension2D getComponentDimensions() {
			return componentDimensions;
		}


		/**
		 * Return the current dimensions of the component's dragbox
		 *
		 * @return The current dimensions
		 */
		public Dimension2D getDragboxDimensions() {
			return dragboxDimensions;
		}


		/**
		 * Return a map containing all interfaces of the component.
		 *
		 * @return A map containing all interfaces of the component
		 */
		public HashMap<String, NetworkComponent.Interface> getPorts() {
			return ports;
		}


		/**
		 * Create a new interface and add it to this component.
		 *
		 * @param pOri Orientation of the new interface
		 * @return The created interface object
		 */
		public Interface createInterface(PortOrientation pOri) {
			Interface rv = TEBackend.TOPOLOGY_STORAGE.createInterface(pOri, getOuterType());

			ports.put(rv.localName, rv);

			if(getOuterType() instanceof NCSwitch) {
				NCSwitch ncs = (NCSwitch) getOuterType();
				ncs.addRealInterface(rv);
			}

			int ifCount = 0;
			for(Map.Entry<String, Interface> ifEntry : ports.entrySet()) {
				if(ifEntry.getValue().getOrientation() == pOri) {
					ifCount++;
				}
			}

			if((pOri == PortOrientation.TOP || pOri == PortOrientation.BOTTOM) && ifCount > componentDimensions.getX()) {
				setComponentDimensions(ifCount, componentDimensions.getY());
			}
			else if((pOri == PortOrientation.LEFT || pOri == PortOrientation.RIGHT) && ifCount > componentDimensions.getY()) {
				setComponentDimensions(componentDimensions.getX(), ifCount);
			}
			else {
				calcDragboxDimensions();
			}

			return rv;
		}


		/**
		 * Remove the interface with the specified local name from the component
		 *
		 * @param pLocName RDF local name of the interface
		 */
		private void removeInterface(String pLocName) {
			if(ports.containsKey(pLocName)) {
				// Only remove the interface from the port map if the component
				// is not being removed from topology itself. This prevents
				// ConcurrentModificationException
				if(!objectBeingRemoved) {
					ports.remove(pLocName);
				}
			}
			else {
				throw new IllegalArgumentException("The interface is not part of this component");
			}
		}


		/**
		 * Assign the component to a new component group
		 *
		 * @param pName Name of the component group
		 */
		public void setComponentGroup(String pName) {
			// pName cannot be empty
			if(!pName.isEmpty()) {
				// Only change group if new group is different from the former
				if(!pName.equals(componentGroup)) {
					ComponentGroup oldCG = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(componentGroup);
					if(oldCG != null) {
						oldCG.removeComponent(getOuterType());
					}

					componentGroup = pName;

					TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(componentGroup).addComponent(getOuterType());

					TEBackend.RDF_MANAGER.updateProperty(getOuterType(), VISABackup.GROUP);

					if(log.isDebugEnabled()) {
						StringBuilder sb = new StringBuilder("Group assigned: ");
						sb.append(localName);
						sb.append(" -> ");
						sb.append(componentGroup);

						log.debug(sb.toString());
					}
				}
			}
			else {
				throw new IllegalArgumentException("Empty string for component group name");
			}
		}


		/**
		 * Return the name of the current component group
		 *
		 * @return The name of the current component group
		 */
		public String getComponentGroup() {
			return componentGroup;
		}


		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject rv = new JSONObject();

			rv.put("ifcount", portcount);

			if(gridLocation != null) {
				rv.put("gloc", gridLocation.toJSON());
			}

			rv.put("compOffset", componentInterfaceOffset.toJSON());
			rv.put("compDim", componentDimensions.toJSON());
			rv.put("dragDim", dragboxDimensions.toJSON());

			JSONObject ifJSON = new JSONObject();
			for(Map.Entry<String, NetworkComponent.Interface> entry : ports.entrySet()) {
				ifJSON.put(entry.getValue().getIdentifier(), entry.getValue().toJSON());
			}
			rv.put("interfaces", ifJSON);

			return rv;
		}


		/**
		 * Calculate the size of the dragbox which surrounds this component. The
		 * calculation takes into account the component dimensions, globally set
		 * the component margin, the interface orientations and some extra space
		 * to prevent multiple components from forming impassable walls.
		 */
		protected void calcDragboxDimensions() {
			if(componentDimensions != null) {
				// Calculate dragbox dimensions, add 2 at the end to make sure
				// components cannot form an impassable wall
				int x = componentDimensions.getX() + TEBackend.getComponentMargin() * 2 + 2;
				int y = componentDimensions.getY() + TEBackend.getComponentMargin() * 2 + 2;

				// Calculate the offset of the component relative to its
				// dragbox. The 1 is added to compensate the added 2 on the
				// dragbox dimensions
				int ifOffsetX = TEBackend.getComponentMargin() + 1;
				int ifOffsetY = TEBackend.getComponentMargin() + 1;

				// Adjust the component offset according to interfaces located
				// on the component
				for(Map.Entry<String, NetworkComponent.Interface> entry : ports.entrySet()) {
					switch(entry.getValue().getOrientation()) {
						case TOP:
							y++;
							ifOffsetY++;
							break;
						case RIGHT:
							x++;
							break;
						case BOTTOM:
							y++;
							break;
						case LEFT:
							x++;
							ifOffsetX++;
							break;
					}
				}

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
		 * Return the enclosing NetworkComponent object
		 *
		 * @return The enclosing NetworkComponent object
		 */
		private NetworkComponent getOuterType() {
			return NetworkComponent.this;
		}
	}


	/**
	 * This class represents an interface of a component in the topology. It can
	 * represent any type of physical interface including ports for cables and
	 * WLAN access points.
	 *
	 * @author Thomas Rix
	 */
	public static class Interface implements INetworkInterface, ITopologyElement, IRemovableComponent, IRDFObject {
		private static Logger log = Logger.getLogger(Interface.class.getName());
		static final String TYPE = "veth";

		private String localName;
		private ArrayList<NetworkComponent.Interface.IPConfig> ipConf;
		private NetworkComponent component;
		private NetworkCable cable;
		private CableEnd cableEnd;
		private PortOrientation orientation;
		private HashSet<VLAN> virtLAN;


		/**
		 * Construct a new Interface object from information received from the
		 * frontend. The orientation argument defines at which position the
		 * interface will be displayed on the editor grid.
		 *
		 * @param pID ID number
		 * @param pOrientation Orientation of the interface, cannot be null
		 * @param pComp Component that the interface is assigned to, cannot be
		 *            null
		 */
		Interface(int pID, PortOrientation pOrientation, NetworkComponent pComp) {
			if(pComp != null) {
				if(pOrientation != null) {
					component = pComp;
					orientation = pOrientation;
					ipConf = new ArrayList<>();
					virtLAN = new HashSet<>();

					StringBuilder sb = new StringBuilder(Interface.TYPE);
					sb.append("_");
					sb.append(pID);

					localName = sb.toString();

					TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.INTERFACE_ORIENTATION);

					if(log.isTraceEnabled()) {
						log.trace("Interface created: " + localName);
					}
				}
				else {
					throw new NullPointerException("NULL pointer for orientation provided");
				}
			}
			else {
				throw new NullPointerException("NULL pointer for component provided");
			}
		}


		/**
		 * Construct a new Interface object from RDF information. The
		 * orientation argument defines at which position the interface will be
		 * displayed on the editor grid.
		 *
		 * @param pOrientation Orientation of the interface, cannot be null
		 * @param pComp The NetworkComponent object this port is attached to,
		 *            cannot be null
		 * @param pLocName The RDF local name of the port, cannot be null
		 */
		Interface(PortOrientation pOrientation, NetworkComponent pComp, String pLocName) {
			if(pComp != null) {
				if(pOrientation != null) {
					component = pComp;
					orientation = pOrientation;
					ipConf = new ArrayList<>();
					virtLAN = new HashSet<>();

					// Check if the given local name is valid
					if(pLocName != null && !pLocName.isEmpty()) {
						localName = pLocName;
					}
					else {
						throw new IllegalArgumentException("Local name cannot be empty");
					}

					TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.INTERFACE_ORIENTATION);

					if(log.isTraceEnabled()) {
						log.trace("Interface created from RDF information: " + localName);
					}
				}
				else {
					throw new NullPointerException("NULL pointer for orientation provided");
				}
			}
			else {
				throw new NullPointerException("NULL pointer for component provided");
			}
		}


		/**
		 * Return the NetworkComponent this port is attached to
		 *
		 * @return The NetworkComponent this port is attached to
		 */
		public NetworkComponent getComponent() {
			return component;
		}


		/**
		 * Connect a NetworkCable to this port
		 *
		 * @param pCable The NetworkCable which will be connected to this port
		 * @param pEnd Indicate on which end of the cable the interface is
		 *            connected
		 */
		void connect(NetworkCable pCable, CableEnd pEnd) {
			if(pCable != null) {
				if(pEnd != null) {
					cable = pCable;
					cableEnd = pEnd;

					TEBackend.RDF_MANAGER.updateProperty(this, VISA.CONNECTED);
				}
				else {
					throw new NullPointerException("NULL pointer for cable end provided");
				}
			}
			else {
				throw new NullPointerException("NULL pointer for cable provided");
			}
		}


		/**
		 * Disconnect the NetworkCable from this port
		 */
		void disconnect() {
			cable = null;
			cableEnd = null;

			TEBackend.RDF_MANAGER.updateProperty(this, VISA.CONNECTED);
		}


		/**
		 * Return whether the interface is connected to another interface or
		 * not.
		 *
		 * @return true if connection exists, false otherwise
		 */
		public boolean isConnected() {
			return (cable != null);
		}


		/**
		 * Return the {@link NetworkCable NetworkCable} connected to this port
		 *
		 * @return The NetworkCable connected to this port, returns null if no
		 *         cable is connected
		 */
		public NetworkCable getCable() {
			return cable;
		}


		/**
		 * Return the end of the cable which is connected to this port
		 *
		 * @return The end of the cable which is connected to this port
		 */
		public CableEnd getCableEnd() {
			return cableEnd;
		}


		/**
		 * Add the specified IP address to the IP configurations set on this
		 * interface. The address must be within the address range of the
		 * provided network.
		 *
		 * @param pAddress A valid IPv4 or IPv6 address, must match the provided
		 *            version
		 * @param pNetwork IP network object
		 */
		@SuppressWarnings("ucd")
		public void configureIP(String pAddress, IPNetwork pNetwork) {
			IPAddress addr = pNetwork.addIPAddress(pAddress);

			configureIP(addr, pNetwork);
		}


		/**
		 * Add the specified IP address to the IP configurations set on this
		 * interface. The address must be within the address range of the
		 * provided network.
		 *
		 * @param pAddress A valid IPv4 or IPv6 address, must match the provided
		 *            version
		 * @param pNetwork IP network object
		 */
		public void configureIP(IPAddress pAddress, IPNetwork pNetwork) {
			ipConf.add(new NetworkComponent.Interface.IPConfig(pAddress, pNetwork));
			TEBackend.RDF_MANAGER.updateProperty(this, VISA.ADDRESS);
		}


		/**
		 * Return a list of all IPConfig objects present on this interface
		 *
		 * @return List of IPConfig objects
		 */
		public ArrayList<NetworkComponent.Interface.IPConfig> getAllIPConfig() {
			return ipConf;
		}


		/**
		 * Return a list of all IPConfig objects present on this interface that
		 * do not have a link local IP address.
		 *
		 * @return List of IPConfig objects
		 */
		public ArrayList<NetworkComponent.Interface.IPConfig> getNonLinkLocalIPConfig() {
			ArrayList<NetworkComponent.Interface.IPConfig> rv = new ArrayList<>();

			for(NetworkComponent.Interface.IPConfig conf : ipConf) {
				if(!conf.isLinkLocal) {
					rv.add(conf);
				}
			}

			return rv;
		}


		/**
		 * Remove the specified {@link IPConfig IPConfig} object from the IP
		 * configuration list.
		 *
		 * @param pIPConf IPConfig object to be removed
		 */
		public void removeIPConfiguration(IPConfig pIPConf) {
			ipConf.remove(pIPConf);
			pIPConf.getNetwork().removeIPAddress(pIPConf.ipAddress);

			TEBackend.RDF_MANAGER.updateProperty(this, VISA.ADDRESS);
		}


		/**
		 * Return whether this interface has at least one IP configuration or
		 * not.
		 *
		 * @return true if IP is configured, false otherwise
		 */
		public boolean isIPConfigured() {
			return (!ipConf.isEmpty());
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

					component.config.calcDragboxDimensions();

					TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.INTERFACE_ORIENTATION);

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


		/**
		 * Set a new set of VLANs for this interface. All existing VLAN
		 * assignments will be removed.
		 *
		 * @param pVLANs Set of VLANs to which this interface will be assigned,
		 *            cannot be null
		 */
		public void setVLAN(HashSet<VLAN> pVLANs) {
			if(pVLANs != null) {
				virtLAN = pVLANs;

				TEBackend.RDF_MANAGER.updateProperty(this, VISA.VLAN);
			}
			else {
				throw new NullPointerException("NULL pointer for VLAN provided");
			}
		}


		/**
		 * Return the VLAN to which this port is assigned
		 *
		 * @return The VLAN to which this port is assigned
		 */
		public HashSet<VLAN> getAllVLAN() {
			return virtLAN;
		}


		@Override
		public String getRDFLocalName() {
			return localName;
		}


		@Override
		public String getIdentifier() {
			return getRDFLocalName();
		}


		@Override
		public boolean isVirtual() {
			return false;
		}


		@Override
		public String getComponentGroup() {
			return component.config.getComponentGroup();
		}


		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject rv = new JSONObject();

			rv.put("identifier", getIdentifier());
			rv.put("component", component.getIdentifier());
			rv.put("isVirtual", false);

			if(cable != null) {
				rv.put("cable", cable.getIdentifier());
			}

			rv.put("orientation", orientation.toString());

			JSONObject vlanJSON = new JSONObject();
			int i = 0;
			for(VLAN vlan : virtLAN) {
				vlanJSON.put(String.valueOf(i), vlan.toJSON());
				i++;
			}
			rv.put("vlan", vlanJSON);

			JSONObject ipJSON = new JSONObject();
			for(int j = 0; i < ipConf.size(); i++) {
				ipJSON.put(String.valueOf(j), ipConf.get(j).toJSON());
			}
			rv.put("ipconf", ipJSON);

			return rv;
		}


		@Override
		public void removeFromTopology() {
			if(cable != null) {
				TEBackend.RDF_MANAGER.removeObject(this);
				TEBackend.TOPOLOGY_STORAGE.removeCable(cable.getIdentifier());

				component.getConfig().removeInterface(localName);
			}
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((component == null) ? 0 : component.hashCode());
			result = prime * result + ((localName == null) ? 0 : localName.hashCode());
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			else if(obj instanceof Interface) {
				Interface i = (Interface) obj;

				return (i.localName.equals(localName) && i.component.equals(component));
			}
			else {
				return false;
			}
		}


		/**
		 * This class stores IP configuration for an interface. A single
		 * interface can have multiple objects of this class. It is designed for
		 * both IPv4 and IPv6.
		 *
		 * @author Thomas Rix
		 */
		public class IPConfig implements IJSON {
			private IPAddress ipAddress;
			private IPNetwork ipNetwork;
			private boolean isLinkLocal;


			/**
			 * Construct a new object with the provided information. The pattern
			 * of the IP address must match the specified IP version and are
			 * <b>not</b> allowed to be abbreviated.<br>
			 * <br>
			 * The IPv4 RegEx pattern was found here, second example:<br>
			 * http://answers.oreilly.com/topic/318-how-to-match-ipv4-addresses-
			 * with
			 * -regular-expressions/<br>
			 * <br>
			 * The IPv6 RegEx pattern was found here:<br>
			 * http://forums.intermapper.com/viewtopic.php?p=2316&sid=7e196806
			 * a7cadda77a4891533eed8e33#2316
			 *
			 * @param pAddress IP address
			 * @param pNetwork Network IP address
			 */
			private IPConfig(String pAddress, IPNetwork pNetwork) {
				// Make sure the address is not an empty string
				if(!pAddress.isEmpty()) {
					// Check if IPv4 or IPv6 is used
					if(pNetwork.getVersion() == IPVersion.V4) {
						// Check if the IP address is valid
						Pattern ip4pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", Pattern.CASE_INSENSITIVE);
						Matcher addrMatcher = ip4pattern.matcher(pAddress);
						if(addrMatcher.matches()) {
							ipNetwork = pNetwork;
							ipAddress = pNetwork.addIPAddress(pAddress);

							// Check if this address is a link-local address
							Pattern linkLocalPattern = Pattern.compile("^169\\.254\\.(?:25[0-4]|2[0-4][0-9]|1[0-9][0-9]?|[1-9])\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", Pattern.CASE_INSENSITIVE);
							Matcher linkLocalMatcher = linkLocalPattern.matcher(pAddress);
							isLinkLocal = linkLocalMatcher.matches();
						}
						else {
							throw new IllegalArgumentException("Malformed IP or network address");
						}
					}
					else if(pNetwork.getVersion() == IPVersion.V6) {
						// Check if the IP address is valid
						Pattern ip6pattern = Pattern.compile("^(((?=(?>.*?::)(?!.*::)))(::)?([0-9A-F]{1,4}::?){0,5}|([0-9A-F]{1,4}:){6})(\\2([0-9A-F]{1,4}(::?|$)){0,2}|((25[0-5]|(2[0-4]|1\\d|[1-9])?\\d)(\\.|$)){4}|[0-9A-F]{1,4}:[0-9A-F]{1,4})(?<![^:]:|\\.)\\z", Pattern.CASE_INSENSITIVE);
						Matcher addrMatcher = ip6pattern.matcher(pAddress);
						if(addrMatcher.matches()) {
							ipNetwork = pNetwork;
							ipAddress = pNetwork.addIPAddress(pAddress);

							// Check if this address is a link-local address
							Pattern linkLocalPattern = Pattern.compile("^FE80(?::0000){3}(?::[0-9A-F]{4}){4}$", Pattern.CASE_INSENSITIVE);
							Matcher linkLocalMatcher = linkLocalPattern.matcher(pAddress);
							isLinkLocal = linkLocalMatcher.matches();
						}
						else {
							throw new IllegalArgumentException("Malformed IP or network address");
						}
					}
					else {
						throw new IllegalArgumentException("Invalid IP version provided");
					}
				}
				else {
					throw new IllegalArgumentException("Empty IP address provided");
				}
			}


			/**
			 * Construct a new object with the provided information. IP versions
			 * of address and network must match. A check if the provided
			 * address is already in use is <strong>not</strong> performed.
			 * Using an IP address for multiple interfaces will make the export
			 * into the IO-Tool impossible.
			 *
			 * @param pAddress IP address object
			 * @param pNetwork Network object
			 */
			private IPConfig(IPAddress pAddress, IPNetwork pNetwork) {
				if(pAddress.getVersion() == pNetwork.getVersion()) {
					if(pNetwork.isAddressInNetworkRange(pAddress)) {
						ipNetwork = pNetwork;
						ipAddress = pAddress;

						Pattern linkLocalPattern;
						Matcher linkLocalMatcher;

						if(pAddress.getVersion() == IPVersion.V4) {
							linkLocalPattern = Pattern.compile("^169\\.254\\.(?:25[0-4]|2[0-4][0-9]|1[0-9][0-9]?|[1-9])\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", Pattern.CASE_INSENSITIVE);
							linkLocalMatcher = linkLocalPattern.matcher(pAddress.getAddressString());
						}
						else if(pAddress.getVersion() == IPVersion.V6) {
							linkLocalPattern = Pattern.compile("^FE80(?::0000){3}(?::[0-9A-F]{4}){4}$", Pattern.CASE_INSENSITIVE);
							linkLocalMatcher = linkLocalPattern.matcher(pAddress.getAddressString());
						}
						else {
							throw new IllegalArgumentException("Invalid IP version provided");
						}

						isLinkLocal = linkLocalMatcher.matches();
					}
					else {
						throw new IllegalArgumentException("Provided IP address is not in address range of the provided network");
					}
				}
				else {
					throw new IllegalArgumentException("IP versions of network and address mismatch");
				}
			}


			/**
			 * Return the current IP address for this port
			 *
			 * @return The current IP address for this port
			 */
			public IPAddress getAddress() {
				return ipAddress;
			}


			/**
			 * Return the current IP subnet mask for this port
			 *
			 * @return The current IP subnet mask for this port
			 */
			public int getSubnet() {
				return ipNetwork.getSubnetMaskLength();
			}


			/**
			 * Return the used IP version for this port
			 *
			 * @return The used IP version for this port
			 */
			public IPVersion getVersion() {
				return ipNetwork.getVersion();
			}


			/**
			 * Return the network address to which the IP address belongs
			 *
			 * @return The network address to which the IP address belongs
			 */
			public IPNetwork getNetwork() {
				return ipNetwork;
			}


			/**
			 * Return whether the stored IP address is part of the link local
			 * range or not
			 *
			 * @return true if IP address is in link local range, false
			 *         otherwise
			 */
			public boolean isLinkLocal() {
				return isLinkLocal;
			}


			@Override
			public JSONObject toJSON() throws JSONException {
				JSONObject rv = new JSONObject();

				rv.put("address", ipAddress.getAddressString());
				rv.put("subnet", ipNetwork.getSubnetMaskLength());
				rv.put("version", ipNetwork.getVersion().toString());
				rv.put("network", ipNetwork.toJSON());
				rv.put("isLinkLocal", isLinkLocal);

				return rv;
			}


			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
				result = prime * result + ((ipNetwork == null) ? 0 : ipNetwork.hashCode());
				return result;
			}


			@Override
			public boolean equals(Object obj) {
				if(this == obj) {
					return true;
				}
				else if(obj instanceof IPConfig) {
					IPConfig ipc = (IPConfig) obj;

					return (ipc.ipAddress.equals(ipAddress) && ipc.ipNetwork.equals(ipNetwork));
				}
				else {
					return false;
				}
			}
		}
	}
}
