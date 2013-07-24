part of ebook;

abstract class AnnotationCache {
  
  int getSidePanelSide();
  bool hasAnnotations();
  String createNewAnnotationId();
  CachedAnnotation newAnnotation(String note, String summary);
  void mergeAnnotations(CachedAnnotation result, List<CachedAnnotation> anns);
  Iterable<String> getAnnotations();
  CachedAnnotation getAnnotation(String annId);
  void removeAllAnnotations();
  CachedAnnotation annotationWithMark(String markId);
  bool hasNote(String markId);
  void cleanAnnotations();
  void marksReplaced(List<String> replacedMarks, String newMarkId);
  void marksDeleted(List<String> marks);
  String createNewMarkId();
  
  String createNewBookmarkId();
  void newBookmark(String bookmarkId, String summary, RangePoint point);
  void deleteBookmark(String bookmarkId);
  Iterable<String> getBookmarks();
  RangePoint getBookmark(String bookmarkId);
  
  void store();

  Object toJsonObject() {
    Map jsonAnnMap = new Map();
    for (String key in getAnnotations()) {
      jsonAnnMap[key] = getAnnotation(key).toJsonObject();
    }
    Map jsonBmMap = new Map();
    for (String key in getBookmarks()) {
      jsonBmMap[key] = getBookmark(key).toJsonObject();
    }
    return {
      'annMap' : jsonAnnMap,
      'bmMap' : jsonBmMap
    };
  }

  void display() {
    window.console.groupCollapsed("Annotations:");
    for (String annId in getAnnotations()) {
      CachedAnnotation c = getAnnotation(annId);
      c.display();
    }
    window.console.groupEnd();
  }
  
}

abstract class CachedAnnotation {
  String getId();
  String getSummary();
  
  bool hasNote();
  String getNote();
  void setNote(String note);
  
  bool hasMarks();
  bool hasMark(String markId);
  CachedMark getMark(String markId);
  Iterable<String> getMarks();
  CachedMark newMark(RangePoints location, bool isBlock);
  void removeMark(String markId);
  void importMarksOf(CachedAnnotation ann);
  
  Object toJsonObject() {
    Map jsonMap = new Map();
    for (String key in getMarks()) {
      jsonMap[key] = getMark(key).toJsonObject();
    }
    return {
      'id' : getId(),
      'summary' : getSummary(),
      'note' : hasNote()?getNote():null,
      'map' : jsonMap
    };
  }
  
  void display() {
    String id = getId();
    if (!hasNote()) {
      window.console.groupCollapsed('\tid="${id}" note=null');
    } else {
      String note = getNote();
      window.console.groupCollapsed('\tid="${id}" note="${note}"');
    }
    for (String markId in getMarks()) {
      CachedMark m = getMark(markId);
      window.console.log("\t\t${m}");
    }
    window.console.groupEnd();
  }
}

class CachedMark {
  final CachedAnnotation annotation;
  final String id;
  final RangePoints location;
  final bool isBlock;

  CachedMark(CachedAnnotation this.annotation, String this.id, RangePoints this.location, bool this.isBlock);
  
  String toString() {
    return 'id=${id} location=${location} block=${isBlock}';
  }
  
  factory CachedMark.fromJson(CachedAnnotation annotation, Map jsonObj) {
    String id = jsonObj["id"];
    Map jsonLoc = jsonObj["loc"];
    bool block = jsonObj["isBlock"];
    return new CachedMark(annotation, id, RangePoints.parseFromObj(jsonLoc), block);
  }

  Object toJsonObject() {
    return {"id":id, "loc":location.toJsonObject(), "isBlock": isBlock};
  }
}


class InternalAnnotationCache extends AnnotationCache {
  static const STORAGE_KEY_ANNOTATIONS = "annotations";
  
  final Map<String,CachedAnnotation> _annMap;
  final Map<String,RangePoint> _bmMap;
  int _lastId;

  InternalAnnotationCache() : 
    _annMap = new Map<String,CachedAnnotation>(),
    _bmMap = new Map<String,RangePoint>()
  {
    _lastId = 0;
  }

  factory InternalAnnotationCache.fromJson(Map map) {
    InternalAnnotationCache cache = new InternalAnnotationCache(); 
    cache._lastId = map['lastId'];
    Map jsonAnnMap = map['annMap'];
    if (jsonAnnMap!=null) {
      for (String key in jsonAnnMap.keys) {
        Object obj = jsonAnnMap[key];
        cache._annMap[key] = new InternalCachedAnnotation.fromJson(cache, obj);
      }
    }
    Map jsonBmMap = map['bmMap'];
    if (jsonBmMap!=null) {
      for (String key in jsonBmMap.keys) {
        Object obj = jsonBmMap[key];
        cache._bmMap[key] = RangePoint.parseFromObj(obj); 
      }
    }
    return cache;
  }
  
  factory InternalAnnotationCache.load() {
    if (window.localStorage.containsKey(STORAGE_KEY_ANNOTATIONS)) {
      try {
        print("loading annotation data");
        String jsonStr = window.localStorage[STORAGE_KEY_ANNOTATIONS];
        Object jsonObj = json.parse(jsonStr);
        AnnotationCache cache = new InternalAnnotationCache.fromJson(jsonObj);
        print("annotation data loaded ${cache}");
        cache.display();
        return cache;
      } catch (e, s) {
        print(e);
        print(s);
      }
    }
    return null;
  }
  
  int getSidePanelSide() {
    return SidePanelSide.RIGHT.val;
  }
  
  bool hasAnnotations() {
    return !_annMap.isEmpty;
  }

  String createNewAnnotationId() {
    return "ann${(_lastId++).toRadixString(16)}";
  }
  
  CachedAnnotation newAnnotation(String note, String summary) {
    String name = createNewAnnotationId();
    CachedAnnotation ann = new InternalCachedAnnotation(this, name, summary);
    ann.setNote(note);
    _annMap[name] = ann;
    return ann;
  }

  void mergeAnnotations(CachedAnnotation result, List<CachedAnnotation> anns) {
    StringBuffer sb = new StringBuffer();
    for (CachedAnnotation ann in anns) {
      if (ann.hasNote()) {
        sb.writeln(ann.getNote());
      }
    }
    result.setNote(sb.toString());
  }

  Iterable<String> getAnnotations() {
    return _annMap.keys;
  }
  
  CachedAnnotation getAnnotation(String annId) {
    return _annMap[annId];
  }
  
  void removeAllAnnotations() {
    _annMap.clear();
  }
  
  CachedAnnotation annotationWithMark(String markId) {
    for (CachedAnnotation ann in _annMap.values) {
      if (ann.hasMark(markId)) {
        return ann;
      }
    }
    throw new Exception("Highlight not found in cache: ${markId}");
  }

  bool hasNote(String markId) {
    CachedAnnotation ann = annotationWithMark(markId);
    return ann.hasNote();
  }

  void cleanAnnotations() {
    List<CachedAnnotation> emptyAnnotations = new List();
    for (CachedAnnotation ann in _annMap.values) {
      if (!ann.hasMarks()) {
        emptyAnnotations.add(ann);
      }
    }
    for (CachedAnnotation ann in emptyAnnotations) {
      _annMap.remove(ann.getId());
    }
  }

  void marksReplaced(List<String> replacedMarks, String newMarkId) {
    marksDeleted(replacedMarks);
  }

  void marksDeleted(List<String> marks) {
    for (String markId in marks) {
      for (CachedAnnotation ann in _annMap.values) {
        if (ann.hasMark(markId)) {
          ann.removeMark(markId);
        }
      }
    }
  }

  String createNewMarkId() {
    return "mark${(_lastId++).toRadixString(16)}";
  }
  
  String createNewBookmarkId() {
    return "bm${(_lastId++).toRadixString(16)}";
  }
  
  void newBookmark(String bookmarkId, String summary, RangePoint point) {
    this._bmMap[bookmarkId] = point;
  }
  
  void deleteBookmark(String bookmarkId) {
    this._bmMap.remove(bookmarkId);
  }
  
  Iterable<String> getBookmarks() {
    return _bmMap.keys;
  }
  
  RangePoint getBookmark(String bookmarkId) {
    return _bmMap[bookmarkId];
  }
  
  void store() {
    print("storing data:");
    //display();
    
    Object jsonObj = this.toJsonObject();
    String jsonStr = json.stringify(jsonObj);
    window.localStorage[STORAGE_KEY_ANNOTATIONS] = jsonStr;
    
    print("data stored");    
  }
  
  Object toJsonObject() {
    Map m = super.toJsonObject();
    m['lastId'] = _lastId;
    return m;
  }
}

class InternalCachedAnnotation extends CachedAnnotation {
  final AnnotationCache _cache;
  final String _id;
  final String _summary;
  String _note;
  final Map<String,CachedMark> _map;

  InternalCachedAnnotation(AnnotationCache this._cache, String this._id, String this._summary) : _note=null, _map = new Map<String,CachedMark>();
  
  String getId() {
    return _id;
  }
  
  String getSummary() {
    return _summary;
  }
  
  bool hasNote() {
    return _note!=null;
  }
  
  String getNote() {
    return _note;
  }
  
  void setNote(String note) {
    _note = note;
  }
  
  bool hasMarks() {
    return !_map.isEmpty;
  }
  
  bool hasMark(String markId) {
    return _map.containsKey(markId);
  }

  CachedMark getMark(String markId) {
    if (_map.containsKey(markId)) {
      return _map[markId];
    } else {
      return null;
    }
  }
  
  Iterable<String> getMarks() {
    return _map.keys;
  }
  
  CachedMark newMark(RangePoints location, bool isBlock) {
    String id = _cache.createNewMarkId();
    CachedMark ann = new CachedMark(this, id, location, isBlock);
    _map[id] = ann;
    return ann;
  }
  
  bool removeMark(String markId) {
    if (_map.containsKey(markId)) {
      _map.remove(markId);
      return true;
    }
    return false;
  }
  
  void importMarksOf(CachedAnnotation ann) {
    List<String> newMarks = new List();
    for (String markId in ann.getMarks()) {
      _map[markId] = ann.getMark(markId);
      newMarks.add(markId);
    }
    for (String markId in newMarks) {
      ann.removeMark(markId);
    }
  }
  
  factory InternalCachedAnnotation.fromJson(AnnotationCache cache, Map json) {
    String id = json['id'];
    String note = json['note'];
    String summary = json['summary'];
    Map jsonMap = json['map'];
    
    InternalCachedAnnotation ann = new InternalCachedAnnotation(cache, id, summary);
    if (note!=null) {
      ann.setNote(note);
    }
    
    for (String key in jsonMap.keys) {
      Map jsonMark = jsonMap[key];
      CachedMark mark = new CachedMark.fromJson(ann, jsonMark);
      ann._map[key] = mark;
    }
    
    return ann;
  }
}

class AndroidAnnotationCache extends InternalAnnotationCache {
  AndroidAnnotationCache() : super() {
  }

  factory AndroidAnnotationCache.fromJson(Map map) {
    AndroidAnnotationCache cache = new AndroidAnnotationCache(); 
    cache._lastId = map['lastId'];
    Map jsonMap = map['map'];
    for (String key in jsonMap.keys) {
      Object obj = jsonMap[key];
      cache._annMap[key] = new InternalCachedAnnotation.fromJson(cache, obj);
    }
    return cache;
  }
  
  factory AndroidAnnotationCache.load() {
    print("Loading Annotations From Android");
    String jsonStr;
    js.scoped((){
      print("js.context.androidAnnotationInterface.getAnnotationsJson = ${js.context.androidAnnotationInterface.getAnnotationsJson}");
      jsonStr = js.context.androidAnnotationInterface.getAnnotationsJson();
    });
    print("\treceived ${jsonStr}");
    
    if (jsonStr==null)
      return null;
    
    Map jsonObj = json.parse(jsonStr);
    return new AndroidAnnotationCache.fromJson(jsonObj);
  }
  
  void store() {
    print("storing data:");
    //display();
    
    Object jsonObj = this.toJsonObject();
    String jsonStr = json.stringify(jsonObj);
    js.scoped((){
      jsonStr = js.context.androidAnnotationInterface.setAnnotationsJson(jsonStr);
    });
    
    print("data stored");
  }
}

class AndroidDbAnnotationCache extends InternalAnnotationCache {
  AndroidDbAnnotationCache() {
  }

  int getSidePanelSide() {
    int res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_getSidePanelSide");
      res = js.context.androidAnnotationInterface.annotationCache_getSidePanelSide();
      print("\treceived result ${res}");
    });
    if (res==null) {
      return SidePanelSide.LEFT.val;
    }
    return res;
  }
  
  bool hasAnnotations() {
    bool res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_hasAnnotations");
      res = js.context.androidAnnotationInterface.annotationCache_hasAnnotations();
      print("\treceived result ${res}");
    });
    return res;
  }
  
  String createNewAnnotationId() {
    String res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_createNewAnnotationId");
      res = js.context.androidAnnotationInterface.annotationCache_createNewAnnotationId();
      print("\treceived result ${res}");
    });
    return res;
  }
  
  CachedAnnotation newAnnotation(String note, String summary) {
    String name = createNewAnnotationId();
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_createNewAnnotation");
      js.context.androidAnnotationInterface.annotationCache_createNewAnnotation(name, summary);
    });
    CachedAnnotation ann = new AndroidDbCachedAnnotation(this, name);
    ann.setNote(note);
    return ann;
  }
  
  void mergeAnnotations(CachedAnnotation result, List<CachedAnnotation> anns) {
    //  TODO: Move this functionality to android side
    StringBuffer sb = new StringBuffer();
    for (CachedAnnotation ann in anns) {
      if (ann.hasNote()) {
        sb.writeln(ann.getNote());
      }
    }
    result.setNote(sb.toString());
  }

  Iterable<String> getAnnotations() {
    String resStr = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_getAnnotations");
      resStr = js.context.androidAnnotationInterface.annotationCache_getAnnotations();
      print("\treceived result ${resStr}");
    });
    return json.parse(resStr);
  }
  
  CachedAnnotation getAnnotation(String annId) {
    return new AndroidDbCachedAnnotation(this, annId);
  }
  
  void removeAllAnnotations() {
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_removeAllAnnotations");
      js.context.androidAnnotationInterface.annotationCache_removeAllAnnotations();
    });
  }
  
  CachedAnnotation annotationWithMark(String markId) {
    String annId = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_annotationWithMark");
      //  returns the annotation-id
      annId = js.context.androidAnnotationInterface.annotationCache_annotationWithMark(markId);
      print("\treceived result ${annId}");
    });
    if (annId==null) {
      return null;
    }
    return new AndroidDbCachedAnnotation(this, annId);
  }

  bool hasNote(String markId) {
    CachedAnnotation ann = annotationWithMark(markId);
    return ann.hasNote();
  }

  void cleanAnnotations() {
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_cleanAnnotations");
      js.context.androidAnnotationInterface.annotationCache_cleanAnnotations();
    });
  }

  void marksReplaced(List<String> replacedMarks, String newMarkId) {
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_marksReplaced");
      js.context.androidAnnotationInterface.annotationCache_marksReplaced(
          json.stringify(replacedMarks), newMarkId);
    });
  }

  void marksDeleted(List<String> marks) {
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_marksDeleted");
      js.context.androidAnnotationInterface.annotationCache_marksDeleted(
          json.stringify(marks));
    });
  }

  String createNewMarkId() {
    String res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_createNewMarkId");
      res = js.context.androidAnnotationInterface.annotationCache_createNewMarkId();
      print("\treceived result ${res}");
    });
    return res;
  }
  
  String createNewBookmarkId() {
    String res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_createNewBookmarkId");
      res = js.context.androidAnnotationInterface.annotationCache_createNewBookmarkId();
      print("\treceived result ${res}");
    });
    return res;
  }
  
  void newBookmark(String bookmarkId, String summary, RangePoint point) {
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_newBookmark");
      js.context.androidAnnotationInterface.annotationCache_newBookmark(
          bookmarkId, summary, json.stringify(point.toJsonObject()));
    });
  }
  
  void deleteBookmark(String bookmarkId) {
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_deleteBookmark");
      js.context.androidAnnotationInterface.annotationCache_deleteBookmark(
          bookmarkId);
    });
  }
  
  Iterable<String> getBookmarks() {
    String resStr = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_getBookmarks");
      resStr = js.context.androidAnnotationInterface.annotationCache_getBookmarks();
      print("\treceived result ${resStr}");
    });
    return json.parse(resStr);
  }
  
  RangePoint getBookmark(String bookmarkId) {
    String resStr = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.annotationCache_getBookmark");
      resStr = js.context.androidAnnotationInterface.annotationCache_getBookmark(
          bookmarkId);
      print("\treceived result ${resStr}");
    });
    return RangePoint.parseFromObj(json.parse(resStr));
  }
  
  void store() {
  }
  
}

class AndroidDbCachedAnnotation extends CachedAnnotation {
  final AnnotationCache _cache;
  final String _id;
  String _summary;
  
  AndroidDbCachedAnnotation(AnnotationCache this._cache, String this._id) {
    _summary = _getSummary(); 
  }
  
  String _getSummary() {
    String res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.cachedAnnotation_getSummary");
      res = js.context.androidAnnotationInterface.cachedAnnotation_getSummary(getId());
      print("\treceived result ${res}");
    });
    return res;    
  }
  
  String getId() {
    return _id;
  }
  
  String getSummary() {
    return _summary;
  }
  
  bool hasNote() {
    bool res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.cachedAnnotation_hasNote");
      res = js.context.androidAnnotationInterface.cachedAnnotation_hasNote(getId());
      print("\treceived result ${res}");
    });
    return res;
  }

  String getNote() {
    String res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.cachedAnnotation_getNote");
      res = js.context.androidAnnotationInterface.cachedAnnotation_getNote(getId());
      print("\treceived result ${res}");
    });
    return res;
  }
  
  void setNote(String note) {
    js.scoped((){
      print("js.context.androidAnnotationInterface.cachedAnnotation_setNote");
      js.context.androidAnnotationInterface.cachedAnnotation_setNote(getId(), note);
    });
  }

  bool hasMarks() {
    bool res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.cachedAnnotation_hasMarks");
      res = js.context.androidAnnotationInterface.cachedAnnotation_hasMarks(getId());
      print("\treceived result ${res}");
    });
    return res;
  }
  
  bool hasMark(String markId) {
    bool res = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.cachedAnnotation_hasMark");
      res = js.context.androidAnnotationInterface.cachedAnnotation_hasMark(
          getId(), markId);
      print("\treceived result ${res}");
    });
    return res;
  }

  CachedMark getMark(String markId) {
    if (hasMark(markId)) {
      String strRes = null;
      js.scoped((){
        print("js.context.androidAnnotationInterface.cachedAnnotation_getMark");
        strRes = js.context.androidAnnotationInterface.cachedAnnotation_getMark(
            getId(), markId);
        print("\treceived result ${strRes}");
      });
      if (strRes==null) {
        return null;
      }
      return new CachedMark.fromJson(this, json.parse(strRes));   
    } else {
      return null;
    }
  }
  
  Iterable<String> getMarks() {
    String strRes = null;
    js.scoped((){
      print("js.context.androidAnnotationInterface.cachedAnnotation_getMarks");
      strRes = js.context.androidAnnotationInterface.cachedAnnotation_getMarks(getId());
      print("\treceived result ${strRes}");
    });
    if (strRes==null) {
      return null;
    }
    return json.parse(strRes);   
  }
  
  CachedMark newMark(RangePoints location, bool isBlock) {
    String id = _cache.createNewMarkId();
    CachedMark mark = new CachedMark(this, id, location, isBlock);
    
    AndroidDbCachedAnnotation that = this;
    js.scoped((){
      print("js.context.androidAnnotationInterface.cachedAnnotation_markAdded");
      //  document-id, annotation-id, new mark JSON  
      js.context.androidAnnotationInterface.cachedAnnotation_markAdded(
          getId(), id, json.stringify(mark.toJsonObject()));
    });
    return mark;
  }
  
  bool removeMark(String markId) {
    if (hasMark(markId)) {
      js.scoped((){
        print("js.context.androidAnnotationInterface.cachedAnnotation_removeMark");
        js.context.androidAnnotationInterface.cachedAnnotation_removeMark(
            getId(), markId);
      });
      return true;
    } else {
      return false;
    }
  }
  
  void importMarksOf(CachedAnnotation ann) {
    List<String> newMarks = new List();
    for (String markId in ann.getMarks()) {
      js.scoped((){
        print("js.context.androidAnnotationInterface.cachedAnnotation_copyMark");
        //  document-id, destination annotation-id, mark-id, source annotation-id 
        js.context.androidAnnotationInterface.cachedAnnotation_copyMark(
            getId(), markId, ann.getId()
            );
      });
      newMarks.add(markId);
    }
    for (String markId in newMarks) {
      ann.removeMark(markId);
    }
  }
  
}

