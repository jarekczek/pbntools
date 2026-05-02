package jc.pbntools.download;

import com.fasterxml.jackson.databind.JsonNode;
import jc.pbntools.PbnTools;

public class JsonHelper {
  public static JsonNode nodeAt(JsonNode parent, String path) throws JcJsonException {
    JsonNode rv = parent.at(path);
    if (rv.isMissingNode()) {
      throw new JcJsonException(PbnTools.getStr("error.nodeNotFound", path));
    }
    return rv;
  }
}
