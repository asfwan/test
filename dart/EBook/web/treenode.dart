part of ebook;

/**
 * Either the selection or meta state of a node/subtree
 */
class State {
  static const State NONE = const State._(0, "None");
  static const State PARTIAL = const State._(1, "Partial");
  static const State FULL = const State._(2, "Full");

  final int val;
  final String name;
  const State._(this.val, this.name);

  String toString() {
    return name;
  }
}

/**
 * A helper class for dealing with meta nodes/information.
 * A single instance of the class is shared among all the nodes of a tree.
 */
class MetaInfoHelper {
  final String cssClassNameHighlight;
  final String cssClassNameNote;
  final String cssClassNameHighlightNote;
  final OutgoingInterface interface;
  final AnnotationCache annotationCache;
  final SidePanelEntryManager sidePanelEntryManager;
  Set<String> _classNames;

  MetaInfoHelper(String this.cssClassNameHighlight, String this.cssClassNameNote, String this.cssClassNameHighlightNote,
      OutgoingInterface this.interface, AnnotationCache this.annotationCache,
      SidePanelEntryManager this.sidePanelEntryManager)
  {
      _classNames = new Set<String>.from([
        this.cssClassNameHighlight,
        this.cssClassNameNote,
        this.cssClassNameHighlightNote
      ]);
  }

  /**
   * Return the name of the style class that corresponds to the given states.
   */
  String getStyleClassFor(bool highlight, bool note) {
    if (!highlight && !note) {
      throw new Exception("Invalid meta-class, neither highlighted nor noted");
    }
    if (highlight && note) {
      return cssClassNameHighlightNote;
    } else {
      if (highlight) {
        return cssClassNameHighlight;
      } else {
        return cssClassNameNote;
      }
    }
  }

  /**
   * Creates a new span node, and assigns a unique ID and title.
   */
  SpanElement createSpanNode(String id, bool block, bool highlight, bool note) {
    SpanElement span = new SpanElement();
    span.classes.add(getStyleClassFor(highlight, note));
    span.id = id;
    span.title = id;
    if (block) {
      span.style.display = "block";
    }
    return span;
  }
  
  /**
   * Checks whether a node is a meta node. Returns true,
   * if the node is a meta node, false if not.
   */
  bool isMetaNode(Node node) {
    return node.nodeType==TreeNode.ELEMENT_NODE
        && ((node as Element).classes.contains(cssClassNameHighlight) ||
            (node as Element).classes.contains(cssClassNameNote) ||
            (node as Element).classes.contains(cssClassNameHighlightNote));
  }

  /**
   * Checks whether node has style set for a note.
   */
  bool hasNoteStyle(Node node) {
    return node.nodeType==TreeNode.ELEMENT_NODE
        && ((node as Element).classes.contains(cssClassNameNote) ||
            (node as Element).classes.contains(cssClassNameHighlightNote));
  }

  /**
   * Checks whether node has style set for a highlight.
   */
  bool hasHighlightStyle(Node node) {
    return node.nodeType==TreeNode.ELEMENT_NODE
        && ((node as Element).classes.contains(cssClassNameHighlight) ||
            (node as Element).classes.contains(cssClassNameHighlightNote));
  }

  /**
   * Sets the style-class of a meta-node depending on whether whether
   * it's highlighted, has a note, or both.
   */
  void setStyle(Element e, bool highlight, bool note) {
    if (!isMetaNode(e)) {
      return;
    }
    for (String className in e.classes) {
      if (_classNames.contains(className)) {
        e.classes.remove(className);
      }
    }
    e.classes.add(getStyleClassFor(highlight,note));
  }
  
  /**
   * Updates the classes of the meta-marks to reflect the current state
   * of the annotations.
   */
  void updateMetaStyle(CachedAnnotation ann) {
    for (String markId in ann.getMarks()) {
      TreeNode node = rootNodeTree.findNodeWithId(markId);
      SpanElement e = node.domNode;
      
      for (String className in _classNames) {
        e.classes.remove(className);
      }
      
      e.classes.add(getStyleClassFor(true, ann.hasNote()));
      
      if (ann.hasNote()) {
        sidePanelEntryManager.createSidePanelEntryFor(node.domNode);
      } else {
        sidePanelEntryManager.removeSidePanelEntryFor(node.getId());
      }
    }
    sidePanelEntryManager.updatePositions();
  }

}

class _NodeGroupProps {
  final bool hasMeta;
 
  _NodeGroupProps(bool this.hasMeta) {
    
  }
  
  bool isEqual(_NodeGroupProps other) {
    return this.hasMeta==other.hasMeta;
  }
}

/**
 * A group of nodes, and some extra information like:
 * 1) Whether the group has meta information or not.
 * 2) Whether the group has a new node/new meta information.
 */
class _NodeGroup<T> {
  final _NodeGroupProps props;
  final List<T> nodes;
  bool hasNewNode;

  _NodeGroup(_NodeGroupProps this.props) : nodes = new List<T>(), hasNewNode = false;

  /**
   * Merge two groups and set the given resultingHasMeta parameter to the resulting group.
   */
  static _NodeGroup mergeGroups(_NodeGroup nodeGroup1, _NodeGroup nodeGroup2, _NodeGroupProps resultingProps) {
    var resGroup = new _NodeGroup(resultingProps);
    resGroup.hasNewNode = nodeGroup1.hasNewNode || nodeGroup2.hasNewNode;
    resGroup.nodes.addAll(nodeGroup1.nodes);
    resGroup.nodes.addAll(nodeGroup2.nodes);
    return resGroup;
  }
}

/**
 * Some extra information to be used when processing nodes during a selection.
 */
class _ProcessSelectionRangeDetails {
  Range range;
  bool foundStart;
  bool foundEnd;

  _ProcessSelectionRangeDetails(Range this.range) : foundStart=false, foundEnd=false;
}

/**
 * Results generated from the highlight process.
 * This includes:
 * -  All meta-nodes added.
 * -  All meta-nodes deleted.
 */
class ProcessHighlightResults {
  List<String> metaNodesAdded;
  List<String> metaNodesDeleted;

  ProcessHighlightResults() : metaNodesAdded=new List(), metaNodesDeleted=new List();

  void mergeWith(ProcessHighlightResults other) {
    this.metaNodesAdded.addAll(other.metaNodesAdded);
    this.metaNodesDeleted.addAll(other.metaNodesDeleted);
  }
}

class _TreeNodeLoc {
  final TreeNode node;
  final int index;

  _TreeNodeLoc(TreeNode this.node, int this.index);
}


class _TreeNodeLocRange {
  final _TreeNodeLoc start;
  final _TreeNodeLoc end;
  
  _TreeNodeLocRange(_TreeNodeLoc this.start, _TreeNodeLoc this.end);
}

class _ComputePathResults {
  final int index;
  final bool prevWasText;
  
  _ComputePathResults(int this.index, bool this.prevWasText);
}

/**
 * Main component of the node tree, which is built one-for-one against the DOM tree.
 * It's nodes maintain meta information used for processing selections, adding meta
 * information, and removing meta information.
 */
class TreeNode {
  static const int ELEMENT_NODE = 1;
  static const int TEXT_NODE = 3;

  static const bool AUTO_JOIN_GROUPS_SEP_BY_WHITESPACE = false;
  static const int MAX_SNAP_TO_END_CHAR_COUNT = 0;
  
  Node domNode;
  TreeNode parent;
  List<TreeNode> children;

//  final MetaInfoHelper metaInfoHelper;
  MetaInfoHelper metaInfoHelper;

  State _selectionState;

//  final bool isElementNode;
  bool isElementNode;
  
//  final bool isTextNode;
  bool isTextNode;
  int _textNodePartialSelectStart;
  int _textNodePartialSelectEnd;

//  final bool isMetaNode;
  bool isMetaNode;
  
//  final bool hasBackground;
  bool hasBackground;

  List<NodeLoc> _nodeLocation;
  
  String bookmarkId;

  TreeNode(TreeNode this.parent, Node domNode, MetaInfoHelper metaInfoHelper) :
      domNode = domNode,
      children = new List<TreeNode>(),
      metaInfoHelper = metaInfoHelper,
      _selectionState = State.NONE,
      _textNodePartialSelectStart = -1,
      _textNodePartialSelectEnd = -1,
      isElementNode = (domNode.nodeType==ELEMENT_NODE),
      isTextNode = (domNode.nodeType==TEXT_NODE),
      isMetaNode = metaInfoHelper.isMetaNode(domNode),
      hasBackground = _hasBackground(domNode)
  {
    //print("processing tree-node for dom-node ${Logger.pathTo(domNode)}");
    updateStructure();
    if (parent==null) {
      computeSubtreePath();
    }
  }

  /**
   * Get the root of the tree.
   */
  TreeNode getRoot() {
    if (this.parent==null) {
      return this;
    }
    return this.parent.getRoot();
  }

  /**
   * Get the first leaf of a subtree.
   */
  TreeNode getSubtreeFirstLeaf() {
    if (this.children.length==0) {
      return this;
    } else {
      this.children.first.getSubtreeFirstLeaf();
    }
  }

  /**
   * Get the last leaf of a subtree.
   */
  TreeNode getSubtreeLastLeaf() {
    if (this.children.length==0) {
      return this;
    } else {
      this.children.last.getSubtreeLastLeaf();
    }
  }

  /**
   * Returns the previous leaf, in a subtree.
   */
  TreeNode getSubtreePrevLeaf() {
    TreeNode prevSibling = getPrevSibling();
    if (prevSibling==null) {
      if (parent!=null) {
        return parent.getSubtreePrevLeaf();
      } else {
        return null;
      }
    } else {
      return prevSibling.getSubtreeLastLeaf();
    }
  }

  /**
   * Returns the next leaf, in a subtree.
   */
  TreeNode getSubtreeNextLeaf() {
    TreeNode nextSibling = getNextSibling();
    if (nextSibling==null) {
      if (parent!=null) {
        return parent.getSubtreeNextLeaf();
      } else {
        return null;
      }
    } else {
      return nextSibling.getSubtreeFirstLeaf();
    }
  }

  /**
   * Returns the first ancestor, that isn't a meta-node.
   */
  TreeNode getNonMetaAncestor() {
    if (this.parent!=null) {
      if (this.parent.isMetaNode) {
        return this.parent.getNonMetaAncestor();
      } else {
        return this.parent;
      }
    } else {
      return null;
    }
  }

  /**
   * Initiates the process in which all elements get assigned "paths".
   * Paths reflect the location of the node in the tree, without any meta-nodes.
   * This method should be called after the tree has been constructed, or
   * after a subtree has been updated.
   * This method relies on the _recursiveComputePath method to perform most of the work.
   */
  void computeSubtreePath() {
    //print("\tcompute subtree path for dom-node ${Logger.pathTo(this.domNode)}");
    
    bool computed = false;
    if (this._nodeLocation==null) {
      if (this.parent==null) {
        this._nodeLocation = new List();
      } else {
        this.parent.computeSubtreePath();
        computed = true;
      }
    }
    if (!computed) {
      if (this.isMetaNode || this.isTextNode) {
        this.parent.computeSubtreePath();
      } else {
        _ComputePathResults childRes = new _ComputePathResults(null, false);
        for (int i=0; i<this.children.length; ++i) {
          childRes = this.children[i]._recursiveComputePath(this._nodeLocation, childRes);
        }
      }
    }
  }
  
  /**
   * Used to assign paths to recursively nodes. Each element and text node in
   * the tree receives a path object which is actually a list reflecting
   * it's location in the original DOM hierarchy, without any meta nodes.
   * Meta nodes (if any exist) are ignored, and their children are processed as
   * if they belong to the meta node's parent.
   */
  _ComputePathResults _recursiveComputePath(List<NodeLoc> parentPath, _ComputePathResults prevRes) {
    //  ignore any nodes that aren't either elements or text nodes
    if (!this.isElementNode && !this.isTextNode) {
      this._nodeLocation = null;
      return new _ComputePathResults(prevRes.index, prevRes.prevWasText);
    }

    List<NodeLoc> path = new List<NodeLoc>();
    path.addAll(parentPath);

    if (this.isMetaNode) {
      //  Meta nodes don't count. Instead their children get counted
      //  as part of their parents' children.
      
      /*
      //  If this meta-node has only one text node as a child...
      if (this.children.length==1 && !this.children.first.isTextNode) {
        TreeNode prevSibling = this.getPrevSibling();
        //  if the previous sibling is a text node
        if (prevSibling!=null && prevSibling.isTextNode) {
          //  ignore this node, text node is part of the previous sibling's text
          return index;
        }
        TreeNode nextSibling = this.getNextSibling();
        if (nextSibling!=null && nextSibling.isTextNode) {
          //  ignore this node, text node is part of the next sibling's text
          return index;
        }
      }
      */
      
      // otherwise pass the message onto children
      for (int i=0; i<this.children.length; ++i) {
        prevRes = this.children[i]._recursiveComputePath(parentPath, prevRes);
      }

      path.add(new NodeLoc.metaNode(getTag()));
    } else {
      if (this.isTextNode) {
        int index = prevRes.index;
        if (index==null) {
          index = 0;
        } else {
          if (!prevRes.prevWasText) {
            ++index;
          }
        }
        path.add(new NodeLoc.textNode(index));
        prevRes = new _ComputePathResults(index, true);
      } else {
        int index = prevRes.index;
        if (index==null) {
          index = 0;
        } else {
          ++index;
        }
        path.add(new NodeLoc(getTag(), index));
        
        _ComputePathResults childRes = new _ComputePathResults(null, false);
        for (int i=0; i<this.children.length; ++i) {
          childRes = this.children[i]._recursiveComputePath(path, childRes);
        }
        
        prevRes = new _ComputePathResults(index, false);
      }
    }
    this._nodeLocation = path;
    return prevRes;
  }
  
  /**
   * Returns the node at the given location (NodeLoc) index.
   */
  TreeNode findChildWithLoc(NodeLoc nodeLoc) {
    for (int i=0; i<children.length; ++i) {
      TreeNode child = children[i];
      if (child.isMetaNode) {
        TreeNode n = child.findChildWithLoc(nodeLoc);
        if (n!=null) {
          return n;
        }
      } else {
        NodeLoc childLoc = child._nodeLocation.last;
        if (childLoc.tag==nodeLoc.tag && childLoc.index==nodeLoc.index) {
          return child;
        }
      }
    }
    return null;
  }
  
  /**
   * Called from root, to find the node with the given path.
   */
  TreeNode findNodeWithPath(List<NodeLoc> path, [int startIndex = 0]) {
    TreeNode parent = this;
    for (int i=startIndex; i<path.length; ++i) {
      TreeNode child = parent.findChildWithLoc(path[i]);
      if (child==null) {
        throw new Exception("Mismatch at index $i of path: ${path[i]}");
      }
      parent = child;
    }
    return parent;
  }

  /**
   * Get the index of this node among it's siblings.
   * Returns -1 if the node has no parent (i.e. the root node).
   */
  int getIndex() {
    if (this.parent==null) {
      return -1;
    } else {
      return this.parent.children.indexOf(this);
    }
  }

  /**
   * Returns the previous sibling, if any.
   */
  TreeNode getPrevSibling() {
    if (this.parent==null) { return null; }
    var index = this.getIndex();
    if (index==0) { return null; }
    return this.parent.children[index-1];
  }

  /**
   * Returns the next sibling, if any.
   */
  TreeNode getNextSibling() {
    if (this.parent==null)  { return null; }
    var index = this.getIndex();
    if (index==this.parent.children.length-1) { return null; }
    return this.parent.children[index+1];
  }

  /**
   * Returns the ID of this node's DOM node.
   * Throws an exception if this node's DOM node is not an element.
   */
  String getId() {
    if (!this.isElementNode) {
      throw new Exception("Attempting to retrieve the ID of a non-element node.");
    }
    return (this.domNode as Element).id;
  }

  /**
   * Returns the TAG of this node's DOM node.
   * Throws an exception if this node's DOM node is not an element.
   */
  String getTag() {
    if (!this.isElementNode) {
      throw new Exception("Attempting to retrieve the tag of a non-element node.");
    }
    return (this.domNode as Element).tagName;
  }

  /**
   * Returns the text content of this node's DOM node.
   * Throws an exception if this node's DOM node is not a text node.
   */
  String getText() {
    if (!this.isTextNode) {
      throw new Exception("Attempting to retrieve the text of a non-text node.");
    }
    return this.domNode.text;
  }

  /**
   * Returns true, if this is an element, and it is a block-element,
   *  (e.g. div, p, etc.)
   */
  bool isOrHasBlockElement() {
    if (!this.isElementNode) {
      return false;
    }

    CssStyleDeclaration css = (this.domNode as Element).getComputedStyle();
    if (css.display.toLowerCase() == "block") {
      return true;
    }

    for (int i=0; i<children.length; ++i) {
      if (children[i].isOrHasBlockElement()) {
        return true;
      }
    }

    return false;
  }
  
  /**
   * Returns whether this node is inline or not.
   */
  bool isInline() {
    if (this.isTextNode) {
      return true;
    }
    if (this.isElementNode) {
      CssStyleDeclaration css = (domNode as Element).getComputedStyle();
      return css.display=="inline";
    }
    return false;
  }
  
  static RegExp _rgbaRegEx = new RegExp("rgba?\\((\\s*[0-9.e]+\\s*)(\\s*,\\s*[0-9.e]+\\s*)+\\)",caseSensitive:false);
  
  static List<double> _parseRgb(String v) {
    int start = v.indexOf("(");
    int end = v.indexOf(")", start);
    String params = v.substring(start+1, end);
    List<String> compsStr = params.split(",");
    List<double> comps = new List(compsStr.length);
    for (int i=0; i<compsStr.length; ++i) {
      comps[i] = double.parse(compsStr[i].trim());
    }
    return comps;
  }
  
  /**
   * Returns whether this node has a defined background or not.
   */
  static bool _hasBackground(Node n) {
    if (n.nodeType==TEXT_NODE) {
      return false;
    } else {
      CssStyleDeclaration css = (n as Element).getComputedStyle();
      
      String bgCol = css.backgroundColor;
      
      if (bgCol!=null) {
        bgCol = bgCol.trim().toLowerCase();
      }
      
      bool hasColor = true;
      
      if (bgCol==null || bgCol.isEmpty || bgCol=="transparent") {
        hasColor = false;
      } else {
        if (_rgbaRegEx.hasMatch(bgCol)) {
          List<double> components = _parseRgb(bgCol);
          if (components.length==4 && components.last==0.0) {
            hasColor = false;
          }
        }        
      }
      
      String bgImg = css.backgroundImage;
      
      if (bgImg!=null) {
        bgImg = bgImg.trim().toLowerCase();
      }
      
      bool hasImg = true;
      
      if (bgImg==null || bgImg.isEmpty || bgImg.toLowerCase()=="none") {
        hasImg = false;
      }
      
      //print(">> node ${Logger.ref(n)}: bg-col=${bgCol}, bg-img=${bgImg} hasColor=${hasColor}, hasImg=${hasImg}");
      return hasColor || hasImg;
    }
  }
  
  bool isBookmarked() {
    return this.bookmarkId!=null;
  }
  
  String getBookmarkId() {
    return this.bookmarkId;
  }
  
  void setBookmarkId(String bookmarkId) {
    this.bookmarkId = bookmarkId;
  }

  /**
   * Returns true, if in the subtree, there is a text-node that is selected, and
   * isn't part of a meta-node's subtree. (i.e. has no meta information)
   */
  bool hasSelectedNonMetaText() {
    if (this.isMetaNode) {
      return false;
    }
    if (this.isTextNode) {
      return this._selectionState!=State.NONE;
    }
    if (this.isElementNode) {
      for (int i=0; i<children.length; ++i) {
        if (children[i].hasSelectedNonMetaText()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Displays the hierarchy of this tree/subtree.
   */
  void _display(int tabs) {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<tabs; ++i) {
      sb.write("  ");
    }
    if (_nodeLocation==null) {
      sb.write(null);
    } else {
      sb.write("{");
      for (int i=0; i<_nodeLocation.length; ++i) {
        if (i>0) {
          sb.write(", ");
        }
        sb.write(_nodeLocation[i]);
      }
      sb.write("}");
    }

    for (int i=0; i<children.length; ++i) {
      children[i]._display(tabs+1);
    }
  }

  /**
   * Returns the index where this text-node's text starts in it's parent.
   * If this text-node's parent is a meta-node, then the index of where it's
   * text starts from, would be the index of it's text, from the meta-node's parent.
   * Otherwise, if the parent is not a meta-node, then the index is 0.
   * Throws an exception if this node is not a text-node.
   */
  _TreeNodeLoc getTextNodeOrigParentTextStartIndex() {
    if (!this.isTextNode) {
      throw new Exception("Attempting to retrieve the text of a non-text node.");
    }

    TreeNode prevNode = this.getPrevSibling();
    if (prevNode==null) {
      if (this.parent!=null && this.parent.isMetaNode) {
        prevNode = this.parent.getPrevSibling();
      }
    }
    
    if (prevNode!=null) {
      if (prevNode.isMetaNode) {
        if (prevNode.children.isEmpty) {
          throw new Exception("Meta node with no children found!");
        }
        prevNode = prevNode.children.last;
      }
      if (prevNode.isTextNode) {
        return prevNode.getTextNodeOrigParentTextEndIndex();
      } else {
        return new _TreeNodeLoc(this, 0);
      }
    } else {
      return new _TreeNodeLoc(this, 0);
    }
  }

  /**
   * Returns the index where this text-node's text ends in it's parent.
   * See [getTextNodeOrigParentTextStartIndex] for more details.
   */
  _TreeNodeLoc getTextNodeOrigParentTextEndIndex() {
    _TreeNodeLoc start = this.getTextNodeOrigParentTextStartIndex();
    return new _TreeNodeLoc(start.node, start.index+getText().length);
  }

  /**
   * Returns this node's current node location.
   */
  List<NodeLoc> getNodeLocation() {
    if (this.isMetaNode) {
      return this.parent.getNodeLocation();
    }
    return _nodeLocation;
  }

  /**
   * Called when this node (if it's a meta node), get's clicked.
   */
  void onMetaClick(MouseEvent e) {
    String id = getId();
    incomingInterface.onMetaNodeClicked(e, id);
  }

  /**
   * Called to retrieve all the IDs of meta-nodes within a given subtree.
   */
  void _getSubtreeMetaNodeIds(List<String> out) {
    if (this.isMetaNode) {
      String id = getId();
      out.add(id);
    } else {
      for (int i=0; i<children.length; ++i) {
        children[i]._getSubtreeMetaNodeIds(out);
      }
    }
  }

  /**
   * Retrieves all the IDs of selected meta-nodes within a given subtree.
   * Includes nodes adjascent to selected text-nodes, that would
   * probably be selected together with the text nodes.
   */
  void compileSubtreeSelectedMetaNodeIds(Set<String> out) {
    if (this.isMetaNode && this.hasSelection) {
      out.add(this.getId());
    } else {
      if (this.isElementNode) {
        for (int i=0; i<children.length; ++i) {
          children[i].compileSubtreeSelectedMetaNodeIds(out);
        }
      }
      if (this.isTextNode && this.hasSelection) {
        if (this._textNodePartialSelectStart<0) {
          TreeNode prev = this.getPrevSibling();
          if (prev!=null && prev.isMetaNode) {
            out.add(prev.getId());
          }
        }
        if (this._textNodePartialSelectEnd<0) {
          TreeNode next = this.getNextSibling();
          if (next!=null && next.isMetaNode) {
            out.add(next.getId());
          }
        }
      }
    }
  }

  /**
   * Returns the state of a given subtree. The parameters checkMetaState,
   * and checkSelectionState determine whether the state is a meta-state,
   * selection-state or both.
   * Throws an exception if both parameters are false.
   */
  State _getSubtreeState(bool checkMetaState, bool checkSelectionState) {
    if (!checkMetaState && !checkSelectionState) {
      throw new Exception("Invalid state, neither mata state nor selection being processed.");
    }

    //  when checking meta state: return full if it has a meta-span specified (then it's fully meta)
    //  when checking selection state: return full if it has been fully selected
    if ((checkMetaState && this.isMetaNode) ||
        (checkSelectionState && this._selectionState==State.FULL)) {
      return State.FULL;
    }
    //  when checking selection state: return partial, if it has been partially selected
    if (checkSelectionState && this._selectionState==State.PARTIAL) {
      return State.PARTIAL;
    }

    //  check whether there are any meta/non-meta children
    bool foundMetaNodes = false;
    bool foundNonMetaNodes = false;
    for ( var i = 0; i < this.children.length; ++i) {
      State childRes = this.children[i]._getSubtreeState(checkMetaState,checkSelectionState);
      //  child is no meta/selection
      if (childRes==State.NONE) {
        foundNonMetaNodes = true;
      }
      //  child has partial meta/selection
      if (childRes==State.PARTIAL) {
        foundMetaNodes = true;
        foundNonMetaNodes = true;
      }
      //  child has full meta/selection
      if (childRes==State.FULL) {
        foundMetaNodes = true;
      }
      //  stop if we have both conditions satisfied
      if (foundMetaNodes && foundNonMetaNodes) {
        break;
      }
    }

    //  if we found both meta & non-meta children, then we're partially meta
    if (foundMetaNodes && foundNonMetaNodes) {
      return State.PARTIAL;
    }

    //  if we found no meta but found non-meta children, then we're not meta
    if (!foundMetaNodes && foundNonMetaNodes) {
      return State.NONE;
    }

    //  if we found meta but no non-meta children, then we're fully meta
    if (foundMetaNodes && !foundNonMetaNodes) {
      return State.FULL;
    }

    //  otherwise, we're not meta
    return State.NONE;
  }

  /**
   * Returns the subtree's meta-state.
   */
  State get subtreeMetaState {
    return this._getSubtreeState(true,false);
  }

  /**
   * Returns the subtree's selection-state.
   */
  State get subtreeSelectionState {
    return this._getSubtreeState(false,true);
  }

  /**
   * Returns the subtree's meta-or-selection combination state.
   */
  State get subtreeMetaOrSelectionState {
    return this._getSubtreeState(true,true);
  }

  /**
   * Returns true if selection-state is NOT NONE (i.e. something is selected)
   */
  bool get hasSelection {
    return this._getSubtreeState(false, true)!=State.NONE;
  }

  /**
   * Returns true if meta-state is NOT NONE (i.e. has some meta)
   */
  bool get hasMeta {
    return this._getSubtreeState(true, false)!=State.NONE;
  }

  /**
   * Returns true if (meta-state or selection-state) is NOT NONE (i.e. has some meta or some selection)
   */
  bool get hasMetaOrSelection {
    return this._getSubtreeState(true, true)!=State.NONE;
  }

  /**
   * Clear's the selection state of the subtree.
   */
  void clearSubtreeSelectionState() {
    this._selectionState = State.NONE;
    this._textNodePartialSelectStart = -1;
    this._textNodePartialSelectEnd = -1;

    for ( int i = 0; i < this.children.length; ++i) {
      this.children[i].clearSubtreeSelectionState();
    }
  }

  /**
   * Invoked to process the range, and set the selection-state of the
   * subtree.
   */
  void processSelectionRange(Range range) {
    this._recursiveProcessSelectionRange(new _ProcessSelectionRangeDetails(range));
  }

  /**
   * Recursively goes through the nodes of the tree, and set's the selection-state.
   * Stops when it meets the end of the range.
   */
  void _recursiveProcessSelectionRange(_ProcessSelectionRangeDetails details) {
    bool isStart = false;
    bool isEnd = false;
    //  if the startContainer of the range is this node's DOM node
    if (details.range.startContainer==this.domNode) {
      //  then this is the start node of the range, and we've found the start
      isStart = true;
      details.foundStart = true;
    }
    //  if the endContainer of the range is this node's DOM node
    if (details.range.endContainer==this.domNode) {
      //  then this is the end of the node, don't set that we've
      //  found the end yet, otherwise the children of this
      //  node won't get processed.
      isEnd = true;
    }

    // leaf node
    if (this.children.length==0) {
      if (details.foundStart) {

        if (this.isTextNode && (isStart || isEnd)) {
          bool alignedToStart = true;
          bool alignedToEnd = true;
          if (isStart) {
            String unselected = this.domNode.text.substring(0, details.range.startOffset);
            unselected = unselected.trim();
            alignedToStart = unselected.length<=MAX_SNAP_TO_END_CHAR_COUNT;
          }
          if (isEnd) {
            String unselected = this.domNode.text.substring(details.range.endOffset);
            unselected = unselected.trim();
            alignedToEnd = unselected.length<=MAX_SNAP_TO_END_CHAR_COUNT;
          }

          if (alignedToStart && alignedToEnd) {
            this._selectionState = State.FULL;
          } else {
            this._selectionState = State.PARTIAL;
            if (!alignedToStart) {
              this._textNodePartialSelectStart = details.range.startOffset;
            }
            if (!alignedToEnd) {
              this._textNodePartialSelectEnd = details.range.endOffset;
            }
          }
        } else {
          this._selectionState = State.FULL;
        }
      }
    } else { // branch node
      for ( int i = 0; details.foundEnd==false && i < this.children.length; ++i) {
        this.children[i]._recursiveProcessSelectionRange(details);
      }
    }
    //  allow full processing of the current node, before exiting
    if (isEnd==true) {
      //  this is the end node, so nodes after this won't get processed.
      details.foundEnd = true;
    }
  }
  
  /**
   * Called from the root, to find which element node contains the point
   * specified by the range.
   */
  TreeNode findNodeWithRange(Range rangePoint) {
    if (rangePoint==null || !this.isElementNode) {
      return null;
    }
    
    TreeNode res = null; 
    if (rangePoint.compareNode(this.domNode)==Range.NODE_BEFORE_AND_AFTER) {
      res = _recursiveFindNodeWithRange(rangePoint);
    }
    return res;
  }
  
  TreeNode _recursiveFindNodeWithRange(Range rangePoint) {
    TreeNode selected = null; 
    for (int i=0; i<children.length; ++i) {
      TreeNode child = children[i];
      if (!child.isElementNode) {
        continue;
      }
      
      Node childDomNode = child.domNode;
      if (rangePoint.compareNode(childDomNode)==Range.NODE_BEFORE_AND_AFTER) {
        selected = child._recursiveFindNodeWithRange(rangePoint);
      }
    }
    if (selected==null) {
      selected = this;
    }
    return selected;
  }
  
  /**
   * Checks in the subtree, and find's the node with the given DOM node.
   * Otherwise return null.
   */
  TreeNode findNodeWithDomNode(Node domNode) {
    if (this.domNode==domNode) {
      return this;
    }
    for (int i=0; i<children.length; ++i) {
      TreeNode node = children[i].findNodeWithDomNode(domNode);
      if (node!=null) {
        return node;
      }
    }
    return null;
  }

  /**
   * Checks in the subtree, and find's the node with the given ID.
   * Otherwise return null.
   */
  TreeNode findNodeWithId(String id) {
    if (!isElementNode) {
      return null;
    }
    if (getId()==id) {
      return this;
    }
    for (int i=0; i<children.length; ++i) {
      TreeNode node = children[i].findNodeWithId(id);
      if (node!=null) {
        return node;
      }
    }
    return null;
  }

  /**
   * Checks within the children of this node, for a node with the
   * given DOM node. Starts checking from the given optional parameter.
   * Otherwise return -1.
   */
  int _findChildWithDomNode(Node domNode, [int startIndex = 0]) {
    for (int i=startIndex; i<children.length; ++i) {
      if (children[i].domNode==domNode) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Remove children starting from the index given by start,
   * to the child preceeding the index pastEnd.
   * i.e. pastEnd = last-child-index + 1
   */
  void _removeChildren(int start, int pastEnd) {
    for (int i=start; i<pastEnd; ++i) {
      if (this.children[i].isMetaNode) {
        print("removing meta child: ${this.children[i].getId()}");
      }
    }
    this.children.removeRange(start, pastEnd);
  }

  /**
   * Find any differences between this node's childrens' DOM nodes
   * and it's DOM node's children, and either inserts new child nodes,
   * or deletes child nodes, so that the structure reflects that of
   * the DOM tree.
   */
  void updateStructure() {
    //print("\tupdate structure for dom-node ${Logger.pathTo(this.domNode)}");
    
    List<Node> domChildNodes = copyChildNodes(domNode);
    
    int domIndex = 0;
    int treeIndex = 0;
    for (; domIndex<domChildNodes.length; ++domIndex) {
      
      Node childDomNode = domChildNodes[domIndex];
      int foundTreeIndex = _findChildWithDomNode(childDomNode, treeIndex);
      
      if (foundTreeIndex>=0) {
        //  dom node found among the children
        if (foundTreeIndex>treeIndex) {
          // children were found past the current index,
          //  which means some children currently in the tree,
          //  have been removed from the dom
          //print("children removed ${treeIndex} to ${foundTreeIndex}");
          _removeChildren(treeIndex, foundTreeIndex);
        }
        //  the extra children would fall back to current location
        //  current tree index already found/matched to current dom index, proceed to next
        ++treeIndex;
      } else {
        //  dom node not found among the children
        TreeNode newChild = new TreeNode(this, childDomNode, this.metaInfoHelper);
        
        this.children.insert(treeIndex, newChild);
//        this.children.insertRange(treeIndex, 1, newChild);
        
        //  if new child is meta, add events to handle
        if (newChild.isMetaNode) {
          print("creating new meta child: ${newChild.getId()}");
          
          Element metaElement = newChild.domNode as Element;
          metaElement.onClick.listen((MouseEvent e){
            newChild.onMetaClick(e);
          });
        }

        //  new child was inserted into current tree index, next check should occur at next index
        ++treeIndex;
      }
    }
    if (treeIndex<this.children.length) {
      //  there are still some unmatched children left
      _removeChildren(treeIndex, this.children.length);
    }
    
  }

  /**
   * Verify that the structure represented by this subtree represents
   * the DOM tree structure.
   */
  void verifyStructure() {
    List<Node> domChildNodes = copyChildNodes(domNode);
    if (children.length!=domChildNodes.length) {
      TreeNode root = this.getRoot();
      List<Node> missingDomNodes = new List();
      List<Node> movedDomNodes = new List();
      for (int i=0; i<domChildNodes.length; ++i) {
        Node node = domChildNodes[i];
        TreeNode foundNode = root.findNodeWithDomNode(node);
        if (foundNode==null) {
          missingDomNodes.add(node);
        } else {
          if (foundNode.parent!=this) {
            movedDomNodes.add(node);
          }
        }
      }
      throw new Exception("Structure doesn't match. Node ${Logger.ref(this.domNode)} "
              "has ${children.length} child nodes instead of ${domChildNodes.length}. "
              "Nodes missing: ${Logger.listToStr(missingDomNodes, (Node n){return Logger.ref(n);})} "
              "Nodes moved: ${Logger.listToStr(movedDomNodes, (Node n){return Logger.ref(n);})} "
              );
    }
    for (int i=0; i<children.length; ++i) {
      if (children[i].domNode!=domChildNodes[i]) {
        throw new Exception("Structure doesn't match. Child ${i} of node ${Logger.ref(this.domNode)} "
        "is has child ${Logger.ref(domChildNodes[i])} instead of ${Logger.ref(children[i].domNode)}");
      }
    }
    for (int i=0; i<children.length; ++i) {
      if (this.isMetaNode && children[i].hasMeta) {
        throw new Exception("Invalid meta information. Child ${i} (${Logger.ref(children[i].domNode)})"
        " of meta-node ${Logger.ref(this.domNode)} also has meta information.");
      }
    }
    for (int i=0; i<children.length; ++i) {
      children[i].verifyStructure();
    }
  }

  /**
   * Merges child text-nodes in the range [start, end] and assigns it to the child at assignIndex.
   */
  void _mergeChildTextNodes(int start, int end, int assignIndex) {
    if (assignIndex<0) {
      assignIndex = start;
    }

    StringBuffer sb = new StringBuffer();
    for (int i=start; i<=end; ++i) {
      sb.write(children[i].getText());
    }

    for (int i=end; i>=start; --i) {
      if (i==assignIndex) {
        children[i].domNode.text = sb.toString();
      } else {
        children[i].domNode.remove();
        children.removeAt(i);
      }
    }
  }

  /**
   * Finds and merges text nodes within the element
   */
  void _findAndMergeTextNodes() {
    if (!this.isElementNode) {
      return;
    }

    //print("Finding and merging text nodes in ${Logger.pathTo(this.domNode)}");

    int textNodeGroupLastIndex = -1;
    for (int i=this.children.length-1; i>=0; --i) {
      if (children[i].isTextNode) {
        if (textNodeGroupLastIndex<0) {
          textNodeGroupLastIndex = i;
        }
      } else {
        if (textNodeGroupLastIndex>=0) {
          int textNodeGroupFirstIndex = i+1;
          if (textNodeGroupFirstIndex<textNodeGroupLastIndex) {
            _mergeChildTextNodes(textNodeGroupFirstIndex, textNodeGroupLastIndex, -1);
          }
          textNodeGroupLastIndex = -1;
        }
      }
    }
    if (textNodeGroupLastIndex>=0) {
      _mergeChildTextNodes(0, textNodeGroupLastIndex, -1);
    }
  }

  /**
   * Recursively finds and merges text nodes within the nodes in this subtree
   */
  void subtreeFindAndMergeTextNodes() {
    this._findAndMergeTextNodes();
    for (int i=this.children.length-1; i>=0; --i) {
      if (this.children[i].isElementNode) {
        this.children[i].subtreeFindAndMergeTextNodes();
      }
    }
  }

  /**
   * Removes (if any) meta-nodes within this subtree.
   * Returns the IDs of the removed nodes.
   */
  List<String> removeMetaFromSubtree() {
    List<String> removedIds = new List();
    if (this.isMetaNode) {
      int myIndex = getIndex();
      
      print("Removing highlight from node ${Logger.pathTo(this.domNode)} '${Logger.summarize(this.domNode.text, 50)}'");
      
      this.metaInfoHelper.sidePanelEntryManager.removeSidePanelEntryFor(this.getId());

      //  remove node from it's parent
      parent.children.removeAt(myIndex);

      //bool foundTextChildNodes = false;

      for (int i=0; i<children.length; ++i) {
        TreeNode child = children[i];
        Node childDomNode = child.domNode;
        /*
        if (child.isTextNode) {
          foundTextChildNodes = true;
        }
        */
        //  if child is not a text-node, simply
        //    insert the child into the parent at the correct index
//        parent.children.insertRange(myIndex+i, 1, child);
        parent.children.insert(myIndex+i, child);
        child.parent = parent;

        parent.domNode.insertBefore(child.domNode, this.domNode);
      }

      //  remove node's DOM node form it's parent
      this.domNode.remove();

      //  clear this node's children records
      this.children.clear();
      //  this child is no longer the child of this parent
      this.parent = null;

      removedIds.add(this.getId());
    } else {
      for (int i=0; i<children.length; ++i) {
        if (children[i].hasMeta) {
          removedIds.addAll(children[i].removeMetaFromSubtree());
        }
      }
      if (this.hasMeta) {
        throw new Exception("Unable to fully remove meta-information from node ${Logger.pathTo(this.domNode)}");
      }
    }
    return removedIds;
  }

  /**
   * Finds selected meta nodes and removes them.
   */
  void processUnhighlight() {
    State stateMeta = this.subtreeMetaState;
    State stateSelection = this.subtreeSelectionState;
    State stateEither = this.subtreeMetaOrSelectionState;

    if (stateMeta==State.NONE) {
      return null;
    }

    if (stateSelection==State.NONE) {
      return null;
    }

    if (stateEither==State.FULL) {
      List<String> removedIds = removeMetaFromSubtree();
      metaInfoHelper.annotationCache.marksDeleted(removedIds);
    } else {
      for (int i=0; i<this.children.length; ++i) {
        this.children[i].processUnhighlight();
      }
    }
  }
  
  void compileMetaAndSelectedText(StringBuffer sb) {
    //State stateMeta = this.subtreeMetaState;
    //State stateSelection = this.subtreeSelectionState;
    State stateEither = this.subtreeMetaOrSelectionState;

    if (stateEither==State.NONE) {
      //  this subtree doesn't have any meta info, and no selection
      return;
    }
    
    if (this.isTextNode) {
      String txt = this.getText();
      if (stateEither==State.FULL) {
        sb.write(txt);
      } else {
        Range range = new Range();
        if (_textNodePartialSelectStart>=0) {
          range.setStart(this.domNode, _textNodePartialSelectStart);
        } else {
          range.setStart(this.domNode, 0);
        }
        if (_textNodePartialSelectEnd>=0) {
          range.setEnd(this.domNode, _textNodePartialSelectEnd);
        } else {
          range.setEnd(this.domNode, txt.length-1);
        }
        sb.write(range.toString());
      }
    } else {
      for (int i=0; i<children.length; ++i) {
        children[i].compileMetaAndSelectedText(sb);
      }
    }
  }

  /**
   * Process a selection, to turn it into a highlight (i.e. adds appropriate
   * meta-nodes).
   */
  void processHighlight(CachedAnnotation annotation) {
    print("processHighlight this=${Logger.pathTo(this.domNode)}");

    State stateMeta = this.subtreeMetaState;
    State stateSelection = this.subtreeSelectionState;
    State stateEither = this.subtreeMetaOrSelectionState;

    if (stateEither==State.NONE) {
      //  this subtree doesn't have any meta info, and no selection
      return;
    }

    if (stateMeta==State.FULL) {
      //  this subree has full meta
      return;
    }

    bool hasFullChildrenReqSel = false;
    for (int i=0; hasFullChildrenReqSel==false && i<this.children.length; ++i) {
      TreeNode child = this.children[i];
      if (child.hasSelection &&
          child.subtreeMetaOrSelectionState==State.FULL) {
        //  child's meta state wasn't full, but
        //  child's meta/selection is now full
        hasFullChildrenReqSel = true;
        break;
      }
    }

    //  we found some full nodes that weren't full nodes before
    if (hasFullChildrenReqSel) {
      //  check and add mark appropriate meta info for fully marked children
      if (this._processFullChildrenMeta(annotation)) {
        updateStructure();
        computeSubtreePath();
      }
    }

    //  find text nodes, that have some selected text in them
    List<TreeNode> textNodes = new List<TreeNode>();
    for (int i=0; i<this.children.length; ++i) {
      TreeNode child = this.children[i];
      if (child.isTextNode &&
          child.hasSelection
          ) {
        textNodes.add(child);
      }
    }

    if (!textNodes.isEmpty) {
      //  process text selection in text nodes
      for (int i=textNodes.length-1; i>=0; --i) {
        TreeNode textNode = textNodes[i];
        textNode._processTextNodeMeta(annotation);
      }
      //  update parent structure to reflect changes
      updateStructure();
      computeSubtreePath();
    }

    for (int i=0; i<this.children.length; ++i) {
      if (this.children[i].hasSelection && this.children[i].subtreeMetaState!=State.FULL) {
        this.children[i].processHighlight(annotation);
      }
    }

  }

  /**
   * Process fully selected child nodes (and any text-nodes, immediately before,
   * or after, a fully selected child node), and surround them with a meta-node.
   * Returns true if changes were made to the node.
   */
  bool _processFullChildrenMeta(CachedAnnotation annotation) {
    print("adding meta to children of ${Logger.pathTo(this.domNode)}");

    //  group nodes based on their meta state
    List<_NodeGroup<TreeNode>> groups = new List<_NodeGroup<TreeNode>>();
    for (int i=0; i<this.children.length; ++i) {
      TreeNode child = this.children[i];

      State childMetaOrSelectionState = child.subtreeMetaOrSelectionState;
      State childMetaState = child.subtreeMetaState;
      
      bool groupable = child.isMetaNode ||
          (!child.hasBackground && child.isInline());
      
      print(" child ${i} is groupable=${groupable} meta=${child.isMetaNode} id=${!child.isElementNode?null:child.getId()}");

      //  new node with full meta-selection state but no previously full meta state
      bool newNode = childMetaOrSelectionState==State.FULL && childMetaState!=State.FULL && groupable;
      //  either a new node with full meta-selection state, or a node with full meta
      bool hasMeta = (childMetaState==State.FULL || newNode)  && groupable;
      
      _NodeGroupProps thisProps = new _NodeGroupProps(hasMeta);
      
      _NodeGroup<TreeNode> nodeGroup;
      if (groups.length==0 || !groups[groups.length-1].props.isEqual(thisProps)) {
        nodeGroup = new _NodeGroup<TreeNode>(thisProps);
        groups.add(nodeGroup);
      } else {
        nodeGroup = groups[groups.length-1];
      }

      nodeGroup.nodes.add(child);
      if (newNode) {
        nodeGroup.hasNewNode = true;
      }
    }

    if (AUTO_JOIN_GROUPS_SEP_BY_WHITESPACE) {
      //  find groups that have whitespace only, and have
      //  a selection/meta on either side, and merge groups
      for (int i=0; i<groups.length; ) {
        bool hasOnlyWhitespace = true;

        List<TreeNode> nodes = groups[i].nodes;
        for (int j=0; hasOnlyWhitespace==true && j<nodes.length; ++j) {
          TreeNode node = nodes[j];

          if (node.isTextNode) {
            String content = node.domNode.text;
            content = content.trim();
            if (content.length>0) {
              hasOnlyWhitespace = false;
            }
          } else {
            hasOnlyWhitespace = false;
          }
        }

        if (hasOnlyWhitespace==true) {
          var prevHasMeta = false;
          var nextHasMeta = false;

          if (i>0) {
            prevHasMeta = groups[i-1].props.hasMeta;
          }
          if (i+1<groups.length) {
            nextHasMeta = groups[i+1].props.hasMeta;
          }

          if (prevHasMeta || nextHasMeta) {
            //  create resulting group
            _NodeGroup<TreeNode> resultingGroup = groups[i];
            if (prevHasMeta) {
              resultingGroup = _NodeGroup.mergeGroups(groups[i-1], resultingGroup, new _NodeGroupProps(true));
            }
            if (nextHasMeta) {
              resultingGroup = _NodeGroup.mergeGroups(resultingGroup, groups[i+1], new _NodeGroupProps(true));
            }
            if (prevHasMeta && nextHasMeta) {
              // replace last group with resulting group
              groups[i-1] = resultingGroup;
              //  delete current & next group
              groups.removeAt(i+1);
              groups.removeAt(i);
              --i; // go back one to recheck the last one
            } else
              if (prevHasMeta) {
                // replace last group with resulting group
                groups[i-1] = resultingGroup;
                //  delete current group
                groups.removeAt(i);
                // stay at current index to recheck
              } else
                if (nextHasMeta) {
                  // replace current group with resulting group
                  groups[i] = resultingGroup;
                  //  delete next group
                  groups.removeAt(i+1);
                  // stay at current index to recheck
                }
          } else {
            //  neither has meta data
            ++i; // move on to next group
          }
        } else {
          ++i; // move on to next group
        }
      }
    }

    //  for groups bordering text-nodes that have been selected
    for (int i=0; i<groups.length; ++i) {
      _NodeGroup<TreeNode> group = groups[i];
      List<TreeNode> nodes = group.nodes;
      TreeNode first = nodes.first;
      TreeNode last = nodes.last;

      TreeNode prevNode = first.getPrevSibling();
      if (prevNode!=null && prevNode.isTextNode && prevNode._selectionState==State.PARTIAL
          && prevNode._textNodePartialSelectEnd<0) {
//        nodes.insertRange(0, 1, prevNode);
        nodes.insert(0, prevNode);
        group.hasNewNode = true;
      }

      TreeNode nextNode = last.getNextSibling();
      if (nextNode!=null && nextNode.isTextNode && nextNode._selectionState==State.PARTIAL
          && nextNode._textNodePartialSelectStart<0) {
//        nodes.insertRange(nodes.length, 1, nextNode);
        nodes.insert(nodes.length, nextNode);
        group.hasNewNode = true;
      }

    }

    //  remove all, except meta groups with new nodes
    groups.retainWhere((_NodeGroup v){
      return v.props.hasMeta==true && v.hasNewNode==true;
    });

    if (groups.length==0) { // no groups to process
      return false;
    }

    for (int g=groups.length-1; g>=0; --g) {
      _NodeGroup<TreeNode> group = groups[g];
      List<TreeNode> nodes = group.nodes;

      List<String> mergedIds = new List<String>();
      for (int n=0; n<nodes.length; ++n) {
        nodes[n]._getSubtreeMetaNodeIds(mergedIds);
      }

      bool hasBlockElement = false;
      _TreeNodeLoc rangeStart = null;
      _TreeNodeLoc rangeEnd = null;

      for (int n=0; n<nodes.length; ++n) {
        TreeNode node = nodes[n];
        if (node.isMetaNode) {
          if (!node.children.isEmpty) {
            if (rangeStart==null) {
              rangeStart = new _TreeNodeLoc(node.children.first, null);
            }
            if (rangeStart!=null) {
              rangeEnd = new _TreeNodeLoc(node.children.last, null);
            }
          }
          for (int i=0; i<node.children.length; ++i) {
            TreeNode child = node.children[i];
            if (child.isElementNode) {
              if (child.isOrHasBlockElement()) {
                hasBlockElement = true;
              }
            }
          }
        } else {
          if (rangeStart==null) {
            if (node._textNodePartialSelectStart>=0) {
              rangeStart = new _TreeNodeLoc(node, node._textNodePartialSelectStart);
            } else {
              rangeStart = new _TreeNodeLoc(node, null);
            }
          }
          if (rangeStart!=null) {
            if (node._textNodePartialSelectEnd>=0) {
              rangeEnd = new _TreeNodeLoc(node, node._textNodePartialSelectEnd);
            } else {
              rangeEnd = new _TreeNodeLoc(node, null);
            }
          }

          if (node.isElementNode) {
            if (node.isOrHasBlockElement()) {
              hasBlockElement = true;
            }
          }
        }
      }

      if (rangeStart==null || rangeEnd==null) {
        throw new Exception("Selection of range in the node ${Logger.pathTo(this.domNode)} could not be evaluated!"
            " Selection was composed of ${Logger.listToStr(group.nodes,
                (TreeNode v){return Logger.pathTo(v.domNode);})}");
      }

      for (int n=nodes.length-1; n>=0; --n) {
        TreeNode node = nodes[n];
        List<String> removedIds = node.removeMetaFromSubtree();
        node.clearSubtreeSelectionState();
        if (node.parent!=null && node.hasMetaOrSelection) {
          throw new Exception("Unable to clear node!");
        }
      }

      print("start=${Logger.pathTo(rangeStart.node.domNode)} index=${rangeStart.index}");
      print("end=${Logger.pathTo(rangeEnd.node.domNode)} index=${rangeEnd.index}");
      
      Range range = _toRange(rangeStart, rangeEnd);
      
      if (range.toString().trim().isEmpty) {
        continue;
      }
      
      RangePoints rangeLoc = _describeRange(rangeStart, rangeEnd);

      //  make a new meta-span
      CachedMark mark = annotation.newMark(rangeLoc, hasBlockElement);
      
      if (!mergedIds.isEmpty) {
        metaInfoHelper.annotationCache.marksReplaced(mergedIds, mark.id);
        for (String markId in mergedIds) {
          metaInfoHelper.sidePanelEntryManager.removeSidePanelEntryFor(markId);
        }
      }

      addMetaToRange(range, annotation, mark, hasBlockElement, true);
    }

    return true;
  }
  
  _TreeNodeLocRange _computeTextSelectRange(List<String> mergedIds) {
    _TreeNodeLoc rangeStart = null;
    _TreeNodeLoc rangeEnd = null;

    if (this._textNodePartialSelectStart>=0) {
      //  if it's starts after the beginning...
      rangeStart = new _TreeNodeLoc(this, this._textNodePartialSelectStart);
    } else {
      TreeNode prevNode = this.getPrevSibling();
      if (prevNode!=null && prevNode.subtreeMetaOrSelectionState==State.FULL) {
        
        if (mergedIds!=null) {
          // if the text node is bordering a full meta node
          //  merge notes
          prevNode._getSubtreeMetaNodeIds(mergedIds);
        }

        //  if it's a meta-node, and would probably be removed
        //    while removing meta from the selection,
        //    then set the start to the node's first child
        if (prevNode.isMetaNode) {
          TreeNode nextStart = prevNode.children.first;
          rangeStart = new _TreeNodeLoc(nextStart, null);
        } else {
          //  otherwise set the start to the node itself
          rangeStart = new _TreeNodeLoc(prevNode, null);
        }

        //  remove meta from the pevious node's subtree,
        //  while merging the text but not to the node before the previous
        //  node.
        List<String> removedIds = prevNode.removeMetaFromSubtree();
      } else {
        rangeStart = new _TreeNodeLoc(this, null);
      }
    }

    if (this._textNodePartialSelectEnd>=0) {
      rangeEnd = new _TreeNodeLoc(this, this._textNodePartialSelectEnd);
    } else {
      TreeNode nextNode = this.getNextSibling();
      if (nextNode!=null && nextNode.subtreeMetaOrSelectionState==State.FULL) {
        if (mergedIds!=null) {
          // if the text node is bordering a full meta node
          //  merge notes
          nextNode._getSubtreeMetaNodeIds(mergedIds);
        }

        //  if it's a meta-node, and would probably be removed
        //    while removing meta from the selection,
        //    then set the end to the node's last child
        if (nextNode.isMetaNode) {
          TreeNode nextLast = nextNode.children.last;
          rangeEnd = new _TreeNodeLoc(nextLast, null);
        } else {
          //  otherwise set the end to the node itself
          rangeEnd = new _TreeNodeLoc(nextNode, null);
        }

        //  remove meta from the next node's subtree,
        //  while merging the text but not to the node after the next
        //  node.
        List<String> removedIds = nextNode.removeMetaFromSubtree();
      } else {
        rangeEnd = new _TreeNodeLoc(this, null);
      }
    }
    
    return new _TreeNodeLocRange(rangeStart, rangeEnd);
  }

  /**
   * Check within a text-node, and surround the selection with a meta-node.
   */
  void _processTextNodeMeta(CachedAnnotation annotation) {
    print("adding meta to text-node ${Logger.pathTo(this.domNode)}");

    if (!this.isTextNode) {
      throw new Exception("Method called on a non-text-node!");
    }

    //  no selection here!
    if (this._textNodePartialSelectStart<0 && this._textNodePartialSelectEnd<0) {
      return;
    }

    List<String> mergedIds = new List<String>();
    
    _TreeNodeLoc rangeStart;
    _TreeNodeLoc rangeEnd;
    
    {
      _TreeNodeLocRange tmpRange = _computeTextSelectRange(mergedIds);
      
      rangeStart = tmpRange.start;
      rangeEnd = tmpRange.end;
    }
    
    print("start=${Logger.pathTo(rangeStart.node.domNode)} index=${rangeStart.index}");
    print("end=${Logger.pathTo(rangeEnd.node.domNode)} index=${rangeEnd.index}");
    
    Range range = _toRange(rangeStart, rangeEnd);
    
    if (range.toString().trim().isEmpty) {
      return;
    }
    
    RangePoints rangeLoc = _describeRange(rangeStart, rangeEnd);

    //  make a new meta-span
    CachedMark mark = annotation.newMark(rangeLoc,false);
    
    if (!mergedIds.isEmpty) {
      metaInfoHelper.annotationCache.marksReplaced(mergedIds, mark.id);
      for (String markId in mergedIds) {
        metaInfoHelper.sidePanelEntryManager.removeSidePanelEntryFor(markId);
      }
    }

    addMetaToRange(range, annotation, mark, false, true);
    
    this.clearSubtreeSelectionState();
  }
  
  Range _toRange(_TreeNodeLoc rangeStart, _TreeNodeLoc rangeEnd) {
    Range range = new Range();
    if (rangeStart.index!=null) {
      range.setStart(rangeStart.node.domNode, rangeStart.index);
    } else {
      range.setStartBefore(rangeStart.node.domNode);
    }
    if (rangeEnd.index!=null) {
      range.setEnd(rangeEnd.node.domNode, rangeEnd.index);
    } else {
      range.setEndAfter(rangeEnd.node.domNode);
    }
    return range;
  }
  
  void addMetaToRange(Range range, CachedAnnotation annotation, CachedMark mark, bool block, bool highlight) {
    print("Highlighting text '${Logger.summarize(range.toString(), 50)}' in ${Logger.pathTo(this.domNode)}");
    
    Element metaNodeSpan = this.metaInfoHelper.createSpanNode(mark.id,block,highlight,annotation.hasNote());

    //  process range
    range.surroundContents(metaNodeSpan);
    range.detach();
    
    if (annotation.hasNote()) {
      this.metaInfoHelper.sidePanelEntryManager.createSidePanelEntryFor(metaNodeSpan);
    }
  }
  
  RangePoints _describeRange(_TreeNodeLoc start, _TreeNodeLoc end) {

    RangePoint toRangePoint(_TreeNodeLoc loc, bool first) {
      if (loc.node.isTextNode) {
        _TreeNodeLoc relIndex;
        int index;
        if (loc.index==null) {
          if (first) {
            relIndex = loc.node.getTextNodeOrigParentTextStartIndex();
          } else {
            relIndex = loc.node.getTextNodeOrigParentTextEndIndex();
          }
          index = relIndex.index;
        } else {
          relIndex = loc.node.getTextNodeOrigParentTextStartIndex();
          index = loc.index + relIndex.index;
        }
        return new RangePoint(relIndex.node.getNodeLocation(), index);
      } else {
        return new RangePoint(loc.node.getNodeLocation(), loc.index);
      }      
    }

    RangePoint startPoint = toRangePoint(start, true);
    print("the location of start node ${Logger.pathTo(start.node.domNode)}:${start.index} is ${startPoint}");

    RangePoint endPoint = toRangePoint(end, false);
    print("the location of end node ${Logger.pathTo(start.node.domNode)}:${end.index} is ${endPoint}");

    return new RangePoints(startPoint, endPoint);
  }
  
  void _subtreeUpdateStructure(int depth) {
    String s = "";
    for (int i=0; i<depth; ++i)
      s += "\t";
    print("${s}${Logger.pathTo(this.domNode)}"
        " (isTextNode=${this.isTextNode},"
        "isElementNode=${this.isElementNode},"
        "isMetaNode=${this.isMetaNode})");
    for (TreeNode t in children) {
      t._subtreeUpdateStructure(depth+1);
    }
  }
  
  void displayStructure() {
    this._subtreeUpdateStructure(0);
  }
  
  void scrollToTop() {
    if (this.isElementNode) {
      Element el = this.domNode as Element;
      
      double top;
      js.scoped((){
        top = js.context.$jquery(el).position().top.toDouble();
      });
      
      outgoingInterface.scrollToLocation(top);
//      window.scrollY = rc.top.toInt();
//      window.scrollTo(rc.left.toInt(), rc.top.toInt());
    }
  }
  
}

