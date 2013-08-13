/* *****************************************************************************

    jedit options: :folding=explicit:tabSize=2:noTabs=true:
    
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

package jc.pbntools.download;

import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import jc.f;
import jc.JCException;
import jc.outputwindow.SimplePrinter;
import jc.pbntools.Card;
import jc.pbntools.Deal;
import jc.pbntools.PbnTools;
import jc.SoupProxy;

/**
<h1>Lin specification</h1>

<dl>
<dt>ah - deal label</dt>
<dt>an - bid explanation (for the previous bid)
<dt>mb - a bid</dt>
<dt>mc - claim, number of tricks
<dt>md - dealer and hands
  <ul>
  <li>First goes a digit denoting the dealer 1 for S, 2 = W, 3 = N, 4 = S
  <li>Right after the dealer digit come dealers cards,
      color and cards</li>
  <li>Commas separate cards of consecutive players, only 3 hands are given
      </li>
  </ul>
  For example:
  <code>3S8QKHJD8TJKC2349J,S23459TAH368DC68K,S67JH45QKD357QC7A,</code>
</dt>
<dt>pc - play card</dt>
<dt>pg - pause game. This is done in lin files downloaded directly,
      after bidding and every 4 cards</dt>
<dt>pn - player names, comma separated</dt>
<dt>rh - ? (seen empty)</dt>
<dt>st - ? (seen empty)</dt>
<dt>sv - vulnerability, o = None, b = Both, n = NS, e = EW
    (<code>o</code> also found to denote None)</dt>
<dt></dt>
<dt></dt>
<dt></dt>
<dt></dt>
</dl>

*/

public class LinReader implements DealReader
{
  protected Document m_doc;
  protected SimplePrinter m_sp;
  protected boolean m_bSilent = true;
  protected int m_nCurCard = 0; // for gathering game play
  
  /** readNumber method {{{
   * Reads deal number from <code>Board xx</code> text.
   */
  private void readNumber(Deal deal, String sBoard)
    throws DownloadFailedException
  {
    sBoard = sBoard.replaceFirst("^.*?([0-9]+).*$", "$1");
    try {
      deal.setNumber(Integer.valueOf(sBoard));
    } catch (NumberFormatException nfe) {}
  } //}}}

  private void readPlayers(Deal deal, String sPlayers) //{{{
    throws DownloadFailedException
  {
    String asPlayers[] = sPlayers.split(",");
    if (asPlayers.length != 4)
      throw new DownloadFailedException(
        PbnTools.getStr("error.incNumberNames", asPlayers.length, sPlayers),
        m_sp, !m_bSilent);
    deal.setIdentField("South",asPlayers[0]); 
    deal.setIdentField("West",asPlayers[1]); 
    deal.setIdentField("North",asPlayers[2]); 
    deal.setIdentField("East",asPlayers[3]); 
  } //}}}

  private void readVulner(Deal deal, String sLinVulner) //{{{
    throws DownloadFailedException
  {
    Map<String, String> msVul = new HashMap<String, String>();
    msVul.put("o", "None");
    msVul.put("b", "All");
    msVul.put("n", "NS");
    msVul.put("e", "EW");
    String sVulner = msVul.get(sLinVulner);
    if (sVulner == null) {
      throw new DownloadFailedException(
        PbnTools.getStr("error.linUnrecognVulner", sLinVulner),
        m_sp, !m_bSilent);
    }
    deal.setVulner(sVulner);
  } //}}}

  // setCards method {{{
  /**
   * Sets cards of given color and given person.
   * @param sArg Whole argument being processed, to show in error message
   */
  private void setCards(Deal deal, int nPerson, int nColor,
    String sCards, String sArg)
    throws DownloadFailedException
  {
    for (int i=0; i < sCards.length(); i++) {
      char chCard = sCards.charAt(i);
      Card card = new Card();
      card.setColor(nColor);
      card.setRankCh(chCard);
      if (card.getRank() == 0)
        throw new DownloadFailedException(
          PbnTools.getStr("error.linUnrecognCard", chCard, sArg),
          m_sp, !m_bSilent);
      deal.setCard(card, nPerson);
    }
  } //}}}

  private void readHands(Deal deal, String sArg) //{{{
    throws DownloadFailedException
  {
    if (sArg.length() < 2)
      throw new DownloadFailedException(
        PbnTools.getStr("error.linCmdShort", "md", sArg),
        m_sp, !m_bSilent);
    String sLinDealer = sArg.substring(0, 1);
    deal.setDealer(getPerson(sLinDealer));
    
    String sHands = sArg.substring(1);
    String asHand[] = sHands.split(",");
    if (asHand.length != 3 && asHand.length != 4)
      throw new DownloadFailedException(
        PbnTools.getStr("error.linHandCount", "3/4", asHand.length, sArg),
        m_sp, !m_bSilent);
    int nPerson = Deal.person('S');
    for (String sHand: asHand) {
      Matcher m = Pattern.compile("^S(.*)H(.*)D(.*)C(.*)$").matcher(sHand);
      if (!m.matches())
        throw new DownloadFailedException(
          PbnTools.getStr("error.linUnrecognCards", Deal.personChar(nPerson),
            "S...H...D...C...", sHand), m_sp, !m_bSilent);
      setCards(deal, nPerson, Card.SPADE, m.group(1), sArg);
      setCards(deal, nPerson, Card.HEART, m.group(2), sArg);
      setCards(deal, nPerson, Card.DIAMOND, m.group(3), sArg);
      setCards(deal, nPerson, Card.CLUB, m.group(4), sArg);
      nPerson = Deal.nextPerson(nPerson);
    }
    deal.dealRemCards();
    deal.fillHands();
  } //}}}

  private void readBid(Deal d, String sArg) //{{{
    throws DownloadFailedException
  {
    sArg = sArg.replaceFirst("!$", "");
    sArg = sArg.replaceFirst("N$", "NT");
    Map<String, String> m = new HashMap<String, String>();
    m.put("p", "Pass");
    m.put("d", "X");
    m.put("d!", "X");
    m.put("r", "XX");
    if (m.containsKey(sArg)) {
      d.addBid(m.get(sArg));
    } else {
      d.addBid(sArg);
    }
  } //}}}

  // readPlay method {{{
  /** Reads played card. */
  private void readPlay(Deal d, String sArg)
    throws DownloadFailedException
  {
    if (sArg.length() != 2)
      throw new DownloadFailedException(
        PbnTools.getStr("error.pbn.wrongCard", sArg));
    Card c = new Card(Card.color(sArg.charAt(0)), Card.rank(sArg.charAt(1)));
    if (!c.isOk())
      throw new DownloadFailedException(
        PbnTools.getStr("error.pbn.wrongCard", sArg));

    m_nCurCard += 1;
    int nHolder = d.getCardHolder(c);
    if (nHolder >= 0)
      d.addPlay((m_nCurCard - 1) / 4 + 1, nHolder, c);
  } //}}}

  // getPerson {{{
  /**
   * Gets a person (from {@link Deal} class, reading from lin char.
   * @param sPerson A string, of which first character denotes person.
   */
  public int getPerson(String sPerson)
    throws DownloadFailedException
  {
    char ch = sPerson.charAt(0);
    switch (ch) {
      case '1': return Deal.S;
      case '2': return Deal.W;
      case '3': return Deal.N;
      case '4': return Deal.E;
      default: throw new DownloadFailedException(
        PbnTools.getStr("error.invalidPerson", sPerson), m_sp, !m_bSilent);
    }
  } //}}}

  // readLin method {{{
  /**
   * Reads deals from a lin contents.
   * @param sLin Lin contents
   */
  public Deal[] readLin(String sLin, boolean bSilent)
    throws DownloadFailedException
  {
    // m_sp.addLine(sLin);
    m_bSilent = bSilent;
    m_nCurCard = 0;
    Deal d = new Deal();
    String sLastComm = "";
    Scanner sc = new Scanner(sLin).useDelimiter("\\|");
    while (sc.hasNext()) {
      String sComm = sc.next();
      if (sComm.length() == 0) {
       throw new DownloadFailedException(
         PbnTools.getStr("lin.error.emptyCmd", sc.match().start()),
           m_sp, !m_bSilent);
      }
      if (!sc.hasNext()) {
       throw new DownloadFailedException(
         PbnTools.getStr("lin.error.noArg", sComm), m_sp, !m_bSilent);
      }
      String sArg = sc.next();
      if (sComm.equals("ah"))
        readNumber(d, sArg);
      else if (sComm.equals("an") && sLastComm.equals("mb")) {
        d.annotateLastBid(sArg);
      } else if (sComm.equals("pc"))
        try {
          readPlay(d, sArg);
        } catch (DownloadFailedException dfe) {
          // ignore played cards errors, as it's not the main functionality
          m_sp.addLine(dfe.getMessage());
        }
      else if (sComm.equals("pg"))
        {} // pause game, ignore it
      else if (sComm.equals("pn"))
        readPlayers(d, sArg);
      else if (sComm.equals("mb"))
        readBid(d, sArg);
      else if (sComm.equals("mc")) {
        try {
          int cTricks = Integer.parseInt(sArg);
          if (cTricks < 0 || cTricks > 13)
            throw new NumberFormatException();
          d.setResult(cTricks);
        } catch (NumberFormatException nfe) {
          m_sp.addLine(PbnTools.getStr("error.linInvArg", sComm, sArg));
        }
      } else if (sComm.equals("md"))
        readHands(d, sArg);
      else if (sComm.equals("rh") || sComm.equals("st")) {
        if (!sArg.isEmpty() && !m_bSilent)
          m_sp.addLine(PbnTools.getStr("msg.interesting", sComm + sArg));
      }
      else if (sComm.equals("sv"))
        readVulner(d, sArg);
      else {
        if (!m_bSilent) {
          m_sp.addLine(PbnTools.getStr("msg.interesting",
            "Command: " + sComm + ", arg: " + sArg));
        }
      }
      sLastComm = sComm;
    }
    return new Deal[] { d };
  } //}}}
  
  public Deal[] readDeals(String sUrl, boolean bSilent) //{{{
    throws DownloadFailedException
  {
    assert(m_doc != null);
    String sLin = m_doc.text();
    return readLin(sLin, bSilent);
  } //}}}

  // verify method {{{
  /** Verifies if the <code>sUrl</code> contains valid data in this format */
  public boolean verify(String sUrl, boolean bSilent)
  {
    // We should read lin file directly, but SoupProxy has a cache
    // so it would be more network efficient to use it.
    m_doc = null;
    try {
      SoupProxy proxy = new SoupProxy();
      m_doc = proxy.getDocument(sUrl);
    }
    catch (JCException e) {
      m_sp.addLine(e.getMessage());
      return false;
    }
    String sLin = m_doc.text();
    for (int i=0; i<sLin.length(); i++) {
      char ch = sLin.charAt(i);
      if (ch < 32 || ch > 126) {
        if (!bSilent || f.isDebugMode())
          m_sp.addLine(PbnTools.getStr("msg.unexpChar", ""+ch, i));
        return false;
      }
    }
    if (sLin.indexOf('|') < 0) {
      if (!bSilent || f.isDebugMode())
        m_sp.addLine(PbnTools.getStr("msg.missChar", "|"));
      return false;
    }
    return true;
  } //}}}
    
  // setOutputWindow method {{{
  /** Sets the window to which output messages will be directed */
  public void setOutputWindow(SimplePrinter sp)
  {
    m_sp = sp;
  } //}}}
}
