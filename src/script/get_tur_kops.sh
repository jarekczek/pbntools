#!/bin/bash

function trapTerm() {
  # kill wget if we get killed
  # java uses TERM for Process.destroy()
  if [[ -n $wgetPid ]]; then kill $wgetPid; fi
  # 143 is usual bash code for TERM signal termination
  exit 143
}
trap trapTerm TERM

shopt -s extglob
# turn on job control
set -m
logfile=`dirname $0`/`basename $0` || exit
logfile=${logfile%\.sh}.log

function usage() {
  echo "Usage: get_tur_kops.sh <kops_tournament_link>"
  echo "kops_tournament_link - a link to whole tournament, including results"
  echo "-d <path> - a path to output directory"
  exit
  #link=http://slzbs.livenet.pl/protokoly//01/chorzow/10/LCH0215/LCH0215.html
}

function parseArgs() {
  cArg=0
  i=1
  while [[ -n ${!i} ]]; do
    arg=${!i}
    #echo "arg $i: $arg"
    if [[ ${arg:0:1} == "-" ]]; then
      if [[ ${arg:1} == "d" ]]; then
        i=$(($i+1))
        mainOutputDir=${!i}
      fi
    else
      cArg=$(($cArg + 1))
      if [[ $cArg == 1 ]]; then link=$arg; fi
    fi
    i=$(($i+1))
  done
}

{

parseArgs "$@"
if [[ -z $link ]]; then echo "no link given" >&2; usage; fi
if [[ -z $mainOutputDir ]]; then echo "working directory (-d) must be given" >&2; usage; fi

sCurPath=${0%[\\/]*}
. "$sCurPath/tur_fun.sh" || exit 2
#testKonf

# there was a possibility to configure tool paths, but it's deprecated
#. "$sCurPath/konfig.sh" || exit 2
gawk=gawk
wget=wget
testProg sed
testProg $gawk
testProg $wget
testDir $mainOutputDir

sOutDir="$mainOutputDir/kops"
bCzysc=1
bDownLoad=1
echo "Output directory: $sOutDir"
sNazwaTur=$(echo $link | sed "s/\.html*$//; s/\/index$//; s/^.*\/\([^\/]*\)$/\1/")
echo NazwaTur:$sNazwaTur

function dos_file_name() {
  echo $1 | sed "s/\//\\\\/g"
  }

function dos_exec() {
  #echo $1
  cmd.exe "/c $1"
  }

function zrob_wyniki() {
  #local fin, sBezRozsz, fout, fpbn, workdir, i, w, iPlayer, sContract, sDeclarer, sResult
  #local sLinia, sWho
  fin=$1
  sBezRozsz=${fin%.*}
  fout=${sBezRozsz}_wyn.pbn
  fpbn=$sBezRozsz.pbn
  workdir=${fout%/*}
  echo "$fin -> $fout"
  rm -f $fout
  # nt contracts are ok, but colored have to be extracted from gif names
  sed 's/<img[^>]*[\/\"]\([SHCD]\)\.gif[^>]*>/\1/g; s/<\/td>//g; s/>//g;' $fin >$workdir/temp2.txt
  "$gawk" -F \<td '{ if (match($5,/^[NESW]$/)) { print $4 " " $5 " " $6 " " $7 " " 6+$4+$7}
                 if (match($4,"pasy")) { print "Pass          "} }' $workdir/temp2.txt >$workdir/temp3.txt
  iStol=0
 
  while read sLinia; do
    #echo $sLinia
    i=0
    for w in $sLinia; do
      i=$(( $i + 1 ))
      case $i in
        1) sContract=$w ;;
        2) sDeclarer=$w ;;
        5) sResult=$w ;;
      esac
    done
    iStol=$(( $iStol + 1 ))
    dorob_wynik $fpbn $fout $iStol $sContract $sDeclarer $sResult
  done <$workdir/temp3.txt
  }

if [[ $bCzysc > 0 ]]; then rm -rf $sOutDir; mkdir $sOutDir; fi
link0=$link
link=${link%/*}/roz.html
echo "Okrajamy link z ostatniego czlonu i wychodzi: $link"
{ echo '<a href="'$sNazwaTur'/index.html">'$sNazwaTur'</a>';
  echo '  <a href="'${sNazwaTur}'/'${sNazwaTur}'_wyn.pbn">pbn</a>';
  echo '  <a href="'$link0'">'pzbs'</a>&nbsp;<br>';
  } >$sOutDir/link.html
echo '<title>'$sNazwaTur'</title>' >$sOutDir/index.html
echo '<h1>'$sNazwaTur'</h1>' >>$sOutDir/index.html
echo '<a href="' $link0 '">link do pzbs</a><br>' >>$sOutDir/index.html
echo '<a href="'${sNazwaTur}'_wyn.pbn">pbn</a>' >> $sOutDir/index.html

if [[ $bDownLoad > 0 ]]; then
#  while [[ a ]]; do a=1; done
  "$wget" -P $sOutDir --page-requisites --no-directories --wait=1 --level=1 --recursive --convert-links $link 2>&1 &
  wgetPid=$!
  wait % ||
  { err=$?; echo "error code: $err"; exit $?; }
  wgetPid=""
fi
if [[ ! -a $sOutDir/roz.html ]]; then
  echo "brak pliku roz.html"
  exit 1
fi
for p in $sOutDir/p*.html; do
  p2=$(echo $p | sed 's/p\([0-9]\.\)/p0\1/')
  if [[ $p2 != $p ]]; then mv -f $p $p2; fi
  gawk=$gawk "$sCurPath/kopswww_to_pbn.sh" "$p2" || exit 6
done
for p in $sOutDir/p+([0-9]).html; do
  zrob_wyniki $p || exit 7
done

#cat $sOutDir/p+([0-9]).pbn >$sOutDir/$sNazwaTur.pbn
rm -f $sOutDir/$sNazwaTur.pbn
for p in $sOutDir/p+([0-9]).pbn; do
  { cat $p && echo "" || exit 8; } >>$sOutDir/$sNazwaTur.pbn
done

cat $sOutDir/p+([0-9])_wyn.pbn >$sOutDir/${sNazwaTur}_wyn.pbn || exit 9

#read -n1 -pgotowe

#} 2>&1 | tee $logfile
}

