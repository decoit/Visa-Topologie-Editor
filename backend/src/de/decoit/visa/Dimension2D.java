package de.decoit.visa;

import java.util.Comparator;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.interfaces.IJSON;


/**
 * This class is used to represent the size of a two dimensional object.
 * Dimensions are limited to values of 0 or greater since objects cannot have
 * negative dimensions. It provides member Comparator classes for comparision of
 * horizontal and vertical dimensions.
 *
 * @author Thomas Rix
 */
public class Dimension2D implements IJSON {
	private int x;
	private int y;


	/**
	 * Construct a new Dimension2D object with the provided size
	 *
	 * @param pX Horizontal dimension, must be 0 or greater
	 * @param pY Vertical dimension, must be 0 or greater
	 */
	public Dimension2D(int pX, int pY) {
		if(pX >= 0 && pY >= 0) {
			x = pX;
			y = pY;
		}
		else {
			throw new IllegalArgumentException("Negative values are not allowed for dimensions");
		}
	}


	/**
	 * Set the dimensions to new values
	 *
	 * @param pX Horizontal dimension, must be 0 or greater
	 * @param pY Vertical dimension, must be 0 or greater
	 */
	public void set(int pX, int pY) {
		if(pX >= 0 && pY >= 0) {
			x = pX;
			y = pY;
		}
		else {
			throw new IllegalArgumentException("Negative values are not allowed for dimensions");
		}
	}


	/**
	 * Return the horizontal dimension
	 *
	 * @return The horizontal dimension
	 */
	public int getX() {
		return x;
	}


	/**
	 * Return the vertical dimension
	 *
	 * @return The vertical dimension
	 */
	public int getY() {
		return y;
	}


	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject rv = new JSONObject();

		rv.put("x", x);
		rv.put("y", y);

		return rv;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		else if(obj instanceof Dimension2D) {
			Dimension2D d = (Dimension2D) obj;

			return (d.x == x && d.y == y);
		}
		else {
			return false;
		}
	}


	/**
	 * This Comparator implementation allows comparison of two Dimension2D
	 * objects. It will use the horizontal (X) dimensions for comparision. The
	 * result follows the Integer comparision: 0 if both values are identical,
	 * negative if the second value is greater and positive if it is less than
	 * the first value.
	 *
	 * @author Thomas Rix
	 */
	@SuppressWarnings("ucd")
	public class XComparator implements Comparator<Dimension2D> {
		@Override
		public int compare(Dimension2D d1, Dimension2D d2) {
			return d1.getX() - d2.getX();
		}
	}


	/**
	 * This Comparator implementation allows comparison of two Dimension2D
	 * objects. It will use the vertical (Y) dimensions for comparision. The
	 * result follows the Integer comparision: 0 if both values are identical,
	 * negative if the second value is greater and positive if it is less than
	 * the first value.
	 *
	 * @author Thomas Rix
	 */
	@SuppressWarnings("ucd")
	public class YComparator implements Comparator<Dimension2D> {
		@Override
		public int compare(Dimension2D d1, Dimension2D d2) {
			return d1.getY() - d2.getY();
		}
	}
}
