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
