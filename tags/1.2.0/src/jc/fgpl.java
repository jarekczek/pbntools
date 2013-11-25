/* ***************************************************************************

    compareString: Copyright (C) 2001 Slava Pestov

    jedit settings: :folding=explicit:indentSize=2:noTabs=true:

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
   ***************************************************************************
   
*/

package jc;

public class fgpl {

  //{{{ compareStrings method
  /**
   * Compares two strings. Copied from jedit: org.gjt.sp.StandardUtilities.
   * Last modified in r3881 in org.gjt.sp.jedit.MiscUtilities.
   * Author: Slava Pestov.<p>
   *
   * Unlike <function>String.compareTo()</function>,
   * this method correctly recognizes and handles embedded numbers.
   * For example, it places "My file 2" before "My file 10".<p>
   *
   * @param str1 The first string
   * @param str2 The second string
   * @param ignoreCase If true, case will be ignored
   * @return negative If str1 &lt; str2, 0 if both are the same,
   * positive if str1 &gt; str2
   * @since jEdit 4.3pre5
   */
  public static int compareStrings(String str1, String str2, boolean ignoreCase)
  {
    char[] char1 = str1.toCharArray();
    char[] char2 = str2.toCharArray();

    int len = Math.min(char1.length,char2.length);

    for(int i = 0, j = 0; i < len && j < len; i++, j++)
    {
      char ch1 = char1[i];
      char ch2 = char2[j];
      if(Character.isDigit(ch1) && Character.isDigit(ch2)
        && ch1 != '0' && ch2 != '0')
      {
        int _i = i + 1;
        int _j = j + 1;

        for(; _i < char1.length; _i++)
        {
          if(!Character.isDigit(char1[_i]))
          {
            //_i--;
            break;
          }
        }

        for(; _j < char2.length; _j++)
        {
          if(!Character.isDigit(char2[_j]))
          {
            //_j--;
            break;
          }
        }

        int len1 = _i - i;
        int len2 = _j - j;
        if(len1 > len2)
          return 1;
        else if(len1 < len2)
          return -1;
        else
        {
          for(int k = 0; k < len1; k++)
          {
            ch1 = char1[i + k];
            ch2 = char2[j + k];
            if(ch1 != ch2)
              return ch1 - ch2;
          }
        }

        i = _i - 1;
        j = _j - 1;
      }
      else
      {
        if(ignoreCase)
        {
          ch1 = Character.toLowerCase(ch1);
          ch2 = Character.toLowerCase(ch2);
        }

        if(ch1 != ch2)
          return ch1 - ch2;
      }
    }

    return char1.length - char2.length;
  } //}}}

}
