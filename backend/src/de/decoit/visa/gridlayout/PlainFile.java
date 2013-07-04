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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.log4j.Logger;
import de.decoit.visa.Dimension2D;
import de.decoit.visa.Position2D;


/**
 * This class reads an output file from graphviz which is in plain format,
 * parses its contents and provides the information as Node objects.
 *
 * @author Thomas Rix
 */
class PlainFile {
	private static Logger log = Logger.getLogger(PlainFile.class.getName());

	private BufferedReader br;
	private HashSet<Node> nodes;
	private Iterator<Node> nodeIterator;
	private double scaleFactor;


	/**
	 * Construct a new PlainFile object using the provided input file and
	 * encoding as source.
	 *
	 * @param pInfile The text file to parse, must be a plain text file in
	 *            graphviz's plain output format
	 * @param pEncoding The encoding of the input file
	 * @param pGridDimensions Dimensions of the grid the components are placed on
	 * @throws IOException if the file cannot be opened or read
	 */
	PlainFile(Path pInfile, Charset pEncoding, Dimension2D pGridDimensions) throws IOException {
		nodes = new HashSet<>();

		if(log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Opening plain file '");
			sb.append(pInfile);
			sb.append("' for reading");
			log.debug(sb.toString());
		}

		// Open an input stream on the target file
		br = Files.newBufferedReader(pInfile, pEncoding);

		// Read the first line of the file
		String line = br.readLine();

		if(log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Read line: ");
			sb.append(line);
			log.debug(sb.toString());
		}

		// Read lines until we reach EOF
		while(line != null) {
			// If it is the graph line, parse the scale factor
			if(line.startsWith("graph")) {
				parseGraphLine(line);
			}
			// If it is a node line, create a new Node object
			else if(line.startsWith("node")) {
				nodes.add(new Node(line, scaleFactor, pGridDimensions));
			}

			// Read next line
			line = br.readLine();

			if(log.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder("Read line: ");
				sb.append(line);
				log.debug(sb.toString());
			}
		}

		// Close the input stream
		br.close();

		// Get an iterator for the nodes
		nodeIterator = nodes.iterator();
	}


	/**
	 * Return the next node in the iterator
	 *
	 * @return The next node in the iterator, null if Iterator.hasNext()==false
	 */
	public Node getNextNode() {
		if(nodeIterator.hasNext()) {
			return nodeIterator.next();
		}
		else {
			return null;
		}
	}


	/**
	 * Parse the graph line of a plain format file and extract the coordinate
	 * scale factor from it.
	 *
	 * @param pLine The graph line in the graphviz plain output format, must
	 *            start with "graph"
	 */
	private void parseGraphLine(String pLine) {
		if(pLine.startsWith("graph")) {
			String[] fragments = pLine.split(" ");

			scaleFactor = Double.parseDouble(fragments[1]);

			if(log.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder("Parsed graph line, scale factor: ");
				sb.append(scaleFactor);
				log.debug(sb.toString());
			}
		}
		else {
			throw new IllegalArgumentException("Provided line was no graph line");
		}
	}


	/**
	 * This class contains information about a graph node as returned by
	 * graphviz
	 *
	 * @author Thomas Rix
	 *
	 */
	static class Node {
		private String localName;
		private Position2D gridLocation;


		/**
		 * Construct a new Node object by parsing a node line from the graphviz
		 * plain output format. Coordinates are multiplied by the scale factor
		 * to reverse the automatic graph scaling done by graphviz.
		 *
		 * @param pLine A node line in the graphviz plain output format, must
		 *            start with "node"
		 * @param pScale Scale factor of the coordinates
		 * @param pGridDimensions Dimensions of the grid the components are placed on
		 */
		private Node(String pLine, double pScale, Dimension2D pGridDimensions) {
			if(pLine.startsWith("node")) {
				String[] fragments = pLine.split(" ");

				localName = fragments[1].replaceAll("\"", "");

				double posX = Math.round(Double.parseDouble(fragments[2]) * pScale);
				double posY = Math.round(Double.parseDouble(fragments[3]) * pScale);

				gridLocation = new Position2D((int) posX, (int) posY, pGridDimensions);
			}
			else {
				throw new IllegalArgumentException("Provided line was no node line");
			}
		}


		/**
		 * Return the local name of this node
		 *
		 * @return The local name of this node
		 */
		public String getLocalname() {
			return localName;
		}


		/**
		 * Return the grid location of this node
		 *
		 * @return The grid location of this node
		 */
		public Position2D getGridLocation() {
			return gridLocation;
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((gridLocation == null) ? 0 : gridLocation.hashCode());
			result = prime * result + ((localName == null) ? 0 : localName.hashCode());
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			else if(obj instanceof Node) {
				Node n = (Node) obj;

				return (n.gridLocation.equals(gridLocation) && n.localName.equals(localName));
			}
			else {
				return false;
			}
		}
	}
}
