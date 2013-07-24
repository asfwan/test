part of ebook;


/**
 * The sides the side panel can be on.  
 */
class SidePanelSide {
  static const SidePanelSide LEFT = const SidePanelSide._(0, "Left");
  static const SidePanelSide RIGHT = const SidePanelSide._(1, "Right");
  
  final int val;
  final String name;
  const SidePanelSide._(this.val, this.name);
  
  static SidePanelSide fromVal(int val) {
    if (val==LEFT.val) {
      return LEFT;
    }
    if (val==RIGHT.val) {
      return RIGHT;
    }
    throw new RuntimeError("Unknown SidePanelSide of value $val");
  }
  
  static SidePanelSide oppositeOf(SidePanelSide side) {
    if (side==LEFT) {
      return RIGHT;
    }
    if (side==RIGHT) {
      return LEFT;
    }
    throw new RuntimeError("Unknown SidePanelSide of value $side");
  }

  String toString() {
    return name;
  }  
}

///**
// * Manages entries on the side panel.
// */
class SidePanelEntryManager {
  
  static final double ROOT_MARGIN = 10.0;
  static final double ENTRY_GAP = 2.0;
  
  SidePanelSide side;
  AnnotationCache annotationCache;
  Element rootPanel;
  Element sidePanel;
  double sidePanelWidth;
  double sidePanelHeight;
  List<_SidePanelEntry> annotationEntries;
  List<_SidePanelEntry> bookmarkEntries;
//  List<Element> annotationEntries;
//  List<Element> bookmarkEntries;
  
  SidePanelEntryManager(
      AnnotationCache this.annotationCache,
      Element this.rootPanel, 
      SidePanelSide this.side) :
        annotationEntries=new List(),
        bookmarkEntries = new List()
  {
    sidePanel = new DivElement();
    sidePanel.id = "ebook_side_panel";
    sidePanel.classes.add("ebook-side-panel");
    
    int index = rootPanel.parent.children.indexOf(rootPanel);
    rootPanel.parent.children.insert(index, sidePanel);
    
    updateSidePanelDim();
    updateRootPanelMargins();
  }
      
  void updateSidePanelDim() {
    sidePanelWidth = null;
    sidePanelHeight = null;
    js.scoped((){
      var innerWid = js.context.$jquery(sidePanel).innerWidth();
      if (innerWid!=null) {
        sidePanelWidth = innerWid.toDouble();
      }
      print("innerWid=${innerWid} innerWid.runtimeType=${innerWid.runtimeType}");

      var innerHei = js.context.$jquery(sidePanel).innerHeight();
      if (innerHei!=null) {
        sidePanelHeight = innerHei.toDouble();
      }
      print("innerHei=${innerHei} innerHei.runtimeType=${innerHei.runtimeType}");
    });    
  }
  
  void updateRootPanelMargins() {
    if (sidePanelWidth!=null && sidePanelHeight!=null) {
      switch (side) {
        case SidePanelSide.LEFT:
          rootPanel.style.marginLeft = "${sidePanelWidth+ROOT_MARGIN}px";
          rootPanel.style.marginRight = "${ROOT_MARGIN}px";
          sidePanel.style.left = "0px";
          break;
        case SidePanelSide.RIGHT:
          rootPanel.style.marginLeft = "${ROOT_MARGIN}px";
          rootPanel.style.marginRight = "${sidePanelWidth+ROOT_MARGIN}px";
          sidePanel.style.right = "0px";
          break;
        default:
          throw new Exception("Unsupported side: ${side.name}");
      }      
    }
  }
  
  void updatePositions() {
    if (sidePanelWidth==null || sidePanelHeight==null) {
      updateSidePanelDim();
      if (sidePanelWidth==null || sidePanelHeight==null) {
        return;
      } else {
        updateRootPanelMargins();
      }
    }
    
    try {
      _updateEntryPositions(annotationEntries, SidePanelSide.oppositeOf(side), true, true);
      _updateEntryPositions(bookmarkEntries, side, false, false);
    } catch (e, s) {
      print("Error: ${e}");
      print("Stacktrace: ${s}");
    }
  }
  
  void _updateEntryPositions(List<_SidePanelEntry> entries, SidePanelSide entrySide, bool checkAnnMet, bool extendFullContentHeight) {
    Set<String> allAnnotationsSoFar;
    
    if (checkAnnMet) {
      allAnnotationsSoFar = new Set();
    }
    
    //print("updating side-panel positions");
    List<_LocRect> entryRects = new List(entries.length);
    for (int i=0; i<entries.length; ++i) {
      _SidePanelEntry entry = entries[i];
      
      _LocRange contentVerLoc = entry.getContentVerLoc();
      
      Set<String> entryAnnotationsMet;
      if (checkAnnMet) {
        entryAnnotationsMet = new Set();
        try {
          String markId = entry.getRefContent().id;
          CachedAnnotation annotation = annotationCache.annotationWithMark(markId);
          String annId = annotation.getId();
          
          print("\t\tside-entry $i is mark $markId and ann ${annId}");
          
          //  this mark has already been processed as part
          //    of another annotation before.
          if (allAnnotationsSoFar.contains(annId)) {
            print("\t\tside-entry $i belongs to an already processed annotation.");
            continue;
          }
          
          allAnnotationsSoFar.add(annId);
          entryAnnotationsMet.add(annId);
          
          //  find the start and end of the combined marks of
          //    the annotation
          Iterable<String> marks = annotation.getMarks();
          for (String mark in marks) {
            if (mark==markId) {
              continue;
            }
            
            bool found = false;
            for (int j=i+1; j<entries.length; ++j) {
              if (entries[j].getRefContent().id==mark) {
                print("\t\tside-entry $j also belongs to the same annotation as side-entry $i.");
                found = true;
                _LocRange tmpVerLoc = entries[j].getContentVerLoc();
                contentVerLoc = new _LocRange(
                    math.min(contentVerLoc.start, tmpVerLoc.start),
                    math.max(contentVerLoc.end, tmpVerLoc.end)
                    );
                break;
              }
            }
            
            if (!found) {
              print("mark $mark of annotation $annId has no entry on the side-panel.");
            }
          }
          
        } catch (e, s) {
          //print("annotation of mark ${entryAnnId} not found in cache");
          print(e);
          print(s);
        }
      }
      
      if (!extendFullContentHeight) {
        _LocRect rc = entry.getEntryLocRect();
        contentVerLoc = new _LocRange(
            contentVerLoc.start,
            contentVerLoc.start+rc.ver.dist
        );
      }
      
      double horLoc = ENTRY_GAP;
      
      //print("\ttop of ${contentElement.id} is ${top}, height is ${height}");
      
      for (int j=0; j<i; ++j) {
        _LocRect entryRect = entryRects[j];
        if (entryRect==null) {
          continue;
        }
        
        if (contentVerLoc.start<=entryRect.ver.end && contentVerLoc.end>=entryRect.ver.start) {
          print("\t\tside-entry $i overlaps $j");
          
          if (checkAnnMet) {
            String markId = entries[j].getRefContent().id;
            try {
              CachedAnnotation annotation = annotationCache.annotationWithMark(markId);
              String annId = annotation.getId();
              if (entryAnnotationsMet.contains(annId)) {
                print("\t\tside-entry $i, has already met annotation $annId");
                continue;
              }
              entryAnnotationsMet.add(annId);
            } catch (e, s) {
              print("annotation of mark ${markId} not found in cache");
              print(e);
              print(s);
            }
          }
          
          if (entrySide==SidePanelSide.RIGHT) {
            double fromRightEnd = sidePanelWidth - entryRect.hor.start;
            horLoc = math.max(horLoc, fromRightEnd+ENTRY_GAP);
          } else {
            horLoc = math.max(horLoc, entryRect.hor.end+ENTRY_GAP);
          }
          
          print("\t\t\thor-loc of side-entry $i is now $horLoc");
        }
      }
      
      entry.setEntryLoc(horLoc, contentVerLoc, entrySide, extendFullContentHeight);
      entryRects[i] = entry.getEntryLocRect();      
    }
  }
  
  
//  String getContentIdForEntryId(String entryId) {
//    int v = entryId.indexOf("-");
//    return entryId.substring(v+1, entryId.length);
//  }
  
  /**
   * Returns a name for the side-panel entry for the given id.
   */
  String getSidePanelEntryNameFor(String contentId) {
    return "side_panel_entry_for-${contentId}";
  }
  
  /**
   * Creates an item in the side panel, for the given mark.
   */
  Element createSidePanelEntryFor(Element mark) {
    //  check if mark wraps whitespace only
    if (mark.text.trim().isEmpty) {
      return null;
    }
    
    //print(" mark text: ${mark.text}");
    
    print("Adding side-panel annotation entry for mark ${mark.id}");
    if (sidePanel!=null) {
      DivElement sideEntry = new DivElement();
      sideEntry.id = getSidePanelEntryNameFor(mark.id);
      sideEntry.classes.add("ebook-side-panel-annotation-entry");
      
      _SidePanelEntry entry = new _SidePanelEntry.withRefNode(sideEntry, mark);
      
      sidePanel.append(sideEntry);
      annotationEntries.add(entry);
      updatePositions();
      
      sideEntry.onClick.listen((MouseEvent e){
        TreeNode node = entry.getTreeNode();
        if (node!=null) {
          node.onMetaClick(e);
        }
      });
      
      return sideEntry;
    } else {
      return null;
    }
  }
  
  /**
   * Removes the entry in the side panel, corresponding to the content element given.
   */
  void removeSidePanelEntryFor(String markId) {
    if (sidePanel!=null) {
      for (_SidePanelEntry spe in annotationEntries) {
        if (spe.getRefContent().id==markId) {
          spe.getEntry().remove();
          annotationEntries.remove(spe);
          break;
        }
      }
    }
  }
  
  /**
   * Adds a bookmark entry to the panel.
   */
  bool addBookmarkEntry(TreeNode node, String bookmarkId) {
    print("Adding side-panel bookmark entry for element ${Logger.pathTo(node.domNode)}");
    if (sidePanel!=null) {
      for (_SidePanelEntry spe in bookmarkEntries) {
        if (spe.getTreeNode()==node) {
          return false;
        }
      }
      
      RangePoint point = new RangePoint(node.getNodeLocation(), null);
      
      DivElement sideEntry = new DivElement();
      sideEntry.id = bookmarkId;
      sideEntry.classes.add("ebook-side-panel-bookmark-entry");
      sideEntry.append(new Text("B"));
      
      sidePanel.append(sideEntry);
      bookmarkEntries.add(new _SidePanelEntry.withTreeNode(sideEntry, node));
      updatePositions();
      
      sideEntry.onClick.listen((MouseEvent e){
        incomingInterface.onBookmarkSideEntryClick(e, bookmarkId);
      });
      
      return true;
    }
    return false;
  }
  
  /**
   * Removes a bookmark entry from the panel.
   */
  void removeBookmarkEntry(TreeNode node) {
    if (sidePanel!=null) {
      for (_SidePanelEntry spe in bookmarkEntries) {
        if (spe.getRefContent()==(node.domNode as Element)) {
          spe.getEntry().remove();
          bookmarkEntries.remove(spe);
          updatePositions();
          break;
        }
      }
    }    
  }
  
}

class _LocRange {
  final double start, end;
  
  _LocRange(double this.start, double this.end);
  
  double get dist {
    return end-start;
  }
}

class _LocRect {
  final _LocRange hor;
  final _LocRange ver;
  
  _LocRect(_LocRange this.hor, _LocRange this.ver);
}

class _SidePanelEntry {
  Element _entry;
  TreeNode _treeNode;
  Element _refContent;
  
  _SidePanelEntry.withTreeNode(Element this._entry, TreeNode treeNode) :
    _treeNode = treeNode,
    _refContent = treeNode.domNode as Element;
  
  _SidePanelEntry.withRefNode(Element this._entry, Element this._refContent);
  
  TreeNode getTreeNode() {
    if (_treeNode==null) {
      _treeNode = rootNodeTree.findNodeWithDomNode(_refContent);
    }
    return _treeNode;
  }
  
  Element getEntry() {
    return this._entry;
  }
  
  Element getRefContent() {
    return this._refContent;
  }
  
  _LocRange getContentVerLoc() {
    double top;
    double height;
    js.scoped((){
      top = js.context.$jquery(_refContent).position().top.toDouble();
      height = js.context.$jquery(_refContent).outerHeight().toDouble();
    });
    double bottom = top + height;
    
    //print("content of ${Logger.pathTo(_refContent)} ver from $top to $bottom ");
    
    return new _LocRange(top, bottom);
  }
  
  _LocRect getEntryLocRect() {
    double elLeft;
    double elTop;
    double elHeight;
    double elWidth;
    js.scoped((){
      dynamic pos = js.context.$jquery(_entry).position();
      elLeft = pos.left.toDouble();
      elTop = pos.top.toDouble();
      elHeight = js.context.$jquery(_entry).outerHeight().toDouble();
      elWidth = js.context.$jquery(_entry).outerWidth().toDouble();
    });
    return new _LocRect(
        new _LocRange(elLeft, elLeft+elWidth),
        new _LocRange(elTop, elTop+elHeight)
        );    
  }
  
  void setEntryLoc(double horLoc, _LocRange verLoc, SidePanelSide entrySide, bool extendFullContentHeight) {
    switch (entrySide) {
      case SidePanelSide.LEFT:
      {
        _entry.style.left = "${horLoc}px";
        _entry.style.right = null;
        //print("\tside-entry $i placed ${horLoc}px from left");
      }
      break;
      case SidePanelSide.RIGHT:
      {
        _entry.style.left = null;
        _entry.style.right = "${horLoc}px";
        //print("\tside-entry $i placed ${horLoc}px from right");
      }
      break;
      default:
        throw new Exception("Unsupported side: ${entrySide.name}");
    }
    
    _entry.style.top = "${verLoc.start}px";
    if (extendFullContentHeight) {
      _entry.style.height = "${verLoc.dist}px";
    }
  }
}
