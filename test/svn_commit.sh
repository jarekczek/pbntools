#!/bin/bash

# rm -rf /tmp/pbncode
# mkdir /tmp/pbncode &&
#svn checkout --username=jarekczek svn+ssh://jarekczek@svn.code.sf.net/p/pbntools/code/trunk /tmp/pbncode &&
pbndir=`dirname $0`/..
version=`sed -n 's/\./_/g; s/^wersja=\(.*\)$/\1/p' $pbndir/src/jc/pbntools/PbnTools.properties` &&
rev=`sed -n 's/^build.number=\(.*\)$/\1/p' $pbndir/build.number` &&
echo $version $rev &&
7z x -y $pbndir/release/PbnTools_${version}_r${rev}_src.zip -o/tmp/pbncode &&
cd /tmp/pbncode &&
svn status

