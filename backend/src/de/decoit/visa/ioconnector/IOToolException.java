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
