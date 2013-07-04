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

package de.decoit.visa.rdf;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;


/**
 * Vocabulary definition of the VISABackup namespace, which is used to store
 * additional information about topology elements in the RDF dataset. The
 * information stored by using this namespace is not sent to the IO-Tool.
 *
 * @author Thomas Rix
 */
public class VISABackup {
	/**
	 * VISABackup namespace definition
	 */
	private static final String URI = "http://visa-project.de/backup/";


	/**
	 * Returns the URI for this schema
	 *
	 * @return the URI for this schema
	 */
	static String getURI() {
		return VISABackup.URI;
	}


	/**
	 * Return the URI for the VISABackend model:<br>
	 * http://visa-project.de/backup/model
	 *
	 * @return The URI for the VISABackend model
	 */
	static String createModelURI() {
		StringBuilder sb = new StringBuilder(VISABackup.URI);
		sb.append("model");

		return sb.toString();
	}

	/**
	 * Internal model used to create the Properties an Literals included in this
	 * class
	 */
	private static Model mod = ModelFactory.createDefaultModel();

	/**
	 * 'compLocX' property, used with a Device resource as subject and a
	 * positive integer (including 0) literal as object. Indicates the
	 * horizontal grid position of the device.
	 */
	@SuppressWarnings("ucd")
	public static final Property COMP_LOC_X = VISABackup.mod.createProperty(VISABackup.URI, "compLocX");

	/**
	 * 'compLocY' property, used with a Device resource as subject and a
	 * positive integer (including 0) literal as object. Indicates the vertical
	 * grid position of the device.
	 */
	@SuppressWarnings("ucd")
	public static final Property COMP_LOC_Y = VISABackup.mod.createProperty(VISABackup.URI, "compLocY");

	/**
	 * 'compDimX' property, used with a Device resource as subject and a
	 * positive integer literal as object. Indicates the horizontal size of the
	 * device.
	 */
	@SuppressWarnings("ucd")
	public static final Property COMP_DIM_X = VISABackup.mod.createProperty(VISABackup.URI, "compDimX");

	/**
	 * 'compDimY' property, used with a Device resource as subject and a
	 * positive integer literal as object. Indicates the vertical size of the
	 * device.
	 */
	@SuppressWarnings("ucd")
	public static final Property COMP_DIM_Y = VISABackup.mod.createProperty(VISABackup.URI, "compDimY");

	/**
	 * 'group' property, used with Device resource as subject and a GROUP
	 * resource as object.
	 */
	@SuppressWarnings("ucd")
	public static final Property GROUP = VISABackup.mod.createProperty(VISABackup.URI, "group");

	/**
	 * 'name' property, used with a VLAN or GROUP resource as subject and a
	 * string literal as object
	 */
	@SuppressWarnings("ucd")
	public static final Property NAME = VISABackup.mod.createProperty(VISABackup.URI, "name");

	/**
	 * 'orientation' property, used with an Interface resource as subject and a
	 * string literal as object
	 */
	@SuppressWarnings("ucd")
	public static final Property INTERFACE_ORIENTATION = VISABackup.mod.createProperty(VISABackup.URI, "orientation");

	/**
	 * 'color' property, used with a VLAN resource as subject and a string
	 * literal as object
	 */
	@SuppressWarnings("ucd")
	public static final Property VLAN_COLOR = VISABackup.mod.createProperty(VISABackup.URI, "color");
}
