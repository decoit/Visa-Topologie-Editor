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

import org.json.JSONException;
import org.json.JSONObject;


/**
 * This interface forces the implementing classes to provide a method which
 * returns a JSONObject representation of their stored information. Which
 * information is included in the JSON object and how it is structured is
 * defined by the classes implementing this interface.
 *
 * @author Thomas Rix
 */
public interface IJSON {
	/**
	 * Return a JSONObject representation of the data stored in this object.
	 *
	 * @return JSONObject representation of the data stored in this object
	 * @throws JSONException if appending information to the JSONObject failed
	 */
	public JSONObject toJSON() throws JSONException;
}
