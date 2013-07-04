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
	 * The modification will alter the host name of a component.
	 */
	COMPONENT_NAME;
}
