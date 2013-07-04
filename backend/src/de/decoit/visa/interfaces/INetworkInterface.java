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
