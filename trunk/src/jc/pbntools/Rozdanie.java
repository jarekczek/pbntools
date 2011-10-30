/* *****************************************************************************

    Copyright (C) 2011 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
   *****************************************************************************
*/

package jc.pbntools;

import java.io.*;
import java.util.regex.*;
import java.util.*;
import jc.f;
import jc.pbntools.*;
import javazoom.jl.player.Player;

class Reka {
  }
  
class Karta {
  static final int PIK = 1;
  static final int KIER = 2;
  static final int KARO = 3;
  static final int TREFL = 4;
  static final String m_asKolAng[] = { "S", "H", "D", "C" };
  static final char m_achWysok[] = { '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A' };
  private int m_nKod;  // 16*kolor(1-4) + wysokosc karty(2-14); czyli od 18 (S2) do 78 (CA)
  static final int MAX_KOD = 78;

  Karta() { zeruj(); }
  
  static int wysok(char ch) {
    if (ch>='2' && ch <='9') {
      return (ch-'2')+2;
      }
    else if (ch=='T') { return 10; }
    else if (ch=='J') { return 11; }
    else if (ch=='Q') { return 12; }
    else if (ch=='K') { return 13; }
    else if (ch=='A') { return 14; }
    else return 0;
    }
  static char znakWysok(int nWys) { return nWys>=2 && nWys<=14 ? m_achWysok[nWys-2] : '?'; }
  static char znakKolor(int nKolor) { return nKolor>=1 && nKolor<=4 ? m_asKolAng[nKolor-1].charAt(0) : '?'; }

  static int kolor(char ch) {
    int i;
    for (i=0; i<m_asKolAng.length; i++) { if (m_asKolAng[i].charAt(0) == ch) { return i+1; } }
    return 0;
    }
  static int nastKolor(int nKolor) { return (nKolor<=0) ? 0 : (nKolor%4)+1; }
  
  static int kod(int nKolor, int nWys) { return (nKolor>=1 && nKolor<=4 && nWys>=2 && nWys<=14) ? 16*nKolor+nWys : 0; }

  void zeruj() { m_nKod = 0; }
  int getKod() { return m_nKod; }
  int getKolor() { return m_nKod<=0 ? 0 : m_nKod/16; }
  int getWysok() { return m_nKod<=0 ? 0 : m_nKod%16; }
  void setKod(int nKod) { m_nKod = nKod; }
  void set(int nKolor, int nWys) { m_nKod = kod(nKolor, nWys); }
  boolean czyOk() { return m_nKod>0 && m_nKod<=MAX_KOD; }
  
  static int kod(String sKarta) {
    if (sKarta.length()!=2) { return 0; }
    return kod(kolor(sKarta.charAt(0)), wysok(sKarta.charAt(1)));
    }
  
  boolean setKolor(char chKolor) {
    int nKolor;
    m_nKod = m_nKod % 16;
    nKolor = kolor(chKolor);
    if (nKolor>0) { m_nKod += nKolor * 16; return true; }
    else { return false; }
    }

  public String toString() { return "" + znakKolor(getKolor()) + znakWysok(getWysok()); }
  }

public class Rozdanie {
  int m_nDealer; // 0-3, -1=brak,blad
  String m_sVulner;
  int m_nNr;
  String m_sDeal;
  Reka m_aRece[];
  String m_sErrors;
  boolean m_bEof;
  boolean m_bEmpty;
  boolean m_bOk;
  int m_anKarty[];
  
  static final String m_asPossVulner[] = { "None", "NS", "EW", "Both" };

  static String m_asOsoby[] = {"N", "E", "S", "W"};

  Rozdanie() {
    m_anKarty = new int[Karta.MAX_KOD+1];
    zeruj();
    }

  public String toString() { return "" + m_nNr; }

  void zeruj() {
    m_nDealer=-1; m_sVulner="?"; m_nNr=-1; m_sDeal="";
    m_aRece = new Reka[4];
    m_sErrors = null;
    m_bEof = true;
    m_bEmpty = true;
    m_bOk = false;
    Arrays.fill(m_anKarty, -1);
    }

  static char znakOsoby(int nOsoba) { return nOsoba>=0 && nOsoba<=3 ? m_asOsoby[nOsoba].charAt(0) : '?'; }
  String dealerToString(int nDealer) { return nDealer<0 ? "?" : m_asOsoby[nDealer]; }
  static int osoba(char ch) {
    for (int i=0; i<m_asOsoby.length; i++) { if (m_asOsoby[i].charAt(0)==ch) { return i; } }
    return -1;
    }
  static int osoba(String sOsoba) { return (sOsoba.length()!=1) ? -1 : osoba(sOsoba.charAt(0)); }
  static int nastOsoba(int nOsoba) { return nOsoba<0 ? nOsoba : ((nOsoba+1)%4); }
  
  public boolean czyOk() {
    m_sErrors = "";
    if (m_nNr<=0) { m_sErrors += String.format("Brak numeru rozdania. "); }
    if (m_nDealer<0) { m_sErrors += String.format("Brak rozdaj¹cego. "); }
    
    // vulnerability
    if (m_sVulner.equals("Love")) { m_sVulner = "None"; }
    if (m_sVulner.equals("-")) { m_sVulner = "None"; }
    if (m_sVulner.equals("All")) { m_sVulner = "Both"; }
    if (m_sVulner.equals("?")) { m_sErrors += String.format("Brak za³o¿eñ. "); }
    else if (!f.stringIn(m_sVulner, m_asPossVulner)) { m_sErrors += String.format("Nieprawid³owe za³o¿enia: "+m_sVulner+". "); }
    
    // trzeba sprawdzic wczytane karty
    if (m_sDeal.isEmpty()) { m_sErrors += "Brak tagu deal. "; }
    //m_sErrors += "B³êdne karty. ";

    if (m_sErrors.isEmpty()) { m_sErrors = null; }
    return (m_bOk = (m_sErrors == null));
    }

  private boolean wczytajTagDeal(String sDeal) {
    int nOsoba, nKolor;
    int nPoz;
    int cKarty, cOsoby;
    Karta k;
    
    m_sDeal = sDeal;
    cOsoby = 0;
    cKarty = 0;
    //System.err.println("len="+sDeal.length()+" _"+sDeal+"_"); 
    try {
      nOsoba = osoba(sDeal.charAt(nPoz = 0));
      cOsoby += 1;
      nKolor = 1;
      k = new Karta();
      if (sDeal.charAt(nPoz+=1) != ':') { System.err.println("B³¹d sk³adni pliku PBN. W tagu deal na pozycji 2 powinien byæ znak :"); return false; }
      while (cKarty<52) {
        char ch = sDeal.charAt(nPoz+=1);
        //System.err.println(""+ch+" m_anKarty[28]="+m_anKarty[28]);
        if (ch=='.') {
          nKolor = Karta.nastKolor(nKolor);
          }
        else if (ch==' ') {
          nOsoba = nastOsoba(nOsoba);
          nKolor = 1;
          cOsoby += 1;
          }
        else if (ch=='-') {
          // standard dopuszcza niepodanie ktorejs z rak, my tego nie obslugujemy
          }
        else {
          // to musi byc wysokosc karty
          
          if (ch=='1') {
            // tego nie ma w standardzie, ale sam chyba taka lewizne wyprodukowalem
            if (sDeal.charAt(nPoz+=1) == '0') { ch = 'T'; }
            }
          
          k.set(nKolor, Karta.wysok(ch));
          if (!k.czyOk()) { System.err.println("B³¹d sk³adni pliku PBN. B³êdna wysokoœæ karty: "+ch);return false; }
          if (m_anKarty[k.getKod()] != -1) { System.err.println("B³¹d sk³adni pliku PBN. Karta "+k.toString()+" ("+k.getKod()+") zosta³a rozdana 2 razy. Poprzednio do "+m_anKarty[k.getKod()]+" a teraz do "+nOsoba); return false; }
          m_anKarty[k.getKod()] = nOsoba;
          //System.err.println("Karta "+k.toString()+" ("+k.getKod()+") idzie do "+znakOsoby(nOsoba));
          cKarty += 1;
          }
        }
      }
    catch (StringIndexOutOfBoundsException e) {
      System.err.println("B³¹d sk³adni pliku PBN. Koniec tagu po wczytaniu "+cKarty+" kart");
      return false;
      //if (cKarty!=52 || cOsoby!=4) { return false; }
      }
    if (cOsoby!=4) { System.err.println("B³¹d sk³adni pliku PBN. Wczytano karty tylko "+cOsoby+" osób"); return false; }
    return true;
    }

  public boolean wczytaj(BufferedReader br) throws IOException {
    Pattern patBoard = Pattern.compile("^\\[Board \"([0-9]+)\".*$");
    Pattern patDealer = Pattern.compile("^\\[Dealer \"([NESW])\".*$");
    Pattern patVulner = Pattern.compile("^\\[Vulnerable \"(.*)\".*$");
    Pattern patDeal = Pattern.compile("^\\[Deal \"(.*)\".*$");
    String sLinia;
    int cLinie = 0;
    
    zeruj();
    while ((sLinia = br.readLine()) != null) {
      Pattern p;
      Matcher m;
      if ((m=patBoard.matcher(sLinia)).matches()) {
        m_nNr = Integer.parseInt(m.group(1));
        }
      if ((m=patDealer.matcher(sLinia)).matches()) {
        String sDealer = m.group(1);
        m_nDealer = osoba(sDealer);
        }
      if ((m=patVulner.matcher(sLinia)).matches()) { m_sVulner = m.group(1); }
      if ((m=patDeal.matcher(sLinia)).matches()) { m_sDeal = m.group(1); }
      if (sLinia.isEmpty()) { break; } // pusta linia oznacza koniec rozdania
      if (sLinia.charAt(0)=='%') { continue; } // % = komentarz
      cLinie++;
      //System.out.println(sLinia);
      }
    //System.out.println("rozdanie nr "+m_nNr+", rozdawal "+dealerToString(m_nDealer)+", po partii: "+m_sVulner+": "+m_sDeal);
    if (!m_sDeal.isEmpty()) { wczytajTagDeal(m_sDeal); }
    m_bEmpty = (cLinie==0);
    m_bEof = (sLinia == null);
    return czyOk();
    }

  class FiltrTekstuRozd extends RunProcess.FiltrTekstu {
    int m_nOstPoz;
    static final String KLUCZ = "PCard:";
    static final int KLUCZ_LEN = 6;

    FiltrTekstuRozd() {
      }
    
    void init() {
      m_nOstPoz = 0;
      }

    private void grajDzwiek(String sNazwa) {
      try {
        String sRes = "res/"+sNazwa+".mp3";
        InputStream is = PbnTools.class.getResourceAsStream(sRes);
        if (is != null) {
          Player p = new Player(is);
          p.play();
          }
        else {
          System.err.println("Nie uda³o siê za³adowaæ zasobu: "+sRes);
          }
        }
      catch (javazoom.jl.decoder.JavaLayerException e) { e.printStackTrace(); }
      }

    private void grajDzwiek(int nOsoba) {
      if (nOsoba<0 || nOsoba>3) { return; }
      grajDzwiek("" + (nOsoba+1));
      }

    void filtruj(StringBuffer sb) {
      int nPoz;
      int nNewOstPoz = -1;
      char chPrev;
      nPoz = m_nOstPoz;
      do {
        nPoz = sb.indexOf(KLUCZ, nPoz);
        if (nPoz>=0) {
          chPrev = (nPoz>0) ? sb.charAt(nPoz-1) : 0;
          if (chPrev=='\n' || chPrev=='\r' || chPrev==0) {
            // trzeba sprawdzic poprzedni znak, bo nas interesuje tylko KLUCZ na poczatku linii lub pliku
            // pozostale trafienia pomijamy
            if (nPoz+KLUCZ_LEN+2 < sb.length()) {
              String sKarta = sb.substring(nPoz+KLUCZ_LEN, nPoz+KLUCZ_LEN+2);
              if (sKarta.equals("**")) {
                grajDzwiek("joker");
                }
              else {
                int nKodKarty = Karta.kod(sKarta);
                if (nKodKarty==0) {
                  System.out.println("Nieznana karta "+sKarta);
                  }
                else {
                  int nOsoba = m_anKarty[nKodKarty];
                  //System.out.println(sKarta + " -> " + nOsoba);
                  sb.replace(nPoz+KLUCZ_LEN, nPoz+KLUCZ_LEN+2, "*"+znakOsoby(nOsoba));
                  grajDzwiek(nOsoba);
                  }
                }
              }
            else {
              // jestesmy na granicy konca bufora - widzimy PCARD:?, ale nie widzimy, jaka to karta
              // trzeba bedzie zaczac potem od tego miejsca
              nNewOstPoz = nPoz;
              }
            }
          else {
            //System.err.println("poprzednim znakiem byl "+(int)chPrev);
            }
          nPoz += 1;
          }
        }
      while (nPoz>=0);
      if (nNewOstPoz<0) {
        // nie zatrzymalismy sie na granicy bufora, ale moze widac fragment napisu PCARD: ?
        // w takim razie zatrzymajmy sie o pare znakow przed koncem obecnego bufora
        nNewOstPoz = sb.length() - KLUCZ_LEN;
        }
      m_nOstPoz = nNewOstPoz;
      }

    void destroy() {
      }
    }
    
  public void rozdaj() {
    String sZbarcam = PbnTools.m_sBinDir + f.sDirSep + "zbarcam";
    String sOpts = PbnTools.m_props.getProperty("zbarcamOpts");
    sOpts = "-q --nodisplay -Spcard.enable " + sOpts;
    if (PbnTools.bLinux) {
      RunProcess.runCmd(null, sZbarcam + " " + sOpts, new FiltrTekstuRozd());
      }
    else {
      RunProcess.runCmd(null, sZbarcam + " " + sOpts, new FiltrTekstuRozd());
      }
    }
  
  public static void main(String args[]) {
//    DlgRozdaj d = new DlgRozdaj(null, true);
//    d.setVisible(true);
    System.out.println("koniec");
    }
  }
