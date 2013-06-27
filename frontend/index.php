<!DOCTYPE html>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<title data-localize="title">VISA Topologie Editor</title>
		<link rel="shortcut icon" href="res/img/favicon.ico" />
		<link rel="stylesheet" href="css/overcast/jquery-ui-1.9.2.custom.min.css" type="text/css" media="screen" />
		<link rel="stylesheet" href="css/jquery.qtip.css" type="text/css" media="screen" />
		<link rel="stylesheet" href="css/jquery.contextMenu.css" type="text/css" media="screen" />
		<link rel="stylesheet" href="css/bootstrap.min.css" type="text/css" media="screen" />
		<link rel="stylesheet" href="css/style.css" type="text/css" media="screen" />
		<script type="text/javascript" src="js/jquery-1.8.3.min.js"></script>
		<script type="text/javascript" src="js/jquery-ui-1.9.2.custom.min.js"></script>
		<script type="text/javascript" src="js/jquery.blockUI.js"></script>
		<script type="text/javascript" src="js/jquery.qtip.min.js"></script>
		<script type="text/javascript" src="js/jquery.localize.js"></script>
		<script type="text/javascript" src="js/jquery.cookie.js"></script>
		<script type="text/javascript" src="js/jquery-collision.min.js"></script>
		<script type="text/javascript" src="js/jquery-ui-draggable-collision.min.js"></script>
		<script type="text/javascript" src="js/jquery.fineuploader-3.0.min.js"></script>
		<script type="text/javascript" src="js/jquery.contextMenu.js"></script>
		<script type="text/javascript" src="js/json2.js"></script>
		<script type="text/javascript" src="js/pathfinder.js"></script>
		<script type="text/javascript" src="js/visa-script.js"></script>
		<script type="text/javascript">
			VISA.documentRoot = "<?php echo dirname($_SERVER["SCRIPT_FILENAME"]);?>";
		</script>
	</head>
	<body>
		<div id="pagecontainer">
			<div class="ui-widget pagehead">
				<div class="ui-widget-header ui-corner-all ui-helper-clearfix">
					<img class="visalogo" src="res/img/visa_logo.png" alt="VISA" /><h1 class="headline" data-localize="headline">Topologie Editor</h1>
				</div>
			</div>

			<div class="ui-helper-clearfix">
				<!-- Begin Topology pane -->
				<div class="topologypane ui-widget">
					<div class="ui-widget-header ui-corner-top">
						<h2 data-localize="topology.title">Topologie</h2>
					</div>
					<div class="ui-widget-content ui-corner-bottom ui-helper-clearfix topooptioncontent">
						<div class="optionbox">
							<div id="optTopoConfig" class="btn btn-small" data-localize="topology.conf">Konfigurieren</div>
						</div>
						<div class="optionbox">
							<div id="optTopoIOTool" class="btn btn-small" data-localize="topology.iotool">IO-Tool</div>
						</div>
						<div class="optionbox">
							<div id="optRDFDo" class="btn btn-small" data-localize="topology.rdf">RDF/XML</div>
						</div>
						<div class="optionbox">
							<div id="optNetworkDo" class="btn btn-small" data-localize="topology.network">Netzwerk</div>
						</div>
						<div class="optionbox">
							<div id="optResetDo" class="btn btn-small" data-localize="topology.reset">Topologie zurücksetzen</div>
						</div>
					</div>
				</div>
				<!-- End Topology pane-->

				<!-- Begin Options pane -->
				<div class="optionspane ui-widget">
					<div class="ui-widget-header ui-corner-top">
						<h2 data-localize="options.title">Optionen</h2>
					</div>
					<div class="ui-widget-content ui-corner-bottom ui-helper-clearfix topooptioncontent">
						<div class="optionbox">
							<input id="optPathFinding" type="checkbox" checked="checked" />
							<label id="optPathFindingLabel" for="optPathFinding" data-localize="options.routing">Wegfindung</label>
						</div>
						<div class="optionbox">
							<input id="optShowNames" type="checkbox" />
							<label id="optShowNamesLabel" for="optShowNames" data-localize="options.names">Namen anzeigen</label>
						</div>
						<div class="optionbox">
							<input id="optShowVLAN" type="checkbox" />
							<label id="optShowVLANLabel" for="optShowVLAN" data-localize="options.vlans">VLAN anzeigen</label>
						</div>
						<div class="optionbox">
							<div id="optShutdownDo" class="btn btn-small" data-localize="options.shutdown">Backend herunterfahren</div>
						</div>
						<div class="optionbox">
							<label id="optLanguageLabel" for="optLanguage" data-localize="options.language.label">Sprache:</label>
							<select id="optLanguage" name="optLanguage" class="input-medium selectMarginBottom">
								<option value="de" data-localize="options.language.de">Deutsch</option>
								<option value="en" data-localize="options.language.en">Englisch</option>
							</select>
						</div>
					</div>
				</div>
				<!-- End Options pane-->
			</div>

			<!-- Begin Drag area -->
			<div id="drag"> <!-- style="visibility: hidden;"-->
				<!-- Begin Component inventory -->
				<div id="componentpane" class="ui-widget">
					<div class="ui-widget-header ui-corner-top">
						<h2 data-localize="components.title">Komponenten</h2>
					</div>
					<div id="inventorytabs" class="ui-widget-content ui-corner-bottom ui-helper-clearfix">
						<div id="inventory">
							<!-- Content will be generated by JavaScript -->
						</div>
					</div>
				</div>
				<!-- End Component inventory -->

				<!-- Begin Editor grid -->
				<div id="editorpane" class="editor ui-widget">
					<div class="ui-widget-header ui-corner-top">
						<h2 data-localize="editor.title">Editor</h2>
					</div>
					<div class="ui-widget-content ui-helper-clearfix">
						<div id="dragcontainment" class="dragcontainment">
							<table id="editorgrid" class="edittable">
								<!-- Content will be generated by JavaScript -->
							</table>
						</div>
					</div>
					<div class="ui-widget-header ui-corner-bottom">
						<h2 class="rightalign" id="topologyID">&nbsp;</h2>
					</div>
				</div>
				<!-- End Editor grid -->

				<div id="dragboxContainer"></div>
			</div>
			<!-- End Drag area -->
		</div>

		<!-- Begin dialog definitions -->
		<div id="dialogs">
			<!-- Begin create component dialog -->
			<div id="createCompDialog" title="Komponente erstellen">
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.createcomp.propertybox.title">Eigenschaften</div>
					<div class="ui-widget-content dialogWidgetContent">
						<label for="createCompDialogName" data-localize="dialog.createcomp.propertybox.name">Name:</label><br />
						<input id="createCompDialogName" type="text" name="compname" /><span class="dialogInfoText" data-localize="dialog.createcomp.propertybox.help">$ID$ wird durch interne ID ersetzt</span><br />
						<label for="createCompDialogSizeX" data-localize="dialog.createcomp.propertybox.width">Breite:</label><input id="createCompDialogSizeX" class="input-mini" type="text" name="compsizeX" />
						<label for="createCompDialogSizeY" data-localize="dialog.createcomp.propertybox.height">Höhe:</label><input id="createCompDialogSizeY" class="input-mini" type="text" name="compsizeY" />
						<div id="createCompDialogSizeApply" class="btn btn-small" data-localize="dialog.createcomp.propertybox.sizeapply">Übernehmen</div><br />
						<label for="createCompDialogIfnum" data-localize="dialog.createcomp.propertybox.ifnum">Anzahl Interfaces:</label><span id="createCompDialogMaxIfnum"></span><br />
						<input id="createCompDialogIfnum" type="text" name="ifnum" value="1" />
						<div id="createCompDialogIfnumApply" class="btn btn-small" data-localize="dialog.createcomp.propertybox.ifnumapply">Übernehmen</div>
					</div>
				</div>
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.createcomp.interfacebox.title">Interfaces</div>
					<div class="ui-widget-content dialogWidgetContent">
						<table id="createCompDialogIfBox">
							<!-- Content will be generated by JavaScript -->
						</table>
					</div>
				</div>
			</div>
			<!-- End create component dialog -->

			<!-- Begin create VSA dialog -->
			<div id="createVSADialog" title="Neue VSA erstellen">
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.createvsa.selectbox.title">VSA-Typ</div>
					<div class="ui-widget-content dialogWidgetContent">
						<label for="" data-localize="dialog.createvsa.selectbox.label">VSA-Typ wählen</label>
						<select id="createVSADialogSelect" class="input-medium" disabled="disabled">
							<option value="disabled" data-localize="dialog.createvsa.selectbox.novsaoption">Keine VSA Templates gefunden</option>
						</select>
					</div>
				</div>
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.createvsa.propertybox.title">Eigenschaften</div>
					<div id="createVSADialogProperties" class="ui-widget-content dialogWidgetContent">
						<!-- Content will be generated by JavaScript -->
					</div>
				</div>
			</div>
			<!-- End create VSA dialog -->

			<!-- Begin cable dialog -->
			<div id="cableDialog" title="Detailansicht - Kabel">
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.cable.componentbox.title">Komponenten</div>
					<div class="ui-widget-content dialogWidgetContent">
						<p id="cableDialogLeft"></p>
						<p id="cableDialogRight"></p>
						<p id="cableDialogVLAN"  class='ui-helper-clearfix'></p>
					</div>
				</div>
				<div id="cableDialogOptionsWidget" class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.cable.optionbox.title">Optionen</div>
					<div class="ui-widget-content dialogWidgetContent">
						<input id="cableDialogRemove" type="button" value="Kabel entfernen" data-localize="dialog.cable.optionbox.button" />
						<input id="cableDialogID" type="hidden" value="" />
					</div>
				</div>
			</div>
			<!-- End cable dialog -->

			<!-- Begin IO-Tool dialog -->
			<div id="iotoolDialog" title="IO-Tool">
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.iotool.title">IO-Tool</div>
					<div class="ui-widget-content dialogWidgetContent">
						<div id="iotoolDialogIOToolConnect">
							<p class="dialogHeadline" data-localize="dialog.iotool.hostname">Hostname / IP-Adresse</p>
							<input id="iotoolDialogHostname" name="ioHostname" type="text" value="" />

							<p class="dialogHeadline" data-localize="dialog.iotool.port">Portnummer</p>
							<input id="iotoolDialogPort" name="ioPort" type="text" value="" />

							<input id="iotoolDialogConnect" class="btn btn-small" type="button" value="Verbinden" data-localize="dialog.iotool.connbutton" />
						</div>
						<div id="iotoolDialogIOToolOptions" style="display: none;">
							<!-- Content will be generated by JavaScript -->
						</div>
					</div>
				</div>
			</div>
			<!-- End IO-Tool dialog -->

			<!-- Begin RDF dialog -->
			<div id="rdfDialog" title="RDF/XML">
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.rdf.importbox.title">Importieren</div>
					<div class="ui-widget-content dialogWidgetContent">
						<input id="rdfDialogImportReplace" type="radio" name="replace" value="true" checked="checked" />
						<label id="rdfDialogImportReplaceLabel" for="rdfDialogImportReplace" data-localize="dialog.rdf.importbox.replace">Topology ersetzen</label><br />
						<input id="rdfDialogImportIntegrate" type="radio" name="replace" value="false" />
						<label id="rdfDialogImportIntegrate" for="rdfDialogImportIntegrate" data-localize="dialog.rdf.importbox.append">In Topologie integrieren</label><br />
						<div id="rdfDialogImport" class="btn btn-small" data-localize="dialog.rdf.importbox.button">RDF/XML Datei hochladen</div>
						<p id="rdfDialogUploadStatus"></p>
					</div>
				</div>
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.rdf.exportbox.title">Exportieren</div>
					<div class="ui-widget-content dialogWidgetContent">
						<p class="dialogHeadline" data-localize="dialog.rdf.exportbox.filename">Dateiname</p>
						<input id="rdfDialogFilename" name="exportFilename" type="text" value="visa_export.rdf" />
						<div id="rdfDialogExport" class="btn btn-small" data-localize="dialog.rdf.exportbox.button">Exportieren</div>

						<p class="dialogHeadline" data-localize="dialog.rdf.exportbox.status.title">Status</p>
						<p id="rdfDialogStatus"></p>

						<p class="dialogHeadline" data-localize="dialog.rdf.exportbox.download.title">Herunterladen</p>
						<input id="rdfDialogDownload" class="btn btn-small" type="button" value="Herunterladen" disabled="disabled" data-localize="dialog.rdf.exportbox.download.button" />
					</div>
				</div>
			</div>
			<!-- End RDF dialog -->

			<!-- Begin topology dialog -->
			<div id="topologyDialog" title="Topologie konfigurieren">
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.topology.name.title">Identifikation</div>
					<div class="ui-widget-content dialogWidgetContent">
						<p class="dialogHeadline" data-localize="dialog.topology.name.topoid">Topologie-ID:</p>
						<p id="topologyDialogID"></p>

						<p class="dialogHeadline" data-localize="dialog.topology.name.toponame">Name:</p>
						<input id="topologyDialogName" name="topoName" type="text" value="" />
						<input id="topologyDialogOldName" name="topoName" type="hidden" value="" />
					</div>
				</div>
			</div>
			<!-- End topology dialog -->

			<!-- Begin VLAN dialog -->
			<div id="networkDialog" title="VLANs verwalten">
				<div class="ui-widget dialogWidget">
					<div id="newNetworkpanel">
						<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.network.newnetwork.title">Neues Netzwerk</div>
						<div class="ui-widget-content dialogWidgetContent">
							<p class="dialogHeadline" data-localize="dialog.network.newnetwork.address">Netzwerk IP Adresse:</p>
							<input id="networkDialogNewIP" name="netIP" type="text" value="" />

							<p class="dialogHeadline" data-localize="dialog.network.newnetwork.subnet">Subnetzmaske (CIDR):</p>
							<input id="networkDialogNewSubnet" name="netSubnet" type="text" value="24" /><br />

							<p class="dialogHeadline" data-localize="dialog.network.newnetwork.version">IP Version:</p>
							<select id="networkDialogNewVersion" name="newnetwork">
								<option value="ipv4" selected="selected">IPv4</option>
								<option value="ipv6">IPv6</option>
							</select><br />

							<div id="networkDialogCreate" class="btn btn-small" data-localize="dialog.network.newnetwork.button">Erstellen</div>
						</div>
					</div>
				</div>
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.network.existingnetwork.title">Vorhandene Subnetze</div>
					<div class="ui-widget-content dialogWidgetContent">
						<table id="networkDialogNetworkTable">
							<tbody id="networkDialogExistingSubnets">
								<tr class="trborder">
									<th data-localize="dialog.network.existingnetwork.address">IP Adresse</th>
									<th data-localize="dialog.network.existingnetwork.subnetmask">Subnetzmaske</th>
									<th data-localize="dialog.network.existingnetwork.version">IP Version</th>
								</tr>
							</tbody>
						</table>
					</div>
				</div>
				<div class="ui-widget dialogWidget">
					<div id="newVLANpanel">
						<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.network.newvlan.title">Neues VLAN</div>
						<div class="ui-widget-content dialogWidgetContent">
							<p class="dialogHeadline" data-localize="dialog.network.newvlan.name">Name:</p>
							<input id="vlanDialogNewName" name="vlanName" type="text" value="" />

							<p class="dialogHeadline" data-localize="dialog.network.newvlan.color">Farbe (#RRGGBB):</p>
							<input id="vlanDialogNewColor" name="vlanColor" type="text" value="" /><br />

							<div id="vlanDialogCreate" class="btn btn-small" data-localize="dialog.network.newvlan.button">Erstellen</div>
						</div>
					</div>
					<div id="editVLANpanel" style="display: none;">
						<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.network.editvlan.title">VLAN bearbeiten</div>
						<div class="ui-widget-content dialogWidgetContent">
							<p class="dialogHeadline" data-localize="dialog.network.editvlan.name">Name:</p>
							<input id="vlanDialogEditName" name="vlanName" type="text" value="" />

							<p class="dialogHeadline" data-localize="dialog.network.editvlan.color">Farbe (#RRGGBB):</p>
							<input id="vlanDialogEditColor" name="vlanColor" type="text" value="" /><br />

							<div id="vlanDialogSaveEdit" class="btn btn-small" data-localize="dialog.network.editvlan.buttons.save">Speichern</div>
							<div id="vlanDialogCancelEdit" class="btn btn-small" data-localize="dialog.network.editvlan.buttons.cancel">Abbrechen</div>
							<input id="vlanDialogEditLocName" name="vlanLocName" type="hidden" value="" />
						</div>
					</div>
				</div>
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.network.existingvlan.title">Vorhandene VLANs</div>
					<div class="ui-widget-content dialogWidgetContent">
						<table id="vlanDialogVLANTable">
							<tbody id="vlanDialogExistingVLANs">
								<tr class="trborder">
									<th data-localize="dialog.network.existingvlan.name">Name</th>
									<th data-localize="dialog.network.existingvlan.color">Farbe</th>
									<th data-localize="dialog.network.existingvlan.ifcount"># Switch-Interfaces</th>
									<th></th>
								</tr>
							</tbody>
						</table>
					</div>
				</div>
			</div>
			<!-- End VLAN dialog -->

			<!-- Begin component dialog -->
			<div id="componentDialog" title="Komponente konfigurieren">
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.comp.identbox.title">Identifikation</div>
					<div class="ui-widget-content dialogWidgetContent">
						<table>
							<tbody>
								<tr>
									<th style="width: 70px;" data-localize="dialog.comp.identbox.name">Name:</th>
									<td><input id="componentDialogName" type="text" name="compname" value="" /><span class="dialogInfoText" data-localize="dialog.createcomp.propertybox.help">$ID$ wird durch interne ID ersetzt</span></td>
								</tr>
								<tr>
									<th style="width: 70px;" data-localize="dialog.comp.identbox.id">ID:</th>
									<td id="componentDialogID"></td>
								</tr>
								<tr>
									<th style="width: 70px;" data-localize="dialog.comp.identbox.type">Typ:</th>
									<td id="componentDialogType"></td>
								</tr>
							</tbody>
						</table>
					</div>

					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.comp.ifbox.title">Interfaces</div>
					<div id="componentDialogIfList" class="ui-widget-content dialogWidgetContent">
						<!-- Content will be generated by JavaScript -->
					</div>
				</div>
			</div>
			<!-- End component dialog -->

			<!-- Begin assign VLAN dialog -->
			<div id="vlanAssignDialog" title="VLAN-Zuordnung ändern">
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.comp.identbox.title">Identifikation</div>
					<div class="ui-widget-content dialogWidgetContent">
						<table>
							<tbody>
								<tr>
									<th style="width: 90px;" data-localize="dialog.comp.vlanassign.compname">Komponente:</th>
									<td id="vlanAssignDialogComponent"></td>
								</tr>
								<tr>
									<th style="width: 90px;" data-localize="dialog.comp.vlanassign.ifname">Interface:</th>
									<td id="vlanAssignDialogInterface"></td>
								</tr>
							</tbody>
						</table>
					</div>
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.comp.vlanassign.title">Vorhandene VLANs</div>
					<div id="vlanAssignDialogList" class="ui-widget-content dialogWidgetContent">
						<!-- Content will be generated by JavaScript -->
					</div>
				</div>
			</div>
			<!-- End assign VLAN dialog -->

			<!-- Begin reset dialog -->
			<div id="resetDialog" title="Topologie zurücksetzen">
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.reset.clearbox.title">Topologie leeren</div>
					<div class="ui-widget-content dialogWidgetContent">
						<input id="resetDialogClear" type="radio" name="reset" value="-1" checked="checked" />
						<label for="resetDialogClear" data-localize="dialog.reset.clearbox.radio">Topolgie leeren</label><br />
					</div>
				</div>
				<div class="ui-widget dialogWidget">
					<div class="ui-widget-header dialogWidgetHeader" data-localize="dialog.reset.statebox.title">Auf Zustand zurücksetzen</div>
					<div id="resetDialogStateList" class="ui-widget-content dialogWidgetContent">
						<p data-localize="dialog.reset.statebox.nostates">Es existieren keine gespeicherten Zustände</p>
						<!-- Content will be generated by JavaScript -->
					</div>
				</div>
			</div>
			<!-- End reset dialog -->

			<!-- Begin subgrid dialog template -->
			<div id="subgridDialogTemplate" title="Komponentengruppe" class="staticDialogContent">
				<div class="ui-widget-header dialogWidgetHeader" data-localize="components.title">Komponenten</div>
				<div id="subgridInventorytabs" class="ui-widget-content ui-helper-clearfix subgridInventory">
					<!-- Content will be generated by JavaScript -->
				</div>
				<div class="ui-widget-header dialogWidgetHeader" data-localize="editor.title">Editor</div>
				<div class="ui-widget-content ui-helper-clearfix dialogWidgetEditor">
					<div id="subgridDragcontainment" class="subgridDragcontainment">
						<table id="subeditorgrid" class="edittable">
							<!-- Content will be generated by JavaScript -->
						</table>
					</div>
				</div>
				<div id="subgridDragboxContainer"></div>
			</div>
			<!-- End subgrid dialog template -->

			<!-- Begin console dialog template -->
			<div id="consoleDialog" title="Konsole">
				<div class="ui-widget-content ui-helper-clearfix dialogWidgetEditor">
					<div id="logConsole" class="logConsole">
						<p class="logLine">0 Konsolentesteintrag!</p>
						<p class="logLine">1 Konsolentesteintrag!</p>
						<p class="logLine">2 Konsolentesteintrag!</p>
						<p class="logLine">3 Konsolentesteintrag!</p>
						<p class="logLine">4 Konsolentesteintrag!</p>
						<p class="logLine">5 Konsolentesteintrag!</p>
						<p class="logLine">6 Konsolentesteintrag!</p>
						<p class="logLine">7 Konsolentesteintrag!</p>
						<p class="logLine">8 Konsolentesteintrag!</p>
						<p class="logLine">9 Konsolentesteintrag!</p>
						<p class="logLine">10 Konsolentesteintrag!</p>
						<p class="logLine">11 Konsolentesteintrag!</p>
						<p class="logLine">12 Konsolentesteintrag!</p>
						<p class="logLine">13 Konsolentesteintrag!</p>
						<p class="logLine">14 Konsolentesteintrag!</p>
						<p class="logLine">15 Konsolentesteintrag!</p>
						<p class="logLine">16 Konsolentesteintrag!</p>
						<p class="logLine">17 Konsolentesteintrag!</p>
						<p class="logLine">18 Konsolentesteintrag!</p>
						<p class="logLine">19 Konsolentesteintrag!</p>
						<p class="logLine">Letzter Konsolentesteintrag!</p>
						<!-- Content will be generated by JavaScript -->
					</div>
				</div>
			</div>
			<!-- End console dialog template -->

			<!-- Begin confirm dialog -->
			<div id="confirmDialog">
				<p id="confirmDialogMsg"></p>
			</div>
			<!-- End confirm dialog -->
		</div>
		<!-- End dialog definitions -->

		<!-- Begin block message definitions -->
		<div id="blockMsgs" style="display: none;">
			<!-- Begin backend not available message -->
			<div id="blockBackendDown" class="ui-widget">
				<div class="ui-widget-header dialogWidgetHeader" data-localize="blockui.nobackend.title">Editor Backend nicht erreichbar</div>
				<div class="ui-widget-content dialogWidgetContent">
					<p data-localize="blockui.nobackend.text">Das Editor Backend ist nicht erreichbar. Bitte das Backend starten und Seite neu laden.</p>
				</div>
			</div>
			<!-- End backend not available message -->

			<!-- Begin IO-Tool communication message -->
			<div id="blockIOToolComm" class="ui-widget">
				<div class="ui-widget-header dialogWidgetHeader" data-localize="blockui.iotoolcomm.title">Bitte warten</div>
				<div class="ui-widget-content dialogWidgetContent">
					<p data-localize="blockui.iotoolcomm.text">Der Editor kommuniziert mit dem IO-Tool, dies kann einige Zeit dauern...</p>
				</div>
			</div>
			<!-- End IO-Tool communication message -->

			<!-- Begin reset backend message -->
			<div id="blockBackendReset" class="ui-widget">
				<div class="ui-widget-header dialogWidgetHeader" data-localize="blockui.reset.title">Bitte warten</div>
				<div class="ui-widget-content dialogWidgetContent">
					<p data-localize="blockui.reset.text">Das Editor Backend wird zurückgesetzt. Die Seite wird nach Abschluss neu geladen.</p>
				</div>
			</div>
			<!-- End reset backend message -->

			<!-- Begin processing topology information message -->
			<div id="blockBuildTopology" class="ui-widget">
				<div class="ui-widget-header dialogWidgetHeader" data-localize="blockui.buildtopology.title">Bitte warten</div>
				<div class="ui-widget-content dialogWidgetContent">
					<p data-localize="blockui.buildtopology.text">Verarbeite Topologieinformationen, dies kann einige Zeit dauern...</p>
				</div>
			</div>
			<!-- End processing topology information message -->
		</div>
		<!-- End block message definitions -->
	</body>
</html>
