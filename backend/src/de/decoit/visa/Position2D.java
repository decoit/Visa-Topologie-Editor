package de.decoit.visa;

import java.util.Comparator;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.interfaces.IJSON;


/**
 * This class represents a position on a two dimensional coordinate grid. By
 * using the member class CoordinateRange it can be limited to accept only
 * coordinates inside the set ranges. It provides member Comparator classes for
 * comparision of X and Y coordinates.
 *
 * @author Thomas Rix
 */
public class Position2D implements IJSON {
	private int x;
	private int y;
	private CoordinateRange rangeX;
	private CoordinateRange rangeY;


	/**
	 * Construct a new Position2D object with the provided coordinates and
	 * without limits.
	 *
	 * @param pX X coordinate
	 * @param pY Y coordinate
	 */
	public Position2D(int pX, int pY) {
		this(pX, pY, null, null);
	}


	/**
	 * Construct a new Position2D object with the provided coordinates.
	 * Coordinate limits are set with a minimum of 0 and a maximum according to
	 * the grid dimension parameter. The coordinates must be located inside the
	 * limits to be accepted.
	 *
	 * @param pX X coordinate
	 * @param pY Y coordinate
	 * @param pGridDim Grid dimension object, cannot be null
	 */
	public Position2D(int pX, int pY, Dimension2D pGridDim) {
		// Grid dimension - 1 is neccessary since pGridDim contains numbers of
		// rows/cols but we start counting at 0
		this(pX, pY, new CoordinateRange(0, pGridDim.getX() - 1), new CoordinateRange(0, pGridDim.getY() - 1));
	}


	/**
	 * Construct a new Position2D object with the provided coordinates and
	 * ranges. The coordinates must be located inside the limits to be accepted.
	 *
	 * @param pX X coordinate
	 * @param pY Y coordinate
	 * @param pRangeX Coordinate range for the X coordinate, may be null if no
	 *            limit is required
	 * @param pRangeY Coordinate range for the Y coordinate, may be null if no
	 *            limit is required
	 */
	@SuppressWarnings("ucd")
	public Position2D(int pX, int pY, CoordinateRange pRangeX, CoordinateRange pRangeY) {
		rangeX = pRangeX;
		rangeY = pRangeY;

		// If a X range limitation was given, make sure the X coordinate is
		// located inside the limit
		if(rangeX != null && !rangeX.inRange(pX)) {
			throw new IllegalArgumentException("X coordinate " + pX + " out of range");
		}

		// If a Y range limitation was given, make sure the Y coordinate is
		// located inside the limit
		if(rangeY != null && !rangeY.inRange(pY)) {
			throw new IllegalArgumentException("Y coordinate " + pY + " out of range");
		}

		x = pX;
		y = pY;
	}


	/**
	 * Set the coordinates of this position to new values. If set, the
	 * coordinates must be located inside the limits to be accepted.
	 *
	 * @param pX X coordinate
	 * @param pY Y coordinate
	 */
	@SuppressWarnings("ucd")
	public void set(int pX, int pY) {
		// If a X range limitation was set, make sure the X coordinate is
		// located inside the limit
		if(rangeX != null && !rangeX.inRange(pX)) {
			throw new IllegalArgumentException("X coordinate " + pX + " out of range");
		}

		// If a Y range limitation was set, make sure the Y coordinate is
		// located inside the limit
		if(rangeY != null && !rangeY.inRange(pY)) {
			throw new IllegalArgumentException("Y coordinate " + pY + " out of range");
		}

		x = pX;
		y = pY;
	}


	/**
	 * Return the X coordinate of this position
	 *
	 * @return The X coordinate of this position
	 */
	public int getX() {
		return x;
	}


	/**
	 * Return the Y coordinate of this position
	 *
	 * @return The Y coordinate of this position
	 */
	public int getY() {
		return y;
	}


	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject rv = new JSONObject();

		rv.put("x", x);
		rv.put("y", y);
		if(rangeX != null) {
			rv.put("rangeX", rangeX.toJSON());
		}
		if(rangeY != null) {
			rv.put("rangeY", rangeY.toJSON());
		}

		return rv;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rangeX == null) ? 0 : rangeX.hashCode());
		result = prime * result + ((rangeY == null) ? 0 : rangeY.hashCode());
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
			Position2D p = (Position2D) obj;

			return (p.x == x && p.y == y && p.rangeX.equals(rangeX) && p.rangeY.equals(rangeY));
		}
		else {
			return false;
		}
	}


	/**
	 * This class defines a coordinate limit for a Position2D object. Minimum
	 * and maximum values allowed are identical to Integer.MIN_VALUE and
	 * Integer.MAX_VALUE.
	 *
	 * @author Thomas Rix
	 */
	private static class CoordinateRange implements IJSON {
		private int minValue;
		private int maxValue;


		/**
		 * Construct a new object with the provided limits. The minimum value
		 * must be less than the maximum value. If no minimum or maximum limit
		 * is required, the values Integer.MIN_VALUE or Integer.MAX_VALUE can be
		 * used.
		 *
		 * @param pMin Minimum allowed value
		 * @param pMax Maximum allowed value
		 */
		private CoordinateRange(int pMin, int pMax) {
			if(pMin < pMax) {
				minValue = pMin;
				maxValue = pMax;
			}
			else {
				throw new IllegalArgumentException("Minimum value must be less than maximum value");
			}
		}


		/**
		 * Check if the provided value lies inside the set range
		 * (minValue <= pPos <= maxValue)
		 *
		 * @param pPos Value to check
		 * @return true: Value lies inside the range, false: Otherwise
		 */
		private boolean inRange(int pPos) {
			return (pPos >= minValue && pPos <= maxValue);
		}


		@Override
		public JSONObject toJSON() throws JSONException {
			JSONObject rv = new JSONObject();

			rv.put("min", minValue);
			rv.put("max", maxValue);

			return rv;
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + maxValue;
			result = prime * result + minValue;
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			else if(obj instanceof CoordinateRange) {
				CoordinateRange cr = (CoordinateRange) obj;

				return (cr.minValue == minValue && cr.maxValue == maxValue);
			}
			else {
				return false;
			}
		}
	}


	/**
	 * This Comparator implementation allows comparison of two Position2D
	 * objects. It will use the horizontal (X) coordinates for comparision. The
	 * result follows the Integer comparision: 0 if both values are identical,
	 * negative if the second value is greater and positive if it is less than
	 * the first value.
	 *
	 * @author Thomas Rix
	 */
	@SuppressWarnings("ucd")
	public class XComparator implements Comparator<Position2D> {
		@Override
		public int compare(Position2D p1, Position2D p2) {
			return p1.getX() - p2.getX();
		}
	}


	/**
	 * This Comparator implementation allows comparison of two Position2D
	 * objects. It will use the vertical (Y) coordinates for comparision. The
	 * result follows the Integer comparision: 0 if both values are identical,
	 * negative if the second value is greater and positive if it is less than
	 * the first value.
	 *
	 * @author Thomas Rix
	 */
	@SuppressWarnings("ucd")
	public class YComparator implements Comparator<Position2D> {
		@Override
		public int compare(Position2D p1, Position2D p2) {
			return p1.getY() - p2.getY();
		}
	}
}
