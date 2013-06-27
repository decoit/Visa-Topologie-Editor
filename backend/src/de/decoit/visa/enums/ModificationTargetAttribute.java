package de.decoit.visa.enums;

/**
 * Defines the targeted attribute of a modification target.
 *
 * @author Thomas Rix
 */
public enum ModificationTargetAttribute {
	/**
	 * The modification will alter the subnet an interface is member of.
	 */
	INTERFACE_NETWORK,

	/**
	 * The modification will alter the orientation of an interface.
	 */
	INTERFACE_ORIENTATION,

	/**
	 * The modification will alter the hostname of a component.
	 */
	COMPONENT_NAME;
}
