package de.decoit.visa.enums;

/**
 * This enum defines return values to indicate the status of requests issued to
 * the IO-Tool via the {@link de.decoit.visa.ioconnector.IOConnector
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
	 * The command was executed successfully, but some errors occured. The
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
