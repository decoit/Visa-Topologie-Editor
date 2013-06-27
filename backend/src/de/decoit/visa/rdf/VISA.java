package de.decoit.visa.rdf;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * This class defines the RDF vocabulary for the VISA project. It includes any
 * property that can occur in VISA RDF models and also the possible literal
 * values for the 'type' property
 *
 * @author Thomas Rix
 */
public class VISA {
	/**
	 * VISA namespace definition
	 */
	private static final String URI = "http://visa-project.de/topology/";


	/**
	 * Private constructor, this class is not meant to be instanciated
	 */
	private VISA() {
		// Empty constructor
	}


	/**
	 * Returns the URI for this schema
	 *
	 * @return the URI for this schema
	 */
	static String getURI() {
		return VISA.URI;
	}


	/**
	 * Create the URI for a model from the model name and the VISA namespace.
	 * The model URI will follow this scheme:<br>
	 * http://visa-project.de/topology/model/&lt;model_name&gt;
	 *
	 * @param pModName Name of the model
	 * @return The model URI following the above scheme
	 */
	static String createModelURI(String pModName) {
		StringBuilder sb = new StringBuilder(VISA.URI);
		sb.append("model/");
		sb.append(pModName);

		return sb.toString();
	}


	/**
	 * Return a Resource object that has a URI built from the VISA namespace and
	 * the specified local name. This method will <b>not</b> return a resource
	 * that already exists in the RDF model in {@link RDFManager} because it
	 * uses its own internal model. The URI of the created resource will follow
	 * this scheme:<br>
	 * http://visa-project.de/topology/&lt;local_name&gt;
	 *
	 * @param pLocName Local name of the new resource
	 * @return The created Resource object
	 */
	static Resource createResource(String pLocName) {
		StringBuilder sbURI = new StringBuilder(VISA.URI);
		sbURI.append(pLocName);

		return mod.createResource(sbURI.toString());
	}

	/**
	 * Internal model used to create the Properties an Literals included in this
	 * class
	 */
	private static Model mod = ModelFactory.createDefaultModel();

	/**
	 * 'device' property, used with a VSA resource as subject and a Device
	 * resource as object
	 */
	@SuppressWarnings("ucd")
	public static final Property DEVICE = VISA.mod.createProperty(VISA.URI, "device");

	/**
	 * 'interface' property, used with a Device resource as subject and an
	 * Interface resource as object
	 */
	@SuppressWarnings("ucd")
	public static final Property INTERFACE = VISA.mod.createProperty(VISA.URI, "interface");

	/**
	 * 'internalName' property, used by the IO-Tool to assign unique identifiers
	 * to resources. Has a string literal as object.
	 */
	@SuppressWarnings("ucd")
	public static final Property INTERNAL_NAME = VISA.mod.createProperty(VISA.URI, "internalName");

	/**
	 * 'name' property, used to assign human readable names to VSA, Device and
	 * Interface resources. Has a string literal as object.
	 */
	@SuppressWarnings("ucd")
	public static final Property NAME = VISA.mod.createProperty(VISA.URI, "name");

	/**
	 * 'connected' property, indicates that two interfaces are connected in the
	 * topology. Has Interface resources as subject and object.
	 */
	@SuppressWarnings("ucd")
	public static final Property CONNECTED = VISA.mod.createProperty(VISA.URI, "connected");

	/**
	 * 'address' property, used with an Interface resource as subject and an
	 * Address resource as object
	 */
	@SuppressWarnings("ucd")
	public static final Property ADDRESS = VISA.mod.createProperty(VISA.URI, "address");

	/**
	 * 'imageFile' property, used to assign a VM image file to a device. Has a
	 * string literal as object.
	 */
	@SuppressWarnings("ucd")
	public static final Property IMAGE_FILE = VISA.mod.createProperty(VISA.URI, "imageFile");

	/**
	 * 'type' property, used with either a Device or Address resource as subject
	 * and a string literal as object. The values of the literal are predefined:
	 * For Device resources it must either be 'switch' or 'host', for Address
	 * and Network resources it must be 'ethernet', 'ipv4' or 'ipv6'.
	 */
	@SuppressWarnings("ucd")
	public static final Property TYPE = VISA.mod.createProperty(VISA.URI, "type");

	/**
	 * 'vlan' property, used with an Interface resource as subject and a VLAN
	 * resource as object
	 */
	@SuppressWarnings("ucd")
	public static final Property VLAN = VISA.mod.createProperty(VISA.URI, "vlan");

	/**
	 * 'id' property, used with a VLAN resource as subject and a positive
	 * integer literal as object
	 */
	@SuppressWarnings("ucd")
	public static final Property ID = VISA.mod.createProperty(VISA.URI, "id");

	/**
	 * 'value' property, used with either an Address or Network resource as
	 * subject and a string literal as object. For Address resources the literal
	 * contains the IP address as string, for Network resources it contains the
	 * network IP address.
	 */
	@SuppressWarnings("ucd")
	public static final Property VALUE = VISA.mod.createProperty(VISA.URI, "value");

	/**
	 * 'network' property, used with an Address resource as subject and a
	 * Network resource as object
	 */
	@SuppressWarnings("ucd")
	public static final Property NETWORK = VISA.mod.createProperty(VISA.URI, "network");

	/**
	 * 'netmaskLength' property, used with a Network resource as subject and a
	 * positive integer literal as object. The literal contains the numeric
	 * (CIDR) representation of the network's subnet mask.
	 */
	@SuppressWarnings("ucd")
	public static final Property NETMASK_LENGTH = VISA.mod.createProperty(VISA.URI, "netmaskLength");

	/**
	 * 'physical' property, used with a Device resource as subject and a boolean
	 * (true/false) literal as object. Indicates if a device is a physical
	 * (true) machine or a virtual machine (false)
	 */
	@SuppressWarnings("ucd")
	public static final Property PHYSICAL = VISA.mod.createProperty(VISA.URI, "physical");

	/**
	 * Predefined 'ethernet' value for the 'type' property of an Address resource
	 */
	@SuppressWarnings("ucd")
	static final Literal ADDR_TYPE_ETHERNET = mod.createLiteral("ethernet");

	/**
	 * Predefined 'ipv4' value for the 'type' property of an Address resource
	 */
	@SuppressWarnings("ucd")
	static final Literal ADDR_TYPE_IPV4 = mod.createLiteral("ipv4");

	/**
	 * Predefined 'ipv6' value for the 'type' property of an Address resource
	 */
	@SuppressWarnings("ucd")
	static final Literal ADDR_TYPE_IPV6 = mod.createLiteral("ipv6");

	/**
	 * Predefined 'switch' value for the 'type' property of a Device resource
	 */
	@SuppressWarnings("ucd")
	static final Literal DEV_TYPE_SWITCH = mod.createLiteral("switch");

	/**
	 * Predefined 'host' value for the 'type' property of a Device resource
	 */
	@SuppressWarnings("ucd")
	static final Literal DEV_TYPE_HOST = mod.createLiteral("host");

	/**
	 * Predefined 'true' value for boolean literals
	 */
	@SuppressWarnings("ucd")
	static final Literal BOOL_TRUE = mod.createLiteral("true");

	/**
	 * Predefined 'false' value for boolean literals
	 */
	@SuppressWarnings("ucd")
	static final Literal BOOL_FALSE = mod.createLiteral("false");
}
