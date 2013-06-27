package de.decoit.visa.enums;

/**
 * This enumeration defines supported versions of the Internet Protocol (IP). It
 * is possible to get a numeric representation (integer) for the versions to use
 * them in text based information like JSON. Additionally it is possible to
 * retrieve the enum object for a given numeric representation (integer)
 *
 * @author Thomas Rix
 */
public enum IPVersion {
	/**
	 * Indicates IPv4. Numeric representation is 4, string representation is
	 * 'ipv4'. Each segment of the address is 8bit long. The string notation has
	 * a radix of 10 and the segments are delimited by a '.'.
	 */
	V4(4, "ipv4", 8, 10, "\\."),

	/**
	 * Indicates IPv6. Numeric representation is 6, string representation is
	 * 'ipv6'. Each segment of the address is 16bit long. The string notation
	 * has a radix of 16 and the segments are delimited by a ':'.
	 */
	V6(6, "ipv6", 16, 16, ":");

	private final int numericRepresentation;
	private final String stringRepresentation;
	private final int segmentBitCount;
	private final int notationRadix;
	private final String notationSplitRegex;


	/**
	 * Constructor
	 *
	 * @param v Numeric representation of this version
	 * @param s String representation as used in RDF data model
	 * @param n Number of bits per segment in address string notation
	 * @param b Radix used in address string notation
	 * @param r Regex used to split the address string notation into groups
	 */
	IPVersion(int v, String s, int n, int b, String r) {
		numericRepresentation = v;
		stringRepresentation = s;
		segmentBitCount = n;
		notationRadix = b;
		notationSplitRegex = r;
	}


	/**
	 * Return the number of bits used to form a segment in the string notation
	 * of this IP version.
	 *
	 * @return Number of bits per segement
	 */
	public int getSegmentBitCount() {
		return segmentBitCount;
	}


	/**
	 * Get the total number of bits used for an address of this IP version
	 *
	 * @return Total number of bits of an address
	 */
	public int getAddressBitCount() {
		switch(numericRepresentation) {
			case 4:
				return segmentBitCount * 4;
			case 6:
				return segmentBitCount * 8;
			default:
				throw new IllegalStateException("Unknown numeric representation detected");
		}
	}


	/**
	 * Get the radix used in the string notation of this IP version.
	 *
	 * @return Radix used in string notation
	 */
	public int getNotationRadix() {
		return notationRadix;
	}


	/**
	 * Return the regex used to split the string notation of an IP address into
	 * segments.
	 *
	 * @return Splitting regex
	 */
	public String getNotationSplitRegex() {
		return notationSplitRegex;
	}


	/**
	 * Return the numeric representation of this version
	 *
	 * @return The numeric representation of this version
	 */
	public int toInteger() {
		return numericRepresentation;
	}


	@Override
	public String toString() {
		return stringRepresentation;
	}


	/**
	 * Return the corresponding enum object to a string version representation
	 *
	 * @param pNum The string version representation
	 * @return Enum object for the string representation, null if an invalid
	 *         representation was provided
	 */
	public static IPVersion getVersion(String pNum) {
		switch(pNum) {
			case "ipv4":
				return V4;
			case "ipv6":
				return V6;
			default:
				return null;
		}
	}
}
