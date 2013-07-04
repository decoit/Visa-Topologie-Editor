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
 * This enumeration defines return values to indicate the status of requests
 * issued to the IO-Tool via the {@link de.decoit.visa.ioconnector.IOConnector
 * IOConnector} class.
 *
 * @author Thomas Rix
 */
public enum IOToolRequestStatus {
	/**
	 * The command was executed successfully without any errors
	 */
	SUCCESS,

	/**
	 * The command was executed successfully, but some errors occurred. The
	 * errors were intended and not critical.
	 */
	SUCCESS_WITH_ERRORS,

	/**
	 * The command is still being executed and the caller has to wait for
	 * completion
	 */
	WAIT,

	/**
	 * The command did not execute successfully. More precise information about
	 * the failure can be found in the return code and message of the response.
	 */
	FAILURE,

	/**
	 * The command cannot execute since the IO-Tool is busy processing a
	 * previous request.
	 */
	IOTOOL_BUSY;
}
