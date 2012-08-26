#!/bin/bash

# jedit options: :folding=explicit:tabSize=2:indentSize=2:noTabs=true:
#
# This is script for downloading Pary tournament, part of PbnTools
#
# Copyright (C) 2011-2 Jaroslaw Czekalski - jarekczek@poczta.onet.pl
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

# Downloads tournament in Pary format in a way suitable to serve locally
# by apaches. Due to Ajax usage it's not possible to use it directly,
# because txt files would not get in their place.

shopt -s extglob
mkdir -p tur_pary_example
cd tur_pary_example
data=120510
wars=http://www.warsbrydz.pl/wyniki/wob
wget -p -k -nH -nd -r -l 3 -w 2 --random-wait -e robots=off \
  -R "W-*.html,H-*.html" -N $wars/WB$data/wb$data.html
for p in WB$data[0-9]*.html; do
  sleep 2
  t=${p%.html}.txt
  wget $wars/WB$data/$t
  sed -i 's|images/||g' $t
done
