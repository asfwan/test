library ebook;

import 'dart:html';
import 'dart:math' as math;
import 'dart:json' as json;
//import 'package:js/js.dart' as js;
import 'dart:async';

part 'dom_interop_dart.dart';
part 'interfacing.dart';
part 'incoming_interface.dart';
part 'outgoing_interface.dart';
part 'annotations.dart';
part 'logger.dart';
part 'sidepanel.dart';
part 'treenode.dart';

const bool LOGGING_ON = true;
const bool DEBUG_MODE = true;

AnnotationCache annotationCache;
IncomingInterface incomingInterface;
TreeNode rootNodeTree;
OutgoingInterface outgoingInterface;

void runAsync(void callback()) {
  Timer.run(callback);
//  return new Timer(new Duration(milliseconds:ms), callback);
}

List<String> parseCssUnits(String v) {
  v = v.trim().toLowerCase();
  String num = v.replaceAll(new RegExp("[^0-9.]+"),"");
  String units = v.replaceAll(new RegExp("[0-9.]+"),"");
  return [num, units];
}

class _RangeDetails {
  final Range range;
  final CachedMark mark;
  final TreeNode parent;
  
  _RangeDetails(Range this.range, CachedMark this.mark, TreeNode this.parent);
}

void remarkTree() {
  List<_RangeDetails> ranges = new List();
  for (String annId in annotationCache.getAnnotations()) {
    CachedAnnotation ann = annotationCache.getAnnotation(annId);
    for (String markId in ann.getMarks()) {
      CachedMark mark = ann.getMark(markId);
      RangePoints rp = mark.location;
      TreeNode startNode, endNode;
      try {
        startNode = rootNodeTree.findNodeWithPath(rp.start.nodeLocs);
      } catch (e, s) {
        outgoingInterface.onError("Error while processing path: ${rp.start.nodeLocs}", e, s, false);
        continue;
      }
      
      try {
        endNode = rootNodeTree.findNodeWithPath(rp.end.nodeLocs);
      } catch (e, s) {
        outgoingInterface.onError("Error while processing path: ${rp.end.nodeLocs}", e, s, false);
        continue;
      }
      
      if (startNode.parent!=endNode.parent) {
        throw new Exception("Fatal Error: start node and end node of range have different parents!" 
          "(start:${Logger.pathTo(startNode.domNode)}, end:${Logger.pathTo(endNode.domNode)})");
      }
      
      try {
        Range r = new Range();
        if (rp.start.textLoc!=null) {
          r.setStart(startNode.domNode, rp.start.textLoc);
        } else {
          r.setStartBefore(startNode.domNode);
        }
        if (rp.end.textLoc!=null) {
          r.setEnd(endNode.domNode, rp.end.textLoc);
        } else {
          r.setEndAfter(endNode.domNode);
        }
        
        ranges.add(new _RangeDetails(r, mark, startNode.parent));
      }  catch (e, s) {
        outgoingInterface.onError("Error while processing mark range: ${rp}", e, s, false);
        continue;
      }
    }
  }
  
  if (ranges.isEmpty) {
    return;
  }
  
  int compareRangeDetails(_RangeDetails a, _RangeDetails b){
    return -a.range.comparePoint(b.range.startContainer, b.range.startOffset);
  };
  
  ranges.sort(compareRangeDetails);
  
  print("Sorted ranges:");
  for (_RangeDetails r in ranges) {
    print("\t${r.range.toString()}");
  }
  
  for (int i=ranges.length-1; i>=0; --i) {
    _RangeDetails r = ranges[i];
    /*
    SpanElement span = rootNodeTree.metaInfoHelper.createSpanNode(
        r.mark.id, r.mark.isBlock, true, r.mark.annotation.hasNote());
    r.range.surroundContents(span);
    r.range.detach();
    */
    r.parent.addMetaToRange(r.range, r.mark.annotation, r.mark, r.mark.isBlock, true);
    
    r.parent.updateStructure();
  }
  
  rootNodeTree.verifyStructure();
  
  //rootNodeTree.displayStructure();
  
  List<String> bookmarksToDelete = new List();
  SidePanelEntryManager sidePanelManager = rootNodeTree.metaInfoHelper.sidePanelEntryManager; 
  for (String bookmarkId in annotationCache.getBookmarks()) {
    print("Adding bookmark ${bookmarkId}");
    RangePoint point = annotationCache.getBookmark(bookmarkId);
    TreeNode node = rootNodeTree.findNodeWithPath(point.nodeLocs);
    if (node!=null) {
      node.setBookmarkId(bookmarkId);
      if (!sidePanelManager.addBookmarkEntry(node, bookmarkId)) {
        bookmarksToDelete.add(bookmarkId);
      }
    }
  }
  
  for (String bookmarkId in bookmarksToDelete) {
    annotationCache.deleteBookmark(bookmarkId);
  }
}


List<Node> copyChildNodes(Node node) {
  List<Node> nodes = new List();
  var x = node.$dom_firstChild;
  while (x!=null) {
    nodes.add(x);
    x = x.nextNode;
  }  
  return nodes;
}

int indexOfElement(Element el) {
  int index = 0;
  Element x = el;
  while (x.previousElementSibling!=null) {
    x = x.previousElementSibling;
    ++index;
  }
  return index;
}

int indexOfNode(Node el) {
  int index = 0;
  Node x = el;
  while (x.previousNode!=null) {
    x = x.previousNode;
    ++index;
  }
  return index;
}

List<Element> copyChildElements(Element node) {
  List<Element> nodes = new List();
  var x = node.$dom_firstElementChild;
  while (x!=null) {
    nodes.add(x);
    x = x.nextElementSibling;
  }  
  return nodes;
}


void main() {
  print("Dart main()");
  
  HeadElement head = document.head;
  BodyElement body = document.body;
  
  Element rootContainer = body.query("#root_ebook_container");

  if (rootContainer==null) {
    throw new Exception("Unable to find root ebook container div");
  }
  
  bool androidVersion = true;
  try {
    print("Attempting android interface.");
    outgoingInterface = new AndroidOutgoingInterface(rootContainer);
    androidVersion = true;
    print("\tUsing android interface.");
  } catch (e, s) {
    print("Android interface doesn't seem to exist, using internal interface instead.");
    outgoingInterface = new InternalOutgoingInterface(rootContainer);
    androidVersion = false;
  }
  
  try {
    if (androidVersion) {
  //    annotationCache = new AndroidAnnotationCache.load();
  //    if (annotationCache==null) {
  //      annotationCache = new AndroidAnnotationCache();
  //    }
      print("Initializing android Db annotation cache.");
      annotationCache = new AndroidDbAnnotationCache();
    } else {
      print("Initializing internal annotation cache.");
      annotationCache = new InternalAnnotationCache.load();
      if (annotationCache==null) {
        annotationCache = new InternalAnnotationCache();
      }
    }
    
    MetaInfoHelper highlightHelper = new MetaInfoHelper(
        "ebook-system-highlight",
        "ebook-system-note",
        "ebook-system-highlight-note",
        outgoingInterface, annotationCache,
        new SidePanelEntryManager(
            annotationCache,
            rootContainer,
            SidePanelSide.fromVal(annotationCache.getSidePanelSide())
            )
        );
  
    if (androidVersion) {
      //  needed android, but are expected to be embedded
      //    onto the android page
      /*
      ScriptElement androidSelection = new ScriptElement();
      androidSelection.src = "android.selection.js";
      
      head.append(androidSelection);
      */
    } else {
      LinkElement w2uiStyle = new LinkElement();
      w2uiStyle.href = "w2ui/w2ui-1.1.css";
      w2uiStyle.rel = "stylesheet";
      w2uiStyle.type = "text/css";
      head.append(w2uiStyle);
      
      ScriptElement w2uiScript = new ScriptElement();
      w2uiScript.src = "w2ui/w2ui-1.1.js";
      
      head.append(w2uiScript);
    }
    
    print("Initializing Node Tree.");
    rootNodeTree = new TreeNode(null, rootContainer, highlightHelper);
    
    print("Verifying Node Tree Structure.");
    rootNodeTree.verifyStructure();
    
    print("Initializing incoming interface.");
    incomingInterface = new IncomingInterface(annotationCache,
        rootNodeTree, outgoingInterface, androidVersion);
    
    if (annotationCache.hasAnnotations()) {
      print("Re-marking tree.");
      remarkTree();
    }
    
    void documentReady() {
      print("displaying document and registering input events");
      document.body.query(".ebook-outer-container").style.display = "block";
      rootNodeTree.metaInfoHelper.sidePanelEntryManager.updatePositions();
      incomingInterface.registerBodyEvents();
      outgoingInterface.documentReady();
    }
    
    if (document.readyState!="complete") {
      StreamSubscription<Event> sub;
      sub = document.onReadyStateChange.listen((Event e){
        if (document.readyState=="complete") {
          print("document now ready!");
          sub.cancel();
          documentReady();
        }
      });
    } else {
      print("document already ready!");
      documentReady();
    }
  } catch (e, s) {
    outgoingInterface.onError("Error thrown while loading", e, s, true);
    return;
  }
  
  js.scoped((){
    js.context.printMsg = new js.Callback.many((dynamic s){
      print(s.toString());
    });
  });
  
  print("Initializing done.");
}
