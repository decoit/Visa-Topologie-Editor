package de.decoit.visa.interfaces;

/**
 * This interface is required for all objects that are stored in the RDF data
 * provided by the IO-Tool from Fraunhofer SIT. It declares methods for
 * retrieving RDF specific information like the local name.
 *
 * @author Thomas Rix
 */
public interface IRDFObject {
	/**
	 * Return the local name of the RDF resource. The local name is the part of
	 * the URI that does not belong to the namespace URI.
	 *
	 * @return The local name of the RDF resource
	 */
	public String getRDFLocalName();
}
