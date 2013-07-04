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
