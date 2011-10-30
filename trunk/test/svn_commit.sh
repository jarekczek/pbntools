#!/bin/bash

rm -rf /tmp/pbncode
mkdir /tmp/pbncode &&
svn checkout --username=jarekczek svn+ssh://jarekczek@svn.code.sf.net/p/pbntools/code/trunk /tmp/pbncode &&
version=`sed -n 's/^wersja=\(.*\)$/\1/p' ../src/jc/pbntools/PbnTools.properties` &&
rev=`sed -n 's/^build.number=\(.*\)$/\1/p' ../build.number` &&
echo $version $rev &&
7z x -y ../release/PbnTools_1_0_0_r206_src.zip -o/tmp/pbncode
cd /tmp/pbncode
svn status


