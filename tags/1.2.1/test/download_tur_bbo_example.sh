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

if [[ -z $1 ]]; then
  tour="2797-1361119500"
  tour="7883-1375722000"
  tour="1607-1376154000"
else
  tour=$1
fi
mkdir -p tur_bbo_example_$tour
cd tur_bbo_example_$tour
wgetOpts="-p -k -nc -E --restrict-file-names=windows --wait=1"
wget $wgetOpts -r -l 1 "http://www.bridgebase.com/myhands/hands.php?tourney=$tour-&offset=0"
wget $wgetOpts "http://webutil.bridgebase.com/v2/tview.php?t=$tour"
# to make it possible to obtain correct tournament title in offline mode:
cp webutil.bridgebase.com/v2/tview*.html www.bridgebase.com/myhands
