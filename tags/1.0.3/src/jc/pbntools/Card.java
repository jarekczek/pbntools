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

public class Card {
  static final int SPADE = 1;
  static final int HEART = 2;
  static final int DIAMOND = 3;
  static final int CLUB = 4;
  static final String m_asKolAng[] = { "S", "H", "D", "C" };
  static final char m_achRank[] = { '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A' };
  private int m_nCode;  // 16*kolor(1-4) + wysokosc karty(2-14); czyli od 18 (S2) do 78 (CA)
  static final int MAX_KOD = 78;

  Card() { zeruj(); }
  
  static int rank(char ch) {
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
  static char rankChar(int nWys) { return nWys>=2 && nWys<=14 ? m_achRank[nWys-2] : '?'; }
  static char colorChar(int nKolor) { return nKolor>=1 && nKolor<=4 ? m_asKolAng[nKolor-1].charAt(0) : '?'; }

  static int kolor(char ch) {
    int i;
    for (i=0; i<m_asKolAng.length; i++) { if (m_asKolAng[i].charAt(0) == ch) { return i+1; } }
    return 0;
    }
  static int nastKolor(int nKolor) { return (nKolor<=0) ? 0 : (nKolor%4)+1; }
  
  static int kod(int nKolor, int nWys) { return (nKolor>=1 && nKolor<=4 && nWys>=2 && nWys<=14) ? 16*nKolor+nWys : 0; }

  void zeruj() { m_nCode = 0; }
  int getCode() { return m_nCode; }
  int getKolor() { return m_nCode<=0 ? 0 : m_nCode/16; }
  int getRank() { return m_nCode<=0 ? 0 : m_nCode%16; }
  void setKod(int nKod) { m_nCode = nKod; }
  void set(int nKolor, int nWys) { m_nCode = kod(nKolor, nWys); }
  boolean czyOk() { return m_nCode>0 && m_nCode<=MAX_KOD; }
  
  static int kod(String sCard) {
    if (sCard.length()!=2) { return 0; }
    return kod(kolor(sCard.charAt(0)), rank(sCard.charAt(1)));
    }
  
  boolean setKolor(char chKolor) {
    int nKolor;
    m_nCode = m_nCode % 16;
    nKolor = kolor(chKolor);
    if (nKolor>0) { m_nCode += nKolor * 16; return true; }
    else { return false; }
    }

  public String toString() { return "" + colorChar(getKolor()) + rankChar(getRank()); }
  }
