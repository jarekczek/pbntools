#!/bin/bash

# jedit options: :folding=explicit:tabSize=2:indentSize=2:noTabs=true:
#
# This is script for downloading Kops tournament, part of PbnTools
#
# Copyright (C) 2013 Jaroslaw Czekalski - jarekczek@poczta.onet.pl
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

dest=$1
if [[ -z $dest ]]; then
  echo "Usage: install_www_tests.sh <www_server_dir>"
  exit
fi

for test in test_4_kops_www_20130807 test_5_pary_www_20130808 \
            test_7_bbo_www_acbl_20130810 test_9_pary_www_szczyrk_20131108 \
            test_10_bbo_www_annamar_1538_php
do
  rm -rf "$dest/$test"
  cp -r $test "$dest"
  if [[ ${test:0:8} == test_10_ ]]; then
    f=$test/*tourney*.html
    f=`basename $f`
    echo $f
    grep php@ $test/$f
    sed 's/php@\(.*\).html/php\?\1/g' $test/$f >"$dest/$test/$f"
  fi
done
