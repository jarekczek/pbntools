SOURCE CODE NOTES

Official language for source variables, comments and stuff is English.
Due to historical reasons some of them are still in Polish, but all new code
should use English.

Hungarian notation should be used. Simple type variables prefixed by
their type letter (e.g. n for integer, s for string). 
Object variables do not require this prefix. Additionally
member variables are prefixed with m_, e.g. m_sPath.

Usually following tab settings are adviced: :tabSize=2:noTabs=true:
Whenever possible, lines should not exceed 78 characters.

In some old files there may be found incorrect indentation like:
int fun() {
  a=1;
  }
More common positioning of the closing bracket is at the start of line
(one tab to the left), and this should be considered correct.

Patches should be generated using diff subcommand of the subversion system,
against the latest repository.
