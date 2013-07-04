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

package de.decoit.visa.ioconnector;

/**
 * This exception is throws if accessing the IO-Tool fails. This is usually the
 * case when it is busy processing a command and the user tries to execute a new
 * command. This is <b>NOT</b> used for connection errors, those will cause an
 * {@link java.io.IOException IOException}.
 *
 * @author Thomas Rix
 */
public class IOToolException extends Exception {
	private static final long serialVersionUID = 1356870128734577879L;


	/**
	 * Forwards to Exception()
	 *
	 * @see java.lang.Exception
	 */
	public IOToolException() {
		super();
	}


	/**
	 * Forwards to Exception(String), constructs an exception with a custom
	 * error message.
	 *
	 * @param pMsg Custom error message for the exception
	 */
	@SuppressWarnings("ucd")
	public IOToolException(String pMsg) {
		super(pMsg);
	}
}
