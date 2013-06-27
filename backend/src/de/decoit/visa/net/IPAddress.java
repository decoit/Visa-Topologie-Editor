package de.decoit.visa.net;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.enums.IPVersion;
import de.decoit.visa.interfaces.IJSON;


/**
 * This class stores an IPv4 or IPv6 address. The address can be provided as
 * String or a bit mask stored as a {@link java.math.BigInteger BigInteger}
 * object. The address will be validated to follow the scheme of the provided IP
 * version.
 *
 * @author Thomas Rix
 */
public class IPAddress implements IJSON {
	private String ipAddress;
	private int[] ipAddressGroups;
	private IPVersion ipVersion;
	private boolean isLinkLocal;


	/**
	 * Construct a new object using the specified IP address and version. The
	 * address will be checked to be valid for the provided version of the IP
	 * protocol. Additonally it will be checked if the address is a link local
	 * address.
	 *
	 * @param pAddress String notation of the IP address, must be valid for the
	 *            provided IP version
	 * @param pVersion Version of the Internet Protocol which will be used for
	 *            this address
	 */
	IPAddress(String pAddress, IPVersion pVersion) {
		String[] addressParts;
		Matcher addrMatcher;
		Matcher linkLocalMatcher;

		if(pVersion == IPVersion.V4) {
			// Check if the IP address is valid
			Pattern ip4pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", Pattern.CASE_INSENSITIVE);
			addrMatcher = ip4pattern.matcher(pAddress);

			Pattern linkLocalPattern = Pattern.compile("^169\\.254\\.(?:25[0-4]|2[0-4][0-9]|1[0-9][0-9]?|[1-9])\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", Pattern.CASE_INSENSITIVE);
			linkLocalMatcher = linkLocalPattern.matcher(pAddress);
		}
		else if(pVersion == IPVersion.V6) {
			// Check if the IP address is valid
			Pattern ip6pattern = Pattern.compile("^(((?=(?>.*?::)(?!.*::)))(::)?([0-9A-F]{1,4}::?){0,5}|([0-9A-F]{1,4}:){6})(\\2([0-9A-F]{1,4}(::?|$)){0,2}|((25[0-5]|(2[0-4]|1\\d|[1-9])?\\d)(\\.|$)){4}|[0-9A-F]{1,4}:[0-9A-F]{1,4})(?<![^:]:|\\.)\\z", Pattern.CASE_INSENSITIVE);
			addrMatcher = ip6pattern.matcher(pAddress);

			// Check if this address is a link-local address
			Pattern linkLocalPattern = Pattern.compile("^FE80(?::0000){3}(?::[0-9A-F]{4}){4}$", Pattern.CASE_INSENSITIVE);
			linkLocalMatcher = linkLocalPattern.matcher(pAddress);
		}
		else {
			throw new IllegalArgumentException("Invalid IP version provided");
		}

		if(addrMatcher.matches()) {
			ipAddress = pAddress;
			ipVersion = pVersion;

			addressParts = pAddress.split(ipVersion.getNotationSplitRegex());
			ipAddressGroups = new int[addressParts.length];
			for(int i = 0; i < addressParts.length; i++) {
				ipAddressGroups[i] = Integer.parseInt(addressParts[i], ipVersion.getNotationRadix());
			}

			// Check if this address is a link-local address
			isLinkLocal = linkLocalMatcher.matches();
		}
		else {
			throw new IllegalArgumentException("Malformed IP or network address");
		}
	}


	/**
	 * Construct a new object from a bit mask stored in a
	 * {@link java.math.BigInteger BigInteger} object. The bit mask will be
	 * translated into a string containing the string notation of the address
	 * and then be passed to the IPAddress(String, IPVersion) constructor.
	 *
	 * @param pMask Object containing the bit mask of the address
	 * @param pVersion Version of the Internet Protocol which will be used for
	 *            this address
	 */
	IPAddress(BigInteger pMask, IPVersion pVersion) {
		this(IPAddress.bitmaskToString(pMask, pVersion), pVersion);
	}


	/**
	 * Return the string notation of this address
	 *
	 * @return The string notation of this address
	 */
	public String getAddressString() {
		return ipAddress;
	}


	/**
	 * Return the Internet Protocol version used for this address
	 *
	 * @return The Internet Protocol version used for this address
	 */
	public IPVersion getVersion() {
		return ipVersion;
	}


	/**
	 * Return true if this address is a link-local address, false otherwise
	 *
	 * @return Address is link-local or not
	 */
	public boolean isLinkLocal() {
		return isLinkLocal;
	}


	/**
	 * Return a {@link java.math.BigInteger BigInteger} object containing the
	 * bit mask of this address.
	 *
	 * @return Bit mask of this address
	 */
	BigInteger toBigIntBitmask() {
		BigInteger rv = BigInteger.ZERO;

		for(int i = 0; i < ipAddressGroups.length; i++) {
			rv = rv.add(BigInteger.valueOf(ipAddressGroups[i]));

			if(i < ipAddressGroups.length - 1) {
				rv = rv.shiftLeft(ipVersion.getSegmentBitCount());
			}
		}

		return rv;
	}


	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject rv = new JSONObject();

		rv.put("address", ipAddress);
		rv.put("version", ipVersion.toString());
		rv.put("isLinkLocal", isLinkLocal);

		return rv;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		result = prime * result + ((ipVersion == null) ? 0 : ipVersion.hashCode());
		result = prime * result + (isLinkLocal ? 1231 : 1237);
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		else if(obj instanceof IPAddress) {
			IPAddress ipa = (IPAddress) obj;

			return (ipa.ipAddress.equals(ipAddress) && ipa.ipVersion.toInteger() == ipVersion.toInteger() && ipa.isLinkLocal == isLinkLocal);
		}
		else {
			return false;
		}
	}


	/**
	 * Static utility function to translate a provided bit mask into the IP
	 * version specific string notation.
	 *
	 * @param pMask Object containing the bit mask of the address to translate
	 * @param pVersion Version of the Internet Protocol which will be used for
	 *            the address
	 * @return The string notation of the provided IP address
	 */
	private static String bitmaskToString(BigInteger pMask, IPVersion pVersion) {
		byte[] maskBytes = pMask.toByteArray();
		StringBuilder rv = new StringBuilder();
		boolean ignoreFirstByte;

		if(pVersion == IPVersion.V4) {
			// If array length is 5 it contains a sign byte we have to ignore
			ignoreFirstByte = (maskBytes.length == 5);

			for(int i = 0; i < maskBytes.length; i++) {
				if(ignoreFirstByte) {
					// Skip the first byte, it's the sign byte
					i++;
					ignoreFirstByte = false;
				}

				rv.append(maskBytes[i] & 0xFF);

				// Only append the '.' if this is not the last segment
				if(i < maskBytes.length - 1) {
					rv.append(".");
				}
			}
		}
		else if(pVersion == IPVersion.V6) {
			// If array length is 17 it contains a sign byte we have to ignore
			ignoreFirstByte = (maskBytes.length == 17);

			for(int i = 0; i < maskBytes.length; i += 2) {
				if(ignoreFirstByte) {
					// Skip the first byte
					i++;
					ignoreFirstByte = false;
				}

				// Notation segments are made of 2 bytes in IPv6
				rv.append(String.format("%02X", maskBytes[i]));
				rv.append(String.format("%02X", maskBytes[i + 1]));

				// Only append the ':' if this is not the last segment
				if(i < maskBytes.length - 2) {
					rv.append(":");
				}
			}
		}
		else {
			throw new IllegalStateException("Unsupported IP version detected");
		}

		return rv.toString();
	}
}
