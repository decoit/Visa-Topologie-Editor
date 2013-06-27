package de.decoit.visa.topology;

import java.util.ArrayList;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import de.decoit.visa.TEBackend;
import de.decoit.visa.interfaces.IJSON;
import de.decoit.visa.interfaces.IRDFObject;
import de.decoit.visa.interfaces.IRemovableComponent;
import de.decoit.visa.interfaces.ITopologyElement;
import de.decoit.visa.rdf.VISA;
import de.decoit.visa.rdf.VISABackup;
import de.decoit.visa.topology.NetworkComponent.Interface;


/**
 * This class represents a VLAN in the topology. A VLAN must have a name and a
 * color. The color will be used to show it on the topology editor grid. The
 * name is used as a human readable identifier but it is not required be unique
 * in a topology. An ID number is used for identification which is set or
 * generated in the constructor and must be unique.
 *
 * @author Thomas Rix
 */
public class VLAN implements ITopologyElement, IRDFObject, IJSON, IRemovableComponent {
	/**
	 * Type string used to build the local name for new components
	 */
	private static final String TYPE = "vlan";

	private static Logger log = Logger.getLogger(VLAN.class.getName());
	private static ColorChooser colorChooser = new ColorChooser();

	private int id;
	private String name;
	private String color;
	private String localName;
	private ArrayList<Interface> interfaces;


	/**
	 * This constructor will use the provided ID for the created VLAN,
	 * color will be chosen from colorChooser, name will be set to default
	 * value:
	 * name: "VLAN id"
	 * The ID will be checked to be unique, providing an already used ID will
	 * cause an InvalidArgumentException
	 *
	 * @param pID The ID which will be used for this VLAN
	 * @param pLocName
	 */
	VLAN(int pID, String pLocName) {
		interfaces = new ArrayList<>();

		id = pID;

		StringBuilder sbName = new StringBuilder("VLAN ");
		sbName.append(pID);
		name = sbName.toString();

		color = colorChooser.getNextColor();

		// Check if the given local name is valid
		if(pLocName != null && !pLocName.isEmpty()) {
			localName = pLocName;
		}
		else {
			StringBuilder sbLocalName = new StringBuilder(VLAN.TYPE);
			sbLocalName.append("_");
			sbLocalName.append(pID);
			localName = sbLocalName.toString();
		}

		TEBackend.RDF_MANAGER.updateProperty(this, VISA.ID);
		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.NAME);
		TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.VLAN_COLOR);

		if(log.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder("VLAN created: ");
			sb.append(localName);

			log.trace(sb.toString());
		}
	}


	/**
	 * Return the ID number of this VLAN
	 *
	 * @return The ID number
	 */
	public int getID() {
		return id;
	}


	@Override
	public String getRDFLocalName() {
		return localName;
	}


	@Override
	public String getIdentifier() {
		return getRDFLocalName();
	}


	/**
	 * Set a new name for this VLAN
	 *
	 * @param pName New name, cannot be empty
	 * @throws IllegalArgumentException
	 */
	public final void setName(String pName) throws IllegalArgumentException {
		// Check if the provided name is not empty
		if(!pName.isEmpty()) {
			name = pName;

			TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.NAME);
		}
		else {
			throw new IllegalArgumentException("Empty name provided");
		}
	}


	/**
	 * Return the current name of this VLAN
	 *
	 * @return The current name of this VLAN
	 */
	public final String getName() {
		return name;
	}


	/**
	 * Set a new display color for this VLAN
	 *
	 * @param pColor New color, must be a hexadecimal RGB color string (#RRGGBB)
	 * @throws IllegalArgumentException
	 */
	public final void setColor(String pColor) throws IllegalArgumentException {
		// Check if the provided string matches the required color format
		if(pColor.matches("#[a-fA-F0-9]{6}")) {
			color = pColor.toUpperCase();

			TEBackend.RDF_MANAGER.updateProperty(this, VISABackup.VLAN_COLOR);
		}
		else {
			throw new IllegalArgumentException("Malformed color string");
		}
	}


	/**
	 * Return the current color of this VLAN
	 *
	 * @return The current color of this VLAN
	 */
	public final String getColor() {
		return color;
	}


	@Override
	public void removeFromTopology() {
		if(interfaces.size() == 0) {
			TEBackend.RDF_MANAGER.removeVLAN(this);
		}
		else {
			throw new IllegalStateException("VLAN is still assigned to interfaces");
		}
	}


	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject rv = new JSONObject();

		rv.put("identifier", getIdentifier());
		rv.put("name", StringEscapeUtils.escapeHtml4(name));
		rv.put("color", color);

		return rv;
	}


	/**
	 * Reset the color chooser to its initial state
	 */
	public static void resetColorChooser() {
		colorChooser.reset();
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((localName == null) ? 0 : localName.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		else if(obj instanceof VLAN) {
			VLAN v = (VLAN) obj;

			return (v.id == id && v.localName.equals(localName));
		}
		else {
			return false;
		}
	}


	/**
	 * This class contains pre-defined colors for VLANs which will be selected
	 * by a round-robin system. It is used if new VLANs without color
	 * information, for example if extracted from an RDF model, are credated.
	 *
	 * @author Thomas Rix
	 */
	private static class ColorChooser {
		private String[] colors = { "#CCCCFF", "#9999FF", "#6666FF", "#3333FF", "#FFB2B2", "#FF9999", "#FF6666", "#FF4D4D", "#00FF00", "#00CC00", "#00B200", "#009900" };
		private int nextColor;
		private int colorCount;


		/**
		 * Construct a new color chooserF
		 */
		public ColorChooser() {
			nextColor = 0;
			colorCount = colors.length;
		}


		/**
		 * Return the next color and increment the next color counter by 1. If
		 * all colors were used the counter will be reset to 0 and all colors
		 * are used again.
		 *
		 * @return The next color string
		 */
		public String getNextColor() {
			String rv = colors[nextColor++];
			if(nextColor == colorCount) {
				nextColor = 0;
			}

			return rv;
		}


		/**
		 * Reset the next color counter to 0
		 */
		public void reset() {
			nextColor = 0;
		}
	}
}
