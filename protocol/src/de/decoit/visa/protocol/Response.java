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

package de.decoit.visa.protocol;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;


/**
 * This class is used by the IO-Tool for a response to a {@link Request} from
 * the Topology-Editor. It contains a return code, a map with data included in
 * the response and an optional message. Data keys, values and the message are
 * stored Base64 encoded to prevent issues with the YAML parser. A Response
 * object is immutable, once created it cannot be altered.
 *
 * @author Thomas Rix
 */
public class Response {
	private int returnCode;
	private String message;
	private HashMap<String, String> data;


	/**
	 * <strong>DO NOT USE!</strong><br>
	 * Constructs a new empty Response object. This is used by SnakeYAML to
	 * construct an object from YAML source. Using this constructor for any
	 * other purpose will result in an unusable Response object.
	 */
	public Response() {
		/* Empty, attributes are set by SnakeYAML */
	}


	/**
	 * Construct a new Response object using the specified return code, data and
	 * message. Message can be set to null if no message is required, it will
	 * translated into an empty String internally.
	 *
	 * @param pCode Return code of this Response
	 * @param pData Map containing the data included in this Response
	 * @param pMsg Optional response message, can be null if not required
	 */
	public Response(int pCode, Map<String, String> pData, String pMsg) {
		returnCode = pCode;

		data = new HashMap<>();

		for(Map.Entry<String, String> s : pData.entrySet()) {
			String key = Base64.encodeBase64String(s.getKey().getBytes());
			String value = Base64.encodeBase64String(s.getValue().getBytes());
			data.put(key, value);
		}

		if(pMsg != null) {
			message = Base64.encodeBase64String(pMsg.getBytes());
		}
		else {
			message = "";
		}
	}


	/**
	 * Get the return code of this Request
	 *
	 * @return The return code
	 */
	public int getReturnCode() {
		return returnCode;
	}


	/**
	 * Get the message of this Response, returns an empty String if no message
	 * was set. Message text is decoded for it is returned.
	 *
	 * @return The message text
	 */
	public String getMessage() {
		byte[] strBytes = Base64.decodeBase64(message.getBytes());
		return new String(strBytes);
	}


	/**
	 * Get the data map of this Response object. Data keys and values are
	 * decoded before they are returned.
	 *
	 * @return Data map with decoded key and values
	 */
	public HashMap<String, String> getData() {
		HashMap<String, String> rv = new HashMap<>();

		for(Map.Entry<String, String> s : data.entrySet()) {
			byte[] keyStrBytes = Base64.decodeBase64(s.getKey().getBytes());
			byte[] valStrBytes = Base64.decodeBase64(s.getValue().getBytes());
			rv.put(new String(keyStrBytes), new String(valStrBytes));
		}

		return rv;
	}
}
