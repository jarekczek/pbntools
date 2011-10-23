SOURCE CODE NOTES

Official language for source variables, comments and stuff is English.
Due to historical reasons some of them are still in Polish, but all new code
should use English.

Usually following tab settings are adviced: :tabSize=2:noTabs=true:

In some old files there may be found incorrect indentation like:
int fun() {
  a=1;
  }
More common positioning of the closing bracket is at the start of line
(one tab to the left), and this should be considered correct.

Patches should be generated using diff subcommand of the subversion system,
against the latest repository.
