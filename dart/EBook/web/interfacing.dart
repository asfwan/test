part of ebook;

class BtnMsg {
  /**
   * Used for popup context menu.
   */
  static const BtnMsg ADD_HIGHLIGHT = const BtnMsg("addHighlight", "Add Highlight", "Highlight the selected text.");
  static const BtnMsg CLEAR_HIGHLIGHT = const BtnMsg("clearHighlight", "Clear Highlight", "Remove selected highlight.");
  static const BtnMsg EXTEND_HIGHLIGHT = const BtnMsg("extendHighlight", "Extend Highlight", "Extend the highlight to the selected text.");
  static const BtnMsg MERGE_HIGHLIGHT = const BtnMsg("mergeHighlight", "Extend & Merge Highlights", "Extend and merge selected highlights.");
  static const BtnMsg ADD_NOTE = const BtnMsg("addNote", "Add Note", "Add note to the selected text.");
  static const BtnMsg CLEAR_NOTE = const BtnMsg("clearNote", "Clear Note", "Delete the selected note.");
  static const BtnMsg EXTEND_NOTE = const BtnMsg("extendNote", "Extend Note", "Extend the note to the selected text.");
  static const BtnMsg MERGE_NOTE = const BtnMsg("mergeNote", "Extend & Merge Notes", "Extend and merge selected notes.");
  static const BtnMsg EXTEND_NOTE_ONTO_HIGHLIGHT = const BtnMsg("extendNoteOntoHighlight", "Extend note onto highlight", "Extend the note onto the highlight.");
  static const BtnMsg MERGE_EXTEND_NOTE_ONTO_HIGHLIGHT = const BtnMsg("mergeExtendNoteOntoHighlight", "Merge notes and extend onto highlight", "Merge the notes and extend the resulting note onto the highlight.");
  static const BtnMsg EDIT_NOTE = const BtnMsg("editNote", "Edit Note", "Edit the note.");
  static const BtnMsg ADD_BOOKMARK = const BtnMsg("addBookmark", "Add Bookmark", "Bookmark the selected location.");
  static const BtnMsg DELETE_BOOKMARK = const BtnMsg("deleteBookmark", "Delete Bookmark", "Delete the bookmark.");
  static const BtnMsg GOTO_BOOKMARK = const BtnMsg("gotoBookmark", "Go To Bookmark", "Scroll to the bookmark.");
  static const BtnMsg GOTO_MARK = const BtnMsg("gotoMark", "Go To Mark", "Scroll to the mark.");
  
  /**
   * Used for note editing dialog.
   */
  static const BtnMsg CREATE_NOTE = const BtnMsg("createNote", "Create", "Create and add the note.");
  static const BtnMsg SAVE_NOTE = const BtnMsg("saveNote", "Save", "Save the note.");

  static const BtnMsg COPY = const BtnMsg("copy", "Copy", "Copy selection to clipboard.");
  static const BtnMsg CANCEL = const BtnMsg("cancel", "Cancel", "Cancel.");

  static const List<BtnMsg> BTN_MSGS = const [
    ADD_HIGHLIGHT,
    CLEAR_HIGHLIGHT,
    EXTEND_HIGHLIGHT,
    MERGE_HIGHLIGHT,
    ADD_NOTE,
    CLEAR_NOTE,
    EXTEND_NOTE,
    MERGE_NOTE,
    EXTEND_NOTE_ONTO_HIGHLIGHT,
    MERGE_EXTEND_NOTE_ONTO_HIGHLIGHT,
    EDIT_NOTE,
    ADD_BOOKMARK,
    DELETE_BOOKMARK,
    GOTO_BOOKMARK,
    GOTO_MARK,
    CREATE_NOTE,
    SAVE_NOTE,
    COPY,
    CANCEL
  ];

  static final Map<String,BtnMsg> BTN_MSG_MAP = _compileBtnMap();

  final String id;
  final String name;
  final String msg;
  final String extra;

  const BtnMsg(String this.id, String this.name, String this.msg) : extra=null;
  
  get hashCode {
    return id.hashCode;
  }
   
  BtnMsg.from(BtnMsg btn, String this.extra) :
    id=btn.id, name=btn.name, msg=btn.msg {
  }

  Object toJsonObject() {
    return {'id':this.id, 'name':this.name, 'msg':this.msg, 'extra':this.extra};
  }

  static Map<String,BtnMsg> _compileBtnMap() {
    Map<String,BtnMsg> map = new Map<String,BtnMsg>();
    for (BtnMsg b in BTN_MSGS) {
      map[b.id] = b;
    }
    return map;
  }

  static BtnMsg findBtnWithId(String id) {
    return BTN_MSG_MAP[id];
  }
}

/*
 * Refers to the location of a node.
 * Tag refers to the tag of the node referred to. Text nodes have a tag of NULL.
 * Index refers to the index of the node within the parent node. Meta-nodes
 * have an index of NULL.
 */
class NodeLoc {
  final String tag;
  final int index;

  NodeLoc(String this.tag, int this.index) {
    if (this.tag==null || this.index==null) {
      throw new Exception("Invalid state.");
    }
  }

  NodeLoc.metaNode(String this.tag) : index = null {
    if (this.tag==null) {
      throw new Exception("Invalid state.");
    }
  }

  NodeLoc.textNode(int this.index) : tag = null {
    if (this.index==null) {
      throw new Exception("Invalid state.");
    }
  }

  bool isTextNode() {
    return this.tag==null;
  }

  bool isMetaNode() {
    return this.index==null;
  }

  String toString() {
    if (isTextNode()) {
      return "<_text>[${this.index}]";
    } else
      if (isMetaNode()) {
        return "<_meta:${this.tag}>";
      }
    return "<${this.tag}>[${this.index}]";
  }

  Object toJsonObject() {
    Map map = new Map();
    map["tag"] = tag;
    map["index"] = index;
    return map;
  }

  static NodeLoc parseFromObj(Map map) {
    String tag = map["tag"];
    int index = map["index"];
    if (tag==null && index!=null) {
      return new NodeLoc.textNode(index);
    } else
      if (tag!=null && index==null) {
        return new NodeLoc.metaNode(tag);
      } else {
        return new NodeLoc(tag, index);
      }
  }
}

class RangePoint {
  final List<NodeLoc> nodeLocs;
  final int textLoc;

  RangePoint(List<NodeLoc> nodeLoc, int this.textLoc) :
    nodeLocs = new List.from(nodeLoc, growable:false)
  {

  }

  Object toJsonObject() {
    Map map = new Map();
    List jsonNodeLocs = new List();
    for (NodeLoc n in nodeLocs) {
      jsonNodeLocs.add(n.toJsonObject());
    }
    map["nodeLocs"] = jsonNodeLocs;
    map["textLoc"] = textLoc;
    return map;
  }
  
  static RangePoint parseFromObj(Map map) {
    List<NodeLoc> nodeLocs = new List();
    for (Map m in map["nodeLocs"]) {
      nodeLocs.add(NodeLoc.parseFromObj(m));
    }
    return new RangePoint(nodeLocs, map["textLoc"]);
  }
  
  String toString() {
    return "${nodeLocs.toString()}:${textLoc}"; 
  }

}

class RangePoints {
  final RangePoint start;
  final RangePoint end;

  RangePoints(RangePoint this.start, RangePoint this.end) {

  }

  String toString() {
    return "{start=${start};end=${end}}"; 
  }
  
  Object toJsonObject() {
    Map map = new Map();
    map["start"] = start.toJsonObject();
    map["end"] = end.toJsonObject();
    return map;
  }

  static RangePoints parseFromObj(Map map) {
    return new RangePoints(RangePoint.parseFromObj(map["start"]),
        RangePoint.parseFromObj(map["end"]));
  }

  static RangePoints parseFromStr(String v) {
    Map map = json.parse(v);
    return parseFromObj(map);
  }
}
