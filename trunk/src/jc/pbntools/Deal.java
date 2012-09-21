/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:indentSize=2:noTabs=true:

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

class Hand { //{{{
  private List<Card> m_lstCards = new ArrayList<Card>();
  void clear() {
    m_lstCards.clear();
  }
  Card[] getCards() {
    Collections.sort(m_lstCards, Collections.reverseOrder());
    return m_lstCards.toArray(new Card[0]);
  }
  void add(Card c) { m_lstCards.add(c); }
} //}}}

/**
 * <code>person</code> - <code>N</code>, <code>E</code>, <code>S</code>
 *   or <code>W</code>. Not set - <code>-1</code>. 
 */

public class Deal implements Cloneable {
  public static final int N = 0;
  public static final int E = 1;
  public static final int S = 2;
  public static final int W = 3;
  /** 0 for N, 1 for E, ..., 3 for W; -1 for absent, error */
  public int m_nDealer;
  /** Default value: <code>"?"</code> */
  public String m_sVulner;
  public String m_sScoring;
  private int m_nNr;
  public String m_sDeal;
  public Hand m_aHands[];
  // Results:
  private int m_nDeclarer;
  private int m_nContractHeight;
  private int m_nContractDouble;
  private int m_nContractColor; // 0 = no trump, Card.SPADE - Card.CLUB
  // contract
  /** Number of tricks taken */
  private int m_nResult;
  
  String m_sErrors;
  boolean m_bEof;
  boolean m_bEmpty;
  boolean m_bOk;
  int m_anCards[];
  protected HashMap<String, String> m_mIdentFields;
  
  static final String m_asPossVulner[] = { "None", "NS", "EW", "All" };
  
  // {{{ PBN standard definitions
  public final static String sLf = "\r\n";
  /** initially they should only be identification fields, but later
    * more were added. Anyway fields after Vulnerable are not considered
    * <code>ident</code> */
  public static final String m_sIdentFields[] = { "Event", "Site", "Date",
    "Board", "West", "North", "East", "South", "Dealer", "Vulnerable" };
    // "Deal", "Scoring", "Declarer", "Contract", "Result" };
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
    m_anCards = new int[Card.MAX_KOD+1];
    zeruj();
    }

  public Deal clone() //{{{
  {
    try {
      Deal d = (Deal)super.clone();
      d.m_aHands = this.m_aHands.clone();
      d.m_anCards = this.m_anCards.clone();
      d.m_mIdentFields = (HashMap<String, String>)this.m_mIdentFields.clone();
      return d;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  } //}}}

  public String toString() { return "" + m_nNr; }

  void zeruj() { //{{{
    m_nDealer=-1; m_sVulner="?"; m_nNr=-1; m_sDeal="";
    m_aHands = new Hand[4];
    for (int i=0; i<m_aHands.length; i++) {
      m_aHands[i] = new Hand();
    }
    m_sErrors = null;
    m_bEof = true;
    m_bEmpty = true;
    m_bOk = false;
    Arrays.fill(m_anCards, -1);
    m_mIdentFields = new HashMap<String, String>();

    m_sScoring = null;
    m_nDeclarer = -1;
    m_nContractHeight = m_nContractColor = m_nContractDouble = -1;
    m_nResult = -1;
    
    } //}}}

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
  public int getNumber() { return m_nNr; }
  public void setDealer(int nDealer) { m_nDealer = nDealer; }
  public void setDeclarer(int nDeclarer) { m_nDeclarer = nDeclarer; }
  public int getDeclarer() { return m_nDeclarer; }
  public void setContractHeight(int h) { m_nContractHeight = h; }
  public void setContractColor(int c) { m_nContractColor = c; }
  public void setContractDouble(int d) { m_nContractDouble = d; }
  public int getContractHeight() { return m_nContractHeight; }
  public int getContractColor() { return m_nContractColor; }
  public int getContractDouble() { return m_nContractDouble; }

  public void setVulner(String sVulner) {
    m_sVulner = "?";
    for (String sValid : m_asPossVulner) {
      if (sValid.equalsIgnoreCase(sVulner)) { m_sVulner = sValid; }
    }
  }
  public String getVulner() { return m_sVulner; }
  
  public void setScoring(String sScoring) { m_sScoring = sScoring; }
  public String getScoring() { return m_sScoring; }
  public void setResult(int nResult) { m_nResult = nResult; }
  
  /** Places a single card into a hand. After all cards are set,
    * <code>fillHands</code> must be called. */
  public void setCard(Card c, int nPerson) {
    m_anCards[c.getCode()] = nPerson;
  }

  // isOk method {{{
  /** Performs deal validation.
   * <ul><li>Sets <code>m_sErrors</code> when errors met, <code>null</code>
   *         otherwise.
   * <li>Corrects some non-standard wording, like vulnerability
   *     <code>Love</code> instead of <code>None</code>.
   * @return Whether the deal is valid.
   */
  public boolean isOk() {
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
    //TODO check if 52 cards

    if (m_sErrors.isEmpty()) { m_sErrors = null; }
    return (m_bOk = (m_sErrors == null));
    } //}}}

  /** fillHands method {{{
    * Fills <code>m_aHands</code> with the cards from
    * <code>m_anCards</code>. Must be called after
    * a sequence of <code>setCard</code>. */
  public void fillHands() {
    for (int i=0; i<m_aHands.length; i++) {
      m_aHands[i].clear();
    }
    for (int i=0; i<m_anCards.length; i++) {
      if (m_anCards[i] >= 0) {
        m_aHands[m_anCards[i]].add(new Card(i));
      }
    }
  } //}}}
    
  private boolean wczytajTagDeal(String sDeal) { //{{{
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
        //System.err.println(""+ch+" m_anCards[28]="+m_anCards[28]);
        if (ch=='.') {
          nKolor = Card.nextColor(nKolor);
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
          if (!k.isOk()) { System.err.println("B³¹d sk³adni pliku PBN. B³êdna wysokoœæ karty: "+ch);return false; }
          if (m_anCards[k.getCode()] != -1) { System.err.println("B³¹d sk³adni pliku PBN. Karta "+k.toString()+" ("+k.getCode()+") zosta³a rozdana 2 razy. Poprzednio do "+m_anCards[k.getCode()]+" a teraz do "+nPerson); return false; }
          m_anCards[k.getCode()] = nPerson;
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
    } //}}}

  public boolean wczytaj(BufferedReader br) throws IOException { //{{{
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
    fillHands();
    m_bEmpty = (cLinie==0);
    m_bEof = (sLinia == null);
    return isOk();
    } //}}}

  class FiltrTekstuRozd extends RunProcess.FiltrTekstu { //{{{
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

    void filtruj(StringBuffer sb) { //{{{
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
                  int nPerson = m_anCards[nKodKarty];
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
      } //}}}

    void destroy() {
      }
    } //}}}
    
  public void rozdaj() { //{{{
    String sZbarcam = PbnTools.m_sBinDir + f.sDirSep + "zbarcam";
    String sOpts = PbnTools.m_props.getProperty("zbarcamOpts");
    sOpts = "-q --nodisplay -Spcard.enable " + sOpts;
    if (PbnTools.bLinux) {
      RunProcess.runCmd(null, sZbarcam + " " + sOpts, new FiltrTekstuRozd());
    }
    else {
      RunProcess.runCmd(null, sZbarcam + " " + sOpts, new FiltrTekstuRozd());
    }
  } //}}}

  // {{{ static methods
  
  public static String getPbnString(Hand h) { //{{{
    StringBuilder sb = new StringBuilder();
    if (h == null) return "";
    ArrayList<Card> aCards[] = new ArrayList[4];
    for (int nColor=1; nColor<=4; nColor++) {
      aCards[nColor-1] = new ArrayList<Card>();
    }
    for (Card c : h.getCards()) {
      aCards[c.getColor() - 1].add(c);
    }
    for (int nColor=1; nColor<=4; nColor++) {
      if (nColor != 1) { sb.append('.'); }
      for (Card c : aCards[nColor - 1]) {
        sb.append(c.getRankChar());
      }
    }

    return sb.toString();
  } //}}}

  public static int getUniqueCount(Deal deals[]) //{{{
  {
    HashSet<Integer> set = new HashSet<Integer>();
    for (Deal d: deals) {
      set.add(d.getNumber());
    }
    return set.size();
  } //}}}

  public static void writeField(Writer w, String sField, String sValue)
    throws java.io.IOException
  {
    w.write("[" + sField + " \"" + sValue + "\"]" + sLf);
  }

  //}}}

  // writeContract method {{{
  /** Writes contract to the <code>Writer</code>. If no contract set,
   *  does not write anything. */
  public void writeContract(Writer w) throws java.io.IOException
  {
    String sContract = "";
    if (m_nContractHeight < 0)
      return;
    else if (m_nContractHeight == 0)
      sContract = "Pass";
    else {
      sContract = String.valueOf(m_nContractHeight);
      if (m_nContractColor == 0)
        sContract += "NT";
      else
        sContract += String.valueOf(Card.colorChar(m_nContractColor));
      if (m_nContractDouble > 0)
        sContract += ( m_nContractDouble == 1 ? "X" : "XX" );
    }
    writeField(w, "Contract", sContract);
  } //}}}
    
  // writeResult method {{{
  /** Writes result to the <code>Writer</code>, according to the pbn
   *  specification. */
  public void writeResult(Writer w) throws java.io.IOException
  {
    if (getContractHeight() < 0)
      return;
    // the result for a passed out deal is empty string
    String sResult = "";
    if (getContractHeight() > 0) {
      sResult = "" + m_nResult;
    }
    writeField(w, "Result", sResult);
  } //}}}

  public void savePbn(Writer w) throws java.io.IOException { //{{{
    // set fields in the map
    if (m_nNr > 0) { setIdentField("Board", "" + m_nNr); }
    if (m_nDealer >= 0) { setIdentField("Dealer", "" + personChar(m_nDealer)); }
    if (m_sVulner!=null && !m_sVulner.equals("?")) {
      setIdentField("Vulnerable", m_sVulner);
    }
    
    // output fields in correct order
    for (String sField : m_sIdentFields) {
      String sValue = m_mIdentFields.get(sField);
      if (sValue != null) {
        writeField(w, sField, sValue);
      }
    }
    
    w.write("[Deal \"");
    int nPerson = m_nDealer >= 0 ? m_nDealer : 0;
    w.write("" + personChar(nPerson) + ":");
    for (int i=0; i<4; i++) {
        if (i > 0) { w.write(" "); }
        w.write(getPbnString(m_aHands[nPerson]));
        nPerson = nextPerson(nPerson);
    }
    w.write("\"]" + sLf);

    if (getScoring() != null)
      writeField(w, "Scoring", getScoring());
    if (m_nDeclarer >= 0)
      writeField(w, "Declarer", "" + personChar(m_nDeclarer));
    writeContract(w);
    writeResult(w);

    w.write(sLf);
  } //}}}
  
  public static void main(String args[]) {
//    DlgRozdaj d = new DlgRozdaj(null, true);
//    d.setVisible(true);
    System.out.println("koniec");
    }
  }

// tabSize=2:noTabs=true:folding=explicit: