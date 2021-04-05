set cookies=Cookie: SRV=aaa; PHPSESSID=xxx
goto dalej

curl -v https://www.bridgebase.com/myhands/hands.php?tourney=17901-1617418791- ^
  -H "%cookies%" ^
  -o hands.php%%3Ftourney=2196-1376162040-.html
  
curl -v https://www.bridgebase.com/myhands/hands.php?traveller=17901-1617418791-72774681 ^
  -H "%cookies%" ^
  -o trav2.html

curl -v https://webutil.bridgebase.com/v2/tview.php?t=17901-1617418791 ^
  -H "%cookies%" ^
  -o tview.html

:dalej

curl -v "https://www.bridgebase.com/myhands/fetchlin.php?id=1047742261&when_played=1617418791" ^
  -H "%cookies%" ^
  -o 1047742261.lin
