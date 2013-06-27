package de.decoit.visa.http;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import de.decoit.visa.TEBackend;


/**
 * This class represents a query string and allows access to all the parameters
 * stored in the query string. Parameter values are stored as QueryStringValue
 * objects. The containsKey() and get() methods just wrap the corresponding
 * HashMap methods since a HashMap is used for storage.
 *
 * @author Thomas Rix
 */
public class QueryString {
	private static Logger log = Logger.getLogger(QueryString.class.getName());

	private HashMap<String, QueryStringValue> queryParams;


	/**
	 * Construct a new object using a URI object. The query string will be
	 * extracted from the URI.
	 *
	 * @param pURI URI object
	 */
	public QueryString(URI pURI) {
		queryParams = new HashMap<>();
		this.evalQueryString(pURI.getQuery());
	}


	/**
	 * Get the QueryStringValue stored under the key pKey. Wraps around
	 * HashMap.get(String).
	 *
	 * @param pKey The key of the value
	 * @return The value stored under the given key, null if the key does not
	 *         exist
	 */
	public QueryStringValue get(String pKey) {
		return queryParams.get(pKey);
	}


	/**
	 * Check if the QueryString contains a key with the name pKey. Wraps around
	 * HashMap.containsKey(String).
	 *
	 * @param pKey Name of the key which shall be checked
	 * @return true: Key exists, false otherwise
	 */
	public boolean containsKey(String pKey) {
		return queryParams.containsKey(pKey);
	}


	/**
	 * Evaluate a given query string and store the parameters in this object. It
	 * will split the query string at all occurences of the "&" sign and add
	 * each parameter to the parameter map
	 *
	 * @param pQuery A string containing the query string of a URI/URL without
	 *            leading "?"
	 */
	private void evalQueryString(String pQuery) {
		// Query parameters are separated by "&", so do a split with "&"
		// delimiter
		String[] queryElements = pQuery.split("&");

		// Iterate through all query parameters
		for(String qe : queryElements) {
			try {
				this.add(qe);
			}
			catch(IllegalArgumentException ex) {

			}
		}
	}


	/**
	 * Add a parameter to the parameter map. The parameter must be of the form
	 * key=value. The 'key' must begin with a letter followed by any number of
	 * alphanumeric characters (a-z, A-Z, 0-9) or an underscore. The last two
	 * characters of the 'key' may be "[]" to mark arrays but are optional since
	 * identical keys on two or more parameters will be treated as array values.
	 * The 'value' can be made of any number of characters.
	 *
	 * @param pParam String containing the parameter in key=value form
	 */
	private void add(String pParam) {
		// Check if the parameter has a valid form
		Pattern p = Pattern.compile("^([a-zA-Z][_\\w]+)(\\[?\\]?)=(.*)$");
		Matcher m = p.matcher(pParam);

		if(m.matches()) {
			try {
				// Check if it is a duplicate parameter => array!
				if(queryParams.containsKey(m.group(1))) {
					queryParams.get(m.group(1)).add(URLDecoder.decode(m.group(3), "UTF-8"));
				}
				else {
					queryParams.put(m.group(1), new QueryStringValue(URLDecoder.decode(m.group(3), "UTF-8")));
				}
			}
			catch(UnsupportedEncodingException ex) {
				TEBackend.logException(ex, log);
			}
		}
		else {
			throw new IllegalArgumentException("Malformed query string parameter");
		}
	}


	/**
	 * This class stores values for a query string parameter. Array values in
	 * query strings are supported by using an ArrayList for storage. To access
	 * array values the get(int) method can be used, for single values the get()
	 * method is available which maps to get(0).
	 *
	 * @author Thomas Rix
	 */
	public class QueryStringValue {
		private ArrayList<String> value;


		/**
		 * Construct a new empty object.
		 */
		public QueryStringValue() {
			value = new ArrayList<>();
		}


		/**
		 * Construct a new object containing the value pVal.
		 *
		 * @param pVal Parameter value, cannot be empty
		 */
		private QueryStringValue(String pVal) {
			value = new ArrayList<>();

			if(!pVal.isEmpty()) {
				value.add(pVal);
			}
			else {
				throw new IllegalArgumentException("Empty value string provided");
			}
		}


		/**
		 * Add the given parameter value to this object. Wraps around
		 * ArrayList.add(String).
		 *
		 * @param pVal Parameter value, cannot be empty
		 */
		public void add(String pVal) {
			if(!pVal.isEmpty()) {
				value.add(pVal);
			}
			else {
				throw new IllegalArgumentException("Empty value string provided");
			}
		}


		/**
		 * Return the first value stored in this object. Short version for
		 * get(0).
		 *
		 * @return The value stored at index 0
		 * @throws IndexOutOfBoundsException if storage ArrayList is empty
		 */
		public String get() {
			return value.get(0);
		}


		/**
		 * Return the value stored at position pIndex. Wraps around
		 * ArrayList.get(int).
		 *
		 * @param pIndex Index of the stored value
		 * @return The value stored at the given position
		 * @throws IndexOutOfBoundsException if the index is out of range (index
		 *             < 0 || index >= size())
		 */
		public String get(int pIndex) {
			return value.get(pIndex);
		}


		/**
		 * Return the list used for internal storage
		 *
		 * @return The list used for internal storage
		 */
		public ArrayList<String> toList() {
			return value;
		}


		/**
		 * Return the number of stored values in this object. Wraps around
		 * ArrayList.size().
		 *
		 * @return The number of stored values in this object
		 */
		public int size() {
			return value.size();
		}
	}
}
