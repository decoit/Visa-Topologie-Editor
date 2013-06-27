package de.decoit.visa.interfaces;

/**
 * Any element that represents an interface must implement this interface. This
 * can be real interfaces of components or virtual helper interfaces which do
 * not represent an object in the real topology.
 *
 * @author Thomas Rix
 */
public interface INetworkInterface extends IJSON {
	/**
	 * Return whether this object is a virtual helper interface or represents a
	 * real component interface. Virtual interface should return true while real
	 * interfaces should return false.
	 *
	 * @return true: This is a virtual helper interface; false: This is a real
	 *         component interface
	 */
	public boolean isVirtual();


	/**
	 * Return the group name of the {@link de.decoit.visa.topology.TopologyStorage.ComponentGroup ComponentGroup} this interface is
	 * located in.
	 *
	 * @return The group name
	 */
	public String getComponentGroup();
}
