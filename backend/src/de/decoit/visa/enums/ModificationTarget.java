package de.decoit.visa.enums;

/**
 * Defines the type of the target of a queued modification.
 *
 * @author Thomas Rix
 */
public enum ModificationTarget {
	/**
	 * Target element is an object of one of the subclasses of
	 * {@link de.decoit.visa.topology.NetworkComponent NetworkComponent}.
	 */
	COMPONENT,

	/**
	 * Target element is an object of
	 * {@link de.decoit.visa.topology.NetworkComponent.Interface Interface}.
	 */
	INTERFACE;
}
