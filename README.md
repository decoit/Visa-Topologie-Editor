# VISA Topology-Editor
The VISA Topology-Editor (VTE) is a topology editor prototype, which was developed by the [DECOIT GmbH](http://www.decoit.de/ "DECOIT GmbH"), located in Bremen (Germany), during the VISA research project. For more information about VISA please visit http://www.visa-project.de/.

To provide full functionality, it requires an installation of the Interconnected-asset Ontology Tool (IO-Tool), developed by the Fraunhofer SIT, which uses OpenStack to replicate the designed topology into a virtual environment. Without that tool it is possible to design topologies from scratch or import and edit topologies from RDF/XML files using the same RDF data model that is used for communication between VTE and IO-Tool. But it is **NOT** possible to create a virtual infrastruture from this information since this requires the topology to be stored in an IO-Tool instance.

The VTE is published under the terms of the GPLv3.

## Backend
### Preparation
The backend needs a Java SSL KeyStore and a Java SSL TrustStore ([Tutorial](http://docs.oracle.com/javaee/1.4/tutorial/doc/Security6.html)) to communicate with the IO-Tool. They should be located in the `backend/res/ssl` directory. If you store them on another place, it may be required to change the `backend/build.xml` file. The server's SSL certificate must be added to those stores. If you do not intend to use the IO-Tool functionality, creation of the Key- and TrustStore is not required.

After creating the SSL stores edit the `backend/visabackend.sh.sample` file. Fill in the locations and passwords to the Key- and TrustStore, then save the file as `backend/visabackend.sh` and make sure it is executable. This last step is required even if you do not use the IO-Tool functionality. In this case you may just rename the sample file. **DO NOT REMOVE THE COMMAND LINE PARAMETERS FROM THIS FILE!** They are required to start the backend. If not needed, just leave them blank.

### Installation
To install the backend you need a Java SE 7 JDK and Apache Ant. Java versions prior to Java SE 7 are not supported. If those requirements are met, just run `ant` in the `backend` directory. It will create a directory called `dist` and places the installation inside. You may copy the contents of this directory to any location you want.

After compiling and packaging using `ant` change directory to the location you copied the `dist` contents to and run `./install.sh`. This will install the symbolic link `/usr/bin/visabackend` to allow starting the backend by just typing `visabackend` on the console. This step is optional if you want to run the startup script directly.

### Usage
*This section is under construction*

## Frontend
### Preparation
*This section is under construction*

### Installation
*This section is under construction*

### Usage
*This section is under construction*
