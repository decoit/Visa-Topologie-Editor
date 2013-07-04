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
 * This interface is required for all objects that are stored in the RDF data
 * provided by the IO-Tool from Fraunhofer SIT. It declares methods for
 * retrieving RDF specific information like the local name.
 *
 * @author Thomas Rix
 */
public interface IRDFObject {
	/**
	 * Return the local name of the RDF resource. The local name is the part of
	 * the URI that does not belong to the namespace URI.
	 *
	 * @return The local name of the RDF resource
	 */
	public String getRDFLocalName();
}
