#!/bin/bash

function dorob_wynik() {
  # 1 - plik pbn z kartami
  # 2 - plik pbn wynikowy, do którego dopiszemy
  # 3 - nr stolu (do stworzenia fikcyjnych nazw graczy)
  # 4 - kontrakt
  # 5 - rozgrywajacy
  # 6 - wynik
  
  #echo dorob_wynik $1 $2 $3 $4 $5 $6
  cat $1 >>$2 &&
  echo [North \"N$3\"] >>$2 &&
  echo [South \"S$3\"] >>$2 &&
  echo [East \"E$3\"] >>$2 &&
  echo [West \"W$3\"] >>$2 &&
  echo [Contract \"$4\"] >>$2 &&
  if [[ $4 != "Pass" ]] ; then
    echo [Declarer \"$5\"] >>$2 &&
    echo [Result \"$6\"] >>$2
  fi &&
  echo "" >>$2 ||
    exit 10
  }
  
function testProg() {
  echo -n "checking $1... "
  "$1" --version >/dev/null 2>&1
  if [[ $? != 0 ]]; then echo "Executing \"$1 --version\" failed."; exit 3; fi
  echo "ok"
}

function testDir() {
  echo -n "checking directory $1... "
  testKat="$1/test"
  rm -rf "$testKat" >nul 2>&1 || { echo "rm failed"; exit 5; }
  x=$(pwd)
  mkdir "$testKat" &&
  cd "$testKat" ||
  { echo "Creating directory $testKat and entering it failed."; exit 5; }
  cd "$x"
  rm -rf "$testKat"
  echo "ok"
}
      
function testKonf() {
  echo -n "Sprawdzam konfiguracje "
  sCurPath=${0%[\\/]*}
  sConfFile=$sCurPath/konfig.sh
  echo "w pliku $sConfFile"
  if [[ ! -a "$sConfFile" ]]; then echo "Brak pliku konfig.sh. Przeprowadz konfiguracje i sprawdz prawo do zapisu w katalogu " $sCurPath; exit; fi
  . "$sConfFile"
  sZmienne="sh wget curl gawk katalog_wyjsciowy"
  for z in $sZmienne; do
    echo -n "$z: "
    wart=${!z}
    if [[ $wart == "" ]]; then echo "Zmienna $z nie jest ustawiona w pliku $sConfFile. Przeprowadz ponownie konfiguracje."; exit; fi
    if [[ ${z:0:7} == katalog ]]; then
      testDir "$wart"
    else
      testProg "$wart"
    fi
  done
  echo "Konfiguracja w pliku $sConfFile jest prawidlowa."
  }
