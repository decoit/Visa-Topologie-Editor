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

package de.decoit.visa.rdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import de.decoit.visa.Dimension2D;
import de.decoit.visa.TEBackend;
import de.decoit.visa.enums.IPVersion;
import de.decoit.visa.enums.PortOrientation;
import de.decoit.visa.interfaces.IRDFObject;
import de.decoit.visa.net.IPNetwork;
import de.decoit.visa.topology.NCSwitch;
import de.decoit.visa.topology.NCVM;
import de.decoit.visa.topology.NetworkComponent;
import de.decoit.visa.topology.NetworkComponent.Interface;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup;
import de.decoit.visa.topology.TopologyStorage.ComponentGroup.GroupInterface;
import de.decoit.visa.topology.VLAN;


/**
 * This class manages the RDF information of the topology. The information is
 * stored in a TDB database at a location specified at object creation. The
 * dataset's default model stores the current topology RDF model. Additionally a
 * named model with the URI 'http://visa-project.de/backup/model' is used for
 * storage of all statements that use the {@link VISABackup VISABackup}
 * namespace. RDF information from uploaded and imported RDF/XML files is stored
 * in named models using URIs following the scheme
 * 'http://visa-project.de/topology/model/&lt;model_name&gt;', where
 * &lt;model_name&gt; is replaced by the name of the uploaded file.
 *
 * @author Thomas Rix
 */
public class RDFManager {
	private static Logger log = Logger.getLogger(RDFManager.class.getName());

	private Dataset ds;
	private Resource rootNode;
	private List<Path> source;
	private List<Document> vsaTemplates;
	private Model activeNamedModel;


	/**
	 * Construct a new RDFManager object. It will open and clear the TDB
	 * database at the specified location if it exists. Otherwise a new database
	 * will be created. The program must have read and write access to the
	 * database location.
	 *
	 * @param pLocation The TDB database will be opened at this location
	 * @throws IOException if the VSA template directory is not accessible
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public RDFManager(String pLocation) throws IOException, ParserConfigurationException, SAXException {
		source = new ArrayList<>();
		vsaTemplates = new ArrayList<>();
		activeNamedModel = null;

		// Load a list of available VSA templates
		DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get("res/vsa"), "*.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		for(Path p : dirStream) {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(Files.newInputStream(p, StandardOpenOption.READ));

			doc.getDocumentElement().normalize();

			vsaTemplates.add(doc);
		}

		// Create or load the TDB database at pLocation
		ds = TDBFactory.createDataset(pLocation);

		ds.begin(ReadWrite.WRITE);

		try {
			// Do some cleanup if last run didn't clear the database
			// Remove all named models from the database
			Iterator<String> itNames = ds.listNames();
			ArrayList<String> names = new ArrayList<>();

			while(itNames.hasNext()) {
				names.add(itNames.next());
			}

			for(String n : names) {
				ds.removeNamedModel(n);
			}

			// Clear the default model
			ds.getDefaultModel().removeAll();

			ds.commit();
		}
		catch(Throwable ex) {
			ds.abort();
		}
		finally {
			ds.end();

			// Sync changes to disk
			TDB.sync(ds);
		}
	}


	/**
	 * Remove all named models and statements in the default model from the TDB
	 * database and delete all uploaded source files. After cleaning up close
	 * the connection to the TDB database.
	 *
	 * @throws IOException
	 */
	public void close() throws IOException {
		ds.begin(ReadWrite.WRITE);

		try {
			clearSourceFiles();

			// Remove all named models from the database that were not removed
			// by clearSourceFiles()
			Iterator<String> itNames = ds.listNames();
			ArrayList<String> names = new ArrayList<>();

			while(itNames.hasNext()) {
				names.add(itNames.next());
			}

			for(String n : names) {
				ds.removeNamedModel(n);
			}

			// Clear the default model
			ds.getDefaultModel().removeAll();

			ds.commit();
		}
		catch(Throwable ex) {
			StringBuilder sb = new StringBuilder("Caught: [");
			sb.append(ex.getClass().getSimpleName());
			sb.append("] ");
			sb.append(ex.getMessage());
			log.error(sb.toString());

			if(log.isDebugEnabled()) {
				for(StackTraceElement ste : ex.getStackTrace()) {
					log.debug(ste.toString());
				}
			}

			ds.abort();

			throw ex;
		}
		finally {
			ds.end();

			// Sync changes to disk
			TDB.sync(ds);

			// Close the TDB database connection
			ds.close();
		}
	}


	/**
	 * Delete all existing uploaded source files and create a new empty RDF
	 * model. The root node will be named by the provided name. It will use
	 * makeLocalName() to remove any characters restricted by URI definition.
	 *
	 * @param pName The name of the root node
	 * @throws IOException
	 */
	public void createEmptyModel(String pName) throws IOException {
		ds.begin(ReadWrite.WRITE);

		try {
			Model mod = ds.getDefaultModel();
			mod.removeAll();

			// Delete all uploaded files (if any) and clear the source file list
			clearSourceFiles();

			// Remove restricted characters from the name string
			String rootLocalName = makeLocalName(pName);

			// Create the new RDF nodes
			Resource newRootNode = VISA.createResource(rootLocalName);
			Literal newNameLit = mod.createLiteral(pName);

			// Add the statement to the model
			mod.add(newRootNode, VISA.NAME, newNameLit);

			// Update the root node
			rootNode = newRootNode;

			ds.commit();
		}
		catch(Throwable ex) {
			ds.abort();

			throw ex;
		}
		finally {
			ds.end();
			TDB.sync(ds);
		}
	}


	/**
	 * Load a RDF/XML file into a named model. The file will be read from the
	 * specified location. It can be specified if the existing model will be
	 * replaced or the new data will be appended to the current model.
	 *
	 * @param pFile Path and file name of the source file
	 * @param pReplace Specify if the new data will replace the existing model
	 *            or if it will be appended to the model
	 * @throws IOException
	 * @throws RDFSourceException
	 */
	public void loadRDF(Path pFile, boolean pReplace) throws IOException, RDFSourceException {
		InputStream is = Files.newInputStream(pFile);

		if(pReplace) {
			source = new ArrayList<>();
			source.add(pFile);
		}
		else {
			source.add(pFile);
		}

		loadRDF(is, pReplace, pFile.getFileName().toString());
	}


	/**
	 * Load RDF/XML data into a named model. The model will be read from the
	 * specified input stream. It can be specified if the existing model will be
	 * replaced or the new data will be appended to the current model.
	 *
	 * @param pIS Input stream to read the model from
	 * @param pReplace Specify if the new data will replace the existing model
	 *            or if it will be appended to the model
	 * @param pModelName The local name of the model the RDF information will be
	 *            stored into
	 * @throws IOException
	 * @throws RDFSourceException
	 */
	public void loadRDF(InputStream pIS, boolean pReplace, String pModelName) throws IOException, RDFSourceException {
		ds.begin(ReadWrite.WRITE);

		try {
			activeNamedModel = ds.getNamedModel(VISA.createModelURI(pModelName));

			// If the model contains statements, clear it before importing the
			// new statements
			if(!activeNamedModel.isEmpty()) {
				activeNamedModel.removeAll();
			}

			// Read the RDF file into the model
			activeNamedModel.read(pIS, null);

			HashSet<String> addedLocNames = new HashSet<>();

			if(pReplace) {
				// Clear the topology storage
				TEBackend.TOPOLOGY_STORAGE.clear();

				// Clear the default model
				ds.getDefaultModel().removeAll();

				// Get the root node (the node which is subject of device
				// properties) of this model
				QueryExecution qexec = QueryExecutionFactory.create(getRootNodeSPARQL(VISA.createModelURI(pModelName)), ds);
				ArrayList<QuerySolution> rsList = resultSetToList(qexec.execSelect());

				if(rsList.size() == 1) {
					rootNode = rsList.get(0).getResource("root");
				}
				else if(rsList.size() > 1) {
					activeNamedModel.write(Files.newOutputStream(Paths.get("error_dump.rdf"), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
					throw new RDFSourceException("Multiple VSA (root) nodes found in RDF/XML file");
				}
				else {
					throw new RDFSourceException("No VSA (root) node found in RDF/XML file");
				}
			}
			else {
				// Alter the local names of the nodes
				preventLocalNameCollisions(VISA.createModelURI(pModelName));

				// Alter the root node to fit the root node of the current model
				alterRootNode(VISA.createModelURI(pModelName), rootNode);
			}

			// Process data stored in the model and create topology objects
			// from it
			addedLocNames = processModel(VISA.createModelURI(pModelName));

			// Insert the new model into the existing one
			ds.getDefaultModel().add(activeNamedModel);

			// Layout the topology
			TEBackend.TOPOLOGY_STORAGE.layoutTopology();

			TEBackend.TOPOLOGY_STORAGE.updateInterfaceOrientations(addedLocNames);

			ds.commit();
		}
		catch(Throwable ex) {
			ds.abort();

			throw ex;
		}
		finally {
			activeNamedModel = null;

			ds.end();
			TDB.sync(ds);

			pIS.close();
		}
	}


	/**
	 * Import the contents of a VSA template into the topology. The devices of
	 * the VSA will be grouped into a group with the specified name. Connections
	 * between VSA and existing topology are created according to the user's
	 * input.
	 *
	 * @param pTplID ID number of the template
	 * @param pGroupName Name of the VSA group
	 * @param pConnTargets Information about connections to the existing
	 *            topology
	 * @param pConnVLANs Information about VLAN assignment of new interfaces
	 * @throws IOException if the template RDF/XML file cannot be accessed
	 * @throws RDFSourceException if the RDF/XML file contains errors
	 */
	public void importRDFTemplate(int pTplID, String pGroupName, Map<String, String> pConnTargets, Map<String, String> pConnVLANs) throws IOException, RDFSourceException {
		ComponentGroup vsaCG = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(pGroupName);

		Document tpl = vsaTemplates.get(pTplID);
		Element vsaElement = (Element) (tpl.getElementsByTagName("VSA").item(0));

		Path vsaRDF = Paths.get("res/vsa", vsaElement.getAttribute("rdf"));

		InputStream is = Files.newInputStream(vsaRDF);
		String modURI = VISA.createModelURI(vsaCG.getIdentifier());

		ds.begin(ReadWrite.WRITE);

		try {
			activeNamedModel = ds.getNamedModel(modURI);

			// If the model contains statements, clear it before importing the
			// new statements
			if(!activeNamedModel.isEmpty()) {
				activeNamedModel.removeAll();
			}

			// Read the RDF file into the model
			activeNamedModel.read(is, null);

			// Remove existing grouping information from the template
			List<RDFNode> cgList = activeNamedModel.listObjectsOfProperty(VISABackup.GROUP).toList();
			for(RDFNode node : cgList) {
				if(node.isResource()) {
					// If the group node is a resource, remove the name literal
					// connected to it
					Resource res = (Resource) node;

					activeNamedModel.removeAll(res, VISA.NAME, null);
				}

				activeNamedModel.removeAll(null, VISABackup.GROUP, node);
			}

			// Remove network information from the model
			List<RDFNode> netList = activeNamedModel.listObjectsOfProperty(VISA.NETWORK).toList();
			for(RDFNode node : netList) {
				if(node.isResource()) {
					Resource res = (Resource) node;

					activeNamedModel.removeAll(res, VISA.INTERNAL_NAME, null);
					activeNamedModel.removeAll(res, VISA.TYPE, null);
					activeNamedModel.removeAll(res, VISA.VALUE, null);
					activeNamedModel.removeAll(res, VISA.NETMASK_LENGTH, null);

					activeNamedModel.removeAll(null, VISA.NETWORK, res);
				}
			}

			// Remove address information from the model
			List<RDFNode> ifList = activeNamedModel.listObjectsOfProperty(VISA.ADDRESS).toList();
			for(RDFNode node : ifList) {
				if(node.isResource()) {
					Resource res = (Resource) node;

					activeNamedModel.removeAll(res, VISA.INTERNAL_NAME, null);
					activeNamedModel.removeAll(res, VISA.TYPE, null);
					activeNamedModel.removeAll(res, VISA.VALUE, null);

					activeNamedModel.removeAll(null, VISA.ADDRESS, res);
				}
			}

			// Add new grouping information to model
			List<RDFNode> devList = activeNamedModel.listObjectsOfProperty(VISA.DEVICE).toList();
			for(RDFNode node : devList) {
				if(node.isResource()) {
					Resource devRes = (Resource) node;

					StringBuilder sbURI = new StringBuilder(VISABackup.getURI());
					sbURI.append(vsaCG.getIdentifier());
					Resource cgRes = activeNamedModel.getResource(sbURI.toString());

					activeNamedModel.add(devRes, VISABackup.GROUP, cgRes);
					activeNamedModel.add(cgRes, VISABackup.NAME, vsaCG.getName());
				}
			}

			// Alter the local names of the nodes
			String lnSuffix = preventLocalNameCollisions(modURI);

			// Alter the root node to fit the root node of the current model
			alterRootNode(modURI, rootNode);

			// Process data stored in the model and create topology objects
			// from it
			HashSet<String> addedLocNames = processModel(modURI);

			// Insert the new model into the existing one
			ds.getDefaultModel().add(activeNamedModel);
			activeNamedModel = null;

			int routerID = 0;
			for(Map.Entry<String, String> connEntry : pConnTargets.entrySet()) {
				Element e = tpl.getElementById(connEntry.getKey());
				StringBuilder sbSrc = new StringBuilder(e.getAttribute("component"));
				sbSrc.append(lnSuffix);

				if(e.hasAttribute("vlan")) {
					StringBuilder sbVLAN = new StringBuilder(e.getAttribute("vlan"));
					sbVLAN.append(lnSuffix);
				}

				// NetworkComponent inside the VSA
				NetworkComponent ncSrc = TEBackend.TOPOLOGY_STORAGE.getComponent(sbSrc.toString());

				// NetworkComponent in the existing topology
				NetworkComponent ncTarget = TEBackend.TOPOLOGY_STORAGE.getComponent(connEntry.getValue());
				addedLocNames.add(ncTarget.getRDFLocalName());

				// Interface inside the VSA (source)
				Interface ifSrc = ncSrc.getConfig().createInterface(PortOrientation.TOP);

				// Interface in the existing topology (target)
				Interface ifTarget = ncTarget.getConfig().createInterface(PortOrientation.TOP);

				GroupInterface gIf = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(pGroupName).createOuterConnection(ifSrc, ifTarget);
				TEBackend.TOPOLOGY_STORAGE.createCable(ifSrc, ifTarget, gIf);

				// Check if the target component is a switch and the connection is part of a VLAN
				if(ncTarget instanceof NCSwitch && pConnVLANs.containsKey(connEntry.getKey())) {
					HashSet<VLAN> vlan = ifTarget.getAllVLAN();

					// Add the VLAN to the target interface
					vlan.add(TEBackend.TOPOLOGY_STORAGE.getVLAN(pConnVLANs.get(connEntry.getKey())));
					ifTarget.setVLAN(vlan);
				}

				// If a target VLAN inside the VSA is specified, create a router VM to connect the topology with that VLAN
				if(e.hasAttribute("vlan") && e.getAttribute("vlan").length() > 0) {
					StringBuilder sbVLAN = new StringBuilder(e.getAttribute("vlan"));
					sbVLAN.append(lnSuffix);

					ArrayList<String> ifOrientation = new ArrayList<>();
					ifOrientation.add(PortOrientation.TOP.toString());
					ifOrientation.add(PortOrientation.TOP.toString());
					StringBuilder sbRtName = new StringBuilder("VSA Router ");
					sbRtName.append(routerID);

					NCVM router = TEBackend.TOPOLOGY_STORAGE.createVM(ifOrientation, sbRtName.toString(), null, null);
					router.getConfig().setComponentGroup(vsaCG.getName());
					HashMap<String, Interface> ifMap = router.getConfig().getPorts();
					Set<String> ifMapKeySet = ifMap.keySet();
					Iterator<String> it = ifMapKeySet.iterator();

					// Configure the interface connected to the VSA
					Interface ifInt = ncSrc.getConfig().createInterface(PortOrientation.TOP);
					Interface rtIfInt = ifMap.get(it.next());
					TEBackend.TOPOLOGY_STORAGE.createCable(ifInt, rtIfInt, null);

					VLAN intVLAN = TEBackend.TOPOLOGY_STORAGE.getVLAN(sbVLAN.toString());
					HashSet<VLAN> ifIntVLANs = new HashSet<>();
					ifIntVLANs.add(intVLAN);
					ifInt.setVLAN(ifIntVLANs);

					// Configure the interface connected to the topology
					Interface ifExt = ncSrc.getConfig().createInterface(PortOrientation.TOP);
					Interface rtIfExt = ifMap.get(it.next());
					TEBackend.TOPOLOGY_STORAGE.createCable(ifExt, rtIfExt, null);

					if(pConnVLANs.containsKey(connEntry.getKey())) {
						VLAN extVLAN = TEBackend.TOPOLOGY_STORAGE.getVLAN(pConnVLANs.get(connEntry.getKey()));
						HashSet<VLAN> ifExtVLANs = new HashSet<>();
						ifExtVLANs.add(extVLAN);
						ifExt.setVLAN(ifExtVLANs);
					}
				}
			}

			// Layout the topology
			TEBackend.TOPOLOGY_STORAGE.layoutTopology();

			TEBackend.TOPOLOGY_STORAGE.updateInterfaceOrientations(addedLocNames);

			ds.commit();
		}
		catch(Throwable ex) {
			ds.abort();

			throw ex;
		}
		finally {
			activeNamedModel = null;

			ds.end();
			TDB.sync(ds);

			is.close();
		}
	}


	/**
	 * Restore the contents of the RDF model to a saved state. Only information
	 * stored in uploaded files will be restored at this time.
	 *
	 * @param pState ID number of the state which will be restored
	 * @throws RDFSourceException if source file contains invalid RDF
	 *             information
	 * @throws IOException
	 */
	public void restore(int pState) throws RDFSourceException, IOException {
		if(pState >= 0 && pState < source.size()) {
			ds.begin(ReadWrite.WRITE);

			try {
				// Check if the provided state ID points to a valid list index

				// Backup the old source file list
				List<Path> oldSource = source;

				// Get a sub list with only the needed source files
				source = new ArrayList<>(oldSource.subList(0, pState + 1));

				// Iterate over the temporary source file list and add all files
				// to the model
				boolean replace = true;
				for(Path f : source) {
					this.loadRDF(f, replace);
					replace = false;
				}

				// Delete all source files that are no longer needed
				for(int i = pState + 1; i < oldSource.size(); i++) {
					Files.delete(oldSource.get(i));

					ds.removeNamedModel(VISA.createModelURI(oldSource.get(i).getFileName().toString()));
				}

				ds.commit();
			}
			catch(Throwable ex) {
				ds.abort();

				throw ex;
			}
			finally {
				ds.end();
				TDB.sync(ds);
			}
		}
		else {
			throw new IllegalArgumentException("Invalid state ID provided");
		}
	}


	/**
	 * Write the RDF model to a RDF/XML file. The output file will be created at
	 * the specified location.
	 *
	 * @param pFile File object with path and file name of the output file
	 * @throws IOException
	 */
	public void writeRDF(Path pFile) throws IOException {
		OutputStream os = Files.newOutputStream(pFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		writeRDF(os);

		Set<PosixFilePermission> attrSet = PosixFilePermissions.fromString("rwxrwxrwx");
		Files.setPosixFilePermissions(pFile, attrSet);
	}


	/**
	 * Write the RDF model to an output stream.
	 *
	 * @param pOS Output stream the model will be written to
	 * @throws IOException
	 */
	public void writeRDF(OutputStream pOS) throws IOException {
		ds.begin(ReadWrite.READ);

		ds.getDefaultModel().write(pOS);
		pOS.close();

		ds.end();
	}


	/**
	 * Add an instance of the IRDFObject interface to the RDF model. If the
	 * provided object is a NetworkComponent instance all of its ports will be
	 * added by using this method. A provided NetworkPort instance will be added
	 * including the 'address' node if IP configuration is present in the port
	 * object. It will be connected to its parent component by the "interface"
	 * property, using the interface node as object. Connections to other ports
	 * will be created using the "connected" property. These will be created in
	 * both directions.
	 *
	 * @param pObj The object to be added to the RDF model
	 */
	public void addObject(IRDFObject pObj) {
		boolean startTransaction = !ds.isInTransaction();

		// Only do transaction management if no file is being parsed at the
		// moment
		if(startTransaction) {
			ds.begin(ReadWrite.WRITE);
		}

		try {
			Model mod = ds.getDefaultModel();

			StringBuilder sbRes = new StringBuilder(VISA.getURI());
			sbRes.append(pObj.getRDFLocalName());
			Resource res = mod.createResource(sbRes.toString());

			if(pObj instanceof NetworkComponent) {
				NetworkComponent nc = (NetworkComponent) pObj;
				mod.add(rootNode, VISA.DEVICE, res);

				// Add statement for type property, switches have type "switch",
				// all others have "vm"
				if(nc instanceof NCSwitch) {
					mod.add(res, VISA.TYPE, VISA.DEV_TYPE_SWITCH);
				}
				else {
					mod.add(res, VISA.TYPE, VISA.DEV_TYPE_HOST);
				}

				// The topology editor does only add VMs to the model, so all
				// added components get a "physical" property with value "false"
				mod.add(res, VISA.PHYSICAL, VISA.BOOL_FALSE);

				// Add statement for name property
				mod.add(res, VISA.NAME, mod.createLiteral(nc.getName()));

				for(Map.Entry<String, NetworkComponent.Interface> ifEntry : nc.getConfig().getPorts().entrySet()) {
					TEBackend.RDF_MANAGER.addObject(ifEntry.getValue());
				}
			}
			else if(pObj instanceof NetworkComponent.Interface) {
				NetworkComponent.Interface np = (NetworkComponent.Interface) pObj;

				// Add "name" property
				mod.add(res, VISA.NAME, mod.createLiteral(np.getRDFLocalName()));

				// If IP is configured on this port add an "address" property
				for(NetworkComponent.Interface.IPConfig conf : np.getAllIPConfig()) {
					Resource addrRes = createAddressResource(conf.getAddress().getAddressString(), conf.getSubnet(), conf.getVersion(), conf.getNetwork().getNetworkAddressString());
					mod.add(res, VISA.ADDRESS, addrRes);
				}

				// Add "interface" property to parent component node
				StringBuilder sbParent = new StringBuilder(VISA.getURI());
				sbParent.append(np.getComponent().getRDFLocalName());
				Resource parentComponent = mod.getResource(sbParent.toString());
				mod.add(parentComponent, VISA.INTERFACE, res);

				// If a cable is connected add "connected" properties for both
				// ends
				if(np.isConnected()) {
					// Get the port on the other end of the cable
					NetworkComponent.Interface remotePort = null;
					switch(np.getCableEnd()) {
						case LEFT:
							remotePort = np.getCable().getRight();
							break;
						case RIGHT:
							remotePort = np.getCable().getLeft();
							break;
						default:
							break;
					}

					StringBuilder sbRemote = new StringBuilder(VISA.getURI());
					sbRemote.append(remotePort.getRDFLocalName());
					Resource remoteRes = mod.getResource(sbRemote.toString());

					mod.add(res, VISA.CONNECTED, remoteRes);
					mod.add(remoteRes, VISA.CONNECTED, res);
				}
			}

			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.commit();
			}
		}
		catch(Throwable ex) {
			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.abort();
			}

			throw ex;
		}
		finally {
			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.end();
				TDB.sync(ds);
			}
		}
	}


	/**
	 * Update a specific property of an object. The old property statement will
	 * be removed and the new one will be added.
	 *
	 * @param pObj Object whose property shall be updated
	 * @param pProp Property which will be updated
	 */
	public void updateProperty(IRDFObject pObj, Property pProp) {
		boolean startTransaction = !ds.isInTransaction();

		// Only do transaction management if no file is being parsed at the
		// moment
		if(startTransaction) {
			ds.begin(ReadWrite.WRITE);
		}

		try {
			Model mod;
			if(activeNamedModel == null) {
				mod = ds.getDefaultModel();
			}
			else {
				mod = activeNamedModel;
			}

			Model backupMod = ds.getNamedModel(VISABackup.createModelURI());

			StringBuilder sbObj = new StringBuilder(VISA.getURI());
			sbObj.append(pObj.getRDFLocalName());
			Resource obj = mod.getResource(sbObj.toString());

			if(pProp.equals(VISA.NAME)) {
				// Update "name" property on the object
				// Allowed for: NetworkComponent, NetworkComponent.Interface
				Literal name;

				// Create the new name literal
				if(pObj instanceof NetworkComponent) {
					name = mod.createLiteral(((NetworkComponent) pObj).getName());
				}
				else if(pObj instanceof NetworkComponent.Interface) {
					name = mod.createLiteral(pObj.getRDFLocalName());
				}
				else {
					throw new IllegalArgumentException("Provided object for 'name' property was no interface or component");
				}

				// Remove the old property and add the new
				mod.removeAll(obj, pProp, null).add(obj, pProp, name);
			}
			else if(pProp.equals(VISA.CONNECTED)) {
				// Update "connected" property on the object
				// Allowed for: NetworkComponent.Interface
				if(pObj instanceof NetworkComponent.Interface) {
					NetworkComponent.Interface np = (NetworkComponent.Interface) pObj;

					// First remove the connected properties of this object
					mod.removeAll(obj, pProp, null);
					mod.removeAll(null, pProp, obj);

					// Second, add new connected properties if the port is
					// connected to a cable
					if(np.isConnected()) {
						// Get the port on the other end of the cable
						NetworkComponent.Interface remotePort = null;
						switch(np.getCableEnd()) {
							case LEFT:
								remotePort = np.getCable().getRight();
								break;
							case RIGHT:
								remotePort = np.getCable().getLeft();
								break;
							default:
								break;
						}

						// Check for null on the remote port to avoid
						// NullPointerException because of dangling cables
						if(remotePort != null) {
							StringBuilder sbRemote = new StringBuilder(VISA.getURI());
							sbRemote.append(remotePort.getRDFLocalName());
							Resource remoteRes = mod.getResource(sbRemote.toString());

							mod.add(obj, VISA.CONNECTED, remoteRes);
							mod.add(remoteRes, VISA.CONNECTED, obj);
						}
					}
				}
				else {
					throw new IllegalArgumentException("Non-interface object provided for 'connected' property");
				}
			}
			else if(pProp.equals(VISA.ADDRESS)) {
				// Update "address" property on the object
				// Allowed for: NetworkComponent.Interface
				if(pObj instanceof NetworkComponent.Interface) {
					NetworkComponent.Interface np = (NetworkComponent.Interface) pObj;

					// Remove existing address property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					List<Statement> stmtList = mod.listStatements(new SimpleSelector(obj, pProp, (RDFNode) null)).toList();
					for(Statement stmt : stmtList) {
						removeStatementRecursive(stmt);
					}

					// If IP is configured on the port add new address property
					for(NetworkComponent.Interface.IPConfig conf : np.getAllIPConfig()) {
						Resource addrRes = createAddressResource(conf.getAddress().getAddressString(), conf.getSubnet(), conf.getVersion(), conf.getNetwork().getNetworkAddressString());
						mod.add(obj, VISA.ADDRESS, addrRes);
					}
				}
				else {
					throw new IllegalArgumentException("Non-interface object provided for 'address' property");
				}
			}
			else if(pProp.equals(VISA.VLAN)) {
				// Update "vlan" property on the object
				// Allowed for: NetworkComponent.Interface
				if(pObj instanceof NetworkComponent.Interface) {
					NetworkComponent.Interface np = (NetworkComponent.Interface) pObj;

					// Remove existing vlan property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					List<Statement> stmtList = mod.listStatements(new SimpleSelector(obj, pProp, (RDFNode) null)).toList();
					for(Statement stmt : stmtList) {
						removeStatementRecursive(stmt);
					}

					for(VLAN vlan : np.getAllVLAN()) {
						Resource vlanRes = createVLANResource(vlan);
						mod.add(obj, pProp, vlanRes);
					}
				}
				else {
					throw new IllegalArgumentException("Non-interface object provided for 'vlan' property");
				}
			}
			else if(pProp.equals(VISA.ID)) {
				// Update "id" property on the object
				// Allowed for: VLAN
				if(pObj instanceof VLAN) {
					VLAN vlan = (VLAN) pObj;
					Literal idLit = mod.createLiteral(String.valueOf(vlan.getID()));

					// Remove existing id property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					mod.removeAll(obj, pProp, (RDFNode) null).add(obj, pProp, idLit);
				}
				else {
					throw new IllegalArgumentException("Non-VLAN object provided for 'id' property");
				}
			}
			else if(pProp.equals(VISABackup.COMP_DIM_X)) {
				// Update "compDimX" property on the object in backup model
				// Allowed for: NetworkComponent
				if(pObj instanceof NetworkComponent) {
					NetworkComponent nc = (NetworkComponent) pObj;

					// Remove existing compDimX property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					backupMod.removeAll(obj, pProp, (RDFNode) null);

					Literal dimLit = backupMod.createLiteral(String.valueOf(nc.getConfig().getComponentDimensions().getX()));
					backupMod.add(obj, pProp, dimLit);
				}
				else {
					throw new IllegalArgumentException("Non-component object provided for 'compDimX' backup property");
				}
			}
			else if(pProp.equals(VISABackup.COMP_DIM_Y)) {
				// Update "compDimY" property on the object in backup model
				// Allowed for: NetworkComponent
				if(pObj instanceof NetworkComponent) {
					NetworkComponent nc = (NetworkComponent) pObj;

					// Remove existing compDimY property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					backupMod.removeAll(obj, pProp, (RDFNode) null);

					Literal dimLit = backupMod.createLiteral(String.valueOf(nc.getConfig().getComponentDimensions().getY()));
					backupMod.add(obj, pProp, dimLit);
				}
				else {
					throw new IllegalArgumentException("Non-component object provided for 'compDimY' backup property");
				}
			}
			else if(pProp.equals(VISABackup.COMP_LOC_X)) {
				// Update "compLocX" property on the object in backup model
				// Allowed for: NetworkComponent
				if(pObj instanceof NetworkComponent) {
					NetworkComponent nc = (NetworkComponent) pObj;

					// Remove existing compLocX property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					backupMod.removeAll(obj, pProp, (RDFNode) null);

					if(nc.getConfig().getGridLocation() != null) {
						Literal dimLit = backupMod.createLiteral(String.valueOf(nc.getConfig().getGridLocation().getX()));
						backupMod.add(obj, pProp, dimLit);
					}
				}
				else {
					throw new IllegalArgumentException("Non-component object provided for 'compLocX' backup property");
				}
			}
			else if(pProp.equals(VISABackup.COMP_LOC_Y)) {
				// Update "compLocY" property on the object in backup model
				// Allowed for: NetworkComponent
				if(pObj instanceof NetworkComponent) {
					NetworkComponent nc = (NetworkComponent) pObj;

					// Remove existing compLocY property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					backupMod.removeAll(obj, pProp, (RDFNode) null);

					if(nc.getConfig().getGridLocation() != null) {
						Literal dimLit = backupMod.createLiteral(String.valueOf(nc.getConfig().getGridLocation().getY()));
						backupMod.add(obj, pProp, dimLit);
					}
				}
				else {
					throw new IllegalArgumentException("Non-component object provided for 'compLocY' backup property");
				}
			}
			else if(pProp.equals(VISABackup.INTERFACE_ORIENTATION)) {
				// Update "orientation" property on the object in backup model
				// Allowed for: NetworkComponent.Interface
				if(pObj instanceof NetworkComponent.Interface) {
					NetworkComponent.Interface nci = (NetworkComponent.Interface) pObj;

					// Remove existing orientation property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					backupMod.removeAll(obj, pProp, (RDFNode) null);

					Literal dimLit = backupMod.createLiteral(String.valueOf(nci.getOrientation().toString()));
					backupMod.add(obj, pProp, dimLit);
				}
				else {
					throw new IllegalArgumentException("Non-interface object provided for 'orientation' backup property");
				}
			}
			else if(pProp.equals(VISABackup.VLAN_COLOR)) {
				// Update "color" property on the object in backup model
				// Allowed for: VLAN
				if(pObj instanceof VLAN) {
					VLAN vlan = (VLAN) pObj;

					// Remove existing property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					backupMod.removeAll(obj, pProp, (RDFNode) null);

					Literal dimLit = backupMod.createLiteral(vlan.getColor());
					backupMod.add(obj, pProp, dimLit);
				}
				else {
					throw new IllegalArgumentException("Non-VLAN object provided for 'color' backup property");
				}
			}
			else if(pProp.equals(VISABackup.NAME)) {
				// Update "name" property on the object in backup model
				// Allowed for: VLAN
				if(pObj instanceof VLAN) {
					VLAN vlan = (VLAN) pObj;

					// Remove existing property
					// Casting null pointer to RDFNode...weird but necessary:
					// http://www.dehora.net/journal/2004/08/jena_simpleselector_hmm_oops_ah_ugh.html
					backupMod.removeAll(obj, pProp, (RDFNode) null);

					Literal dimLit = backupMod.createLiteral(vlan.getName());
					backupMod.add(obj, pProp, dimLit);
				}
				else {
					throw new IllegalArgumentException("Non-VLAN object provided for 'name' backup property");
				}
			}
			else if(pProp.equals(VISABackup.GROUP)) {
				// Update "name" property on the object in backup model
				// Allowed for: VLAN
				if(pObj instanceof NetworkComponent) {
					NetworkComponent nc = (NetworkComponent) pObj;

					List<RDFNode> nodeList = mod.listObjectsOfProperty(obj, pProp).toList();
					if(nodeList.size() == 1) {
						Resource cgRes = nodeList.get(0).asResource();

						mod.remove(obj, pProp, cgRes);

						List<Resource> cgDevList = mod.listSubjectsWithProperty(pProp, cgRes).toList();
						if(cgDevList.size() == 0) {
							mod.removeAll(cgRes, VISABackup.NAME, (RDFNode) null);
						}
					}

					StringBuilder sb = new StringBuilder(VISABackup.getURI());
					sb.append(TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(nc.getConfig().getComponentGroup()).getIdentifier());
					Resource groupRes = mod.createResource(sb.toString());

					Literal groupLit = mod.createLiteral(TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(nc.getConfig().getComponentGroup()).getName());
					mod.add(obj, pProp, groupRes);
					mod.add(groupRes, VISABackup.NAME, groupLit);
				}
				else {
					throw new IllegalArgumentException("Non-component object provided for 'group' property");
				}
			}

			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.commit();
			}
		}
		catch(Throwable ex) {
			if(startTransaction) {
				ds.abort();
			}

			throw ex;
		}
		finally {
			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.end();
				TDB.sync(ds);
			}
		}
	}


	/**
	 * Remove a component from the RDF model. All statements connected to this
	 * node will be removed too. Statements which have this node as subject will
	 * be followed recursively to remove the connected nodes.
	 *
	 * @param pObj The IRDFObject whose RDF node shall be removed
	 */
	public void removeObject(IRDFObject pObj) {
		boolean startTransaction = !ds.isInTransaction();

		// Only do transaction management if no file is being parsed at the
		// moment
		if(startTransaction) {
			ds.begin(ReadWrite.WRITE);
		}

		try {
			Model mod = ds.getDefaultModel();
			Model backupMod = ds.getNamedModel(VISABackup.createModelURI());

			StringBuilder sb = new StringBuilder(VISA.getURI());
			sb.append(pObj.getRDFLocalName());

			// Get the RDF resource which represents this component
			Resource res = mod.getResource(sb.toString());

			// Get all statements having the component as subject
			List<Statement> stmtList = res.listProperties().toList();
			for(Statement stmt : stmtList) {
				// Remove this statement recursively by removing the whole
				// branch of
				// the RDF model
				this.removeStatementRecursive(stmt);
			}

			// Remove all statements that use this component as object
			mod.removeAll(null, null, res);

			// Remove all statements from the backup model that reference this
			// resource
			backupMod.removeAll(res, null, null);

			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.commit();
			}
		}
		catch(Throwable ex) {
			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.abort();
			}

			throw ex;
		}
		finally {
			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.end();
				TDB.sync(ds);
			}
		}
	}


	/**
	 * Remove the specified VLAN from the model. This will remove any statement
	 * that uses the VLAN as subject or object.
	 *
	 * @param pVLAN The VLAN which will be removed
	 */
	public void removeVLAN(VLAN pVLAN) {
		boolean startTransaction = !ds.isInTransaction();

		// Only do transaction management if no file is being parsed at the
		// moment
		if(startTransaction) {
			ds.begin(ReadWrite.WRITE);
		}

		try {
			Model mod = ds.getDefaultModel();

			StringBuilder sb = new StringBuilder(VISA.getURI());
			sb.append(pVLAN.getRDFLocalName());

			// Get the RDF resource which represents this component
			Resource res = mod.getResource(sb.toString());

			// Remove all statements that use the VLAN as subject or object
			mod.removeAll(res, null, (RDFNode) null).removeAll(null, null, res);

			// Remove all statements that use the VLAN as subject from the
			// backup model
			ds.getNamedModel(VISABackup.createModelURI()).removeAll(res, null, (RDFNode) null);

			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.commit();
			}
		}
		catch(Throwable ex) {
			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.abort();
			}

			throw ex;
		}
		finally {
			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.end();
				TDB.sync(ds);
			}
		}
	}


	/**
	 * Remove the connected statements that have the specified interface as
	 * subject or object from the model.
	 *
	 * @param pIf Interface whose connected statements shall be removed
	 */
	public void removeConnectedProperty(NetworkComponent.Interface pIf) {
		boolean startTransaction = !ds.isInTransaction();

		// Only do transaction management if no file is being parsed at the
		// moment
		if(startTransaction) {
			ds.begin(ReadWrite.WRITE);
		}

		try {
			Model mod = ds.getDefaultModel();

			StringBuilder sb = new StringBuilder(VISA.getURI());
			sb.append(pIf.getRDFLocalName());

			// Get the RDF resource which represents this component
			Resource res = mod.getResource(sb.toString());

			// Get all statements that have the resource as subject or object
			List<Statement> stmtList = mod.listStatements(res, VISA.CONNECTED, (RDFNode) null).toList();
			stmtList.addAll(mod.listStatements(null, VISA.CONNECTED, res).toList());

			// Remove these statements
			mod.remove(stmtList);

			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.commit();
			}
		}
		catch(Throwable ex) {
			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.abort();
			}

			throw ex;
		}
		finally {
			// Only do transaction management if no file is being parsed at the
			// moment
			if(startTransaction) {
				ds.end();
				TDB.sync(ds);
			}
		}
	}


	/**
	 * Change the value of the 'name' property of the root node
	 *
	 * @param pName The new value
	 */
	public void setRootNodeName(String pName) {
		if(!pName.isEmpty()) {
			ds.begin(ReadWrite.WRITE);

			Literal nameLit = ds.getDefaultModel().createLiteral(pName);

			ds.getDefaultModel().removeAll(rootNode, VISA.NAME, (RDFNode) null).add(rootNode, VISA.NAME, nameLit);

			ds.commit();
			ds.end();
		}
		else {
			throw new IllegalArgumentException("Empty string for topology name provided");
		}
	}


	/**
	 * Return the literal object value of the statement with the root node as
	 * subject and the predicate 'name'
	 *
	 * @return The literal object value
	 */
	public String getRootNodeName() {
		ds.begin(ReadWrite.READ);

		String rv = ds.getDefaultModel().listStatements(rootNode, VISA.NAME, (RDFNode) null).toList().get(0).getObject().asLiteral().toString();

		ds.end();

		return rv;
	}


	/**
	 * Return the local name of the root node
	 *
	 * @return The local name of the root node
	 */
	public String getRootNodeLocalName() {
		return rootNode.getLocalName();
	}


	/**
	 * Synchronize the topology storage with the RDF model. This method will
	 * remove all objects stored in the topology storage and rebuild the storage
	 * from the data stored in the RDF model. After rebuilding the topology it
	 * will use the data stored in the backup model (VISABackup vocabulary
	 * definition) to update the objects to their last known state. If the
	 * synchronization fails it will be assumed that the model is corrupted and
	 * the whole backend will be cleared (same as calling "clear topology" from
	 * the frontend).
	 *
	 * @throws RDFSourceException if the model contains structural errors
	 */
	public void syncTopologyToRDF() throws RDFSourceException {
		// Clear the topology
		TEBackend.TOPOLOGY_STORAGE.clear();

		Model saveMod = null;
		StringBuilder sb = null;
		boolean syncFailed = false;

		ds.begin(ReadWrite.WRITE);

		try {
			// Save the backup model from being overwritten
			sb = new StringBuilder(VISABackup.getURI());
			sb.append("savebackupmod");

			saveMod = ds.getNamedModel(sb.toString());
			saveMod.add(ds.getNamedModel(VISABackup.createModelURI()));

			ds.commit();
		}
		catch(Throwable ex) {
			ds.abort();

			log.error("SYNC PREPARATION: Rebuilding the topology from RDF model failed, model is propably corrupted!");

			syncFailed = true;

			throw ex;
		}
		finally {
			ds.end();

			if(syncFailed) {
				// Synchronization failed, clear the backend
				try {
					TEBackend.clearBackend();
				}
				catch(IOException e) {
					log.error("SYNC PREPARATION: Clearing backend after failed sync caused an exception!");
					TEBackend.logException(e, log);
					/* Ignored */
				}
			}
		}

		if(!syncFailed) {
			// Reprocess the current model to build a new topology
			ds.begin(ReadWrite.WRITE);

			try {
				// Process the default model
				processModel(null);

				// Process data in the backup model
				processBackupModel(saveMod);

				// The backup model should now contain the correct values, so we
				// can delete the copy of the old model
				ds.removeNamedModel(sb.toString());

				ds.commit();
			}
			catch(Throwable ex) {
				ds.abort();

				log.error("SYNC: Rebuilding the topology from RDF model failed, model is propably corrupted!");

				syncFailed = true;

				throw ex;
			}
			finally {
				ds.end();
				TDB.sync(ds);

				if(syncFailed) {
					// Synchronization failed, clear the backend
					try {
						TEBackend.clearBackend();
					}
					catch(IOException e) {
						log.error("SYNC: Clearing backend after failed sync caused an exception!");
						TEBackend.logException(e, log);
						/* Ignored */
					}
				}
			}
		}
	}


	/**
	 * Create a JSON object which contains a list of all imported RDF/XML source
	 * files
	 *
	 * @return JSON object with import history
	 * @throws JSONException
	 */
	public JSONObject historyToJSON() throws JSONException {
		JSONObject rv = new JSONObject();

		int i = 0;
		for(Path f : source) {
			rv.put(String.valueOf(i), f.getFileName().toString());
			i++;
		}

		return rv;
	}


	/**
	 * Return a JSONObject containing a list of the existing VSA templates and
	 * information about these.
	 *
	 * @return JSONObject containing a list of VSA templates
	 * @throws JSONException
	 */
	public JSONObject vsaTemplatesToJSON() throws JSONException {
		JSONObject rv = new JSONObject();

		// Iterate over all existing VSA templates
		for(Document doc : vsaTemplates) {
			JSONObject docJSON = new JSONObject();

			docJSON.put("id", vsaTemplates.indexOf(doc));

			// Add the values of the "name" and "description" properties to the
			// JSONObject
			Node vsaNode = doc.getElementsByTagName("VSA").item(0);

			// Check if the node is an Element. Should be the case, just for
			// security reasons
			if(vsaNode.getNodeType() == Node.ELEMENT_NODE) {
				// Cast the node to Element to access the attributes
				Element e = (Element) vsaNode;

				docJSON.put("name", e.getAttribute("name"));
			}
			docJSON.put("description", doc.getElementsByTagName("VSA_DESCRIPTION").item(0).getTextContent().trim());

			// Create a JSONObject for the outer connections of the VSA
			JSONObject connJSON = new JSONObject();

			// Get all elements of type CONNECTION and iterate over the list
			NodeList nl = doc.getElementsByTagName("CONNECTION");
			for(int i = 0; i < nl.getLength(); i++) {
				JSONObject tmpConnJSON = new JSONObject();

				Node n = nl.item(i);

				// Check if the node is an Element. Should be the case, just for
				// security reasons
				if(n.getNodeType() == Node.ELEMENT_NODE) {
					// Cast the node to Element to access the attributes
					Element e = (Element) n;

					tmpConnJSON.put("identifier", e.getAttribute("id"));
					tmpConnJSON.put("description", e.getAttribute("description"));

					connJSON.put(e.getAttribute("id"), tmpConnJSON);
				}
			}

			docJSON.put("connections", connJSON);
			rv.put(String.valueOf(vsaTemplates.indexOf(doc)), docJSON);
		}

		return rv;
	}


	/**
	 * Get the XML document of the VSA template with the specified ID. The
	 * templates are stored in an {@link java.util.ArrayList ArrayList} so
	 * requesting a not existing template may cause an exception.
	 *
	 * @param pID ArrayList index of the template
	 * @return XML Document containing the VSA template information
	 * @see java.util.ArrayList
	 */
	public Document getVSATemplate(int pID) {
		return vsaTemplates.get(pID);
	}


	/**
	 * Delete all uploaded RDF/XML source files and the corresponding named
	 * models
	 *
	 * @throws IOException
	 */
	private void clearSourceFiles() throws IOException {
		if(source.size() > 0) {
			// Delete all uploaded files (if any) and clear the source file list
			for(Path f : source) {
				Files.delete(f);

				// Remove the named model the contains the information in this
				// file
				ds.removeNamedModel(VISA.createModelURI(f.getFileName().toString()));
			}

			source.clear();
		}
	}


	/**
	 * Remove a statement recursively. The method will follow the statements
	 * that have the object of this statement as subject and remove them too.
	 * The only predicate which is not followed recursively is 'connected'.
	 * Since it is a two way connection both statements will be removed and the
	 * recursion ends there. After all recursion calls returned the provided
	 * statement will be removed.
	 *
	 * @param pSt Statement which will be removed
	 */
	private void removeStatementRecursive(Statement pSt) {
		Model mod = ds.getDefaultModel();

		// Ignore nodes with the local name "__". Jena creates these on import
		// of RDF/XML data and they build weird statements
		if(!pSt.getSubject().getLocalName().equals("__")) {
			// Get the object of the statement
			RDFNode obj = pSt.getObject();

			// Check if the predicate is 'connected'
			if(pSt.getPredicate().equals(VISA.CONNECTED)) {
				// Get the subject of the statement
				Resource subj = pSt.getSubject();

				// Remove the 'connected' statement using this subject as object
				// ('connected' properties always exist in both directions)
				// The cast to Resource is OK here since 'connected' statements
				// always include two resources, never literals
				mod.remove(obj.asResource(), VISA.CONNECTED, subj);
			}
			else {
				// Check if the object is a resource and ignore resources with
				// the local name "__". Jena creates these on import of RDF/XML
				// data and they build weird statements
				if(obj.isResource() && !obj.asResource().getLocalName().equals("__")) {
					// Get all statements that have the object as subject
					List<Statement> stmtList = obj.asResource().listProperties().toList();
					for(Statement stmt : stmtList) {
						// Remove the current statement recursively
						this.removeStatementRecursive(stmt);
					}
				}
			}
		}

		// Remove this statement
		mod.remove(pSt);
	}


	/**
	 * Create a Resource object to link to a RDF node of a NetworkPort by using
	 * the "address" property. The created resource will have all necessary
	 * literals connected. These are the literals for the "address" and "type"
	 * (IP version) properties.
	 *
	 * @param pIPAddr IP address for the "address" literal
	 * @param pSubnet Length of the subnet mask (CIDR)
	 * @param pIPV IPVersion object to determine the value for the "type"
	 *            literal
	 * @param pNetwork IPNetwork object used for the network resource
	 * @return The address resource
	 */
	private Resource createAddressResource(String pIPAddr, int pSubnet, IPVersion pIPV, String pNetwork) {
		Model mod = ds.getDefaultModel();

		// Build the local name for the address node
		StringBuilder sbLocName = new StringBuilder("__");
		sbLocName.append(pIPV.toString());
		sbLocName.append("_address_");
		sbLocName.append(pIPAddr.replaceAll("\\.", "_").replaceAll(":", "_"));
		sbLocName.append("/");
		sbLocName.append(pSubnet);
		sbLocName.append("__");

		Resource addrRes = VISA.createResource(sbLocName.toString());

		// Create the necessary literals
		Literal ipAddrLiteral = mod.createLiteral(pIPAddr);
		Literal ipVersionLiteral = mod.createLiteral(pIPV.toString());

		// Add the literals
		mod.add(addrRes, VISA.VALUE, ipAddrLiteral);
		mod.add(addrRes, VISA.TYPE, ipVersionLiteral);

		Resource netwRes = createNetworkResource(pNetwork, pSubnet, pIPV);
		mod.add(addrRes, VISA.NETWORK, netwRes);

		return addrRes;
	}


	/**
	 * Create a Resource object to link to a RDF address node using the
	 * "network" property. All necessary literal statements will be created and
	 * inserted into the model.
	 *
	 * @param pNetwork Network address
	 * @param pSubnet Subnet length
	 * @param pIPV Version of Internet Protocol
	 * @return The created resource
	 */
	private Resource createNetworkResource(String pNetwork, int pSubnet, IPVersion pIPV) {
		Model mod = ds.getDefaultModel();

		StringBuilder sbLocName = new StringBuilder("__");
		sbLocName.append(pIPV.toString());
		sbLocName.append("_network_");
		sbLocName.append(pNetwork);
		sbLocName.append("/");
		sbLocName.append(pSubnet);
		sbLocName.append("__");

		Resource netwRes = VISA.createResource(sbLocName.toString());

		// Create the necessary literals
		Literal networkLiteral = mod.createLiteral(pNetwork);
		Literal subnetLiteral = mod.createLiteral(String.valueOf(pSubnet));
		Literal ipVersionLiteral = mod.createLiteral(pIPV.toString());

		// Add the literals
		mod.add(netwRes, VISA.VALUE, networkLiteral);
		mod.add(netwRes, VISA.NETMASK_LENGTH, subnetLiteral);
		mod.add(netwRes, VISA.TYPE, ipVersionLiteral);

		return netwRes;
	}


	/**
	 * Create a Resource object to link to a RDF interface node using the "vlan"
	 * property. All necessary literal statements will be created and inserted
	 * into the model.
	 *
	 * @param pVLAN VLAN object containing the required information
	 * @return The created resource
	 */
	private Resource createVLANResource(VLAN pVLAN) {
		Model mod = ds.getDefaultModel();

		Resource vlanRes = VISA.createResource(pVLAN.getRDFLocalName());

		Literal idLiteral = mod.createLiteral(String.valueOf(pVLAN.getID()));

		mod.add(vlanRes, VISA.ID, idLiteral);

		return vlanRes;
	}


	/**
	 * Remove restricted characters from the provided string, so it can be used
	 * as a local name for a RDF node. It will first replace the following
	 * characters:
	 * ä->ae, ö->oe, ü->ue, ß->ss, /->-
	 * Any remaining invalid characters (any character that is not a-z, A-Z,
	 * 0-9, '_' or '-') will be replaced by '_'
	 *
	 * @param pName String which will be checked for restricted characters
	 * @return The new string which only contains valid characters
	 */
	private String makeLocalName(String pName) {
		String rv = pName.replaceAll("ä", "ae").replaceAll("ö", "oe").replaceAll("ü", "ue").replaceAll("ß", "ss").replaceAll("/", "-").replaceAll("[^-\\w]", "_");

		return rv;
	}


	/**
	 * Process the contents of an RDF model. All objects of the property
	 * 'device' will be converted to NetworkComponent objects. Their type is
	 * defined by the 'type' property: 'switch' will cause an NCSwitch to be
	 * created, 'vm' will create a NCVM object. All objects of the property
	 * 'interface' will be converted to interfaces on the component that is
	 * defined by the subject of the 'interface' property. The interfaces will
	 * be IP configured if an 'address' property of type 'ipv4' or 'ipv6' is
	 * present. All 'connected' statements will be converted to NetworkCable
	 * objects that connect the interfaces which are object and subject to the
	 * statement.
	 *
	 * @param pMod The named model URI which will be processed. Can be set to
	 *            null to use the default model.
	 * @return A set of the local names of all non-switch devices.
	 * @throws RDFSourceException if the model contains semantic errors
	 */
	private HashSet<String> processModel(String pMod) throws RDFSourceException {
		addGroupStatements(pMod);

		HashSet<String> rv = new HashSet<>();

		// Get all networks in the topology
		QueryExecution qexec = QueryExecutionFactory.create(getNetworkListSPARQL(pMod), ds);
		ArrayList<QuerySolution> globalNetworkList = resultSetToList(qexec.execSelect());
		qexec.close();

		for(QuerySolution qs : globalNetworkList) {
			String addr = qs.getLiteral("nwaddr").getString();
			int netmask;
			IPVersion version = IPVersion.getVersion(qs.getLiteral("nwtype").getString());

			if(qs.get("nwmask") != null) {
				netmask = qs.getLiteral("nwmask").getInt();
			}
			else {
				netmask = 24;
			}

			TEBackend.TOPOLOGY_STORAGE.createNetwork(addr, netmask, version);
		}

		// Get all VLANs in the topology
		qexec = QueryExecutionFactory.create(getVLANListSPARQL(pMod), ds);
		ArrayList<QuerySolution> globalVLANList = resultSetToList(qexec.execSelect());
		qexec.close();

		for(QuerySolution qs : globalVLANList) {
			try {
				Resource vlanRes = qs.getResource("vlan");
				String vlan = vlanRes.getLocalName();
				Literal idLit = qs.getLiteral("id");
				int id = idLit.getInt();

				VLAN newVLAN = TEBackend.TOPOLOGY_STORAGE.createVLAN(vlan, id);

				if(newVLAN.getID() != id) {
					Model nMod = ds.getNamedModel(pMod);
					nMod.removeAll(vlanRes, VISA.ID, null);
					nMod.add(vlanRes, VISA.ID, nMod.createLiteral(String.valueOf(newVLAN.getID())));
				}
			}
			catch(IllegalArgumentException ex) {
				TEBackend.logException(ex, log);

				StringBuilder sb = new StringBuilder("VLAN ");
				sb.append(qs.getResource("vlan").getLocalName());
				sb.append(" will be missing in topology!");
				log.error(sb.toString());
			}
		}

		// All connections between ports will be stored here
		HashMap<String, String> connections = new HashMap<>();

		// Get all connections in the topology
		qexec = QueryExecutionFactory.create(getConnectionListSPARQL(pMod), ds);
		ResultSet connectionList = qexec.execSelect();

		while(connectionList.hasNext()) {
			QuerySolution qs = connectionList.next();

			String src = qs.getResource("sif").getLocalName();
			String target = qs.getResource("oif").getLocalName();

			// Only add the connection to the map if the source is not present
			// in the map
			if(!connections.containsKey(src) && !connections.containsValue(src)) {
				connections.put(src, target);
			}
		}

		qexec.close();

		HashSet<NCSwitch> switchSet = new HashSet<>();

		// Get all devices in the topology
		qexec = QueryExecutionFactory.create(getDevListSPARQL(rootNode, pMod), ds);
		ArrayList<QuerySolution> deviceList = resultSetToList(qexec.execSelect());
		qexec.close();

		// Iterate over all devices and process their information
		for(QuerySolution qs : deviceList) {
			ArrayList<HashMap<String, String>> ports = new ArrayList<>();

			Resource dev = qs.getResource("dev");
			String devLocalName = dev.getLocalName();

			// Get literal properties of the node
			String devName = qs.getLiteral("name").getString();
			String devTypeLit = qs.getLiteral("type").getString();
			String devGroup = qs.getLiteral("cgname").getString();

			String devType;
			if(devTypeLit.equals("switch")) {
				devType = devTypeLit;
			}
			else {
				if(qs.getLiteral("phys").equals(VISA.BOOL_TRUE)) {
					devType = "host";
				}
				else {
					devType = "vm";
				}
			}

			// Get all interfaces of this device
			QueryExecution ifListQexec = QueryExecutionFactory.create(getInterfaceListSPARQL(dev, pMod), ds);
			ArrayList<QuerySolution> interfaceList = resultSetToList(ifListQexec.execSelect());
			ifListQexec.close();

			// Iterate over all interfaces
			for(QuerySolution ifQS : interfaceList) {
				Resource ifRes = ifQS.getResource("if");

				HashMap<String, String> port = new HashMap<>();

				// Get local name of the interface
				String ifLocalName = ifRes.getLocalName();
				port.put("localName", ifLocalName);

				if(devType.equals("switch")) {
					// Switch interfaces always point upwards
					port.put("orientation", PortOrientation.TOP.toString());

					// VLAN processing is only useful on switch interfaces
					QueryExecution vlanQexec = QueryExecutionFactory.create(getInterfaceDetailVLANSPARQL(ifRes, pMod), ds);
					ArrayList<QuerySolution> vlanList = resultSetToList(vlanQexec.execSelect());
					vlanQexec.close();

					// Process VLANs if there are any
					if(vlanList.size() > 0) {
						StringBuilder sbVLAN = new StringBuilder();

						// Store the VLAN local names as a single string
						// separated by ";"
						boolean first = true;
						for(QuerySolution vlanQs : vlanList) {
							Resource vlanRes = vlanQs.getResource("vlan");
							if(!first) {
								sbVLAN.append(";");
							}
							else {
								first = false;
							}
							sbVLAN.append(vlanRes.getLocalName());
						}

						port.put("vlan", sbVLAN.toString());
					}
				}
				else {
					// Add a random interface orientation to the map, this will
					// be replaced later
					int rnd = (int) Math.ceil(Math.random() * 4);
					switch(rnd) {
						case 1:
							port.put("orientation", PortOrientation.TOP.toString());
							break;
						case 2:
							port.put("orientation", PortOrientation.BOTTOM.toString());
							break;
						case 3:
							port.put("orientation", PortOrientation.LEFT.toString());
							break;
						case 4:
							port.put("orientation", PortOrientation.RIGHT.toString());
							break;
						default:
							port.put("orientation", PortOrientation.TOP.toString());
					}

					// Only non-switch components will have their interface
					// orientations updated after model processing
					rv.add(devLocalName);
				}

				// Get all address nodes connected to this interface
				QueryExecution addrQexec = QueryExecutionFactory.create(getInterfaceDetailAddressSPARQL(ifRes, pMod), ds);
				ResultSet addrIt = addrQexec.execSelect();

				// Iterate over all address nodes
				while(addrIt.hasNext()) {
					QuerySolution addrQs = addrIt.next();

					// IP address and version are always present
					port.put("IPAddress", addrQs.getLiteral("addrip").getString());
					// port.put("IPVersion",
					// addrQs.getLiteral("addrtype").getString());

					// Check if a network resource is connected to this address
					// node
					if(addrQs.get("nettype") != null && addrQs.get("netip") != null && addrQs.get("netmask") != null) {
						// Network resource is present, read the values
						// port.put("IPSubnetMask",
						// addrQs.getLiteral("netmask").getString());
						port.put("IPNetwork", addrQs.getLiteral("netip").getString());
					}
					else {
						if(addrQs.getLiteral("addrtype").getString().equals(IPVersion.V4.toString())) {
							// No network resource found, use default values:
							// Assume a subnet mask length of 24bit as default
							// if no
							// subnet mask was defined
							// port.put("IPSubnetMask", "24");

							// Using a 24bit subnet mask the network address is
							// "xxx.yyy.zzz.0"
							// Extract the subnet from the IP address
							Pattern ipPattern = Pattern.compile("^(.{1,3}\\..{1,3}\\..{1,3}\\.).{1,3}$");
							Matcher ipMatcher = ipPattern.matcher(port.get("IPAddress"));
							ipMatcher.matches();

							// Append the 0 as network address
							StringBuilder sbNetwork = new StringBuilder(ipMatcher.group(1));
							sbNetwork.append("0");

							if(TEBackend.TOPOLOGY_STORAGE.getNetwork(sbNetwork.toString()) == null) {
								IPNetwork ipNet = TEBackend.TOPOLOGY_STORAGE.createNetwork(sbNetwork.toString(), 24, IPVersion.getVersion(addrQs.getLiteral("addrtype").getString()));

								port.put("IPNetwork", ipNet.getNetworkAddressString());
							}
							else {
								port.put("IPNetwork", sbNetwork.toString());
							}
						}
						else if(addrQs.getLiteral("addrtype").getString().equals(IPVersion.V6.toString())) {
							// TODO handle IPv6 networks!
							throw new UnsupportedOperationException("IPv6 addresses without network node are not supported yet");
						}
						else {
							throw new IllegalStateException("Unknown IP version detected");
						}
					}

					// TODO consider multiple address nodes per interface
					// Break loop since multiple address nodes are not supported
					break;
				}

				ports.add(port);
			}

			try {
				NetworkComponent newNC;

				switch(devType) {
					case "vm":
						newNC = TEBackend.TOPOLOGY_STORAGE.createVM(ports, devName, null, null, devLocalName);
						break;
					case "host":
						newNC = TEBackend.TOPOLOGY_STORAGE.createHost(ports, devName, null, null, devLocalName);
						break;
					case "switch":
						newNC = TEBackend.TOPOLOGY_STORAGE.createSwitch(ports, devName, new Dimension2D(ports.size(), 3), null, devLocalName);
						switchSet.add((NCSwitch) newNC);
						break;
					default:
						throw new RDFSourceException("Unknown component type provided");
				}

				// Add the new component to the group it is assigned to
				newNC.getConfig().setComponentGroup(devGroup);
			}
			catch(IllegalArgumentException | RDFSourceException ex) {
				TEBackend.logException(ex, log);

				StringBuilder sb = new StringBuilder("Component ");
				sb.append(devLocalName);
				sb.append(" will be missing in topology!");
				log.error(sb.toString());
			}
		}

		// Create cables for the connections
		for(Map.Entry<String, String> conn : connections.entrySet()) {
			// Get source and target interfaces
			NetworkComponent.Interface src = TEBackend.TOPOLOGY_STORAGE.getInterface(conn.getKey());
			NetworkComponent.Interface target = TEBackend.TOPOLOGY_STORAGE.getInterface(conn.getValue());

			GroupInterface gIf = null;

			// If the connected devices belong to different groups, create a
			// connection using a GroupInterface
			if(!src.getComponentGroup().equals(target.getComponentGroup())) {
				// Determine which component is inner and outer, only create
				// GroupInterface connection if the outer component is no switch
				if(src.getComponentGroup().equals("0.0.0.0") && !(src.getComponent() instanceof NCSwitch)) {
					if(log.isDebugEnabled()) {
						StringBuilder sb = new StringBuilder("Group-Interface connection: ");
						sb.append(target.getComponentGroup());
						sb.append(" (inner) <-> ");
						sb.append(src.getComponentGroup());
						sb.append(" (outer)");

						log.debug(sb.toString());

						sb = new StringBuilder("Interfaces: ");
						sb.append(target.getIdentifier());
						sb.append(" (inner) <-> ");
						sb.append(src.getIdentifier());
						sb.append(" (outer)");

						log.debug(sb.toString());
					}

					gIf = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(target.getComponentGroup()).createOuterConnection(target, src);
				}
				else if(target.getComponentGroup().equals("0.0.0.0") && !(target.getComponent() instanceof NCSwitch)) {
					if(log.isDebugEnabled()) {
						StringBuilder sb = new StringBuilder("Group-Interface connection: ");
						sb.append(src.getComponentGroup());
						sb.append(" (inner) <-> ");
						sb.append(target.getComponentGroup());
						sb.append(" (outer)");

						log.debug(sb.toString());

						sb = new StringBuilder("Interfaces: ");
						sb.append(src.getIdentifier());
						sb.append(" (inner) <-> ");
						sb.append(target.getIdentifier());
						sb.append(" (outer)");

						log.debug(sb.toString());
					}

					gIf = TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(src.getComponentGroup()).createOuterConnection(src, target);
				}
				else {
					log.error("Connection between two non-global groups detected");

					if(log.isDebugEnabled()) {
						StringBuilder sb = new StringBuilder("Connected groups: ");
						sb.append(src.getComponentGroup());
						sb.append(" <-> ");
						sb.append(target.getComponentGroup());

						log.debug(sb.toString());

						sb = new StringBuilder("Interfaces: ");
						sb.append(src.getIdentifier());
						sb.append(" <-> ");
						sb.append(target.getIdentifier());

						log.debug(sb.toString());
					}
				}
			}

			// Create the NetworkCable
			TEBackend.TOPOLOGY_STORAGE.createCable(src, target, gIf);
		}

		// Create group switches where necessary
		for(NCSwitch ncs : switchSet) {
			ncs.createGroupSwitches();
		}

		return rv;
	}


	/**
	 * Process the contents of a backup model (VISABackup vocabulary definition)
	 * and update all stored objects according to the stored information
	 *
	 * @param pMod A model containing backup information of topology objects
	 */
	private void processBackupModel(Model pMod) {
		List<Resource> resList = pMod.listSubjects().toList();

		for(Resource res : resList) {
			// Get the reference to the object with that local name
			IRDFObject irdfo = TEBackend.TOPOLOGY_STORAGE.getRDFObject(res.getLocalName());

			if(irdfo instanceof NetworkComponent) {
				// Object is a NetworkComponent
				NetworkComponent nc = (NetworkComponent) irdfo;

				// Get component dimensions from the model
				int dimX = pMod.listStatements(res, VISABackup.COMP_DIM_X, (RDFNode) null).toList().get(0).getInt();
				int dimY = pMod.listStatements(res, VISABackup.COMP_DIM_Y, (RDFNode) null).toList().get(0).getInt();

				// Update component dimensions
				nc.getConfig().setComponentDimensions(dimX, dimY);

				// The grid location properties are not necessarily present, so
				// first search for them
				if(pMod.contains(res, VISABackup.COMP_LOC_X) && pMod.contains(res, VISABackup.COMP_LOC_Y)) {
					// Grid location properties are present, read the values
					int locX = pMod.listStatements(res, VISABackup.COMP_LOC_X, (RDFNode) null).toList().get(0).getInt();
					int locY = pMod.listStatements(res, VISABackup.COMP_LOC_Y, (RDFNode) null).toList().get(0).getInt();

					// Update grid location
					nc.getConfig().setGridLocation(locX, locY);
				}
			}
			else if(irdfo instanceof NetworkComponent.Interface) {
				// Object is a NetworkComponent.Interface
				NetworkComponent.Interface nci = (NetworkComponent.Interface) irdfo;

				// Read port orientation from the model
				PortOrientation po = PortOrientation.valueOf(pMod.listStatements(res, VISABackup.INTERFACE_ORIENTATION, (RDFNode) null).toList().get(0).getString());

				// Update port orientation
				nci.setOrientation(po);
			}
			else if(irdfo instanceof VLAN) {
				// Object is a VLAN
				VLAN vlan = (VLAN) irdfo;

				// Update color and name with values from the model
				vlan.setColor(pMod.listStatements(res, VISABackup.VLAN_COLOR, (RDFNode) null).toList().get(0).getString());
				vlan.setName(pMod.listStatements(res, VISABackup.NAME, (RDFNode) null).toList().get(0).getString());
			}
		}
	}


	/**
	 * Change the subject of all device properties in a RDF model to the
	 * specified node
	 *
	 * @param pModURI The URI of RDF model to be altered
	 * @param pRoot The node which will be the new subject for all device
	 *            statements
	 */
	private void alterRootNode(String pModURI, Resource pRoot) {
		Model mod = ds.getNamedModel(pModURI);

		// Get all device statements
		List<Statement> stmtList = mod.listStatements(null, VISA.DEVICE, (RDFNode) null).toList();

		// Remove those statements
		mod.remove(stmtList);

		// Iterate over all extracted statements and insert them with altered
		// subject
		for(Statement stmt : stmtList) {
			mod.add(pRoot, VISA.DEVICE, stmt.getObject());
		}
	}


	/**
	 * Prevent local name collisions. This method alters the local name of every
	 * resource of a RDF model to make sure its local name is not present in the
	 * currently stored model. The local name of the resource is appended with
	 * an underscore followed by the MD5 hash of the Unix timestamp at method
	 * invocation. The statement is removed from the model and a new statement
	 * with the altered nodes is inserted. Literals will not be altered.
	 *
	 * @param pModURI The URI of RDF model to be altered
	 * @return The suffix which was appended to all local names
	 */
	private String preventLocalNameCollisions(String pModURI) {
		Model mod = ds.getNamedModel(pModURI);

		// Get the current date
		Date currDate = new Date();

		// Create the MD5 hash which will be appended to all nodes
		StringBuilder sbHash = new StringBuilder(mod.listSubjectsWithProperty(VISA.DEVICE).next().getLocalName());
		sbHash.append(currDate.getTime());
		String hash = DigestUtils.md5Hex(sbHash.toString());

		// Get a list of all statements
		List<Statement> stmtList = mod.listStatements().toList();

		for(Statement stmt : stmtList) {
			// Extract subject, predicate and object from the statement
			Resource subject = stmt.getSubject();
			RDFNode object = stmt.getObject();
			Property prop = stmt.getPredicate();

			StringBuilder sbLocName;

			// Ignore nodes with the local name "__".
			// Jena creates these on import of RDF/XML data and they build weird
			// statements
			if(!subject.getLocalName().equals("__")) {
				if(log.isDebugEnabled()) {
					StringBuilder sb = new StringBuilder("SUBJECT: Old local name: ");
					sb.append(subject.getLocalName());
					log.debug(sb.toString());
				}

				// Create the new subject resource
				sbLocName = new StringBuilder(subject.getLocalName());
				sbLocName.append("_");
				sbLocName.append(hash);

				subject = VISA.createResource(sbLocName.toString());

				if(log.isDebugEnabled()) {
					StringBuilder sb = new StringBuilder("SUBJECT: New local name: ");
					sb.append(subject.getLocalName());
					log.debug(sb.toString());
				}
			}

			// If the object is a resource we will create a new one, otherwise
			// (object is a literal) we'll just use the existing node.
			// Ignore nodes with the local name "__".
			// Jena creates these on import of RDF/XML data and they build weird
			// statements
			if(object.isResource() && !object.asResource().getLocalName().equals("__")) {
				if(log.isDebugEnabled()) {
					StringBuilder sb = new StringBuilder("OBJECT: Old local name: ");
					sb.append(object.asResource().getLocalName());
					log.debug(sb.toString());
				}

				sbLocName = new StringBuilder(object.asResource().getLocalName());
				sbLocName.append("_");
				sbLocName.append(hash);
				object = VISA.createResource(sbLocName.toString());

				if(log.isDebugEnabled()) {
					StringBuilder sb = new StringBuilder("OBJECT: New local name: ");
					sb.append(object.asResource().getLocalName());
					log.debug(sb.toString());
				}
			}

			// Insert the new statement into the model
			mod.add(subject, prop, object);
		}

		mod.remove(stmtList);

		StringBuilder rv = new StringBuilder("_");
		rv.append(hash);

		return rv.toString();
	}


	/**
	 * Get a Query object to determine the root node of a model. The
	 * QuerySolutions produced by this Query will contain the fields 'root'
	 * (RDFNode of the root node) and 'name' (value of the 'visa:name'
	 * property).
	 *
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getRootNodeSPARQL(String pMod) {
		return readSPARQL("rootnode.sparql", null, pMod);
	}


	/**
	 * Get a Query object to list all devices with their literal properties in a
	 * model. The QuerySolutions produced by this Query will contain the fields
	 * 'dev' (RDFNode of the device), 'name' (value of 'visa:name'), 'type'
	 * (value of 'visa:type'), 'phys' (value of 'visa:physical') and 'imgfile'
	 * (value of 'visa:imageFile').
	 *
	 * @param pRootNode Resource object of the root node
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getDevListSPARQL(Resource pRootNode, String pMod) {
		return readSPARQL("devlist.sparql", pRootNode, pMod);
	}


	/**
	 * Get a Query object to list all VLANs with their 'id' property in a model.
	 * The QuerySolutions produced by this Query will contain the fields 'vlan'
	 * (RDFNode of the VLAN) and 'id' (value of 'visa:id').
	 *
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getVLANListSPARQL(String pMod) {
		return readSPARQL("vlanlist.sparql", null, pMod);
	}


	/**
	 * Get a Query object to list all networks with their 'value',
	 * 'subnetmaskLength' and 'type' properties in a model. The QuerySolutions
	 * produced by this Query will contain the fields 'nwaddr' (value of
	 * visa:value), 'nwtype' (value of visa:type) and 'nwmask' (value of
	 * visa:netmaskLength). The last field is optional in may be empty if it is
	 * not found in the model.
	 *
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getNetworkListSPARQL(String pMod) {
		return readSPARQL("networklist.sparql", null, pMod);
	}


	/**
	 * Get a Query object to list all connections between interfaces in the
	 * model. The QuerySolutions produced by this Query will contain the fields
	 * 'sif' (RDFNode of the subject interface) and 'oif' (RDFNode of the object
	 * interface).
	 *
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getConnectionListSPARQL(String pMod) {
		return readSPARQL("connectionlist.sparql", null, pMod);
	}


	/**
	 * Get a Query object to list all interfaces and their literal properties of
	 * a device. The QuerySolutions produced by this Query will contain the
	 * fields 'if' (RDFNode of the interface) and 'name' (value of 'visa:name').
	 *
	 * @param pDevice Resource object of the device
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getInterfaceListSPARQL(Resource pDevice, String pMod) {
		return readSPARQL("iflist.sparql", pDevice, pMod);
	}


	/**
	 * Get a Query object to get the VLAN(s) an interface is attached to. Only
	 * useful on switch interfaces, all other interfaces will produce an empty
	 * ResultSet. The QuerySolutions produced by this Query will contain the
	 * field 'vlan' (RDFNode of the VLAN).
	 *
	 * @param pInterface Resource object of the interface
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getInterfaceDetailVLANSPARQL(Resource pInterface, String pMod) {
		return readSPARQL("ifdetailvlan.sparql", pInterface, pMod);
	}


	/**
	 * Get a Query object to get the IPv4 and IPv6 addresses bound to an
	 * interface. If a Network resource is attached to an Address Resource it
	 * will be returned too. The QuerySolutions produced by this Query will
	 * contain the fields 'addrtype' (value of visa:type on Address node),
	 * 'addrip' (value of visa:value on address node), 'nettype' (value of
	 * visa:type on network node), 'netip' (value of visa:value on network node)
	 * and 'netmask' (value of visa:netmaskLength).
	 *
	 * @param pInterface Resource object of the interface
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getInterfaceDetailAddressSPARQL(Resource pInterface, String pMod) {
		return readSPARQL("ifdetailaddr.sparql", pInterface, pMod);
	}


	/**
	 * Get a Query object to get the group information for a switch from a
	 * model. The QuerySolutions produced by this Query will contain the fields
	 * 'vlan' (RDFNode of a VLAN), 'netaddr' (value of visa:value on a network
	 * node) and 'netmask' (value of visa:netmaskLength). All fields may be
	 * empty if a switch is not attached to VLANs or no network nodes could be
	 * found on the connected interfaces.
	 *
	 * @param pSwitch Resource object of the switch
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getSwitchGroupSPARQL(Resource pSwitch, String pMod) {
		return readSPARQL("switchgroups.sparql", pSwitch, pMod);
	}


	/**
	 * Get a Query object to get the group information for a host device from a
	 * model. The QuerySolutions produced by this Query will contain the fields
	 * 'netaddr' (value of visa:value on a network node) and 'netmask' (value of
	 * visa:netmaskLength). All fields may be empty if no network nodes could be
	 * found on the interfaces of the device.
	 *
	 * @param pDev Resource object of the device
	 * @param pMod Optional URI of the model on which the query will be
	 *            executed. If set to null, the query will execute on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query getDevGroupSPARQL(Resource pDev, String pMod) {
		return readSPARQL("devgroups.sparql", pDev, pMod);
	}


	/**
	 * Read a SPARQL query from a file into a String and create a Query object
	 * from that. If a resource was specified all occurrences of $URI$
	 * placeholder in the read query will be replaced with the URI of the
	 * resource. If a model URI is specified, GRAPH lines will be added to the
	 * query using the placeholders $S_MOD$ and $E_MOD$.
	 *
	 * @param pFileName File name of the SPARQL file. The file must exist and be
	 *            located in 'res/sparql'
	 * @param pRes Optional resource object, will be used to replace the $URI$
	 *            placeholder. Can be set to null if not required.
	 * @param pMod Optional model URI, will be used to add GRAPH lines to the
	 *            query. If set to null the query will be executed on the
	 *            default model of the dataset.
	 * @return A Query object containing the read SPARQL query, null if the
	 *         input file cannot be read
	 */
	private Query readSPARQL(String pFileName, Resource pRes, String pMod) {
		try {
			// Open the SPARQL file for reading
			Path inFile = Paths.get("res/sparql", pFileName);
			BufferedReader br = Files.newBufferedReader(inFile, StandardCharsets.UTF_8);

			// Read all lines and concatenate them using a StringBuilder
			StringBuilder rv = new StringBuilder();
			String line = br.readLine();
			while(line != null) {
				rv.append(line);
				rv.append(System.lineSeparator());

				line = br.readLine();
			}
			br.close();

			// Get the String from the StringBuilder and, if required, replace
			// the $URI$ placeholder
			String rvStr = rv.toString();
			if(pRes != null) {
				rvStr = rvStr.replaceAll("\\$URI\\$", pRes.getURI());
			}

			if(pMod != null && !pMod.isEmpty()) {
				StringBuilder graphLine = new StringBuilder("GRAPH <");
				graphLine.append(pMod);
				graphLine.append("> {");

				rvStr = rvStr.replaceAll("\\$S_MOD\\$", graphLine.toString()).replaceAll("\\$E_MOD\\$", "}");
			}
			else {
				rvStr = rvStr.replaceAll("\\$S_MOD\\$", "").replaceAll("\\$E_MOD\\$", "");
			}

			// Build a Query object and return it
			return QueryFactory.create(rvStr);
		}
		catch(IOException ex) {
			StringBuilder sb = new StringBuilder("Caught: [");
			sb.append(ex.getClass().getSimpleName());
			sb.append("] ");
			sb.append(ex.getMessage());
			log.error(sb.toString());

			if(log.isDebugEnabled()) {
				for(StackTraceElement ste : ex.getStackTrace()) {
					log.debug(ste.toString());
				}
			}

			return null;
		}
	}


	/**
	 * Transfer all solutions in a ResultSet into a list. The ResultSet cannot
	 * be used afterwards because it is iterated using the next() method.
	 *
	 * @param pRS ResultSet containing the solutions
	 * @return A list containing all QuerySolution objects
	 */
	private ArrayList<QuerySolution> resultSetToList(ResultSet pRS) {
		ArrayList<QuerySolution> rv = new ArrayList<>();

		while(pRS.hasNext()) {
			rv.add(pRS.next());
		}

		return rv;
	}


	/**
	 * Determine the groups for all devices. Devices are grouped together
	 * according to the networks they are attached to. If a device is attached
	 * to 2 or more different networks it will not be grouped and assigned to
	 * the global group 0.0.0.0 that collects all devices which will be located
	 * on the base layer of the editor grid. Group information will be added to
	 * the backup model for further processing.
	 *
	 * @param pMod The URI of the model which contains the devices
	 */
	private void addGroupStatements(String pMod) {
		QueryExecution qexec = QueryExecutionFactory.create(getDevListSPARQL(rootNode, pMod), ds);
		ArrayList<QuerySolution> devList = resultSetToList(qexec.execSelect());
		qexec.close();

		// Iterate over all devices
		for(QuerySolution qs : devList) {
			// If no group information is set, determine the group based on
			// device type
			if(qs.get("cgname") == null) {
				Resource dev = qs.getResource("dev");
				ArrayList<QuerySolution> groupInfo;

				Model mod;
				if(pMod != null && !pMod.isEmpty()) {
					mod = ds.getNamedModel(pMod);
				}
				else {
					mod = ds.getDefaultModel();
				}

				// Execute the SPARQL query that matches the device type
				if(qs.getLiteral("type").equals(VISA.DEV_TYPE_HOST)) {
					// Determine the groups for host devices
					QueryExecution groupQexec = QueryExecutionFactory.create(getDevGroupSPARQL(dev, pMod), ds);
					groupInfo = resultSetToList(groupQexec.execSelect());
					groupQexec.close();
				}
				else {
					// Determine the groups for switches
					QueryExecution groupQexec = QueryExecutionFactory.create(getSwitchGroupSPARQL(dev, pMod), ds);
					groupInfo = resultSetToList(groupQexec.execSelect());
					groupQexec.close();
				}

				// If exactly 1 line of group information was found and
				// 'netaddr' exists, apply that value as group. Otherwise use
				// global group 0.0.0.0
				if(groupInfo.size() == 1 && groupInfo.get(0).get("netaddr") != null) {
					QuerySolution groupQS = groupInfo.get(0);

					// Combine 'netaddr' and 'netmask' to form the group name
					StringBuilder sbGroupName = new StringBuilder(groupQS.getLiteral("netaddr").getString());
					sbGroupName.append("/");
					sbGroupName.append(groupQS.getLiteral("netmask").getString());

					StringBuilder sbLocName = new StringBuilder(VISA.getURI());
					sbLocName.append(TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName(sbGroupName.toString()).getIdentifier());

					Resource groupRes = ds.getDefaultModel().getResource(sbLocName.toString());

					// Add the group information to the model
					mod.add(dev, VISABackup.GROUP, groupRes);
					mod.add(groupRes, VISABackup.NAME, ds.getDefaultModel().createLiteral(sbGroupName.toString()));
				}
				else {
					StringBuilder sbLocName = new StringBuilder(VISA.getURI());
					sbLocName.append(TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName("0.0.0.0").getIdentifier());

					Resource groupRes = ds.getDefaultModel().getResource(sbLocName.toString());

					// Add the group information to the backup model
					mod.add(dev, VISABackup.GROUP, groupRes);
					mod.add(groupRes, VISABackup.NAME, ds.getDefaultModel().createLiteral(TEBackend.TOPOLOGY_STORAGE.getComponentGroupByName("0.0.0.0").getName()));
				}
			}
		}
	}
}
