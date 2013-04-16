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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * color: 1-4 (SHDC).<br>
 * rank: 0 = invalid
 */  
public class Card implements Comparable<Card> {
  public static final int SPADE = 1;
  public static final int HEART = 2;
  public static final int DIAMOND = 3;
  public static final int CLUB = 4;
  static final String m_asKolAng[] = { "S", "H", "D", "C" };
  static final char m_achRank[] = { '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A' };
  /** 16*color(1-4) + rank(2-14), that is from 18 (S2) up to 78 (CA),
    * excluding some numbers from the range */
  private int m_nCode;
  static final int MIN_CODE = 18;
  static final int MAX_CODE = 78;
  public static final int MIN_RANK = 2;
  public static final int MAX_RANK = 14;

  public Card() { clear(); }
  public Card(int nCode) { clear(); setCode(nCode); }
  
  /** The suggested constructor. For example call:
   *  <code>new Card(Card.color(chColor), Card.rank(chRank))</code>
   */
  public Card(int nColor, int nRank)
  {
    clear(); setRank(nRank); setColor(nColor);
  }
  
  public static int rank(char ch) {
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
    
  public static int rank(String sRank)
  {
    if ("10".equals(sRank)) { sRank = "T"; }
    if (sRank == null || sRank.length() != 1) {
      throw new IllegalArgumentException("Card rank not known: " + sRank);
    }
    return rank(sRank.charAt(0));
  }
  
  public static char rankChar(int nWys) { return nWys>=2 && nWys<=14 ? m_achRank[nWys-2] : '?'; }
  public static char colorChar(int nColor) { return nColor>=1 && nColor<=4 ? m_asKolAng[nColor-1].charAt(0) : '?'; }
  public boolean isOk() { return m_nCode >= MIN_CODE && m_nCode <= MAX_CODE
                                 && m_nCode%16 >= MIN_RANK
                                 && m_nCode%16 <= MAX_RANK; }

  public static int color(char ch) {
    int i;
    for (i=0; i<m_asKolAng.length; i++) { if (m_asKolAng[i].charAt(0) == ch) { return i+1; } }
    return 0;
    }
  static int nextColor(int nColor) { return (nColor<=0) ? 0 : (nColor%4)+1; }
  
  // firstCard method {{{
  /** 
   * Returns the first card to allow enumerating all 52 cards,
   * together with {@link #nextCard} function.
   * See also {@link #getIter}.
   */
  public static Card firstCard()
  {
    return new Card(MIN_CODE);
  } //}}}
  
  // nextCard method {{{
  /** Returns next card, to allow enumerating 52 cards. See also
    * {@link #firstCard}.
    * @returns <code>null</code> if the card invalid or the last one.
    */
  public Card nextCard()
  {
    if (!isOk())
      return null;
    int nRank = getRank();
    int nColor = getColor();
    Card c;
    if (nRank == MAX_RANK) {
      if (nColor == 4)
        return null;
      else
        c = new Card(nColor + 1, MIN_RANK);
    } else {
      c = new Card(nColor, nRank + 1);
    }
    return c;
  } //}}}
  
  static int kod(int nColor, int nWys) { return (nColor>=1 && nColor<=4 && nWys>=2 && nWys<=14) ? 16*nColor+nWys : 0; }

  public void clear() { m_nCode = 0; }
  public int getCode() { return m_nCode; }
  public int getColor() { return m_nCode<=0 ? 0 : m_nCode/16; }
  public int getRank() { return m_nCode<=0 ? 0 : m_nCode%16; }
  public char getRankChar() { return m_nCode<=0 ? 0 : rankChar(m_nCode%16); }
  public void setCode(int nCode) { m_nCode = nCode; }
  public void setRank(String sRank) { setRank(rank(sRank)); }
  public void setRankCh(char chRank) { setRank(rank(chRank)); }
  public void set(int nColor, int nWys) { m_nCode = kod(nColor, nWys); }
  
  static int kod(String sCard) {
    if (sCard.length()!=2) { return 0; }
    return kod(color(sCard.charAt(0)), rank(sCard.charAt(1)));
    }
  
  public boolean setColor(char chColor) {
    int nColor = color(chColor);
    return setColor(nColor);
    }

  public boolean setColor(int nColor) {
    m_nCode = m_nCode % 16;
    if (nColor>0) { m_nCode += nColor * 16; return true; }
    else { return false; }
    }

  public boolean setRank(int nRank) {
    m_nCode = m_nCode & 0xFFF0;
    if (nRank > 0) { m_nCode += nRank; return true; }
    else { return false; }
    }

  // getIter method {{{
  protected static class AllCardsIterator
    implements Iterable<Card>, Iterator<Card>
  {
    Card m_card = null;
    
    public boolean hasNext()
    {
      return m_card == null || m_card.nextCard() != null;
    }
    
    public Card next()
    {
      Card nextCard;
      if (m_card == null)
        nextCard = firstCard();
      else
        nextCard = m_card.nextCard();
      if (nextCard == null)
        throw new NoSuchElementException();
      m_card = nextCard;
      return m_card;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
    
    public Iterator<Card> iterator()
    {
      return this;
    }
  }
  
  /** Returns an iterable object over 52 cards, to use in for(x: ...) */
  public static Iterable<Card> getIter()
  {
    return new AllCardsIterator();
  } //}}}
    
  public String toString() { return "" + colorChar(getColor()) + rankChar(getRank()); }
  
  public int compareTo(Card c) {
    return new Integer(this.getCode()).compareTo(c.getCode());
  }
}
