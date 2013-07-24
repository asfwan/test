part of ebook;

class DomInterop {
  
  static Node jsToDartNode(var jsNode) {
    Node node = document;
    js.scoped((){
      var loc = js.context.domInterop.getNodeIndexesToNode(jsNode);
      
      for (int i=0; i<loc.length; ++i) {
        //print("parent = ${node}");
        //print("processing ${loc[i]}");
        node = node.nodes[loc[i]];
        //print("child = ${node}");
      }
    });
    return node;
  }
  
  static Range jsToDartRange(var jsRange) {
    Range r = new Range();
    r.setStart(jsToDartNode(jsRange.startContainer), jsRange.startOffset);
    r.setEnd(jsToDartNode(jsRange.endContainer), jsRange.endOffset);
    return r;
  }
  
}
