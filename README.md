<table>
    <tr>
        <td> <a href="http://www.visa-project.de/"><img src="https://decoit.de/files/DECOIT/logos/forschungsprojekte/visa121x121.jpg" alt="VISA"></a></td>
        <td><img src="http://www.visa-project.de/cms/upload/bilder/TE-Logo_words.png" alt="VISA Topologie Editor"></td>
        <td> <a href="http://www.decoit.de/"><img alt="DECOIT GmbH" src="https://decoit.de/files/DECOIT/logos/logo-decoit-R-200x56.png"</a> </td>
    </tr>
</table>

# VISA Topology-Editor
The VISA Topology-Editor (VTE) is a topology editor prototype, which was developed by the [DECOIT GmbH](http://www.decoit.de/ "DECOIT GmbH"), located in Bremen (Germany), during the VISA research project. For more information about VISA please visit http://www.visa-project.de/. Please remember this is a prototype release and it is neither optimized for performance nor stability! Do not use it in a production environment!

To provide full functionality, it requires an installation of the Interconnected-asset Ontology Tool (IO-Tool), developed by the Fraunhofer SIT, which uses OpenStack to replicate the designed topology into a virtual environment. Without that tool it is possible to design topologies from scratch or import and edit topologies from RDF/XML files using the same RDF data model that is used for communication between VTE and IO-Tool. But it is **NOT** possible to create a virtual infrastruture from this information since this requires the topology to be stored in an IO-Tool instance.

The VTE is copyrighted (C) 2013 by DECOIT GmbH and published under the terms of the GPLv3.

## Backend
### Preparation
The backend needs a Java SSL KeyStore and a Java SSL TrustStore ([Tutorial](http://docs.oracle.com/javaee/1.4/tutorial/doc/Security6.html)) to communicate with the IO-Tool. They should be located in the `backend/res/ssl` directory. If you store them in another place, it may be required to change the `backend/build.xml` file. The IO-Tool server's SSL certificate must be added to those stores. If you do not intend to use the IO-Tool functionality, creation of the Key- and TrustStore is not required.

After creating the SSL stores edit the `backend/visabackend.sh.sample` file. Fill in the locations and passwords to the Key- and TrustStore (see Usage section for further information), then save the file as `backend/visabackend.sh` and make sure it is executable. This last step is required even if you do not use the IO-Tool functionality. In this case you may just rename the sample file. **DO NOT REMOVE THE COMMAND LINE PARAMETERS FROM THIS FILE!** They are required to start the backend. If not needed, just leave them blank.

Besides the SSL stores an installation of graphviz version 2.28 or higher is required. Versions below 2.28 will not work properly because of problems with the `neato` layouter. Ubuntu ships with graphviz 2.26 but there are development packages available from the [graphviz homepage](http://www.graphviz.org/ "graphviz") or by adding [this PPA](https://launchpad.net/~gviz-adm/+archive/graphviz-dev "Graphviz PPA") to apt.

### Installation
To install the backend you need a Java SE 7 JDK and Apache Ant. Java versions prior to Java SE 7 are not supported. If those requirements are met, just run `ant` in the `backend` directory. It will create a directory called `dist` and places the installation inside. You may copy the contents of this directory to any location you want. The user which will run the backend needs some filesystem permissions set in the directory of the backend:
* The installation directory must be writeable to create the log files
* The `export/` directory must be writeable
* All other backend files and directories must be readable

After compiling and packaging using `ant` change directory to the location you copied the `dist` contents to and run `./install.sh`. This will install the symbolic link `/usr/bin/visabackend` to allow starting the backend by just typing `visabackend` on the console. This step is optional if you want to run the startup script directly.

To create JavaDoc from the source files run `ant javadoc`. The documentation will be generated in the `doc/` directory.

### Usage
The backend can be started by running `visabackend` on the command line, if you installed the symbolic link in the last installation step. If you did not, you have to change directory to the location the backend files are installed and manually run the script `./visabackend.sh`. Both ways will start the backend, by default listening on port 8080.

There are some command line switches available to the show information or change parameters of the backend. You can view them on the console by calling `visabackend -h`. The following switches are available:
* `-h, --help`
    * Print usage information and quit
* `-v, --version`
    * Print version information and quit
* `-p [port], --port [port]`
    * Change the listening port for the backend to `[port]`

Additionally, there are the SSL parameters available, which are already set in in the `visabackend.sh` script. You may use these switches to override the values set in the script (the direct parameters are parsed after the parameters in the script), but it is recommended to change them in the startup script instead. The available commands are:
* `--sslkey [path]`
    * Path to the SSL KeyStore file
* `--sslkeyp [pwd]`
    * Password/secret required to access the SSL KeyStore
* `--ssltrust [path]`
    * Path to the SSL TrustStore file
* `--ssltrustp [pwd]`
    * Password/secret required to access the SSL TrustStore

## Frontend
### Preparation
The frontend requires an Apache HTTP Server. It was developed on version 2.2 but may run on earlier versions (2.0 or 2.1) too. The following modules must be installed and enabled:
* `mod_php5`, version 5.3 or later
* `mod_proxy`
* `mod_proxy_http`
* `mod_rewrite`

The following settings should be made in the `php.ini` file to allow the file upload function to work properly:
* `post_max_size = 10M`
* `upload_max_filesize = 10M`

### Installation
Copy the contents of the `frontend/` directory of the distribution to the document root or a subdirectory of any configured vHost. The following filesystem permissions must be set (all apply to the user running the HTTP server):
* All files and directories of the frontend must be readable
* The `import/` directory must be writeable

After copying the files to the desired location the configuration file for the vHost hosting the frontend files must be edited. Inside the `VirtualHost` block the following directives must be added:
* `RewriteEngine On`
* `RewriteOptions Inherit`

Inside the `Directory` block, which describes the directory the frontend files are located in, the following directives must be inserted. If you put the files in a subdirectory and no `Directory` block for that exists in the configuration file, create one.
* `RewriteEngine On`
* `RewriteBase /`
* `RewriteRule ^ajax(.*$) http://localhost:8080/ajax$1 [P]`

This will redirect all AJAX calls from the frontend to the backend listening on port 8080. If you run the backend on another port, adjust the port in the `RewriteRule` directive to match your chosen port.

### Usage
After you completed all installation steps just fire up your browser and open the URL of the frontend on your webserver to start using the VTE. The backend must be running to use the editor at all. If the connection to the backend fails the GUI will block and ask you to start the backend. After you have done that just refresh the page. If it still fails most likely there is something wrong with the configuration you made for the Apache HTTP server.
