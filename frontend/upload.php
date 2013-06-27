<?php
/************************
 * Based on the server-side example from server/php.php
 * file of GitHub project https://github.com/valums/file-uploader
 ************************/

// list of valid extensions, ex. array("jpeg", "xml", "bmp")
$allowedExtensions = array("rdf");
// max file size in bytes
$sizeLimit = 10 * 1024 * 1024; // 10MB

require("fineuploader.php");
$uploader = new qqFileUploader($allowedExtensions, $sizeLimit);

// Call handleUpload() with the name of the folder, relative to PHP's getcwd()
$result = $uploader->handleUpload("import/");
chmod("import/" . $uploader->getUploadName(), 0666);

// Include local and remote filenames in AJAX response
$result["local_file"] = $uploader->getName();
$result["remote_file"] = $uploader->getUploadName();

// to pass data through iframe you will need to encode all html tags
echo htmlspecialchars(json_encode($result), ENT_NOQUOTES);
?>