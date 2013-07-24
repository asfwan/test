part of ebook;

class IncomingInterface {
  final bool androidVersion;
  AnnotationCache _annotationCache;
  TreeNode _rootNode;
  OutgoingInterface _outgoingInterface;
  AbstractSelectionManager _selectionManager;
  Set<BtnMsg> _lastBtns;

  IncomingInterface(AnnotationCache this._annotationCache,
      TreeNode this._rootNode, OutgoingInterface this._outgoingInterface,
      bool this.androidVersion)
  {
    js.scoped((){
      js.context.selectionClickResult = new js.Callback.many((String result, String extraInfo){
        print("received selectionClickResult result=${result} extraInfo=${extraInfo}");
        
        BtnMsg msg = BtnMsg.findBtnWithId(result);
        if (msg==null) {
          _outgoingInterface.onErrorMsg("Unidentified button ${result}", "selectionClickResult", false);
          return;
        }
        
        runAsync((){
          try {
            print("now processing selectionClickResult result=${result}");
            switch (msg) {
              case BtnMsg.ADD_HIGHLIGHT:
                addAnnotation(null);
                break;
              case BtnMsg.CLEAR_HIGHLIGHT:
                delAnnotations(extraInfo);
                break;
              case BtnMsg.ADD_NOTE:
                openNewNoteDialog(extraInfo);
                break;
              case BtnMsg.CLEAR_NOTE:
                //delAnnotations(extraInfo);
                clearNote(extraInfo);
                break;
              case BtnMsg.EXTEND_HIGHLIGHT:
              case BtnMsg.MERGE_HIGHLIGHT:
              case BtnMsg.EXTEND_NOTE:
              case BtnMsg.MERGE_NOTE:
              case BtnMsg.EXTEND_NOTE_ONTO_HIGHLIGHT:
              case BtnMsg.MERGE_EXTEND_NOTE_ONTO_HIGHLIGHT:
                extendAnnotations();
                break;
              case BtnMsg.EDIT_NOTE:
                openEditNoteDialog(extraInfo);
                break;
              case BtnMsg.ADD_BOOKMARK:
                addBookmark(extraInfo);
                break;
              case BtnMsg.DELETE_BOOKMARK:
                deleteBookmark(extraInfo);
                break;
              case BtnMsg.GOTO_BOOKMARK:
                gotoBookmark(extraInfo);
                break;
              case BtnMsg.GOTO_MARK:
                gotoMark(extraInfo);
                break;
              case BtnMsg.COPY:
                copy(extraInfo);
                break;
              case BtnMsg.CREATE_NOTE:
                Map<String,String> vals = json.parse(extraInfo);
                String text = vals["text"];
                print("creating note with text: '${text}'");
                addAnnotation(text);
                break;
              case BtnMsg.SAVE_NOTE:
                saveNote(extraInfo);
                break;
              case BtnMsg.CANCEL:
                break;
              default:
                throw new Exception("Unsupported button function ${msg.id}");
            }
            print("done processing selectionClickResult result=${result}");
          } catch (e, s) {
            _outgoingInterface.onError("Error while processing button ${msg}", e, s, false);
            return;
          }
        });
        print("returning from selectionClickResult result=${result}");
      });
    });
  }

  void openNewNoteDialog(String annId) {
    if (annId==null || annId.isEmpty) {
      runAsync((){
        print("Calling for new note dialog");
        _outgoingInterface.onNoteEdit("Add Note",
            "New Note Text",
            OutgoingInterface.encodeBtns([BtnMsg.CREATE_NOTE,BtnMsg.CANCEL]),
            "");
      });
    } else {
      runAsync((){
        print("Calling for new note dialog");
        _outgoingInterface.onNoteEdit("Add Note",
            "New Note Text",
            OutgoingInterface.encodeBtns([new BtnMsg.from(BtnMsg.SAVE_NOTE, annId),BtnMsg.CANCEL]),
            "");
      });
    }
  }

  void openEditNoteDialog(String annId) {
    CachedAnnotation ann = _annotationCache.getAnnotation(annId);
    if (ann==null) {
      throw new Exception("Annotation ${annId} not found!");
    }
    String text = null;
    if (ann.hasNote()) {
      text = ann.getNote();
    }
    runAsync((){
      _outgoingInterface.onNoteEdit("Edit Note",
          text,
          OutgoingInterface.encodeBtns([new BtnMsg.from(BtnMsg.SAVE_NOTE,annId),BtnMsg.CANCEL]),
          annId);
    });
  }

  void addAnnotation(String noteText) {
    StringBuffer summary = new StringBuffer();
    _rootNode.compileMetaAndSelectedText(summary);
    print("Annotation summary: ${summary.toString()}");
    
    CachedAnnotation annotation = _annotationCache.newAnnotation(noteText, summary.toString());
    _rootNode.processHighlight(annotation);
    _rootNode.subtreeFindAndMergeTextNodes();
    _rootNode.verifyStructure();
    _annotationCache.cleanAnnotations();
    if (!window.getSelection().isCollapsed) {
      window.getSelection().collapseToEnd();
    }
    _annotationCache.display();
    
    storeAnnotations();
  }

  void delAnnotations(String annId) {
    if (annId==null) {
      _rootNode.processUnhighlight();
    } else {
      CachedAnnotation ann = _annotationCache.getAnnotation(annId);
      if (ann==null) {
        throw new Exception("Annotation with id '${annId}' couldn't be found!");
      }
      List<TreeNode> nodesToRemove = new List();
      for (String markId in ann.getMarks()) {
        CachedMark mark = ann.getMark(markId);
        String nodeId = mark.id;
        TreeNode node = _rootNode.findNodeWithId(nodeId);
        if (node==null) {
          throw new Exception("Node with id '${nodeId}' couldn't be found!");
        }
        nodesToRemove.add(node);
      }
      
      for (TreeNode node in nodesToRemove) {
        List<String> removedIds = node.removeMetaFromSubtree();
        _annotationCache.marksDeleted(removedIds);
      }
    }
    _rootNode.subtreeFindAndMergeTextNodes();
    _rootNode.verifyStructure();
    _rootNode.metaInfoHelper.sidePanelEntryManager.updatePositions();
    _annotationCache.cleanAnnotations();
    if (!window.getSelection().isCollapsed) {
      window.getSelection().collapseToEnd();
    }
    _annotationCache.display();
    
    storeAnnotations();
  }

  void extendAnnotations() {
    Set<String> markIds = new Set<String>();
    Set<String> annIdsWithNotes = new Set<String>();
    Set<String> annIdsWithoutNotes = new Set<String>();
    
    _compileAnnotations(markIds, annIdsWithNotes, annIdsWithoutNotes);
    
    Set<String> annIds = new Set();
    annIds.addAll(annIdsWithNotes);
    annIds.addAll(annIdsWithoutNotes);

    String note = null;
    
    if (!annIdsWithNotes.isEmpty) {
      StringBuffer sb = new StringBuffer();
      for (int i=0; i<annIdsWithNotes.length; ++i) {
        String noteId = annIdsWithNotes.elementAt(i);
        if (i>0) {
          sb.writeln("---- merged note ----");
        }
        CachedAnnotation ann = _annotationCache.getAnnotation(noteId); 
        if (ann==null) {
          throw new Exception("Annotation not found ${noteId}");
        }
        sb.writeln(ann.getNote());
      }
      note = sb.toString();
    }
    
    StringBuffer summary = new StringBuffer();
    _rootNode.compileMetaAndSelectedText(summary);
    print("Annotation summary: ${summary.toString()}");
    
    CachedAnnotation newAnn = _annotationCache.newAnnotation(note,
        summary.toString());
    _rootNode.processHighlight(newAnn);
    _rootNode.subtreeFindAndMergeTextNodes();
    _rootNode.verifyStructure();
    
    for (String annId in annIds) {
      CachedAnnotation ann = _annotationCache.getAnnotation(annId);
      if (ann.hasMarks()) {
        newAnn.importMarksOf(ann);
      }
    }
    
    _annotationCache.cleanAnnotations();
    
    if (!window.getSelection().isCollapsed) {
      window.getSelection().collapseToEnd();
    }
    _annotationCache.display();
    
    storeAnnotations();
  }

  void _compileAnnotations(Set<String> markIds,
                           Set<String> annotationsWithNotes,
                           Set<String> annotationsWithoutNotes)
  {
    markIds.clear();
    annotationsWithNotes.clear();
    annotationsWithoutNotes.clear();

    rootNodeTree.compileSubtreeSelectedMetaNodeIds(markIds);
    for (String nodeId in markIds) {
      CachedAnnotation ann = annotationCache.annotationWithMark(nodeId);
      if (ann.hasNote()) {
        annotationsWithNotes.add(ann.getId());
      } else {
        annotationsWithoutNotes.add(ann.getId());
      }
    }
  }
  
  void clearNote(String annId) {
    CachedAnnotation ann = _annotationCache.getAnnotation(annId);
    if (ann==null) {
      throw new Exception("Annotation ${annId} could not be found!");
    }
    ann.setNote(null);
    _rootNode.metaInfoHelper.updateMetaStyle(ann);
    storeAnnotations();    
  }
  
  void saveNote(String extraInfo) {
    Map<String,String> vals = json.parse(extraInfo);
    String noteId = vals["extra"];
    String text = vals["text"];
    print("saving note ${noteId} with text: '${text}'");
    CachedAnnotation ann = _annotationCache.getAnnotation(noteId);
    if (ann==null) {
      throw new Exception("Annotation ${noteId} could not be found!");
    }
    ann.setNote(text);
    _rootNode.metaInfoHelper.updateMetaStyle(ann);
    storeAnnotations();    
  }
  
  void copy(String text) {
    print('copy text: "${text}"');
    _outgoingInterface.onTextCopy(text);
  }
  
  /**
   * Called when the user has cleared the selection, e.g. by clicking
   * somewhere else on the page. This method sends a message to cancel
   * and UI highlight context options, and also clears the internal
   * state of the root node tree.
   */
  void clearSelection() {
    // No need to clear selection state
    //rootNodeTree.clearSubtreeSelectionState();
    
    _outgoingInterface.onSelectionFinished();
  }

  void registerBodyEvents() {
    unregisterBodyEvents();
    
    if (androidVersion) {
      _selectionManager = new MobileSelectionManager(this);
    } else {
      _selectionManager = new BrowserSelectionManager(this);
    }

    _selectionManager.registerEvents();
  }

  void unregisterBodyEvents() {
    if (_selectionManager!=null) {
      _selectionManager.unregisterEvents();
      _selectionManager = null;
    }
  }


  /**
   * Called either when the user has made a new selection,
   * or after the selection has changed, in order to update the UI
   * highlight context options that depend on the region highlighted.
   */
  void processSelection(Range range, Point clientPt, Point pagePt, bool first) {
    String text = range.toString();
    print("received selected range: ${Logger.summarize(text, 50)}");
    
    //  clear selection state
    _rootNode.clearSubtreeSelectionState();
    _rootNode.processSelectionRange(range);

    bool hasSelectedNonMetaNodes = _rootNode.hasSelectedNonMetaText();
    Set<String> markIds = new Set<String>();
    Set<String> annotationsWithNotes = new Set<String>();
    Set<String> annotationsWithoutNotes = new Set<String>();
    _compileAnnotations(markIds, annotationsWithNotes, annotationsWithoutNotes);
    
    print("\tmarkIds                 (${markIds.length}): ${markIds}");
    print("\tannotationsWithNotes    (${annotationsWithNotes.length}): ${annotationsWithNotes}");
    print("\tannotationsWithoutNotes (${annotationsWithoutNotes.length}): ${annotationsWithoutNotes}");

    List<BtnMsg> determineBtns() {
      if (markIds.isEmpty && hasSelectedNonMetaNodes) {
        return [BtnMsg.ADD_HIGHLIGHT, BtnMsg.ADD_NOTE];
      }
      if (!markIds.isEmpty && !hasSelectedNonMetaNodes && annotationsWithNotes.isEmpty && annotationsWithoutNotes.length==1) {
        return [new BtnMsg.from(BtnMsg.CLEAR_HIGHLIGHT, annotationsWithoutNotes.first),
                new BtnMsg.from(BtnMsg.ADD_NOTE, annotationsWithoutNotes.first)
        ];
      }
      if (!markIds.isEmpty && !hasSelectedNonMetaNodes && annotationsWithNotes.length==1 && annotationsWithoutNotes.isEmpty) {
        return [new BtnMsg.from(BtnMsg.CLEAR_NOTE,annotationsWithNotes.first)];
      }
      if (annotationsWithNotes.isEmpty && annotationsWithoutNotes.length==1 && hasSelectedNonMetaNodes) {
        return [new BtnMsg.from(BtnMsg.EXTEND_HIGHLIGHT,annotationsWithoutNotes.first)];
      }
      if (annotationsWithNotes.length==1 && annotationsWithoutNotes.isEmpty && hasSelectedNonMetaNodes) {
        return [new BtnMsg.from(BtnMsg.EXTEND_NOTE,annotationsWithNotes.first)];
      }
      if (annotationsWithNotes.isEmpty && annotationsWithoutNotes.length>1) {
        return [BtnMsg.MERGE_HIGHLIGHT];
      }
      if (annotationsWithNotes.length>1 && annotationsWithoutNotes.isEmpty) {
        return [BtnMsg.MERGE_NOTE];
      }
      if (annotationsWithNotes.length==1 && !annotationsWithoutNotes.isEmpty) {
        return [BtnMsg.EXTEND_NOTE_ONTO_HIGHLIGHT];
      }
      if (annotationsWithNotes.length>1 && !annotationsWithoutNotes.isEmpty) {
        return [BtnMsg.MERGE_EXTEND_NOTE_ONTO_HIGHLIGHT];
      }

      return [];
    }

    List<BtnMsg> btns = determineBtns();
    btns.add(new BtnMsg.from(BtnMsg.COPY,text));
    
    Set<BtnMsg> btnSet = new Set();
    for (BtnMsg b in btns) {
      btnSet.add(b);
    }
    
    double cX = clientPt.x.toDouble();
    double cY = clientPt.y.toDouble();
    double pX = pagePt.x.toDouble();
    double pY = pagePt.y.toDouble();

    if (first) {
      _lastBtns = btnSet;
      
      runAsync((){
        _outgoingInterface.onSelectionStarted(
            cX, cY, pX, pY,
            OutgoingInterface.encodeBtns(btns));
      });
    } else {
      if (_lastBtns!=null) {
        if (_lastBtns.containsAll(btnSet) &&
            btnSet.containsAll(_lastBtns)) {
          return;
        }
      }
      
      runAsync((){
        _outgoingInterface.onSelectionUpdated(
            cX, cY, pX, pY,
            OutgoingInterface.encodeBtns(btns));
      });
    }
  }
  

  void onMetaNodeClicked(MouseEvent e, String id) {
    CachedAnnotation ann = annotationCache.annotationWithMark(id);

    List<BtnMsg> btns = new List();

    if (ann.hasNote()) {
      btns.add(new BtnMsg.from(BtnMsg.EDIT_NOTE, ann.getId()));
      btns.add(new BtnMsg.from(BtnMsg.CLEAR_NOTE, ann.getId()));
    } else {
      btns.add(new BtnMsg.from(BtnMsg.ADD_NOTE, ann.getId()));
      btns.add(new BtnMsg.from(BtnMsg.CLEAR_HIGHLIGHT, ann.getId()));
    }
    
    StringBuffer sb = new StringBuffer();
    for (String markId in ann.getMarks()) {
      TreeNode node = _rootNode.findNodeWithId(id);
      if (node==null) {
        throw new Exception("Node ${id} could not be found!");
      }
      sb.writeln(node.domNode.text);
    }

    if (!sb.isEmpty) {
      btns.add(new BtnMsg.from(BtnMsg.COPY, sb.toString()));
    }
    
    runAsync((){
      _outgoingInterface.onMetaNodeClicked(
          e.client.x.toDouble(), e.client.y.toDouble(),
          e.page.x.toDouble(), e.page.y.toDouble(),
          OutgoingInterface.encodeBtns(btns));
    });
  }
  
  void onBookmarkSideEntryClick(MouseEvent e, String bookmarkId) {
    List<BtnMsg> btns = new List();

    btns.add(new BtnMsg.from(BtnMsg.DELETE_BOOKMARK, bookmarkId));
    
    runAsync((){
      _outgoingInterface.onShowBookmarkingOptions(
          e.client.x.toDouble(), e.client.y.toDouble(),
          e.page.x.toDouble(), e.page.y.toDouble(),
          OutgoingInterface.encodeBtns(btns));
    });
  }
  
  void storeAnnotations() {
    annotationCache.store();
  }

  void addBookmark(String extraInfo) {
    Map coords = json.parse(extraInfo);
    print("Adding bookmark to ${coords}");

    double cX = coords['cX'];
    double cY = coords['cY'];
//    double pX = coords['pX'];
//    double pY = coords['pY'];
    
//    double cX = pX - window.scrollX.toDouble();
//    double cY = pY - window.scrollY.toDouble();
    
    Range range = document.caretRangeFromPoint(cX.toInt(), cY.toInt());
    
    TreeNode node = _rootNode.findNodeWithRange(range);
    
    if (node!=null && !node.isBookmarked()) {
      SidePanelEntryManager sidePanelManager = _rootNode.metaInfoHelper.sidePanelEntryManager;
      
      RangePoint rangePoint = new RangePoint(node.getNodeLocation(), null);
      print("range-point: ${rangePoint}");
      
      String id = _annotationCache.createNewBookmarkId();
      if (sidePanelManager.addBookmarkEntry(node, id)) {
        node.setBookmarkId(id);
        print("bookmark summary: ${node.domNode.text}");
        _annotationCache.newBookmark(id, node.domNode.text, rangePoint);
        _annotationCache.store();
      }
      
    }
    
  }
  
  void deleteBookmark(String extraInfo) {
    String bookmarkId = extraInfo;
    RangePoint point = _annotationCache.getBookmark(bookmarkId);
    if (point==null) {
      return;
    }
    
    print("Deleting bookmark from ${point}");
    TreeNode node = _rootNode.findNodeWithPath(point.nodeLocs);
    if (node==null) {
      throw new Exception("Node with path: ${point.nodeLocs} could not be found!");
    }
    
    if (node.isBookmarked()) {
      _rootNode.metaInfoHelper.sidePanelEntryManager.removeBookmarkEntry(node);
      node.setBookmarkId(null);
      _annotationCache.deleteBookmark(bookmarkId);
      _annotationCache.store();
    }
  }
  
  void gotoBookmark(String extraInfo) {
    String bookmarkId = extraInfo;
    RangePoint point = _annotationCache.getBookmark(bookmarkId);
    print("Go to bookmark ${bookmarkId} ${point}");
    
    TreeNode node = _rootNode.findNodeWithPath(point.nodeLocs);
    if (node==null) {
      throw new Exception("Node with path: ${point.nodeLocs} could not be found!");
    }
    
    node.scrollToTop();
  }

  void gotoMark(String extraInfo) {
    String markId = extraInfo;
    
    CachedAnnotation ann = _annotationCache.annotationWithMark(markId);
    if (ann==null) {
      throw new Exception("Annotation with mark ${markId} could not be found!");
    }
    
    CachedMark mark = ann.getMark(markId);
    if (mark==null) {
      throw new Exception("Mark ${markId} could not be found in annotation ${ann.getId()}!");
    }
    
    RangePoints rangePoints = mark.location;
    RangePoint point = rangePoints.start;
    
    print("Go to mark ${ann.getId()} ${markId} ${point}");
    
    TreeNode node = _rootNode.findNodeWithId(markId);
    if (node==null) {
      throw new Exception("Node with path: ${point.nodeLocs} could not be found!");
    }
    
    node.scrollToTop();
  }
}

abstract class AbstractSelectionManager {
  void registerEvents();
  void unregisterEvents();
}

class MobileSelectionManager extends AbstractSelectionManager {
  final IncomingInterface _incomingInterface;
  List<dynamic> _callbacks = new List();
  
  MobileSelectionManager(IncomingInterface this._incomingInterface)
  {
  }
  
  void registerEvents() {
    print("MobileSelectionManager.registerEvents()");
    js.scoped((){
      dynamic startSelectionModeCallback = new js.Callback.many((){
        try {
          print("startSelectionModeCallback");
          runAsync(_startSelectionMode);
        } catch (e, s) {
          outgoingInterface.onError("Error in startSelectionModeCallback", e, s, false);
        }
      });
      dynamic updateSelectionCallback = new js.Callback.many((){
        try {
          print("updateSelectionCallback");
          runAsync(_updateSelection);
        } catch (e, s) {
          outgoingInterface.onError("Error in updateSelectionCallback", e, s, false);
        }
      });
      dynamic endSelectionModeCallback = new js.Callback.many((){
        try {
          print("endSelectionModeCallback");
          runAsync(_endSelectionMode);
        } catch (e, s) {
          outgoingInterface.onError("Error in endSelectionModeCallback", e, s, false);
        }
      });

      dynamic callbackMap = js.map({
        'startSelectionMode' : startSelectionModeCallback,
        'updateSelection' : updateSelectionCallback,
        'endSelectionMode' : endSelectionModeCallback
      });
      
      js.context.ebookSelection = callbackMap;
      
      print("ebookSelection registered");
      
      _callbacks.add(startSelectionModeCallback);
      _callbacks.add(updateSelectionCallback);
      _callbacks.add(endSelectionModeCallback);
    });
    
  }
  
  void unregisterEvents() {
    print("MobileSelectionManager.unregisterEvents()");
    for (dynamic s in _callbacks) {
      s.dispose();
    }
    _callbacks.clear();
  }
  
  void clearSelection() {
    js.scoped((){
      js.context.android.selection.clearSelection();
    });
  }
  
  void _startSelectionMode() {
    _processSelection(true);
  }
  
  void _endSelectionMode() {
    _incomingInterface.clearSelection();
  }
  
  void _updateSelection() {
    _processSelection(false);
  }
  
  void _processSelection(bool first) {
    Range range;
    js.scoped((){
      try {
        dynamic r = js.context.android.selection.getSelectionRange();
        range = DomInterop.jsToDartRange(r);
      } catch (e, s) {
        outgoingInterface.onError("Error in MobileSelectionManager._processSelection", e, s, false);
      }
    });
    if (range!=null) {
      _incomingInterface.processSelection(range, new Point(0, 0), new Point(0, 0), first);
    }
  }
}

class BrowserSelectionManager extends AbstractSelectionManager {
  static final int RIGHT_MOUSE_BUTTON = 3; 
  
  final IncomingInterface _incomingInterface;
  List<StreamSubscription> _subscriptions = new List();
  
  BrowserSelectionManager(IncomingInterface this._incomingInterface)
  {
  }
  
  void registerEvents() {
    print("listening for mouse up");
    Element root = _incomingInterface._rootNode.domNode as Element;
    StreamSubscription bodyMouseUpEventSubsription = root.onMouseUp.listen((MouseEvent e){
      print("mouse up received! at: ${e.client.x} ${e.client.y}");
      window.console.log("Processing selected text");
      
      Selection selection = window.getSelection();
      if (!selection.isCollapsed && selection.rangeCount==1) {
        Range range = selection.getRangeAt(0);
        range.expand("word");
        selection.addRange(range);
        js.scoped((){
          js.context.selectedRange = range;
        });
        _incomingInterface.processSelection(range, e.client, e.page, true);
      }
    });
    StreamSubscription bodyRightClickSubscription = root.onClick.listen((MouseEvent e){
      processShowBookmarkOptions(e.client, e.page);
      e.preventDefault();
    });
    
    _subscriptions.add(bodyMouseUpEventSubsription);
    _subscriptions.add(bodyRightClickSubscription);
  }
  
  void unregisterEvents() {
    print("BrowserSelectionManager.unregisterEvents()");
    for (StreamSubscription s in _subscriptions) {
      s.cancel();
    }
    _subscriptions.clear();
  }
  
  /**
   * Called to show the book-mark menu option.
   */
  void processShowBookmarkOptions(Point clientPt, Point pagePt) {
    double cX = clientPt.x.toDouble();
    double cY = clientPt.y.toDouble();
    double pX = pagePt.x.toDouble();
    double pY = pagePt.y.toDouble();
    
    Range range = document.caretRangeFromPoint(cX.toInt(), cY.toInt());
    TreeNode node = _incomingInterface._rootNode.findNodeWithRange(range);
    
    if (node!=null) {
      List<BtnMsg> btnMsgs = new List();
      
      if (node.isBookmarked()) {
        RangePoint rangePoint = new RangePoint(node.getNodeLocation(), null);
        btnMsgs.add(
            new BtnMsg.from(BtnMsg.DELETE_BOOKMARK, 
                node.getBookmarkId())
            );
      } else {
        btnMsgs.add(
            new BtnMsg.from(BtnMsg.ADD_BOOKMARK, 
                json.stringify({
                  'cX':cX,
                  'cY':cY,
                  'pX':pX,
                  'pY':pY
                }))
            );
      }
      
      runAsync((){
        _incomingInterface._outgoingInterface.onShowBookmarkingOptions(
            cX, cY, pX, pY,
            OutgoingInterface.encodeBtns(btnMsgs));
      });    
    } else {
      print("No node found at that location!");
    }
  }
  

}
