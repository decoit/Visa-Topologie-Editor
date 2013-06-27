package de.decoit.visa.topology;

import java.util.LinkedList;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.Position2D;
import de.decoit.visa.TEBackend;
import de.decoit.visa.enums.CableEnd;
import de.decoit.visa.interfaces.IRemovableComponent;
import de.decoit.visa.interfaces.ITopologyElement;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup.GroupInterface;


/**
 * This class represents a connection between two interfaces in the topology.
 * This connection can be of any type, including cables, WLAN and VPN tunnels.
 *
 * @author Thomas Rix
 */
public class NetworkCable implements ITopologyElement, IRemovableComponent {
	private static Logger log = Logger.getLogger(NetworkCable.class.getName());

	/**
	 * Type string used to build the local name for new components
	 */
	static final String TYPE = "ncable";

	private String identifier;
	private NetworkComponent.Interface left;
	private NetworkComponent.Interface right;
	private GroupInterface groupGateway;
	private String groupName;
	private LinkedList<Position2D> pathCorners;


	/**
	 * Construct a new NetworkCable object that connects the specified
	 * interfaces. It can use an optional
	 * {@link de.decoit.visa.topology.TopologyStorage.ComponentGroup.GroupInterface
	 * GroupInterface} as a gateway if one of the connected interfaces is
	 * located in the global
	 * {@link de.decoit.visa.topology.TopologyStorage.ComponentGroup
	 * ComponentGroup} an the other in a different group. The only difference
	 * between left and right interface is that the notation 'left is connected
	 * to right' is used.
	 *
	 * @param pID ID number
	 * @param pLeft Left interface to connect
	 * @param pRight Right interface to connect
	 * @param pGroupGW Optional group gateway, can be set to null if not required
	 */
	NetworkCable(int pID, NetworkComponent.Interface pLeft, NetworkComponent.Interface pRight, GroupInterface pGroupGW) {
		if(pLeft != null && pRight != null) {
			StringBuilder sb = new StringBuilder(NetworkCable.TYPE);
			sb.append("_");
			sb.append(pID);
			identifier = sb.toString();

			left = pLeft;
			left.connect(this, CableEnd.LEFT);

			right = pRight;
			right.connect(this, CableEnd.RIGHT);

			groupGateway = pGroupGW;

			if(groupGateway != null) {
				groupName = "0.0.0.0";
			}
			else {
				if(pLeft.getComponentGroup().equals(pRight.getComponentGroup())) {
					groupName = pLeft.getComponentGroup();
				}
				else if(pLeft.getComponentGroup().equals("0.0.0.0")) {
					groupName = pRight.getComponentGroup();
				}
				else {
					groupName = pLeft.getComponentGroup();
				}
			}

			pathCorners = new LinkedList<>();

			if(log.isTraceEnabled()) {
				log.trace("NetworkCable created");
			}
		}
		else {
			throw new NullPointerException("Null pointer for left and/or right interface provided");
		}
	}


	// Inherited from ITopologyElement
	@Override
	public String getIdentifier() {
		return identifier;
	}


	/**
	 * Return the NetworkComponent.Interface to which the left end of the cable
	 * is connected
	 *
	 * @return The NetworkComponent.Interface to which the left end of the cable
	 *         is connected, null if not connected
	 */
	public NetworkComponent.Interface getLeft() {
		return left;
	}


	/**
	 * Return the NetworkComponent.Interface to which the right end of the cable
	 * is connected
	 *
	 * @return The NetworkComponent.Interface to which the right end of the
	 *         cable is connected, null if not connected
	 */
	public NetworkComponent.Interface getRight() {
		return right;
	}


	/**
	 * Return the gateway interface this cable uses to enter a group
	 *
	 * @return The gateway
	 */
	public GroupInterface getGroupGateway() {
		return groupGateway;
	}


	/**
	 * Set the corners of the cable path. The list must have at least 1 element,
	 * otherwise an InvalidArgumentException will be caused.
	 *
	 * @param pCorners A list of the path corners of the cable, cannot be null
	 *            or empty
	 */
	public void setPathCorners(LinkedList<Position2D> pCorners) {
		if(pCorners != null && !pCorners.isEmpty()) {
			pathCorners = pCorners;
		}
		else {
			throw new IllegalArgumentException("Empty list or null provided");
		}
	}


	/**
	 * Return the currently stored path corners
	 *
	 * @return The currently stored path corners
	 */
	public LinkedList<Position2D> getPathCorners() {
		return pathCorners;
	}


	/**
	 * Set the group this cable will be displayed in
	 *
	 * @param pGroup Name of the group
	 */
	public void setGroupName(String pGroup) {
		if(!pGroup.isEmpty()) {
			TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(groupName).removeCable(this);

			groupName = pGroup;

			TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(groupName).addCable(this);
		}
		else {
			throw new IllegalArgumentException("Empty group name provided");
		}
	}


	/**
	 * Get the name of the group this cable will be displayed in
	 *
	 * @return Name of the group
	 */
	public String getGroupName() {
		return groupName;
	}


	// Inherited from IRemovableComponent
	@Override
	public void removeFromTopology() {
		TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(groupName).removeCable(this);

		if(groupGateway != null) {
			groupGateway.removeFromGroup();
			groupGateway = null;
		}

		if(left != null) {
			TEBackend.RDF_MANAGER.removeConnectedProperty(left);

			left.disconnect();
			left = null;
		}
		if(right != null) {
			right.disconnect();
			right = null;
		}
	}


	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject rv = new JSONObject();

		rv.put("identifier", getIdentifier());
		rv.put("left", left.getIdentifier());
		rv.put("right", right.getIdentifier());
		if(groupGateway != null) {
			rv.put("via", groupGateway.getRDFLocalName());
		}

		JSONObject pathJSON = new JSONObject();
		int i = 0;
		for(Position2D pos : pathCorners) {
			pathJSON.put(String.valueOf(i), pos.toJSON());
		}

		rv.put("path", pathJSON);

		return rv;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		else if(obj instanceof NetworkCable) {
			NetworkCable nc = (NetworkCable) obj;

			return (nc.identifier.equals(identifier) && nc.left.equals(left) && nc.right.equals(right));
		}
		else {
			return false;
		}
	}
}