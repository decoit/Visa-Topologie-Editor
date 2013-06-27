package de.decoit.visa.rdf;

/**
 * This exception can be used if an empty, malformed or otherwise invalid
 * argument was passed to a method. It is just a different name for Exception,
 * constructors directly forward inputs to the Exception constructors
 *
 * @author Thomas Rix
 */
public class RDFSourceException extends Exception {
	private static final long serialVersionUID = 1356870128734577879L;


	/**
	 * Forwards to Exception()
	 *
	 * @see java.lang.Exception
	 */
	public RDFSourceException() {
		super();
	}


	/**
	 * Forwards to Exception(String), constructs an exception with a custom
	 * error message.
	 *
	 * @param pMsg Custom error message for the exception
	 */
	@SuppressWarnings("ucd")
	public RDFSourceException(String pMsg) {
		super(pMsg);
	}
}
