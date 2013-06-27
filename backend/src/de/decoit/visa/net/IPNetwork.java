package de.decoit.visa.net;

import java.math.BigInteger;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.enums.IPVersion;
import de.decoit.visa.interfaces.IJSON;


// TODO make this class an RDFObject and store it in the RDF model

/**
 * This class represents an IP network in the topology. It allows finding free
 * IP addresses in the network to use for new interfaces added to it.
 *
 * @author Thomas Rix
 */
public class IPNetwork implements IJSON {
	private IPAddress networkAddress;
	private int subnetMask;
	private IPVersion version;
	private BigInteger maxIPAddressCount;
	private HashMap<String, IPAddress> addressesInUse;
	private BigInteger nextAddressMask;
	private BigInteger lastAddressMask;


	/**
	 * Construct a new network object. The provided address must match the
	 * address notation of the specified IP version.
	 *
	 * @param pNetworkAddress String notation of the network IP address, must be
	 *            valid for the provided IP version
	 * @param pSubnetMaskLength Bit length of the subnet mask used for this
	 *            network
	 * @param pVersion Version of the Internet Protocol which will be used for
	 *            this network
	 */
	public IPNetwork(String pNetworkAddress, int pSubnetMaskLength, IPVersion pVersion) {
		int deviceMaskLength = pVersion.getAddressBitCount() - pSubnetMaskLength;

		if(deviceMaskLength >= 0 && deviceMaskLength <= pVersion.getAddressBitCount()) {
			// Calculate maximum number of device addresses in this network
			maxIPAddressCount = BigInteger.valueOf(2).pow(deviceMaskLength);
			// Substract network and broadcast addresses
			maxIPAddressCount.subtract(BigInteger.valueOf(2));
		}
		else {
			throw new IllegalArgumentException("Invalid subnet mask length provided");
		}

		addressesInUse = new HashMap<>();

		version = pVersion;
		subnetMask = pSubnetMaskLength;
		networkAddress = new IPAddress(pNetworkAddress, version);
		nextAddressMask = incrAddressMask(networkAddress.toBigIntBitmask(), BigInteger.ONE);
		lastAddressMask = incrAddressMask(networkAddress.toBigIntBitmask(), maxIPAddressCount);
	}


	/**
	 * Return the IP network address of this network
	 *
	 * @return The IP network address
	 */
	public IPAddress getNetworkAddress() {
		return networkAddress;
	}


	/**
	 * Return the string notation of the IP network address of this network
	 *
	 * @return The string notation of the IP network address
	 */
	public String getNetworkAddressString() {
		return networkAddress.getAddressString();
	}


	/**
	 * Return the bit length of the subnet mask used for this network
	 *
	 * @return The bit length of the subnet mask
	 */
	public int getSubnetMaskLength() {
		return subnetMask;
	}


	/**
	 * Return the Internet Protocol version used for this network
	 *
	 * @return The Internet Protocol version used for this network
	 */
	public IPVersion getVersion() {
		return version;
	}


	/**
	 * Mark the provided IP address as in-use. The specified address must be in
	 * the string notation matching the IP version of this network and not be
	 * in-use when calling this method. Providing an already in-use address will
	 * cause an exception.
	 *
	 * @param pAddress The IP address to mark as in-use
	 * @return The added IP address object
	 */
	public IPAddress addIPAddress(String pAddress) {
		IPAddress newAddr = new IPAddress(pAddress, version);

		if(!isAddressInNetworkRange(newAddr)) {
			throw new IllegalArgumentException("Address not in address range of this network");
		}

		if(!addressesInUse.containsKey(newAddr.getAddressString())) {
			addressesInUse.put(newAddr.getAddressString(), newAddr);
		}
		else {
			throw new IllegalArgumentException("IP address already in use");
		}

		return newAddr;
	}


	/**
	 * Remove the in-use mark of the provided IP address. If the removed IP
	 * address was lower than the current next free IP address pointer, the
	 * pointer will be set to this address. The IP address object should not be
	 * used anymore after calling this method, it could result in a corrupted
	 * topology or RDF model.
	 *
	 * @param pIPA The IP address to mark as free
	 */
	public void removeIPAddress(IPAddress pIPA) {
		addressesInUse.remove(pIPA.getAddressString());

		if(nextAddressMask.compareTo(pIPA.toBigIntBitmask()) > 0) {
			nextAddressMask = pIPA.toBigIntBitmask();
		}
	}


	/**
	 * This method returns the next free IP address in this network. If no free
	 * addresses are available, it will return null.
	 *
	 * @return Next free IP address, null if no free address is available
	 */
	public IPAddress getFreeIPAddress() {
		IPAddress newAddr;
		boolean cannotFindFreeAddress = false;

		do {
			// If no free address can be found, return null
			if(cannotFindFreeAddress) {
				return null;
			}

			newAddr = new IPAddress(nextAddressMask, version);
			try {
				incrNextAddressMask();
			}
			catch(IllegalStateException ex) {
				// Next address would be out of range, so we break the loop by
				// returning null if the current address is already in use
				cannotFindFreeAddress = true;
			}
		} while(addressesInUse.containsKey(newAddr.getAddressString()));

		return newAddr;
	}


	/**
	 * Test if the provided address is located in the address range for this network
	 *
	 * @param pAddress
	 * @return true if the address is in range, false otherwise
	 */
	public boolean isAddressInNetworkRange(IPAddress pAddress) {
		// Check if the provided address is greater than the network address and
		// less than or equal to the last valid address in this network
		return (pAddress.toBigIntBitmask().compareTo(networkAddress.toBigIntBitmask()) > 0 && pAddress.toBigIntBitmask().compareTo(lastAddressMask) <= 0);
	}


	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject rv = new JSONObject();

		rv.put("address", networkAddress.toJSON());
		rv.put("subnetMask", subnetMask);
		rv.put("version", version.toString());

		return rv;
	}


	/**
	 * Increment the next free address pointer by 1. If the pointer leaves the
	 * address range of this network an exception will be thrown.
	 *
	 * @throws IllegalStateException if the pointer leaves the address range of
	 *             this network
	 */
	private void incrNextAddressMask() {
		BigInteger tmpMask = incrAddressMask(nextAddressMask, BigInteger.ONE);

		if(tmpMask.compareTo(lastAddressMask) > 0) {
			throw new IllegalStateException("New address not in address range of this network");
		}

		nextAddressMask = tmpMask;
	}


	/**
	 * Increment the provided address bit mask by the specified amount and
	 * return it. Since {@link java.math.BigInteger BigInteger} objects are
	 * immutable this method will return a new object. The provided object will
	 * remain unchanged.
	 *
	 * @param pMask The mask of the address which will be incremented
	 * @param pAmount The amount which will be added to the address
	 * @return The incremented bit mask
	 */
	private BigInteger incrAddressMask(BigInteger pMask, BigInteger pAmount) {
		return pMask.add(pAmount);
	}
}
