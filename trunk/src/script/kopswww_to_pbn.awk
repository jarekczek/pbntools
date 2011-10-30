#gawk 3.1.6

BEGIN {
  sRegRozdawal = "<h4>([NESW])<br>((NS)|(EW)|(obie)|(nikt))</h4>"

  asTlum["obie"] = "Both"
  asTlum["nikt"] = "None"
  asTlum["NS"] = "NS"
  asTlum["EW"] = "EW"
  }

/<title>/ {
  iRozd = ""
  sRozdawal = ""
  sZalozenia = ""
  iSide = 0

  sub("<title>ROZDANIE NR", "")
  sub("</title>", "")
  iRozd = $0 + 0
  #print "_" iRozd "_"
  }

sRegRozdawal {
  if (iRozd) {
    if (match($0, sRegRozdawal, a)) {
      sRozdawal = a[1]
      sZalozenia = a[2]
      #print iRozd ":" sRozdawal " " sZalozenia >"/dev/stderr"
      }
    }
  }

/[SHDC]\.gif\"?>&nbsp;/ {
  s = $0
  while (match(s, /[SHDC]\.gif\"?>&nbsp;/)) {
    s = substr(s, RSTART)
    sKolor = substr(s,1,1)
    nPoz1 = RLENGTH+1
    nPoz2 = ""
    if (match(s, "<")) nPoz2 = RSTART
    if (nPoz1 && nPoz2) {
      if (sKolor=="S") sSide += 1
      asKarty[sSide,sKolor] = substr(s, nPoz1, nPoz2-nPoz1)
      #print sSide, sKolor, asKarty[sSide,sKolor]
      }
    s = substr(s, nPoz2)
    }
  }

function Karty(sSide) {
  return asKarty[sSide, "S"] "." asKarty[sSide, "H"] "." asKarty[sSide, "D"] "." asKarty[sSide, "C"]
  }

/<\/table>/ {
  if (iRozd) {
    #print FILENAME >"/dev/stderr"
    sPlikOut = ""
    if (match(FILENAME, /\.([^\.]+)$/, a)) {
      sPlikOut = substr(FILENAME, 1, RSTART-1) ".pbn"
      if (match(sPlikOut, /\\p([0-9]+)\./, a)) {
        sPlikOut = substr(sPlikOut,1,RSTART+1) sprintf("%02d",a[1]) substr(sPlikOut,RSTART+RLENGTH-1)
        }
      print FILENAME " -> " sPlikOut
      # >"/dev/stderr"
      }
    print sprintf("[Board \"%02d\"]", iRozd) >sPlikOut
    print "[Dealer \"" sRozdawal "\"]" >>sPlikOut
    print "[Vulnerable \"" asTlum[sZalozenia] "\"]" >>sPlikOut
    # on chce w kolejnoœci NESW, a my mamy w NWES
    print "[Deal \"N:" Karty(1) " " Karty(3) " " Karty(4) " " Karty(2) "\"]" >>sPlikOut
    iRozd = ""
    }
  }
