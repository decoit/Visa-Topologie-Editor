package de.decoit.visa.gridlayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import de.decoit.visa.Dimension2D;
import de.decoit.visa.TEBackend;
import de.decoit.visa.topology.NCSwitch;
import de.decoit.visa.topology.NCSwitch.GroupSwitch;
import de.decoit.visa.topology.NetworkCable;
import de.decoit.visa.topology.NetworkComponent;
import de.decoit.visa.topology.TopologyStorage;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup;


/**
 * This class allows layout of the components in a topology by using one of the
 * graphviz command line tools. It is optimized for using fdp or neato but all
 * graphviz executables are supported.
 *
 * @author Thomas Rix
 */
public class GridLayout {
	private static Logger log = Logger.getLogger(GridLayout.class.getName());

	private Path layoutInput;		// graphviz input file
	private Path layoutOutput;		// graphviz output file
	private Charset encoding;
	private DOTFile inputFile;
	private PlainFile outputFile;
	private HashSet<String> localNames;
	private Dimension2D gridDimensions;
	private boolean groupSwitchAdded;
	private HashMap<String, String> groupSwitches;


	/**
	 * Construct a new GridLayout object. Creates Path objects for graphviz
	 * input and output files. Files have the local name of the RDF root node as
	 * name and the extensions ".dot" (input file) or ".gv" (output file). They
	 * are located in the subfolder "layout". The input file will be created and
	 * filled with header information.
	 *
	 * @param pGridDimensions Dimensions of the grid
	 * @throws IOException if graphviz input file cannot be opened or written to
	 */
	public GridLayout(Dimension2D pGridDimensions) throws IOException {
		localNames = new HashSet<>();

		gridDimensions = pGridDimensions;
		groupSwitchAdded = false;
		groupSwitches = new HashMap<>();

		// Check if the layout directory exists and if not, create it
		Path layoutPath = Paths.get("layout");
		if(!Files.exists(layoutPath)) {
			Files.createDirectory(layoutPath);
		}
		Set<PosixFilePermission> attrSet = PosixFilePermissions.fromString("rwxrwxrwx");
		Files.setPosixFilePermissions(layoutPath, attrSet);

		// Create Path object for input file
		StringBuffer sbIn = new StringBuffer(TEBackend.RDF_MANAGER.getRootNodeLocalName());
		sbIn.append(".dot");
		layoutInput = layoutPath.resolve(sbIn.toString());

		// Create Path object for output file
		StringBuffer sbOut = new StringBuffer(TEBackend.RDF_MANAGER.getRootNodeLocalName());
		sbOut.append(".gv");
		layoutOutput = layoutPath.resolve(sbOut.toString());

		// Get charset
		encoding = StandardCharsets.UTF_8;

		// Create the input file
		inputFile = new DOTFile(layoutInput, encoding, gridDimensions);
	}


	/**
	 * Use the provided NetworkComponent object to add a node to the DOT file
	 *
	 * @param pNC NetworkComponent providing the node information
	 * @return true if adding the component was successful, false otherwise
	 * @throws IOException if writing to the DOT file failed
	 */
	public boolean addComponent(NetworkComponent pNC) throws IOException {
		boolean rv = inputFile.addNode(pNC);

		// If the position will be calculated, add the local name to the list of
		// positioned nodes
		if(rv) {
			localNames.add(pNC.getRDFLocalName());
		}

		return rv;
	}


	/**
	 * Use the provided NetworkGroup object to add a node to the DOT file
	 *
	 * @param pCG NetworkGroup providing the node information
	 * @return true if adding the component group was successful, false
	 *         otherwise
	 * @throws IOException if writing to the DOT file failed
	 */
	public boolean addComponentGroup(TopologyStorage.ComponentGroup pCG) throws IOException {
		boolean rv = inputFile.addNode(pCG);

		// If the position will be calculated, add the local name to the list of
		// positioned nodes
		if(rv) {
//			StringBuilder sb = new StringBuilder("netgrp_");
//			sb.append(pCG.getName());
			localNames.add(pCG.getIdentifier());
		}

		return rv;
	}


	/**
	 * Use the provided GroupSwitch object to add a node to the DOT file
	 *
	 * @param pGS GroupSwitch providing the node information
	 * @return true if adding the group switch was successful, false otherwise
	 * @throws IOException if writing to the DOT file failed
	 */
	public boolean addGroupSwitch(GroupSwitch pGS) throws IOException {
		boolean rv = inputFile.addNode(pGS);

		// If the position will be calculated, add the local name to the list of
		// positioned nodes
		if(rv) {
			localNames.add(pGS.getSwitchRDFLocalName());

			groupSwitches.put(pGS.getSwitchRDFLocalName(), pGS.getGroupName());

			groupSwitchAdded = true;
		}

		return rv;
	}


	/**
	 * Use the provided NetworkCable object to add an edge to the DOT file
	 *
	 * @param pNC NetworkCable providing the edge information, must have both
	 *            ends connected
	 * @throws IOException if writing to the DOT file failed
	 */
	public void addCable(NetworkCable pNC) throws IOException {
		inputFile.addEdge(pNC);
	}


	/**
	 * Run the grid location calculation. This will close the graphviz input
	 * file, run the specified executable and reconfigure the grid locations of
	 * all NetworkComponents which were added before.
	 *
	 * @param pCmd Name of the graphviz executable to use
	 * @throws IOException if file operations or execution of graphviz failed or
	 *             the DOT file contained syntax errors
	 */
	public void run(String pCmd) throws IOException {
		// Close the input file
		inputFile.close();

		// Run the graphviz executable
		runGraphviz(pCmd);

		// Open output file and parse the properties returned by graphviz
		outputFile = new PlainFile(layoutOutput, encoding, gridDimensions);

		// Iterate over all nodes and reconfigure the components
		PlainFile.Node node = outputFile.getNextNode();
		while(node != null) {
			if(node.getLocalname().startsWith("cgroup")) {
				// If the current node is in our list of processed objects,
				// relocate it
				if(localNames.contains(node.getLocalname())) {
					ComponentGroup ng = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByID(node.getLocalname());

					int x = node.getGridLocation().getX() - (ng.getDragboxDimensions().getX() / 2);
					int y = node.getGridLocation().getY() - (ng.getDragboxDimensions().getY() / 2);

					// Prevent positions outside the editor grid
					if(x < 0) {
						x = 0;
					}
					else if(x >= (gridDimensions.getX() - ng.getDragboxDimensions().getX())) {
						x = gridDimensions.getX() - ng.getDragboxDimensions().getX() - 1;
					}

					if(y < 0) {
						y = 0;
					}
					else if(y >= (gridDimensions.getY() - ng.getDragboxDimensions().getY())) {
						y = gridDimensions.getY() - ng.getDragboxDimensions().getY() - 1;
					}

					ng.setGridLocation(x, y);
				}
			}
			else {
				// If the current node is in our list of processed objects,
				// relocate it
				if(localNames.contains(node.getLocalname())) {
					NetworkComponent nc = TEBackend.TOPOLOGY_STORAGE.getComponent(node.getLocalname());

					String group = groupSwitches.get(nc.getRDFLocalName());
					if(groupSwitchAdded && group != null) {
						NCSwitch ncs = (NCSwitch) nc;
						GroupSwitch gs = ncs.getGroupSwitch(group);

						int x = node.getGridLocation().getX() - (gs.getDragboxDimensions().getX() / 2);
						int y = node.getGridLocation().getY() - (gs.getDragboxDimensions().getY() / 2);

						// Prevent positions outside the editor grid
						if(x < 0) {
							x = 0;
						}
						else if(x >= (gridDimensions.getX() - gs.getDragboxDimensions().getX())) {
							x = gridDimensions.getX() - gs.getDragboxDimensions().getX() - 1;
						}

						if(y < 0) {
							y = 0;
						}
						else if(y >= (gridDimensions.getY() - gs.getDragboxDimensions().getY())) {
							y = gridDimensions.getY() - gs.getDragboxDimensions().getY() - 1;
						}

						gs.setGridLocation(x, y);
					}
					else {
						int x = node.getGridLocation().getX() - (nc.getConfig().getDragboxDimensions().getX() / 2);
						int y = node.getGridLocation().getY() - (nc.getConfig().getDragboxDimensions().getY() / 2);

						// Prevent positions outside the editor grid
						if(x < 0) {
							x = 0;
						}
						else if(x >= (gridDimensions.getX() - nc.getConfig().getDragboxDimensions().getX())) {
							x = gridDimensions.getX() - nc.getConfig().getDragboxDimensions().getX() - 1;
						}

						if(y < 0) {
							y = 0;
						}
						else if(y >= (gridDimensions.getY() - nc.getConfig().getDragboxDimensions().getY())) {
							y = gridDimensions.getY() - nc.getConfig().getDragboxDimensions().getY() - 1;
						}

						nc.getConfig().setGridLocation(x, y);
					}
				}
			}

			node = outputFile.getNextNode();
		}
	}


	/**
	 * Lookup the absolute path of the provided graphviz executable using which
	 * and then execute the program with options -s72.0 -y -Tplain. Input and
	 * output files are read from class attributes. The name of the executable
	 * must match one of the following (executables included in graphviz
	 * distribution):
	 * dot, neato, fdp, sfdp, twopi, circo
	 *
	 * @param pCmd Name of the graphviz executable, must be available on the
	 *            system
	 * @throws IOException if execution of which or the executable failed or the
	 *             DOT file contained syntax errors
	 */
	private void runGraphviz(String pCmd) throws IOException {
		// Check if the provided executable is part of the graphviz distribution
		if(pCmd.equals("dot") || pCmd.equals("neato") || pCmd.equals("fdp") || pCmd.equals("sfdp") || pCmd.equals("twopi") || pCmd.equals("circo")) {
			// Construct the command string for "which"
			StringBuilder whichCmd = new StringBuilder("which ");
			whichCmd.append(pCmd);

			// Run "which" to find the absolute path to the executable
			Runtime run = Runtime.getRuntime();
			Process whichProc = run.exec(whichCmd.toString());

			try {
				// Wait for "which" to finish
				whichProc.waitFor();
			}
			catch(InterruptedException ex) { /* Ignore InterruptedException */
			}

			// Read the output of "which"
			BufferedReader whichReader = new BufferedReader(new InputStreamReader(whichProc.getInputStream()));
			String gvLocation = whichReader.readLine();
			whichReader.close();

			// Output is an absolute path, if it starts with "which" the
			// command failed
			if(gvLocation != null && !gvLocation.startsWith("which")) {
				// Construct the command string for the provided graphviz
				// executable
				StringBuilder gvCmd = new StringBuilder(gvLocation);
				gvCmd.append(" -s72.0 -y -Tplain -o");
				gvCmd.append(layoutOutput.toString());
				gvCmd.append(" ");
				gvCmd.append(layoutInput.toString());

				// Run graphviz executable
				Process gvProc = run.exec(gvCmd.toString());

				try {
					// Wait for graphviz to finish
					gvProc.waitFor();
				}
				catch(InterruptedException ex) {
					/* Ignore InterruptedException */
				}

				// Read the output of graphviz into an ArrayList
				BufferedReader gvReader = new BufferedReader(new InputStreamReader(gvProc.getInputStream()));
				ArrayList<String> gvOutput = new ArrayList<>();
				String line = gvReader.readLine();
				while(line != null) {
					gvOutput.add(line);
					line = gvReader.readLine();
				}
				gvReader.close();

				// If graphviz produced any output, check if an error
				// occured and output that error to the log file
				if(gvOutput.size() > 0 && gvOutput.get(0).startsWith("Error:")) {
					StringBuilder sb = new StringBuilder("Graphviz (");
					sb.append(pCmd);
					sb.append(") error:");
					log.error(sb.toString());

					for(String msg : gvOutput) {
						log.error(msg);
					}

					throw new IOException("Error during graphviz call");
				}
			}
			else {
				throw new IllegalArgumentException("Provided executable not found on the system");
			}
		}
		else {
			throw new IllegalArgumentException("Provided executable is not part of the graphviz distribution");
		}
	}
}
