/*
 *  Copyright (C) 2013, DECOIT GmbH
 *
 *	This file is part of VISA Topology-Editor.
 *
 *	VISA Topology-Editor is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by the
 *	Free Software Foundation, either version 3 of the License, or (at your option)
 *	any later version.
 *
 *	VISA Topology-Editor is distributed in the hope that it will be useful, but
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *	more details.
 *
 *	You should have received a copy of the GNU General Public License along with
 *	VISA Topology-Editor. If not, see <http://www.gnu.org/licenses/>.
 */

package de.decoit.visa.gridlayout;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.log4j.Logger;
import de.decoit.visa.Dimension2D;
import de.decoit.visa.Position2D;
import de.decoit.visa.TEBackend;
import de.decoit.visa.interfaces.INetworkInterface;
import de.decoit.visa.topology.NCSwitch.GroupSwitch;
import de.decoit.visa.topology.NCSwitch.VirtualInterface;
import de.decoit.visa.topology.NetworkCable;
import de.decoit.visa.topology.NetworkComponent;
import de.decoit.visa.topology.NetworkComponent.Interface;
import de.decoit.visa.topology.TopologyStorage;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup.GroupInterface;


/**
 * This class creates an input file for graphviz in DOT format. It uses some
 * hard coded global and node settings and producing the remaining preferences
 * from the current environment or provided NetworkComponent (node) and
 * NetworkCable (edge) objects. It can only be used for non-directed graphs.
 *
 * @author Thomas Rix
 */
class DOTFile {
	private static Logger log = Logger.getLogger(DOTFile.class.getName());

	private BufferedWriter bw;
	private boolean closed;
	private HashMap<String, String> groupSwitchAnchors;
	private HashMap<String, Boolean> addedGroupSwitchAnchors;
	private HashSet<String> nodeLines;
	private HashSet<String> edgeLines;


	/**
	 * Construct a new DOTFile object using the provided output file and
	 * encoding as sink. The opening graph line and the global graph, node and
	 * edge preferences.
	 *
	 * @param pOutfile The output file to write to
	 * @param pEncoding Encoding used for the output
	 * @param pGridDimensions Dimensions of the grid the components are placed on
	 * @throws IOException if output file cannot be opened or writing fails
	 */
	DOTFile(Path pOutfile, Charset pEncoding, Dimension2D pGridDimensions) throws IOException {
		closed = false;

		groupSwitchAnchors = new HashMap<>();
		addedGroupSwitchAnchors = new HashMap<>();
		nodeLines = new HashSet<>();
		edgeLines = new HashSet<>();

		createGroupSwitchAnchors(pGridDimensions);

		if(log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Opening DOT file '");
			sb.append(pOutfile);
			sb.append("' for writing");
			log.debug(sb.toString());
		}

		// Open an output stream on the target file
		bw = Files.newBufferedWriter(pOutfile, pEncoding, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		// Construct headline "graph NAME {" and write it to the file
		StringBuilder sbOutput = new StringBuilder("graph ");
		sbOutput.append(TEBackend.RDF_MANAGER.getRootNodeLocalName());
		sbOutput.append(" {");

		bw.write(sbOutput.toString());
		bw.newLine();

		// Construct global graph configuration line and write it to the
		// file
		sbOutput = new StringBuilder("\tgraph [size=\"");
		sbOutput.append(pGridDimensions.getX());
		sbOutput.append(",");
		sbOutput.append(pGridDimensions.getY());
		sbOutput.append("\", ratio=\"fill\", overlap=\"false\", start=\"2\"];");

		bw.write(sbOutput.toString());
		bw.newLine();

		// Write global node configuration line to the file
		bw.write("\tnode [style=\"solid\"];");
		bw.newLine();

		// Write global edge configuration line to the file
		bw.write("\tedge [len=\"5\", style=\"solid\"];");
		bw.newLine();
	}


	/**
	 * Add a new node line to the output file. Identifier and dimensions are
	 * taken from the provided {@link de.decoit.visa.topology.NetworkComponent}. This method must be
	 * called before calling {@link #close()}. Calling it after closing the file
	 * will result in an IllegalStateException.
	 *
	 * @param pNC NetworkComponent object providing the node information
	 * @return true: Object position will be calculated, false: Object position
	 *         was already defined
	 * @throws IOException if writing to output file failed
	 */
	boolean addNode(NetworkComponent pNC) throws IOException {
		if(!closed) {
			boolean rv = true;

			// Construct a new node line with the information found in pNC
			// and write it to the file
			StringBuilder sbOutput = new StringBuilder("\t\"");
			sbOutput.append(pNC.getRDFLocalName());
			sbOutput.append("\"");
			sbOutput.append(" [width=\"");
			sbOutput.append(pNC.getConfig().getDragboxDimensions().getX());
			sbOutput.append("\", height=\"");
			sbOutput.append(pNC.getConfig().getDragboxDimensions().getY());
			sbOutput.append("\", shape=\"rectangle\", fixedsize=\"true\", label=\"\"");

			Position2D nodePos = pNC.getConfig().getGridLocation();
			if(nodePos != null) {
				int posX = nodePos.getX() + pNC.getConfig().getDragboxDimensions().getX() / 2;
				int posY = nodePos.getY() + pNC.getConfig().getDragboxDimensions().getY() / 2;

				sbOutput.append(" pos=\"");
				sbOutput.append(posX);
				sbOutput.append(",");
				sbOutput.append(posY);
				sbOutput.append("\" pin=\"true\"");

				rv = false;
			}

			sbOutput.append("];");

			nodeLines.add(sbOutput.toString());

			if(log.isDebugEnabled()) {
				log.debug("Node line added to DOT file:");
				log.debug(sbOutput.toString());
			}

			return rv;
		}
		else {
			if(log.isDebugEnabled()) {
				log.debug("addNode() called on already closed DOTFile");
			}
			throw new IllegalStateException("Output stream already closed");
		}
	}


	/**
	 * Add a new node line to the output file. Identifier and dimensions are
	 * taken from the provided {@link de.decoit.visa.topology.TopologyStorage.ComponentGroup ComponentGroup}. This
	 * method must be called before calling {@link #close()}. Calling it after
	 * closing the file will result in an IllegalStateException.
	 *
	 * @param pCG ComponentGroup object providing the node information
	 * @return true: Object position will be calculated, false: Object position
	 *         was already defined
	 * @throws IOException if writing to output file failed
	 */
	boolean addNode(TopologyStorage.ComponentGroup pCG) throws IOException {
		if(!closed) {
			boolean rv = true;

			// Construct a new node line with the information found in pNC
			// and write it to the file
			StringBuilder sbOutput = new StringBuilder("\t\"");
			sbOutput.append(pCG.getIdentifier());
			sbOutput.append("\"");
			sbOutput.append(" [width=\"");
			sbOutput.append(pCG.getDragboxDimensions().getX());
			sbOutput.append("\", height=\"");
			sbOutput.append(pCG.getDragboxDimensions().getY());
			sbOutput.append("\", shape=\"rectangle\", fixedsize=\"true\", label=\"\"");

			Position2D nodePos = pCG.getGridLocation();
			if(nodePos != null) {
				sbOutput.append(" pos=\"");
				sbOutput.append(nodePos.getX());
				sbOutput.append(",");
				sbOutput.append(nodePos.getY());
				sbOutput.append("\" pin=\"true\"");

				rv = false;
			}

			sbOutput.append("];");

			nodeLines.add(sbOutput.toString());

			if(log.isDebugEnabled()) {
				log.debug("Node line added to DOT file:");
				log.debug(sbOutput.toString());
			}

			for(GroupInterface gi : pCG.getGroupInterfaces()) {
				INetworkInterface ni = gi.getOuterConnection();

				StringBuilder sbIface = new StringBuilder("\t\"");
				if(ni instanceof Interface) {
					Interface iface = (Interface) ni;

					sbIface.append(iface.getComponent().getRDFLocalName());
				}
				else if(ni instanceof VirtualInterface) {
					VirtualInterface vi = (VirtualInterface) ni;

					sbIface.append(vi.getSwitchRDFLocalName());
				}

				sbIface.append("\" -- \"");
				sbIface.append(pCG.getIdentifier());
				sbIface.append("\"");

				edgeLines.add(sbIface.toString());

				if(log.isDebugEnabled()) {
					log.debug("Edge line added to DOT file:");
					log.debug(sbIface.toString());
				}
			}

			return rv;
		}
		else {
			if(log.isDebugEnabled()) {
				log.debug("addNode() called on already closed DOTFile");
			}
			throw new IllegalStateException("Output stream already closed");
		}
	}


	/**
	 * Add a new node line to the output file. Identifier and dimensions are
	 * taken from the provided {@link de.decoit.visa.topology.NCSwitch.GroupSwitch GroupSwitch}. This method must
	 * be called before calling {@link #close()}. Calling it after closing the
	 * file will result in an IllegalStateException.
	 *
	 * @param pGS GroupSwitch object providing the node information
	 * @return true: Object position will be calculated, false: Object position
	 *         was already defined
	 * @throws IOException if writing to output file failed
	 */
	boolean addNode(GroupSwitch pGS) throws IOException {
		if(!closed) {
			boolean rv = true;

			// Construct a new node line with the information found in pNC
			// and write it to the file
			StringBuilder sbOutput = new StringBuilder("\t\"");
			sbOutput.append(pGS.getSwitchRDFLocalName());
			sbOutput.append("\"");
			sbOutput.append(" [width=\"");
			sbOutput.append(pGS.getDragboxDimensions().getX());
			sbOutput.append("\", height=\"");
			sbOutput.append(pGS.getDragboxDimensions().getY());
			sbOutput.append("\", shape=\"rectangle\", fixedsize=\"true\", label=\"\"");

			Position2D nodePos = pGS.getGridLocation();
			if(nodePos != null) {
				sbOutput.append(" pos=\"");
				sbOutput.append(nodePos.getX());
				sbOutput.append(",");
				sbOutput.append(nodePos.getY());
				sbOutput.append("\" pin=\"true\"");

				rv = false;
			}

			sbOutput.append("];");

			nodeLines.add(sbOutput.toString());

			if(log.isDebugEnabled()) {
				log.debug("Node line added to DOT file:");
				log.debug(sbOutput.toString());
			}

			StringBuilder sbEdge = new StringBuilder("\t\"");
			sbEdge.append(pGS.getSwitchRDFLocalName());

			// Connect the group switches to anchors located in the middle of
			// each grid border. The used anchor is selected by the orientation
			// of the virtual interface of the switch
			switch(pGS.getVirtualInterface().getOrientation()) {
				case TOP:
					// Add the anchor to the DOT file if it was not added before
					if(!addedGroupSwitchAnchors.get("topAnchor").booleanValue()) {
						nodeLines.add(groupSwitchAnchors.get("topAnchor"));

						if(log.isDebugEnabled()) {
							log.debug("Node line added to DOT file:");
							log.debug(groupSwitchAnchors.get("topAnchor"));
						}

						addedGroupSwitchAnchors.put("topAnchor", new Boolean(true));
					}

					// Complete the edge line
					sbEdge.append("\" -- \"topAnchor\"");

					break;
				case BOTTOM:
					// Add the anchor to the DOT file if it was not added before
					if(!addedGroupSwitchAnchors.get("bottomAnchor").booleanValue()) {
						nodeLines.add(groupSwitchAnchors.get("bottomAnchor"));

						if(log.isDebugEnabled()) {
							log.debug("Node line added to DOT file:");
							log.debug(groupSwitchAnchors.get("bottomAnchor"));
						}

						addedGroupSwitchAnchors.put("bottomAnchor", new Boolean(true));
					}

					// Complete the edge line
					sbEdge.append("\" -- \"bottomAnchor\"");

					break;
				case LEFT:
					// Add the anchor to the DOT file if it was not added before
					if(!addedGroupSwitchAnchors.get("leftAnchor").booleanValue()) {
						nodeLines.add(groupSwitchAnchors.get("leftAnchor"));

						if(log.isDebugEnabled()) {
							log.debug("Node line added to DOT file:");
							log.debug(groupSwitchAnchors.get("leftAnchor"));
						}

						addedGroupSwitchAnchors.put("leftAnchor", new Boolean(true));
					}

					// Complete the edge line
					sbEdge.append("\" -- \"leftAnchor\"");

					break;
				case RIGHT:
					// Add the anchor to the DOT file if it was not added before
					if(!addedGroupSwitchAnchors.get("rightAnchor").booleanValue()) {
						nodeLines.add(groupSwitchAnchors.get("rightAnchor"));

						if(log.isDebugEnabled()) {
							log.debug("Node line added to DOT file:");
							log.debug(groupSwitchAnchors.get("rightAnchor"));
						}

						addedGroupSwitchAnchors.put("rightAnchor", new Boolean(true));
					}

					// Complete the edge line
					sbEdge.append("\" -- \"rightAnchor\"");

					break;
			}

			edgeLines.add(sbEdge.toString());

			if(log.isDebugEnabled()) {
				log.debug("Edge line added to DOT file:");
				log.debug(sbEdge.toString());
			}

			return rv;
		}
		else {
			if(log.isDebugEnabled()) {
				log.debug("addNode() called on already closed DOTFile");
			}
			throw new IllegalStateException("Output stream already closed");
		}
	}


	/**
	 * Add a new edge line to the output file. Identifiers of the connected
	 * nodes are taken from the provided {@link de.decoit.visa.topology.NetworkCable NetworkCable}. This method must be
	 * called before calling {@link #close()}. Calling it after closing the file
	 * will result in an IllegalStateException.
	 *
	 * @param pNC NetworkCable object providing the two connected nodes, must
	 *            have both ends connected
	 * @throws IOException if writing to output file failed
	 */
	void addEdge(NetworkCable pNC) throws IOException {
		if(!closed) {
			// Construct a new edge line with the information found in pNC
			// and write it to the file
			StringBuilder sbOutput = new StringBuilder("\t\"");
			sbOutput.append(pNC.getLeft().getComponent().getRDFLocalName());
			sbOutput.append("\" -- \"");
			sbOutput.append(pNC.getRight().getComponent().getRDFLocalName());
			sbOutput.append("\"");

			edgeLines.add(sbOutput.toString());

			if(log.isDebugEnabled()) {
				log.debug("Edge line added to DOT file:");
				log.debug(sbOutput.toString());
			}
		}
		else {
			if(log.isDebugEnabled()) {
				log.debug("addEdge() called on already closed DOTFile");
			}
			throw new IllegalStateException("Output stream already closed");
		}
	}


	/**
	 * Add the final "}" to the file and close the output stream. After calling
	 * this method no more writing to the file can occur, causing addNode() and
	 * addEdge() methods to fail.
	 *
	 * @throws IOException if writing to output file failed
	 */
	void close() throws IOException {
		if(!closed) {
			// Write node lines to file
			for(String line : nodeLines) {
				bw.write(line);
				bw.newLine();
				bw.flush();
			}

			// Write edge lines to file
			for(String line : edgeLines) {
				bw.write(line);
				bw.newLine();
				bw.flush();
			}

			// Write the final closing "}" and close the output stream
			bw.write("}");
			bw.close();

			closed = true;

			if(log.isDebugEnabled()) {
				log.debug("DOT file closed");
			}
		}
	}


	/**
	 * Return if the file was already closed.
	 *
	 * @return true if the file was closed, false otherwise
	 */
	public boolean isClosed() {
		return closed;
	}


	/**
	 * Create fixed anchors for group switches. The node lines are stored in a
	 * HashMap for later use. They are not added to the DOT file by this method.
	 *
	 * @param pGridDimensions Dimensions of the editor grid the anchors are
	 *            located in
	 */
	private void createGroupSwitchAnchors(Dimension2D pGridDimensions) {
		int midX = pGridDimensions.getX() / 2;
		int midY = pGridDimensions.getY() / 2;

		StringBuilder sbLeftAnchor = new StringBuilder("\t\"leftAnchor\" [width=\"5\", height=\"5\", shape=\"rectangle\", fixedsize=\"true\", label=\"\" pos=\"");
		sbLeftAnchor.append(5);
		sbLeftAnchor.append(",");
		sbLeftAnchor.append(midY);
		sbLeftAnchor.append("\" pin=\"true\"]");

		StringBuilder sbRightAnchor = new StringBuilder("\t\"rightAnchor\" [width=\"5\", height=\"5\", shape=\"rectangle\", fixedsize=\"true\", label=\"\" pos=\"");
		sbRightAnchor.append(pGridDimensions.getX() - 5);
		sbRightAnchor.append(",");
		sbRightAnchor.append(midY);
		sbRightAnchor.append("\" pin=\"true\"]");

		StringBuilder sbTopAnchor = new StringBuilder("\t\"topAnchor\" [width=\"5\", height=\"5\", shape=\"rectangle\", fixedsize=\"true\", label=\"\" pos=\"");
		sbTopAnchor.append(midX);
		sbTopAnchor.append(",");
		sbTopAnchor.append(5);
		sbTopAnchor.append("\" pin=\"true\"]");

		StringBuilder sbBottomAnchor = new StringBuilder("\t\"bottomAnchor\" [width=\"5\", height=\"5\", shape=\"rectangle\", fixedsize=\"true\", label=\"\" pos=\"");
		sbBottomAnchor.append(midX);
		sbBottomAnchor.append(",");
		sbBottomAnchor.append(pGridDimensions.getY() - 5);
		sbBottomAnchor.append("\" pin=\"true\"]");

		groupSwitchAnchors.put("leftAnchor", sbLeftAnchor.toString());
		groupSwitchAnchors.put("rightAnchor", sbRightAnchor.toString());
		groupSwitchAnchors.put("topAnchor", sbTopAnchor.toString());
		groupSwitchAnchors.put("bottomAnchor", sbBottomAnchor.toString());

		addedGroupSwitchAnchors.put("leftAnchor", new Boolean(false));
		addedGroupSwitchAnchors.put("rightAnchor", new Boolean(false));
		addedGroupSwitchAnchors.put("topAnchor", new Boolean(false));
		addedGroupSwitchAnchors.put("bottomAnchor", new Boolean(false));
	}
}
