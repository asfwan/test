part of ebook;

class Logger {

  static const int ELEMENT_NODE = 1;
  static const int TEXT_NODE = 3;
  
  bool _loggingOn;
  bool _logToCommadLog;

  Logger._internal(bool this._loggingOn, bool this._logToCommadLog);

  void _eval(String s) {
    //print("evaluating: '${s}'");
//    js.scoped(() {
//      js.context.eval(s);
//    });
  }

  static String summarize(String v, int maxCharCount) {
    if (v.length>maxCharCount) {
      int endSize = (maxCharCount-2)~/2;
      String start = v.substring(0, endSize);
      String end = v.substring(v.length-endSize, v.length);
      return "${start}..${end}";
    } else {
      return v;
    }
  }

  static void _genRef(Node v, StringBuffer s) {
    if (v.parent!=null) {
      _genRef(v.parent, s);
      s.write(".");
      s.write("childNodes[${v.parent.nodes.indexOf(v)}]");
    } else {
      s.write("document.documentElement");
    }
  }

  static void _genXPathRef(Node v, StringBuffer s) {
    if (v.parent!=null) {
      _genXPathRef(v.parent, s);
      s.write("/");
      if (v.nodeType==ELEMENT_NODE) {
        Element el  = v as Element;
        int index;
        if (el.tagName.toLowerCase()=="body") {
          index = 0;
        } else {
          index = indexOfElement(el);
        }
        s.write("${(v as Element).tagName}[${index}]");
      } else {
        int index = indexOfNode(v);
        if (v.nodeType==TEXT_NODE) {
          s.write("'${summarize(v.text, 20)}'[${index}]");
        } else {
          s.write("${v.runtimeType.toString()}[${index}]");
        }
      }
    }
  }

  static String ref(Node v) {
    StringBuffer s = new StringBuffer();
    _genRef(v, s);
    return s.toString();
  }

  static String pathTo(Node v) {
    StringBuffer s = new StringBuffer();
    _genXPathRef(v, s);
    return s.toString();
  }

  static String listToStr(List objects, var objToStrFunc) {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<objects.length; ++i) {
      if (i>0) {
        sb.write(", ");
      }
      sb.write(objToStrFunc(objects[i]) as String);
    }
    return sb.toString();
  }

  void _log(bool group, bool collapse, List<Object> params) {
    if (!_loggingOn) {
      return;
    }

    StringBuffer strParams = new StringBuffer();
    for (int i=0; i<params.length; ++i) {
      Object param = params[i];
      if (i>0) {
        strParams.write(", ");
      }
      if (param is String) {
        String msg = param as String;
        msg = msg.replaceAll('"', '\\"');
        msg = msg.replaceAll("'", "\\'");
        msg = msg.replaceAll('\t', '\\t');
        msg = msg.replaceAll('\r', '\\r');
        msg = msg.replaceAll('\n', '\\n');
        strParams.write('"${msg}"');
      } else {
        if (param is Node) {
          if (_logToCommadLog) {
            _genRef(param as Node, strParams);
          } else {
            strParams.write(param.toString());
          }
        } else {
          strParams.write(param.toString());
        }
      }
    }

    if (_logToCommadLog) {
      String command;
      if (group) {
        if (collapse) {
          command = 'console.groupCollapsed(${strParams.toString()});';
        } else {
          command = 'console.group(${strParams.toString()});';
        }
      } else {
        command = 'console.log(${strParams.toString()});';
      }
      _eval(command);
    } else {
      print(params);
    }
  }

  void log(List<Object> params) {
    _log(false, false, params);
  }

  void group(List<Object> params) {
    _log(true, false, params);
  }

  void groupCollapsed(List<Object> params) {
    _log(true, true, params);
  }

  void groupEnd() {
    if (_logToCommadLog) {
      _eval("console.groupEnd();");
    }
  }
}



