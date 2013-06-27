package de.decoit.visa.ioconnector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import de.decoit.visa.TEBackend;
import de.decoit.visa.enums.IOToolRequestStatus;
import de.decoit.visa.protocol.Commands;
import de.decoit.visa.protocol.Request;
import de.decoit.visa.protocol.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;


/**
 * This class is used to connect to an instance of the IO-Tool at the given
 * location. It provides utility methods to issue commands to the IO-Tool and
 * read the received data, return codes and messages.
 *
 * @author Thomas Rix
 */
public class IOConnector {
	private static Logger log = Logger.getLogger(IOConnector.class.getName());

	private int lastReturnCode;
	private String lastReturnMsg;
	private Map<String, String> lastReturnData;
	private int lastWaitReturnCode;
	private String lastWaitReturnMsg;
	private Map<String, String> lastWaitReturnData;
	private ConnectionThread connThread;
	private String ioHost;
	private int ioPort;


	/**
	 * Create a new IOConnector. It will create the SSLSocketFactory and Yaml
	 * instances and open a SSL secured socket to the IO-Tool. Last known return
	 * code and message are initialized with -2 and "" respectively.
	 *
	 * @param pHost Hostname or IP-Address of the IO-Tool server
	 * @param pPort Port on which the IO-Tool server is listening
	 * @throws IOException if the host cannot be resolved, writing or reading
	 *             the socket fails
	 */
	public IOConnector(String pHost, int pPort) throws IOException {
		lastReturnCode = -2;
		lastReturnMsg = "";
		lastReturnData = null;

		lastWaitReturnCode = -2;
		lastWaitReturnMsg = "";
		lastWaitReturnData = null;

		ioHost = pHost;
		ioPort = pPort;
	}


	/**
	 * Return the return code of the last received Response object. All values
	 * from 0 to 255 are expected values while 0 means no error and any value
	 * greater than 0 means error. Initial value is -2 meaning no Response was
	 * received. A value of -1 shows that the last received data did not
	 * describe a valid Response object.
	 *
	 * @return The return value of the last Response, -1 or -2 in the decribed
	 *         cases
	 */
	public int getLastReturnCode() {
		return lastReturnCode;
	}


	/**
	 * Return the message of the last received Response object. Returns an empty
	 * string if no message was included or an error occured.
	 *
	 * @return The message of the last Response
	 */
	public String getLastReturnMsg() {
		return lastReturnMsg;
	}


	/**
	 * Return the data map of the last received Response object. Returns null if
	 * the last received object was no valid Response object.
	 *
	 * @return The data map of the last Response
	 */
	public Map<String, String> getLastReturnData() {
		return lastReturnData;
	}


	/**
	 * Return the return code of the last Response object that was received
	 * after a 'please wait' signal. All values from 0 to 255 are expected
	 * values while 0 means no error and any value greater than 0 means error.
	 * Initial value is -2 meaning no Response was received. A value of -1 shows
	 * that the last received data did not describe a valid Response object.
	 *
	 * @return The return value of the last Response, -1 or -2 in the decribed
	 *         cases
	 */
	public int getLastWaitReturnCode() {
		return lastWaitReturnCode;
	}


	/**
	 * Return the message of the last Response object that was received after a
	 * 'please wait' signal. Returns an empty string if no message was included
	 * or an error occured.
	 *
	 * @return The message of the last Response
	 */
	public String getLastWaitReturnMsg() {
		return lastWaitReturnMsg;
	}


	/**
	 * Return the data map of the last Response object that was received after a
	 * 'please wait' signal. Returns null if the last received object was no
	 * valid Response object.
	 *
	 * @return The data map of the last Response
	 */
	public Map<String, String> getLastWaitReturnData() {
		return lastWaitReturnData;
	}


	/**
	 * Issue a GET_TOPOLOGY_LIST command to the IO-Tool. The returned
	 * {@link de.decoit.visa.enums.IOToolRequestStatus IOToolRequestStatus}
	 * constant will indicate the status of the request. The method may return
	 * one of the constants SUCCESS, FAILURE and IOTOOL_BUSY.<br>
	 * <br>
	 * Possible return codes are:<br>
	 * 0: Success
	 *
	 * @return Enum constant according to request status
	 * @throws IOException if writing to or reading from the socket fails
	 * @see de.decoit.visa.enums.IOToolRequestStatus
	 */
	public IOToolRequestStatus requestTopologyList() throws IOException {
		if(connThread == null || !connThread.isAlive()) {
			Commands c = Commands.GET_TOPOLOGY_LIST;

			// Construct the Request object and serialize it
			Request req = new Request(c, Commands.getEmptyArgumentMap(c));
			return doDataExchange(req);
		}
		else {
			return IOToolRequestStatus.IOTOOL_BUSY;
		}
	}


	/**
	 * Issue a GET_TOPOLOGY command to the IO-Tool. The returned
	 * {@link de.decoit.visa.enums.IOToolRequestStatus IOToolRequestStatus}
	 * constant will indicate the status of the request. The method may return
	 * one of the constants SUCCESS, FAILURE and IOTOOL_BUSY.<br>
	 * <br>
	 * Possible return codes are:<br>
	 * 0: Success<br>
	 * 100: Unknown topology ID
	 *
	 * @param pTopoID ID string of the requested topology
	 * @return Enum constant according to request status
	 * @throws IOException if writing to or reading from the socket fails
	 * @see de.decoit.visa.enums.IOToolRequestStatus
	 */
	public IOToolRequestStatus requestTopology(String pTopoID) throws IOException {
		if(connThread == null || !connThread.isAlive()) {
			Commands c = Commands.GET_TOPOLOGY;
			Map<String, String> args = Commands.getEmptyArgumentMap(c);
			args.put("topology", pTopoID);

			// Construct the Request object and serialize it
			Request req = new Request(c, args);
			return doDataExchange(req);
		}
		else {
			return IOToolRequestStatus.IOTOOL_BUSY;
		}
	}


	/**
	 * Issue a WRITE_TOPOLOGY command to the IO-Tool. The returned
	 * {@link de.decoit.visa.enums.IOToolRequestStatus IOToolRequestStatus}
	 * constant will indicate the status of the request. The method may return
	 * one of the constants SUCCESS, FAILURE and IOTOOL_BUSY. A new topology
	 * will be created if the ID string was not used before. Otherwise the
	 * existing topology will be overwritten, provided that it is not write
	 * protected.<br>
	 * <br>
	 * Possible return codes are:<br>
	 * 0: Success<br>
	 * 102: RDF could not be parsed<br>
	 * 103: RDF contains invalid data<br>
	 * 106: Topology is write protected
	 *
	 * @param pTopoID ID string of the written topology
	 * @param pXML RDF/XML serialization of the topology's RDF model
	 * @param pDesc Description, also known as human readable name, of the
	 *            topology
	 * @return Enum constant according to request status
	 * @throws IOException if writing to or reading from the socket fails
	 * @see de.decoit.visa.enums.IOToolRequestStatus
	 */
	public IOToolRequestStatus writeTopology(String pTopoID, String pXML, String pDesc) throws IOException {
		if(connThread == null || !connThread.isAlive()) {
			Commands c = Commands.WRITE_TOPOLOGY;
			Map<String, String> args = Commands.getEmptyArgumentMap(c);
			args.put("topology", pTopoID);
			args.put("data", pXML);
			args.put("description", pDesc);

			// Construct the Request object and serialize it
			Request req = new Request(c, args);
			return doDataExchange(req);
		}
		else {
			return IOToolRequestStatus.IOTOOL_BUSY;
		}
	}


	/**
	 * Issue a DROP_TOPOLOGY command to the IO-Tool. The returned
	 * {@link de.decoit.visa.enums.IOToolRequestStatus IOToolRequestStatus}
	 * constant will indicate the status of the request. The method may return
	 * one of the constants SUCCESS, FAILURE and IOTOOL_BUSY. The specified
	 * topology will be removed from the IO-Tool and cannot be accessed
	 * afterwards.<br>
	 * <br>
	 * Possible return codes are:<br>
	 * 0: Success<br>
	 * 100: Unknown topology ID
	 * 107: Topology is delete protected
	 *
	 * @param pTopoID ID string of the topology to remove
	 * @return Enum constant according to request status
	 * @throws IOException if writing to or reading from the socket fails
	 * @see de.decoit.visa.enums.IOToolRequestStatus
	 */
	public IOToolRequestStatus dropTopology(String pTopoID) throws IOException {
		if(connThread == null || !connThread.isAlive()) {
			Commands c = Commands.DROP_TOPOLOGY;
			Map<String, String> args = Commands.getEmptyArgumentMap(c);
			args.put("topology", pTopoID);

			// Construct the Request object and serialize it
			Request req = new Request(c, args);
			return doDataExchange(req);
		}
		else {
			return IOToolRequestStatus.IOTOOL_BUSY;
		}
	}


	/**
	 * Issue a COLLECT command to the IO-Tool. The returned
	 * {@link de.decoit.visa.enums.IOToolRequestStatus IOToolRequestStatus}
	 * constant will indicate the status of the request. The method may return
	 * one of the constants SUCCESS, WAIT, FAILURE and IOTOOL_BUSY.<br>
	 * <br>
	 * Possible return codes are:<br>
	 * 0: Success<br>
	 * 104: Connection failure<br>
	 * 105: Command execution failure<br>
	 * 106: Topology is write protected<br>
	 * 254: Please wait
	 *
	 * @param pTopoID ID string of the topology to remove
	 * @return Enum constant according to request status
	 * @throws IOException if writing to or reading from the socket fails
	 * @see de.decoit.visa.enums.IOToolRequestStatus
	 */
	public IOToolRequestStatus collectTopology(String pTopoID) throws IOException {
		if(connThread == null || !connThread.isAlive()) {
			Commands c = Commands.COLLECT;
			Map<String, String> args = Commands.getEmptyArgumentMap(c);
			args.put("topology", pTopoID);

			// Construct the Request object and serialize it
			Request req = new Request(c, args);
			return doDataExchange(req);
		}
		else {
			return IOToolRequestStatus.IOTOOL_BUSY;
		}
	}


	/**
	 * Issue a REPLICATE command to the IO-Tool. The returned
	 * {@link de.decoit.visa.enums.IOToolRequestStatus IOToolRequestStatus}
	 * constant will indicate the status of the request. The method may return
	 * one of the constants SUCCESS, WAIT, FAILURE and IOTOOL_BUSY.<br>
	 * <br>
	 * Possible return codes are:<br>
	 * 0: Success<br>
	 * 100: Unknown topology ID<br>
	 * 104: Connection failure<br>
	 * 105: Command execution failure<br>
	 * 254: Please wait
	 *
	 * @param pTopoID ID string of the topology to replicate
	 * @return Enum constant according to request status
	 * @throws IOException if writing to or reading from the socket fails
	 * @see de.decoit.visa.enums.IOToolRequestStatus
	 */
	public IOToolRequestStatus replicateTopology(String pTopoID) throws IOException {
		if(connThread == null || !connThread.isAlive()) {
			Commands c = Commands.REPLICATE;
			Map<String, String> args = Commands.getEmptyArgumentMap(c);
			args.put("topology", pTopoID);

			// Construct the Request object and serialize it
			Request req = new Request(c, args);
			return doDataExchange(req);
		}
		else {
			return IOToolRequestStatus.IOTOOL_BUSY;
		}
	}


	/**
	 * Issue a CLEANUP command to the IO-Tool. The returned
	 * {@link de.decoit.visa.enums.IOToolRequestStatus IOToolRequestStatus}
	 * constant will indicate the status of the request. The method may return
	 * one of the constants SUCCESS, WAIT, FAILURE and IOTOOL_BUSY.<br>
	 * <br>
	 * Possible return codes are:<br>
	 * 0: Success<br>
	 * 104: Connection failure<br>
	 * 105: Command execution failure<br>
	 * 254: Please wait
	 *
	 * @return Enum constant according to request status
	 * @throws IOException if writing to or reading from the socket fails
	 * @see de.decoit.visa.enums.IOToolRequestStatus
	 */
	public IOToolRequestStatus cleanUpIOTool() throws IOException {
		if(connThread == null || !connThread.isAlive()) {
			Commands c = Commands.CLEANUP;
			Map<String, String> args = Commands.getEmptyArgumentMap(c);

			// Construct the Request object and serialize it
			Request req = new Request(c, args);
			return doDataExchange(req);
		}
		else {
			return IOToolRequestStatus.IOTOOL_BUSY;
		}
	}


	/**
	 * Issue a RESET command to the IO-Tool. The returned
	 * {@link de.decoit.visa.enums.IOToolRequestStatus IOToolRequestStatus}
	 * constant will indicate the status of the request. The method may return
	 * one of the constants SUCCESS, SUCCESS_WITH_ERORRS, WAIT, FAILURE and
	 * IOTOOL_BUSY.<br>
	 * <br>
	 * Possible return codes are:<br>
	 * 0: Success<br>
	 * 1: Success, but intended fail for some jobs<br>
	 * 104: Connection failure<br>
	 * 105: Command execution failure<br>
	 * 254: Please wait
	 *
	 * @return Enum constant according to request status
	 * @throws IOException if writing to or reading from the socket fails
	 * @see de.decoit.visa.enums.IOToolRequestStatus
	 */
	public IOToolRequestStatus resetIOTool() throws IOException {
		if(connThread == null || !connThread.isAlive()) {
			Commands c = Commands.RESET;
			Map<String, String> args = Commands.getEmptyArgumentMap(c);

			// Construct the Request object and send it
			Request req = new Request(c, args);
			return doDataExchange(req);
		}
		else {
			return IOToolRequestStatus.IOTOOL_BUSY;
		}
	}


	/**
	 * Disconnect from the IO-Tool. Setting the parameter to true will
	 * force close the socket causing any read or write operation to
	 * instantly break. This may cause undesired behaviour. If the parameter
	 * is set to false, calling this method while waiting for the IO-Tool to
	 * complete a job will cause an IOToolException.
	 *
	 * @param pForce true: force close the socket, false: do not force close
	 * @throws IOException if accessing the socket fails
	 * @throws IOToolException if the IO-Tool is busy and no force close is
	 *             required
	 */
	public void disconnect(boolean pForce) throws IOException, IOToolException {
		if(connThread != null) {
			connThread.close(pForce);
			connThread = null;
		}
	}


	/**
	 * Exchange information with the IO-Tool. The provided Request object will
	 * be sent and the received Response object will be returned.
	 *
	 * @param pReq The Request object which will be sent
	 * @throws IOException if reading or writing to the socket fails
	 */
	private IOToolRequestStatus doDataExchange(Request pReq) throws IOException {
		if(connThread == null || !connThread.isAlive()) {
			// Check a waiting thread finished before this call
			if(connThread != null) {
				// Get the final response object
				Response waitResponse = connThread.getWaitResponse();

				if(waitResponse != null) {
					// Store the information returned from the IO-Tool
					lastWaitReturnCode = waitResponse.getReturnCode();
					lastWaitReturnMsg = waitResponse.getMessage();
					lastWaitReturnData = waitResponse.getData();
				}
				else {
					// Set return code and message to default values to indicate
					// that the last received object was no valid response
					lastWaitReturnCode = -1;
					lastWaitReturnMsg = "";
					lastWaitReturnData = null;
				}
			}

			try {
				// Set run parameters for the connection thread
				connThread = new ConnectionThread(ioHost, ioPort, pReq, Thread.currentThread());

				// Run the thread
				connThread.start();

				// Wait for the thread to terminate. If the IO-Tool sends a
				// 'please
				// wait' response this thread will be interrupted
				connThread.join();

				// If the thread terminated, get the Response object
				Response res = connThread.getResponse();
				connThread = null;

				if(res != null) {
					// Store the information returned from the IO-Tool
					lastReturnCode = res.getReturnCode();
					lastReturnMsg = res.getMessage();
					lastReturnData = res.getData();

					if(lastReturnCode == 0) {
						return IOToolRequestStatus.SUCCESS;
					}
					else if(lastReturnCode == 1) {
						return IOToolRequestStatus.SUCCESS_WITH_ERRORS;
					}
					else {
						return IOToolRequestStatus.FAILURE;
					}
				}
				else {
					// Set return code and message to default values to indicate
					// that the last received object was no valid response
					lastReturnCode = -1;
					lastReturnMsg = "";
					lastReturnData = null;

					return IOToolRequestStatus.FAILURE;
				}
			}
			catch(InterruptedException ex) {
				// The join() was interrupted, this usually means that the
				// IO-Tool
				// sent a "please wait" message
				lastReturnCode = 254;
				lastReturnMsg = "";
				lastReturnData = null;

				return IOToolRequestStatus.WAIT;
			}
		}
		else {
			return IOToolRequestStatus.IOTOOL_BUSY;
		}
	}


	/**
	 * This class creates a SSL secured connection to the IO-Tool at the
	 * specified location. It is implemented as a subclass of Thread. This
	 * allows waiting for responses while not stalling the whole editor backend.<br>
	 * <br>
	 * The main idea of this class is that every call of start() will execute a
	 * request. If the command takes a long time and the IO-Tool sends a 'please
	 * wait' response, this thread will interrupt the thread that asked for
	 * command execution. This signals that the request will take some time and
	 * the user can do some other stuff. If the response succeeds or fails the
	 * thread terminates and the result can be processed.<br>
	 * <br>
	 * Because of this behavior the calling thread should call Thread.join() on
	 * this thread surrounded by a try-catch-structure for InterruptedException.
	 * If the exception is caught, the IO-Tool sent 'please wait'. Otherwise the
	 * command execution is completed and the result can be read an processed.
	 *
	 * @author Thomas Rix
	 */
	private class ConnectionThread extends Thread {
		private Logger threadLog = Logger.getLogger(ConnectionThread.class.getName());

		private SSLSocketFactory sslFact;
		private SSLSocket sock;
		private BufferedWriter bw;
		private BufferedReader br;
		private Yaml yaml;
		private Request req;
		private Response res;
		private Response waitRes;
		private Thread callbackThread;


		/**
		 * Construct a new connection thread. It will connect to the socket with
		 * provided hostname and port.
		 *
		 * @param pHost Hostname or IP-Address of the IO-Tool server
		 * @param pPort Port on which the IO-Tool server is listening
		 * @throws IOException if creating the socket fails
		 */
		private ConnectionThread(String pHost, int pPort, Request pReq, Thread pCallbackThread) throws IOException {
			super("IO-Tool Thread");

			yaml = new Yaml();
			yaml.setBeanAccess(BeanAccess.FIELD);
			req = pReq;
			callbackThread = pCallbackThread;

			res = null;
			waitRes = null;

			// Open the socket to the IO-Tool
			sslFact = (SSLSocketFactory) SSLSocketFactory.getDefault();
			sock = (SSLSocket) sslFact.createSocket(InetAddress.getByName(pHost), pPort);

			bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
		}


		@Override
		public void run() {
			if(req != null && callbackThread != null) {
				try {
					String reqYaml = yaml.dump(req);
					req = null;

					// Write the serialized object to the socket
					writeLine(reqYaml, true);

					// Read the response from the socket
					String resYaml = readLine();

					// Parse an object from the received YAML code
					Object resObj = yaml.load(resYaml);

					if(resObj instanceof Response) {
						// Cast the received object to a Response
						res = (Response) resObj;

						// If the return code of the response is 254, we have to
						// wait for completion but wake up the calling thread
						if(res.getReturnCode() == 254) {
							// Wake up the calling thread, this tells it that we
							// have to wait for completion
							callbackThread.interrupt();

							// Wait to receive the final message from the
							// IO-Tool
							resYaml = readLine();

							// Parse the final message
							resObj = yaml.load(resYaml);

							if(resObj instanceof Response) {
								// Cast the received object to a Response
								waitRes = (Response) resObj;
							}
							else {
								// No valid response object received
								waitRes = null;
							}
						}
					}
					else {
						// No valid response object received
						res = null;
					}
				}
				catch(SocketException se) {
					// The socket was closed while writing to or reading from it
					res = null;
					TEBackend.logException(se, threadLog);
				}
				catch(IOException ioe) {
					// Writing or reading the socket failed
					res = null;
					TEBackend.logException(ioe, threadLog);
				}
			}
			else {
				// No request and callback thread set
				res = null;
				threadLog.error("Run parameters not set");
			}
		}


		/**
		 * Get the last received Reponse object
		 *
		 * @return The last received Reponse object
		 */
		public Response getResponse() {
			return res;
		}


		public Response getWaitResponse() {
			return waitRes;
		}


		/**
		 * Close the socket to the IO-Tool. Setting the parameter to true will
		 * force close the socket causing any read or write operation to
		 * instantly break. This may cause undesired behaviour. If the parameter
		 * is set to false, calling this method while the thread is waiting for
		 * the IO-Tool to complete a job will cause an IOToolException.
		 *
		 * @param pForce true: force close the socket, false: do not force close
		 * @throws IOException if accessing the socket fails
		 * @throws IOToolException if the IO-Tool is busy and no force close is
		 *             required
		 */
		public void close(boolean pForce) throws IOException, IOToolException {
			if(sock != null) {
				if(pForce || !isAlive()) {
					sock.close();
				}
				else {
					throw new IOToolException("Waiting for IO-Tool to complete its current job, cannot close socket.");
				}
			}
		}


		/**
		 * Write a line into the {@link BufferedWriter} that wraps the
		 * OutputStream of the socket. The string will be encoded with Base64
		 * before sending and a new line character is appended.
		 *
		 * @param pStr The string which will be written to the socket
		 * @param pDoFlush true: Call BufferedWriter.flush() after writing the
		 *            string, false: do nothing
		 * @throws IOException if writing to the socket fails
		 */
		private void writeLine(String pStr, boolean pDoFlush) throws IOException {
			String base64str = Base64.encodeBase64String(pStr.getBytes());

			if(log.isTraceEnabled()) {
				StringBuilder sbStr = new StringBuilder("String sent: ");
				sbStr.append(pStr);
				log.trace(sbStr.toString());

				StringBuilder sbB64 = new StringBuilder("Base64 sent: ");
				sbB64.append(base64str);
				log.trace(sbB64.toString());
			}

			bw.write(base64str);
			bw.newLine();

			if(pDoFlush) {
				bw.flush();
				log.debug("Buffer flushed");
			}
		}


		/**
		 * Read a line from the {@link BufferedReader} that wraps the
		 * InputStream of the socket. It is assumed that the received string is
		 * Base64 encoded, so it will be decoded before return.
		 *
		 * @return The decoded string
		 * @throws IOException if reading from the socket fails
		 */
		private String readLine() throws IOException {
			String resBase64str = br.readLine();
			String resStr = new String(Base64.decodeBase64(resBase64str));

			if(log.isTraceEnabled()) {
				StringBuilder sbB64 = new StringBuilder("Base64 received: ");
				sbB64.append(resBase64str);
				log.trace(sbB64.toString());

				StringBuilder sbStr = new StringBuilder("String received: ");
				sbStr.append(resStr);
				log.trace(sbStr.toString());
			}

			return resStr;
		}
	}
}
