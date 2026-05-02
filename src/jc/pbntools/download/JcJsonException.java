package jc.pbntools.download;

import jc.JCException;

public class JcJsonException extends JCException {
  public JcJsonException(Throwable t) {
    super(t);
  }

  public JcJsonException(String sMessage) {
    super(sMessage);
  }
}
