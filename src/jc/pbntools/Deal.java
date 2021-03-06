/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:indentSize=2:noTabs=true:

    Copyright (C) 2011-3 Jaroslaw Czekalski - jarekczek@poczta.onet.pl

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

class Bid { //{{{
  String m_sBid;
  String m_sAnno;

  public Bid(String sBid)
  {
    m_sBid = sBid;
    m_sAnno = null;
  }
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
  protected String m_sId;
  public String m_sDeal;
  public Hand m_aHands[];
  /** In order of appearance, starting from dealer */
  private ArrayList<Bid> m_aBids;
  /** Cards played. Index is <code>(nTrick - 1) * 4 + nPerson</code> */
  private Card[] m_aPlays; // cards played
  // Results:
  private int m_nDeclarer;
  private int m_nContractHeight;
  private int m_nContractDouble;
  private int m_nContractColor; // 0 = no trump, Card.SPADE - Card.CLUB
  // contract
  /** Number of tricks taken */
  private int m_nResult;
  
  ArrayList<String> m_asErrors;
  boolean m_bEof;
  boolean m_bEmpty;
  boolean m_bOk;
  /* For card code (0..Card.MAX_CODE inclusive) stores the person
     (card holder). Initialized with -1. */
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
    m_anCards = new int[Card.MAX_CODE + 1];
    zeruj();
    }

  public Deal clone() //{{{
  {
    try {
      Deal d = (Deal)super.clone();
      d.m_aHands = this.m_aHands.clone();
      d.m_anCards = this.m_anCards.clone();
      d.m_aBids = (ArrayList<Bid>)this.m_aBids.clone();
      d.m_aPlays = this.m_aPlays.clone();
      d.m_mIdentFields = (HashMap<String, String>)this.m_mIdentFields.clone();
      return d;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  } //}}}

  public String toString() { return "" + m_nNr; }

  void zeruj() { //{{{
    m_nDealer=-1; m_sVulner="?"; m_nNr=-1; m_sId=""; m_sDeal="";
    m_aHands = new Hand[4];
    for (int i=0; i<m_aHands.length; i++) {
      m_aHands[i] = new Hand();
    }
    m_aBids = new ArrayList<Bid>();
    clearPlays();
    m_asErrors = null;
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

  public static char personChar(int nPerson)
  {
    return nPerson>=0 && nPerson<=3 ? m_asPersons[nPerson].charAt(0) : '?';
  }
  
  String dealerToString(int nDealer) { return nDealer<0 ? "?" : m_asPersons[nDealer]; }
  public static int person(char ch) {
    for (int i=0; i<m_asPersons.length; i++) { if (m_asPersons[i].charAt(0)==ch) { return i; } }
    return -1;
    }

  /** Returns person number.
    * @param sPerson Must be of length 1. */
  public static int person(String sPerson)
  {
    return (sPerson.length()!=1) ? -1 : person(sPerson.charAt(0));
  }
  
  public static int nextPerson(int nPerson)
  {
    return nPerson<0 ? nPerson : ((nPerson+1) % 4);
  }

  public static int prevPerson(int nPerson)
  {
    if (nPerson >= 0) {
      nPerson--;
      if (nPerson < 0) nPerson += 4;
    }
    return nPerson;
  }

  // party method {{{
  /** Returns <code>N</code>, <code>E</code> or <code>-1</code> -
   *  the party of the given player. */
  public int party(int nPlayer)
  {
    if (nPlayer == N || nPlayer == S)
      return N;
    else if (nPlayer == E || nPlayer == W)
      return E;
    else
      return -1;
  }
  
  
  public void setIdentField(String sField, String sValue) {
    String sFieldNorm = m_mIdentFieldNames.get(sField.toUpperCase());
    if (sFieldNorm == null) {
      throw new RuntimeException("Invalid identification field name: " + sField);
    }
    m_mIdentFields.put(sFieldNorm, sValue);
  }
  
  public void setNumber(int nNr) { m_nNr = nNr; }
  public void setId(String sId) { m_sId = sId; }
  public String getId() { return m_sId; }
  public int getNumber() { return m_nNr; }
  public int getDealer() { return m_nDealer; }
  public void setDealer(int nDealer) { m_nDealer = nDealer; }
  public void setDeclarer(int nDeclarer) { m_nDeclarer = nDeclarer; }
  public int getDeclarer() { return m_nDeclarer; }
  public void setContractHeight(int h) { m_nContractHeight = h; }
  /** 0 - NT */
  public void setContractColor(int c) { m_nContractColor = c; }
  public void setContractDouble(int d) { m_nContractDouble = d; }
  public int getContractHeight() { return m_nContractHeight; }
  /** 0 - NT */
  public int getContractColor() { return m_nContractColor; }
  public int getContractDouble() { return m_nContractDouble; }

  // setVulner method {{{
  /**
   * Sets vulnerability.
   * @param sVulner One of <code>"None", "NS", "EW", "All"</code>
   */
  public void setVulner(String sVulner) {
    m_sVulner = "?";
    for (String sValid : m_asPossVulner) {
      if (sValid.equalsIgnoreCase(sVulner)) { m_sVulner = sValid; }
    }
  } //}}}
  public String getVulner() { return m_sVulner; }
  
  public void setScoring(String sScoring) { m_sScoring = sScoring; }
  public String getScoring() { return m_sScoring; }
  public void setResult(int nResult) { m_nResult = nResult; }
  public int getResult() { return m_nResult; }
  
  /** Places a single card into a hand. After all cards are set,
    * <code>fillHands</code> must be called. */
  public void setCard(Card c, int nPerson) {
    m_anCards[c.getCode()] = nPerson;
  }

  /** Returns card holder or <code>-1</code> if not known. */
  public int getCardHolder(Card c) {
    return m_anCards[c.getCode()];
  }

  // dealRemCards method {{{
  /**
   * Deals the remaining cards. This call is legal only if there is
   * 13 or less cards left and only one player has less than 13 cards.
   * Does not require <code>fillHands</code> to be called before.
   * If all cards are already dealt, does nothing.
   * @throws IllegalArgumentException when the configuration of remaining
   * cards and players is illegal.
   */
  public void dealRemCards()
  {
    int cLeft = 0;
    int acPerson[] = new int[4]; // number of cards already dealt
    for (Card c: Card.getIter()) {
      if (m_anCards[c.getCode()] < 0)
        cLeft++;
      else {
        acPerson[m_anCards[c.getCode()]]++;
      }
    }
    if (cLeft > 13)
      throw new IllegalArgumentException(
        "More than 13 cards left to deal (" + cLeft + ")");
    if (cLeft == 0)
      // all cards dealt, that's fine
      return;

    int iFreePerson = -1;
    for (int i=0; i<=3; i++) {
      if (acPerson[i] < 13) {
        if (iFreePerson >= 0) {
          throw new IllegalArgumentException(
            "More than one player has less than 13 cards");
        }
        iFreePerson = i;
      }
    }
    
    for (Card c: Card.getIter()) {
      if (m_anCards[c.getCode()] < 0)
        setCard(c, iFreePerson);
    }
    
  } //}}}
  
  public void addBid(String sBid) //{{{
  {
    m_aBids.add(new Bid(sBid));
  } //}}}
  
  public void annotateLastBid(String sAnno) //{{{
  {  
    if (m_aBids.size() > 0)
      m_aBids.get(m_aBids.size()-1).m_sAnno = sAnno;
  } //}}}
  
  public void addPlay(int nTrick, int nPerson, Card card) //{{{
  {
    if (nPerson < 0) return;
    m_aPlays[(nTrick-1) * 4 + nPerson] = (Card)card.clone();
  } //}}}
  
  public void clearPlays() //{{{
  {
    m_aPlays = new Card[52];
  } //}}}
  
  // areBidsOk method {{{
  /** Part of {@link #isOk} checks. */
  protected boolean areBidsOk() {
    boolean bOk = true;
    Pattern pat = Pattern.compile("(Pass)|(X)|(XX)|([1-7]([CDHS]|(NT)))");
    for (Bid bid: m_aBids) {
      Matcher m = pat.matcher(bid.m_sBid);
      if (!m.matches()) {
        m_asErrors.add(PbnTools.getStr("error.pbn.wrongBid", bid.m_sBid));
        bOk = false;
      }
    }
    return bOk;
  } //}}}

  // setContractFromAuction method {{{
  /**
   * @param asErrors Errors that occurred will be added to this list.
   *        Can be <code>null</code>.
   * @return <code>true</code> if succeeded.
   */
  public boolean setContractFromAuction(ArrayList<String> asErrorsC)
  {
    ArrayList<String> asErrors = new ArrayList<String>();
    // the first player to bid the given color for the given party
    int[] anFirst = new int[5*2];
    Arrays.fill(anFirst, -1);
    if (!areBidsOk()) return false;
    if (getDealer() < 0) return false;
    int cPass = 0;
    int nDouble = 0;
    int nHeight = 0;
    int nColor = -1;
    int nPlayer = getDealer();
    int nParty = -1;
    String sLastBid = "";
    Pattern pat = Pattern.compile("([1-7])([CDHS]|(NT))");
    for (Bid bid: m_aBids) {
      if (bid.m_sBid.equals("Pass"))
        cPass++;
      else {
        cPass = 0;
        if (bid.m_sBid.equals("X"))
          nDouble = 1;
        else if (bid.m_sBid.equals("XX"))
          nDouble = 2;
        else {
          Matcher m = pat.matcher(bid.m_sBid);
          m.matches();
          assert(m.matches());
          sLastBid = bid.m_sBid;
          nDouble = 0;
          try {
            nHeight = Integer.parseInt(m.group(1));
          } catch (NumberFormatException nfe) {
            nHeight = -1;
          }
          nColor = Card.color(bid.m_sBid.charAt(1));
          nParty = party(nPlayer);
          if (anFirst[2*nColor + nParty] == -1)
            anFirst[2*nColor + nParty] = nPlayer;
        }
      }
      nPlayer = nextPerson(nPlayer);
    }
    
    if (cPass != 3 && !(cPass == 4 && m_aBids.size() == 4))
      asErrors.add(PbnTools.getStr("error.noOfPasses", 3, cPass));
    else if (cPass == 4) {
      setContractHeight(0);
    } else {
      if (nColor < 0)
        asErrors.add(PbnTools.getStr("error.noContractColorLB", sLastBid));
      else if (nHeight < 1)
        asErrors.add(PbnTools.getStr("error.noContractHeightLB", sLastBid));
      else {
        setContractHeight(nHeight);
        setContractColor(nColor);
        setContractDouble(nDouble);
        setDeclarer(anFirst[2*nColor + nParty]);
      }
    }

    if (asErrorsC != null)
      asErrorsC.addAll(asErrors);
    return asErrors.size() == 0;
  } //}}}
  
  public int countPlayedCards() //{{{
  {
    int cCards = 0;
    for(int iCard = 0; iCard < 52; iCard++) {
      if (m_aPlays[iCard] != null) cCards++;
    }
    return cCards;
  } //}}}
  
  // arePlaysOk method {{{
  /** Part of {@link #isOk} checks. */
  protected boolean arePlaysOk() {
    boolean bOk = true;
    int cCards = countPlayedCards();
    
    // guess nothing to do without cards
    if (cCards == 0) return true;

    if (m_nDeclarer < 0 && cCards > 0) {
      m_asErrors.add(PbnTools.getStr("error.pbn.playsButNoDeclarer", cCards));
      return false;
    }
    
    for (int nTrick = 1; nTrick <= 13; nTrick++) {
      int nPerson = m_nDeclarer;
      for (int iCard = 0; iCard < 4; iCard++) {
        nPerson = Deal.nextPerson(nPerson);
        Card card = m_aPlays[(nTrick-1)*4 + nPerson];
        if (card == null) continue;
        if (getCardHolder(card) != nPerson) {
          m_asErrors.add(PbnTools.getStr("error.pbn.wrongPlay", card,
            personChar(nPerson)));
          bOk = false;
        }
        cCards++;
      }
    }
    
    return bOk;
  } //}}}

  // setResultFromPlays method {{{
  /**
   * @param asErrors Errors that occurred will be added to this list.
   *        Can be <code>null</code>.
   * @return <code>true</code> if succeeded.
   */
  public boolean setResultFromPlays(ArrayList<String> asErrorsC)
  {
    ArrayList<String> asErrors = new ArrayList<String>();
    if (!arePlaysOk()) return false;
    // count of tricks for parties
    int[] acTricks = new int[2];
    if (getDeclarer() < 0 || getContractHeight() < 0) return false;
    if (countPlayedCards() != 52) return false;

    int nWinner = nextPerson(getDeclarer());
    int nTrump = getContractColor();
    for (int nTrick = 1; nTrick <= 13; nTrick++) {
      // cards for each player
      Card[] aCards = new Card[4];
      int nPerson = getDeclarer();
      for (int iCard = 0; iCard < 4; iCard++) {
        nPerson = Deal.nextPerson(nPerson);
        Card card = m_aPlays[(nTrick-1)*4 + nPerson];
        aCards[nPerson] = card;
      }
      
      nPerson = nWinner;
      Card winCard = aCards[nWinner];
      if (f.getDebugLevel() > 1)
        System.out.println("trick is started with " + winCard
          + " by " + personChar(nPerson));
      for (int iCard = 0; iCard < 4; iCard++) {
        nPerson = nextPerson(nPerson);
        if (winCard.isLessThan(aCards[nPerson], nTrump)) {
          nWinner = nPerson;
          winCard = aCards[nPerson];
          if (f.getDebugLevel() > 1)
            System.out.println("trick is intercepted by "
              + personChar(nWinner) + " with " + winCard);
        }
      }
      if (f.getDebugLevel() > 1)
        System.out.println("trick winner: " + personChar(nWinner));
      acTricks[party(nWinner)]++;
    }

    assert(acTricks[0] + acTricks[1] == 13);
    setResult(acTricks[party(getDeclarer())]);

    if (asErrorsC != null)
      asErrorsC.addAll(asErrors);
    return asErrors.size() == 0;
  } //}}}

  // isOk method {{{
  /** Performs deal validation.
   * <ul><li>Sets <code>m_asErrors</code> when errors met, <code>null</code>
   *         otherwise.
   * <li>Corrects some non-standard wording, like vulnerability
   *     <code>Love</code> instead of <code>None</code>.
   * @return Whether the deal is valid.
   */
  public boolean isOk() {
    m_asErrors = new ArrayList<String>();
    if (m_nNr<=0) m_asErrors.add(PbnTools.getStr("error.pbn.noDealNumber"));
    if (m_nDealer<0) m_asErrors.add(PbnTools.getStr("error.pbn.noDealer"));
    
    // vulnerability
    if (m_sVulner.equals("Love")) { m_sVulner = "None"; }
    if (m_sVulner.equals("-")) { m_sVulner = "None"; }
    if (m_sVulner.equals("Both")) { m_sVulner = "All"; }
    if (m_sVulner.equals("?"))
      m_asErrors.add(PbnTools.getStr("error.pbn.noVuln"));
    else if (!f.stringIn(m_sVulner, m_asPossVulner))
      m_asErrors.add(PbnTools.getStr("error.pbn.wrongVuln", m_sVulner));
    
    Card c = Card.firstCard();
    int cDealt = 0;
    Card cardMiss = null;
    while (c != null) {
      if (m_anCards[c.getCode()] < 0) {
        cardMiss = c;
      } else {
        cDealt++;
      }
      c = c.nextCard();
    }
    if (cDealt != 52) {
      m_asErrors.add(PbnTools.getStr("error.pbn.notAllCards",
        52 - cDealt, 52, cardMiss.toString()));
    }
    
    areBidsOk();
    arePlaysOk();

    if (m_asErrors.size() == 0) { m_asErrors = null; }
    return (m_bOk = (m_asErrors == null));
    } //}}}

  // getErrors method {{{
  /** Returns errors detected by #isOk.
    * @return <code>null</code> if no errors. */
  public String[] getErrors()
  {
    if (m_asErrors == null)
      return null;
    return m_asErrors.toArray(new String[0]);
  } //}}}

  // getErrorsStr method {{{
  /** Returns all errors concatenated to a single string, using
    * given separator.
    * @return Error string, never <code>null</code>.
    */
  public String getErrorsStr(String sSep)
  {
    StringBuilder sb = new StringBuilder();
    if (m_asErrors != null) {
      for (String sErr: m_asErrors) {
        if (sb.length() != 0)
          sb.append(sSep);
        sb.append(sErr);
      }
    }
    return sb.toString();
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
      if (sDeal.charAt(nPoz+=1) != ':') { System.err.println("Błąd składni pliku PBN. W tagu deal na pozycji 2 powinien być znak :"); return false; }
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
          if (!k.isOk()) { System.err.println("Błąd składni pliku PBN. Błędna wysokość karty: "+ch);return false; }
          if (m_anCards[k.getCode()] != -1) { System.err.println("Błąd składni pliku PBN. Karta "+k.toString()+" ("+k.getCode()+") została rozdana 2 razy. Poprzednio do "+m_anCards[k.getCode()]+" a teraz do "+nPerson); return false; }
          m_anCards[k.getCode()] = nPerson;
          //System.err.println("Karta "+k.toString()+" ("+k.getCode()+") idzie do "+znakOsoby(nPerson));
          cKarty += 1;
          }
        }
      }
    catch (StringIndexOutOfBoundsException e) {
      System.err.println("Błąd składni pliku PBN. Koniec tagu po wczytaniu "+cKarty+" kart");
      return false;
      //if (cKarty!=52 || cOsoby!=4) { return false; }
      }
    if (cOsoby!=4) { System.err.println("Błąd składni pliku PBN. Wczytano karty tylko "+cOsoby+" osób"); return false; }
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
        String sRes = "/"+sNazwa+".mp3";
        InputStream is = PbnTools.class.getResourceAsStream(sRes);
        if (is != null) {
          Player p = new Player(is);
          p.play();
          }
        else {
          System.err.println("Nie udało się załadować zasobu: "+sRes);
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
    if (m_nDeclarer >= 0)
      writeField(w, "Declarer", "" + personChar(m_nDeclarer));
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

  // writeAuction method {{{
  public void writeAuction(Writer w) throws java.io.IOException
  {
    if (m_aBids.size() == 0)
      return;
    Map<String, Integer> mAnno = new HashMap<String, Integer>();
    ArrayList<String> asAnno = new ArrayList<String>();
    writeField(w, "Auction", "" + personChar(getDealer()));
    int i = 0;
    for(Bid bid: m_aBids) {
      if (i != 0) {
        if ((i % 4) == 0) {
          w.write(sLf);
        } else {
          w.write(" ");
        }
      }
      w.write(bid.m_sBid);
      if (bid.m_sAnno != null) {
        Integer iAnno = mAnno.get(bid.m_sAnno);
        if (iAnno == null) {
          iAnno = mAnno.size() + 1;
          mAnno.put(bid.m_sAnno, iAnno);
          asAnno.add(bid.m_sAnno.replaceAll("\"", ""));
        }
        if (iAnno <= 32)
          w.write(" =" + mAnno.get(bid.m_sAnno) + "=");
      }
      i += 1;
    }
    w.write(sLf);
    
    if (asAnno.size() > 0) {
      for (i = 0; i < asAnno.size(); i++) {
        writeField(w, "Note", "" + (i+1) + ":" + asAnno.get(i));
      }
    }
  } //}}}

  // writePlays method {{{
  public void writePlays(Writer w) throws java.io.IOException
  {
    if (getDeclarer() < 0 || countPlayedCards() == 0) return;
    writeField(w, "Play", "" + personChar(nextPerson(getDeclarer())));
    
    ArrayList<String> asPlays = new ArrayList<String>();
    for (int nTrick = 1; nTrick <= 13; nTrick++) {
      int nPerson = m_nDeclarer;
      for (int iCard = 0; iCard < 4; iCard++) {
        nPerson = Deal.nextPerson(nPerson);
        Card card = m_aPlays[(nTrick-1)*4 + nPerson];
        if (iCard != 0) w.write(" ");
        if (card != null && card.isOk())
          w.write("" + card);
        else
          w.write("-");
      }
      w.write(sLf);
    }
    // TODO * if no more plays available
  } //}}}

  public void writeSupplementalTags(Writer w) throws java.io.IOException //{{{
  {
    if (m_sId.length() > 0)
      writeField(w, "DealId", m_sId);
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
    writeContract(w);
    writeResult(w);
    writeAuction(w);
    writePlays(w);
    writeSupplementalTags(w);

    w.write(sLf);
  } //}}}
  
}
// tabSize=2:noTabs=true:folding=explicit:
