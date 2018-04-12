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

mkdir -p bbo_myhands_temp
cd bbo_myhands_temp
wgetOpts="-p -k -nc -E --restrict-file-names=windows --wait=1"
wgetOpts="--html-extension --keep-session-cookies --restrict-file-names=windows --wait=1"
wget $wgetOpts "http://www.bridgebase.com/myhands/index.php" --save-cookies=cookies.txt
sleep 1
wget $wgetOpts "http://www.bridgebase.com/myhands/myhands_login.php?t=%2Fmyhands%2Findex.php%3F" --load-cookies=cookies.txt --save-cookies=cookies2.txt --post-file=..\\bbo_post_data.txt
sleep 1
wget $wgetOpts "http://www.bridgebase.com/myhands/hands.php?tourney=5236-1523471493-&offset=0" --load-cookies=cookies.txt
