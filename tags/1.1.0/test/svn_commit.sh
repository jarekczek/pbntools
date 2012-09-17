#!/bin/bash

pbndir=`dirname $0`/..
rm -rf /tmp/pbncode
mkdir /tmp/pbncode &&
svnUser=`sed -n 's/\./_/g; s/^svn_user=\(.*\)$/\1/p' $pbndir/local.properties` &&
# svnPass=`sed -n 's/\./_/g; s/^svn_pass=\(.*\)$/\1/p' $pbndir/local.properties` &&
svn checkout --non-interactive --username $svnUser svn+ssh://${svnUser}@svn.code.sf.net/p/pbntools/code/trunk /tmp/pbncode &&
version=`sed -n 's/\./_/g; s/^wersja=\(.*\)$/\1/p' $pbndir/src/jc/pbntools/PbnTools.properties` &&
rev=`sed -n 's/^build.number=\(.*\)$/\1/p' $pbndir/build.number` &&
echo $version $rev &&
7z x -y $pbndir/release/PbnTools_${version}_r${rev}_src.zip -o/tmp/pbncode &&
cd /tmp/pbncode &&
svn status

