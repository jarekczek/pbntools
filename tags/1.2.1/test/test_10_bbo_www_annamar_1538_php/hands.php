<?php
$f = preg_replace('%.*/%', '', $_SERVER['REQUEST_URI']);
$f = preg_replace('%\?%', '@', $f);
include($f . ".html");
?>
