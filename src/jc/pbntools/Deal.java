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
import java.io.Writer;
import java.util.regex.*;
import java.util.*;
import jc.f;
import jc.pbntools.*;
import javazoom.jl.player.Player;

class Reka {
  }

public class Deal {
  public static final int N = 0;
  public static final int E = 1;
  public static final int S = 2;
  public static final int W = 3;
  /** 0 for N, 1 for E, ..., 3 for W; -1 for absent, error */
  public int m_nDealer;
  /** Default value: <code>"?"</code> */
  public String m_sVulner;
  public int m_nNr;
  public String m_sDeal;
  public Reka m_aRece[];
  String m_sErrors;
  boolean m_bEof;
  boolean m_bEmpty;
  boolean m_bOk;
  int m_anKarty[];
  protected HashMap<String, String> m_mIdentFields;
  
  static final String m_asPossVulner[] = { "None", "NS", "EW", "All" };
  
  // {{{ PBN standard definitions
  public final static String sLf = "\r\n";
  public static final String m_sIdentFields[] = { "Event", "Site", "Date",
    "Board", "West", "North", "East", "South", "Dealer", "Vulnerable",
    "Deal", "Scoring", "Declarer", "Contract", "Result" };
  // }}}
  /** <code>m_sIdentFields</code> inserted into map: uppercased -> normalized */
  private static HashMap<String, String> m_mIdentFieldNames;

  static String m_asPersons[] = {"N", "E", "S", "W"};

  static {
    m_mIdentFieldNames = new HashMap<String, String>();
    for (String s : m_sIdentFields) {
      m_mIdentFieldNames.put(s.toUpperCase(), s);
    }
  }
  
  public Deal() {
    m_anKarty = new int[Card.MAX_KOD+1];
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
    m_mIdentFields = new HashMap<String, String>();
    }

  static char personChar(int nPerson) { return nPerson>=0 && nPerson<=3 ? m_asPersons[nPerson].charAt(0) : '?'; }
  String dealerToString(int nDealer) { return nDealer<0 ? "?" : m_asPersons[nDealer]; }
  static int person(char ch) {
    for (int i=0; i<m_asPersons.length; i++) { if (m_asPersons[i].charAt(0)==ch) { return i; } }
    return -1;
    }
  /** Returns person number.
    * @param sPerson Must be of length 1. */
  public static int person(String sPerson) { return (sPerson.length()!=1) ? -1 : person(sPerson.charAt(0)); }
  static int nextPerson(int nPerson) { return nPerson<0 ? nPerson : ((nPerson+1)%4); }

  public void setIdentField(String sField, String sValue) {
    String sFieldNorm = m_mIdentFieldNames.get(sField.toUpperCase());
    if (sFieldNorm == null) {
      throw new RuntimeException("Invalid identification field name: " + sField);
    }
    m_mIdentFields.put(sFieldNorm, sValue);
  }
  
  public void setNumber(int nNr) { m_nNr = nNr; }
  public void setDealer(int nDealer) { m_nDealer = nDealer; }

  public void setVulner(String sVulner) {
    m_sVulner = "?";
    for (String sValid : m_asPossVulner) {
      if (sValid.equalsIgnoreCase(sVulner)) { m_sVulner = sValid; }
    }
  }
  
  public void setCard(Card c, int nPerson) {
    m_anKarty[c.getCode()] = nPerson;
  }
  
  public boolean czyOk() {
    m_sErrors = "";
    if (m_nNr<=0) { m_sErrors += String.format("Brak numeru rozdania. "); }
    if (m_nDealer<0) { m_sErrors += String.format("Brak rozdaj¹cego. "); }
    
    // vulnerability
    if (m_sVulner.equals("Love")) { m_sVulner = "None"; }
    if (m_sVulner.equals("-")) { m_sVulner = "None"; }
    if (m_sVulner.equals("Both")) { m_sVulner = "All"; }
    if (m_sVulner.equals("?")) { m_sErrors += String.format("Brak za³o¿eñ. "); }
    else if (!f.stringIn(m_sVulner, m_asPossVulner)) { m_sErrors += String.format("Nieprawid³owe za³o¿enia: "+m_sVulner+". "); }
    
    // trzeba sprawdzic wczytane karty
    if (m_sDeal.isEmpty()) { m_sErrors += "Brak tagu deal. "; }
    //m_sErrors += "B³êdne karty. ";

    if (m_sErrors.isEmpty()) { m_sErrors = null; }
    return (m_bOk = (m_sErrors == null));
    }

  private boolean wczytajTagDeal(String sDeal) {
    int nPerson, nKolor;
    int nPoz;
    int cKarty, cOsoby;
    Card k;
    
    m_sDeal = sDeal;
    cOsoby = 0;
    cKarty = 0;
    //System.err.println("len="+sDeal.length()+" _"+sDeal+"_"); 
    try {
      nPerson = person(sDeal.charAt(nPoz = 0));
      cOsoby += 1;
      nKolor = 1;
      k = new Card();
      if (sDeal.charAt(nPoz+=1) != ':') { System.err.println("B³¹d sk³adni pliku PBN. W tagu deal na pozycji 2 powinien byæ znak :"); return false; }
      while (cKarty<52) {
        char ch = sDeal.charAt(nPoz+=1);
        //System.err.println(""+ch+" m_anKarty[28]="+m_anKarty[28]);
        if (ch=='.') {
          nKolor = Card.nastKolor(nKolor);
          }
        else if (ch==' ') {
          nPerson = nextPerson(nPerson);
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
          
          k.set(nKolor, Card.rank(ch));
          if (!k.czyOk()) { System.err.println("B³¹d sk³adni pliku PBN. B³êdna wysokoœæ karty: "+ch);return false; }
          if (m_anKarty[k.getCode()] != -1) { System.err.println("B³¹d sk³adni pliku PBN. Karta "+k.toString()+" ("+k.getCode()+") zosta³a rozdana 2 razy. Poprzednio do "+m_anKarty[k.getCode()]+" a teraz do "+nPerson); return false; }
          m_anKarty[k.getCode()] = nPerson;
          //System.err.println("Karta "+k.toString()+" ("+k.getCode()+") idzie do "+znakOsoby(nPerson));
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
        m_nDealer = person(sDealer);
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

    private void grajDzwiek(int nPerson) {
      if (nPerson<0 || nPerson>3) { return; }
      grajDzwiek("" + (nPerson+1));
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
              String sCard = sb.substring(nPoz+KLUCZ_LEN, nPoz+KLUCZ_LEN+2);
              if (sCard.equals("**")) {
                grajDzwiek("joker");
                }
              else {
                int nKodKarty = Card.kod(sCard);
                if (nKodKarty==0) {
                  System.out.println("Nieznana karta "+sCard);
                  }
                else {
                  int nPerson = m_anKarty[nKodKarty];
                  //System.out.println(sKarta + " -> " + nPerson);
                  sb.replace(nPoz+KLUCZ_LEN, nPoz+KLUCZ_LEN+2, "*"+personChar(nPerson));
                  grajDzwiek(nPerson);
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
    sOpts = "-q --nodisplay --no-probe -Spcard.enable " + sOpts;
    if (PbnTools.bLinux) {
      RunProcess.runCmd(null, sZbarcam + " " + sOpts, new FiltrTekstuRozd());
    }
    else {
      RunProcess.runCmd(null, sZbarcam + " " + sOpts, new FiltrTekstuRozd());
    }
  }
  
  public void savePbn(Writer w) throws java.io.IOException {
    if (m_nNr > 0) { setIdentField("Board", "" + m_nNr); }
    if (m_nDealer >= 0) { setIdentField("Dealer", "" + personChar(m_nDealer)); }
    if (m_sVulner!=null && !m_sVulner.equals("?")) {
      setIdentField("Vulnerable", m_sVulner);
    }
    for (String sField : m_sIdentFields) {
      String sValue = m_mIdentFields.get(sField);
      if (sValue != null) {
        w.write("[" + sField + " \"" + sValue + "\"]" + sLf);
      }
    }
    w.write(sLf);
  }
  
  public static void main(String args[]) {
//    DlgRozdaj d = new DlgRozdaj(null, true);
//    d.setVisible(true);
    System.out.println("koniec");
    }
  }
