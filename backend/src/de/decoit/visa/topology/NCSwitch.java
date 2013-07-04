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
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.Dimension2D;
import de.decoit.visa.Position2D;
import de.decoit.visa.TEBackend;
import de.decoit.visa.enums.PortOrientation;
import de.decoit.visa.interfaces.IJSON;
import de.decoit.visa.interfaces.INetworkInterface;
import de.decoit.visa.interfaces.IRDFObject;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup.GroupInterface;


/**
 * This class represents a switch in the topology. It provides utility methods
 * and nested classes which are only applicable for switches. The nested classes
 * do <b>not</b> represent real object in the topology, they are used by the
 * frontend for display purposes.
 *
 * @author Thomas Rix
 */
public class NCSwitch extends NetworkComponent {
	/**
	 * Type string used to build the local name for new components
	 */
	public static final String TYPE = "switch";
	private static int nextVIfID = 1;

	private HashMap<String, GroupSwitch> groupSwitches;
	private HashMap<String, VirtualInterface> virtualInterfaces;
	private HashMap<String, Interface> realInterfaces;


	/**
	 * Construct a new NCSwitch object from information received from the
	 * frontend.
	 *
	 * @param pID ID number
	 * @param pPortInfo Interface configuration
	 * @param pName Human readable name
	 * @param pCompDimensions Component dimensions
	 * @param pGridLoc Position of the component on the editor grid
	 */
	NCSwitch(int pID, ArrayList<String> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc) {
		super(pID, NCSwitch.TYPE, pName, pPortInfo, pCompDimensions, pGridLoc);

		groupSwitches = new HashMap<>();
		virtualInterfaces = new HashMap<>();
		realInterfaces = new HashMap<>();

		if(log.isTraceEnabled()) {
			log.trace("NCSwitch created");
		}
	}


	/**
	 * Construct a new NCSwitch object from RDF information.
	 *
	 * @param pID ID number
	 * @param pPortInfo Interface configuration
	 * @param pName Human readable name
	 * @param pCompDimensions Component dimensions
	 * @param pGridLoc Position of the component on the editor grid
	 * @param pLocName RDF local name
	 */
	NCSwitch(int pID, ArrayList<HashMap<String, String>> pPortInfo, String pName, Dimension2D pCompDimensions, Position2D pGridLoc, String pLocName) {
		super(pID, pName, pPortInfo, pCompDimensions, pGridLoc, pLocName);

		groupSwitches = new HashMap<>();
		virtualInterfaces = new HashMap<>();
		realInterfaces = new HashMap<>();

		if(log.isTraceEnabled()) {
			log.trace("NCSwitch created from RDF information");
		}
	}


	/**
	 * Add an interface to the list of real interfaces on this switch
	 *
	 * @param pIface The real interface
	 */
	protected void addRealInterface(Interface pIface) {
		realInterfaces.put(pIface.getRDFLocalName(), pIface);
	}


	/**
	 * Automatically create group switches for all groups connected to this
	 * switch
	 */
	public void createGroupSwitches() {
		// Clear existing group switches and virtual interfaces
		groupSwitches.clear();
		virtualInterfaces.clear();

		// Iterate over all interfaces
		for(Map.Entry<String, Interface> ifEntry : config.ports.entrySet()) {
			// Only care about connected interfaces
			if(config.getComponentGroup().equals("0.0.0.0") && ifEntry.getValue().isConnected()) {
				// Get the component group of the connected component
				String group = null;
				switch(ifEntry.getValue().getCableEnd()) {
					case LEFT:
						group = ifEntry.getValue().getCable().getRight().getComponentGroup();
						break;
					case RIGHT:
						group = ifEntry.getValue().getCable().getLeft().getComponentGroup();
						break;
				}

				// Is the interface connected to another group than 0.0.0.0?
				if(!group.equals("0.0.0.0")) {
					GroupSwitch gs;

					// If no switch for that group exists, create one. Otherwise
					// use the existing group switch
					if(!groupSwitches.containsKey(group)) {
						VirtualInterface vIf = new VirtualInterface(config.getComponentGroup());
						virtualInterfaces.put(vIf.localName, vIf);

						gs = new GroupSwitch(group, vIf);
						groupSwitches.put(group, gs);
					}
					else {
						gs = groupSwitches.get(group);
					}

					// Add the real interface to the group switch
					gs.addInterface(ifEntry.getValue());
				}
				else {
					// Mark the interface as a "real" interface that is located
					// at the switch
					realInterfaces.put(ifEntry.getKey(), ifEntry.getValue());
				}
			}
			else {
				// Mark the interface as a "real" interface that is located at
				// the switch
				realInterfaces.put(ifEntry.getKey(), ifEntry.getValue());
			}
		}
	}


	/**
	 * Get the group switch for the specified group
	 *
	 * @param pGroup Group which the switch is located in
	 * @return The group switch object
	 */
	public GroupSwitch getGroupSwitch(String pGroup) {
		if(!pGroup.isEmpty()) {
			return groupSwitches.get(pGroup);
		}
		else {
			throw new IllegalArgumentException("Empty group name provided");
		}
	}


	// Inherited from IRemovableComponent
	@Override
	public void removeFromTopology() {
		for(Map.Entry<String, GroupSwitch> switchEntry : groupSwitches.entrySet()) {
			switchEntry.getValue().removeFromGroup();
			groupSwitches.remove(switchEntry.getKey());
		}

		super.removeFromTopology();
	}


	@Override
	public JSONObject toJSON() throws JSONException {
		// Get the JSONObject of the NetworkComponent
		JSONObject rv = super.toJSON();

		// Add the virtual interfaces to the JSONObject
		JSONObject vIfJSON = new JSONObject();
		for(Map.Entry<String, VirtualInterface> vIfEntry : virtualInterfaces.entrySet()) {
			vIfJSON.put(vIfEntry.getKey(), vIfEntry.getValue().toJSON());
		}
		rv.put("virtIf", vIfJSON);

		// Add the real interfaces to the JSONObject
		JSONObject rIfJSON = new JSONObject();
		for(Map.Entry<String, Interface> rIfEntry : realInterfaces.entrySet()) {
			rIfJSON.put(rIfEntry.getKey(), rIfEntry.getValue().toJSON());
		}
		rv.put("realIf", rIfJSON);

		// Add the group switches to the JSONObject
		JSONObject gsJSON = new JSONObject();
		for(Map.Entry<String, GroupSwitch> gsEntry : groupSwitches.entrySet()) {
			gsJSON.put(gsEntry.getKey(), gsEntry.getValue().toJSON());
		}
		rv.put("groupSwitches", gsJSON);

		return rv;
	}


	/**
	 * A virtual interface to connect a real switch (NCSwitch) with a virtual
	 * GroupSwitch. Each of both has an object of this class which are connected
	 * by a GroupInterface acting as gateway through the group border.
	 *
	 * @author Thomas Rix
	 */
	public class VirtualInterface implements INetworkInterface, IJSON, IRDFObject {
		private GroupInterface connectedTo;
		private String localName;
		private PortOrientation orientation;
		private String groupName;


		/**
		 * Construct a new virtual interface. It is not connected at time of
		 * creation.
		 *
		 * @param pGroup Name of the component group this interface is located in
		 */
		private VirtualInterface(String pGroup) {
			StringBuilder sbLocName = new StringBuilder("vIf_");
			sbLocName.append(nextVIfID);
			sbLocName.append("_");
			sbLocName.append(getOuterType().localName);
			localName = sbLocName.toString();

			if(!pGroup.isEmpty()) {
				groupName = pGroup;
			}
			else {
				throw new IllegalArgumentException("Empty group name provided");
			}

			nextVIfID++;

			connectedTo = null;

			orientation = PortOrientation.TOP;
		}


		/**
		 * Connect this virtual interface to a group interface. This cannot be
		 * undone.
		 *
		 * @param pGI Reference to the group interface, cannot be null
		 */
		public void setConnectedTo(GroupInterface pGI) {
			if(pGI != null) {
				connectedTo = pGI;
			}
			else {
				throw new NullPointerException("NULL pointer for group interface");
			}
		}


		/**
		 * Get the group interface this virtual interface is connected to.
		 *
		 * @return The group interface this virtual interface is connected to
		 */
		public GroupInterface getConnectedTo() {
			return connectedTo;
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

					getOuterType().config.calcDragboxDimensions();

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
		 * Return the local name of the NCSwitch object that contains this
		 * virtual interface
		 *
		 * @return RDF local name of the containing switch
		 */
		public String getSwitchRDFLocalName() {
			return getOuterType().localName;
		}


		@Override
		public boolean isVirtual() {
			return true;
		}


		@Override
		public String getComponentGroup() {
			return groupName;
		}


		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject rv = new JSONObject();

			rv.put("identifier", localName);
			rv.put("orientation", orientation.toString());
			rv.put("isVirtual", true);

			if(connectedTo != null) {
				rv.put("connectedTo", connectedTo.getRDFLocalName());
			}

			return rv;
		}


		@Override
		public String getRDFLocalName() {
			return localName;
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			// result = prime * result + getOuterType().hashCode();
			result = prime * result + ((localName == null) ? 0 : localName.hashCode());
			result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			else if(obj instanceof VirtualInterface) {
				VirtualInterface vi = (VirtualInterface) obj;

				return (vi.localName.equals(localName) && vi.groupName.equals(groupName));
			}
			else {
				return false;
			}
		}


		/**
		 * Return the enclosing NCSwitch object
		 *
		 * @return The enclosing NCSwitch object
		 */
		private NCSwitch getOuterType() {
			return NCSwitch.this;
		}
	}


	/**
	 * A virtual switch that connects the components in a group to a switch that
	 * is located outside the group. It is connected to the real switch by a
	 * virtual interface and has references to the real interfaces to allow
	 * direct connection between components and the real switch. This virtual
	 * switch is not part of the actual topology and does not show up in the RDF
	 * model that is exchanged with the IO Tool.
	 *
	 * @author Thomas Rix
	 */
	public class GroupSwitch implements IJSON, IRDFObject {
		private HashSet<Interface> interfaces;
		private VirtualInterface virtInterface;
		private String localName;
		private String groupName;
		private Dimension2D componentDimensions;
		private Dimension2D dragboxDimensions;
		private Position2D componentInterfaceOffset;
		private Position2D gridLocation;


		/**
		 * Construct a new virtual switch. It will be located inside the
		 * specified group and connected to the specified virtual interface of
		 * the real switch.
		 *
		 * @param pGroup Name of the group this virtual switch will located in
		 * @param pSwitchVI Virtual interface of the real switch which will be
		 *            connected to this virtual switch, must be a member of the
		 *            same object that this group switch belongs to
		 */
		private GroupSwitch(String pGroup, VirtualInterface pSwitchVI) {
			interfaces = new HashSet<>();
			virtInterface = new VirtualInterface(pGroup);
			componentDimensions = null;
			dragboxDimensions = null;
			componentInterfaceOffset = null;
			gridLocation = null;

			ComponentGroup cg = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(pGroup);

			if(getOuterType().equals(pSwitchVI.getOuterType())) {
				GroupInterface gi = cg.createOuterConnection(virtInterface, pSwitchVI);

				virtInterface.setConnectedTo(gi);
				pSwitchVI.setConnectedTo(gi);
			}
			else {
				throw new IllegalArgumentException("The specified virtual interface is not part of the enclosing switch object");
			}

			StringBuilder sb = new StringBuilder("gSwitch_");
			sb.append(cg.getIdentifier());
			sb.append("_");
			sb.append(getOuterType().localName);

			localName = sb.toString();

			groupName = pGroup;

			calcDragboxDimensions();

			TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(pGroup).addGroupSwitch(this);
		}


		/**
		 * Return the virtual interface of this virtual switch
		 *
		 * @return The virtual interface of this virtual switch
		 */
		public VirtualInterface getVirtualInterface() {
			return virtInterface;
		}


		/**
		 * Set a new grid location for this group switch.
		 *
		 * @param pX Horizontal position
		 * @param pY vertical position
		 */
		public void setGridLocation(int pX, int pY) {
			gridLocation = new Position2D(pX, pY, TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(groupName).getSubgridDimensions());

			if(log.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder("Component relocated to x=");
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
		 * Return the dimensions of the dragbox surrounding this component
		 *
		 * @return The dimensions of the dragbox
		 */
		public Dimension2D getDragboxDimensions() {
			return dragboxDimensions;
		}


		/**
		 * Return the local name of the NCSwitch object that contains this
		 * group switch.
		 *
		 * @return RDF local name of the containing switch
		 */
		public String getSwitchRDFLocalName() {
			return getOuterType().localName;
		}


		/**
		 * Return the name of the group this group switch is assigned to.
		 *
		 * @return The name of the group
		 */
		public String getGroupName() {
			return groupName;
		}


		/**
		 * Remove this group switch from the group it is assigned to and remove
		 * the virtual interface of the switch that connects the switch and the
		 * group switch.
		 */
		void removeFromGroup() {
			for(Interface iface : interfaces) {
				getOuterType().realInterfaces.put(iface.getRDFLocalName(), iface);
			}

			TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(groupName).removeGroupSwitch(this);

			getOuterType().virtualInterfaces.remove(virtInterface.connectedTo.getOuterConnection());
		}


		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject rv = new JSONObject();

			rv.put("identifier", localName);
			rv.put("isSwitch", true);
			rv.put("isGroupSwitch", true);

			StringBuilder sb = new StringBuilder("Group switch for '");
			sb.append(groupName);
			sb.append("' of '");
			sb.append(getOuterType().name);
			sb.append("'");
			rv.put("name", sb.toString());

			JSONObject confJSON = new JSONObject();
			confJSON.put("compOffset", componentInterfaceOffset.toJSON());
			confJSON.put("compDim", componentDimensions.toJSON());
			confJSON.put("dragDim", dragboxDimensions.toJSON());
			confJSON.put("gloc", gridLocation.toJSON());

			// Add the virtual interface to the JSONObject
			JSONObject vIfJSON = new JSONObject();
			vIfJSON.put(virtInterface.getRDFLocalName(), virtInterface.toJSON());
			rv.put("virtIf", vIfJSON);

			// Add the real interfaces to the JSONObject
			JSONObject rIfJSON = new JSONObject();
			for(Interface rIfEntry : interfaces) {
				rIfJSON.put(rIfEntry.getIdentifier(), rIfEntry.toJSON());
			}
			rv.put("realIf", rIfJSON);

			rv.put("config", confJSON);

			return rv;
		}


		@Override
		public String getRDFLocalName() {
			return localName;
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			// result = prime * result + getOuterType().hashCode();
			result = prime * result + ((localName == null) ? 0 : localName.hashCode());
			result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
			// result = prime * result + ((virtInterface == null) ? 0 :
			// virtInterface.hashCode());
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			else if(obj instanceof GroupSwitch) {
				GroupSwitch gs = (GroupSwitch) obj;

				return (gs.localName.equals(localName) && gs.groupName.equals(groupName));
			}
			else {
				return false;
			}
		}


		/**
		 * Add an interface of the real switch to this virtual switch.
		 *
		 * @param pIf Interface of the real switch, must be an interface of the
		 *            enclosing switch object
		 * @return true if the interface was added successfully, false otherwise
		 */
		private boolean addInterface(Interface pIf) {
			if(pIf != null) {
				if(getOuterType().config.getPorts().containsValue(pIf)) {
					boolean rv = interfaces.add(pIf);
					calcDragboxDimensions();

					return rv;
				}
				else {
					throw new IllegalArgumentException("Provided interface is not part of the enclosing switch");
				}
			}
			else {
				throw new NullPointerException("NULL pointer for interface provided");
			}
		}


		/**
		 * Calculate the size of the dragbox which surrounds this component. The
		 * calculation takes into account the component dimensions, globally set
		 * the component margin, the interface orientations and some extra space
		 * to prevent multiple components from forming impassable walls.
		 */
		private void calcDragboxDimensions() {
			PortOrientation ori = null;

			ori = PortOrientation.TOP;
			virtInterface.setOrientation(PortOrientation.BOTTOM);
			componentDimensions = new Dimension2D(interfaces.size(), 3);

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
			for(Interface iface : interfaces) {
				iface.setOrientation(ori);

				switch(ori) {
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


		/**
		 * Return the enclosing NCSwitch object
		 *
		 * @return The enclosing NCSwitch object
		 */
		private NCSwitch getOuterType() {
			return NCSwitch.this;
		}
	}
}
