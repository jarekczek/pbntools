#!/bin/bash

sCurPath=${0%[\\/]*}
#. "$sCurPath/konfig.sh"
fin=$1
if [[ -z $gawk ]]; then gawk=gawk; fi
$gawk -f "$sCurPath/kopswww_to_pbn.awk" $fin

