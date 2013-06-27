<?php
$file = basename($_REQUEST["exportFile"]);
header("Content-disposition: attachment; filename=" . $file);
header("Content-type: application/rdf+xml");
readfile($_REQUEST["exportFile"]);
?>