package de.decoit.visa.interfaces;

/**
 * Any topolgy element that will be stored in the {@link de.decoit.visa.topology.TopologyStorage TopologyStorage} must
 * implement this interface. It will make sure those classes implement a method
 * to retrieve a unique identifier for the object.
 *
 * @author Thomas Rix
 */
public interface ITopologyElement extends IJSON {
	/**
	 * Retrieve a string that can be used as a unique identifier to this object.
	 *
	 * @return The identifier of this element
	 */
	public String getIdentifier();
}
