package jc;

abstract public class MyAction extends javax.swing.AbstractAction {
  MyAction() { super(); }
  public MyAction(String sProp) {
    super();
    f.setTextAndMnem(this, sProp);
  }
}

/*abstract public class MyAction extends javax.swing.AbstractAction {
  protected java.awt.Window m_parent;
  MyAction() { super(); }
  public MyAction(java.awt.Window parent, String sProp) {
    super();
    m_parent = parent;
    f.setTextAndMnem(this, sProp);
  }
}
*/
