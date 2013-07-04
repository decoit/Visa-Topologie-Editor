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
 * This class is used the Topology-Editor to send a command to the IO-Tool. A
 * request contains a command from the {@link Commands} enumeration and an argument
 * map. Which arguments are allowed for a specific command is specified in the
 * JavaDoc of the {@link Commands} enumeration. Command string, argument keys and
 * values are stored Base64 encoded to prevent issues with the YAML parser. A
 * Request object is immutable, once created it cannot be altered.
 *
 * @author Thomas Rix
 */
public class Request {
	private String command;
	private HashMap<String, String> arguments;


	/**
	 * <strong>DO NOT USE!</strong><br>
	 * Constructs a new empty Request object. This is used by SnakeYAML to
	 * construct an object from YAML source. Using this constructor for any
	 * other purpose will result in an unusable Request object.
	 */
	public Request() {
		/* Empty, attributes are set by SnakeYAML */
	}


	/**
	 * Construct a new Request object using the specified command and arguments.
	 * The command is converted to String. The argument map will only be
	 * accepted if its keyset matches the keyset of an empty map requested from
	 * {@link Commands#getEmptyArgumentMap Command.getEmptyArgumentMap()}.
	 *
	 * @param pCommand Command to send
	 * @param pArguments Arguments for the command
	 * @see Commands
	 */
	public Request(Commands pCommand, Map<String, String> pArguments) {
		// Check if the provided argument map contains all required keys (and
		// only these keys)
		if(pArguments.keySet().equals(Commands.getEmptyArgumentMap(pCommand).keySet())) {
			command = Base64.encodeBase64String(pCommand.toString().getBytes());

			arguments = new HashMap<>();

			for(Map.Entry<String, String> s : pArguments.entrySet()) {
				String key = Base64.encodeBase64String(s.getKey().getBytes());
				String value = Base64.encodeBase64String(s.getValue().getBytes());
				arguments.put(key, value);
			}
		}
		else {
			throw new IllegalArgumentException("Invalid argument map provided");
		}
	}


	/**
	 * Get the Commands value of this Request.
	 *
	 * @return Command of this request
	 * @see Commands
	 */
	public Commands getCommand() {
		byte[] strBytes = Base64.decodeBase64(command.getBytes());
		return Commands.getCommand(new String(strBytes));
	}


	/**
	 * Get the command of this Request as String. The Sting is decoded before it
	 * is returned.
	 *
	 * @return String representation of the command
	 */
	public String getCommandAsString() {
		byte[] strBytes = Base64.decodeBase64(command.getBytes());
		return new String(strBytes);
	}


	/**
	 * Get the argument map of this Request object. Argument keys and values are
	 * decoded before they are returned.
	 *
	 * @return Argument map with decoded keys and values
	 */
	public HashMap<String, String> getArguments() {
		HashMap<String, String> rv = new HashMap<>();

		for(Map.Entry<String, String> s : arguments.entrySet()) {
			byte[] keyStrBytes = Base64.decodeBase64(s.getKey().getBytes());
			byte[] valStrBytes = Base64.decodeBase64(s.getValue().getBytes());
			rv.put(new String(keyStrBytes), new String(valStrBytes));
		}

		return rv;
	}
}
