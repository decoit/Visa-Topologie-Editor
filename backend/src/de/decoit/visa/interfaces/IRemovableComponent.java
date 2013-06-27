package de.decoit.visa.interfaces;

/**
 * This interface has to be included by all components and cables in the
 * topology. Components are all classes that extend the {@link de.decoit.visa.topology.NetworkComponent NetworkComponent}
 * class. It makes sure that no references to removed elements are stored in
 * interfaces or cables. It is required for cables since those can be removed
 * without removing the connected components.
 *
 * @author Thomas Rix
 */
public interface IRemovableComponent {
	/**
	 * This method is called when a component is removed from the
	 * topology. It does cleanup jobs to prevent references to objects
	 * that are no longer part of the topology.
	 */
	public void removeFromTopology();
}
