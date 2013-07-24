part of ebook;


abstract class OutgoingInterface {

  static String encodeBtns(List<BtnMsg> btns) {
    List lst = new List();
    for (BtnMsg btn in btns) {
      lst.add(btn.toJsonObject());
    }
    return json.stringify(lst);
  }
  
  void documentReady();

  void onSelectionStarted(double clientX, double clientY, double pageX, double pageY, String jsonButtons);
  void onSelectionUpdated(double clientX, double clientY, double pageX, double pageY, String jsonButtons);
  void onSelectionFinished();
  
  void onShowBookmarkingOptions(double clientX, double clientY, double pageX, double pageY, String jsonButtons);
  
  void onMetaNodeClicked(double clientX, double clientY, double pageX, double pageY, String jsonButtons);
  
  void onNoteEdit(String title, String text, String jsonButtons, String noteId);
  
  void onTextCopy(String text);
  
  void scrollToLocation(double pageY);
  
  void onError(String msg, var error, StackTrace stackTrace, bool quit) {
    onErrorMsg("Error: ${msg};\nDetails: ${error}", stackTrace.toString(), quit);
  }

  void onErrorMsg(String msg, String loc, bool quit);
  
}


class UiLockOverlay {
  final DivElement overlay;
  List<StreamSubscription<MouseEvent>> _subscriptions = new List();
  bool _locked = false;
  
  UiLockOverlay() : overlay=new DivElement() {
    overlay.id = "ebook-system-ui-lock";
    overlay.classes.add("ebook-block-overlay");
  }
  
  bool get locked {
    return _locked;
  }
  
  
  void lock({bool unlockOnMouseDown:true, bool unlockOnMouseUp:true, bool allowScroll:false, void unlockCallback():null}) {
    if (!allowScroll) {
      _subscriptions.add(overlay.onMouseWheel.listen((MouseEvent e){
        e.stopImmediatePropagation();
        e.preventDefault();
      }));
      overlay.style.removeProperty("position");
      overlay.style.removeProperty("height");
      overlay.style.removeProperty("width");
    } else {
      overlay.style.position = "absolute";
      overlay.style.height = "${document.documentElement.getBoundingClientRect().height}px";
      overlay.style.width = "${document.documentElement.getBoundingClientRect().width}px";
    }
    if (unlockOnMouseDown) {
      _subscriptions.add(overlay.onMouseDown.listen((MouseEvent e){
        print("somewhere else mouse down!");
        e.stopImmediatePropagation();
        unlock();
        if (unlockCallback!=null) {
          unlockCallback();
        }
      }));
    }
    if (unlockOnMouseUp) {
      _subscriptions.add(overlay.onMouseUp.listen((MouseEvent e){
        print("somewhere else mouse up!");
        e.stopImmediatePropagation();
  //      e.preventDefault();
        unlock();
        if (unlockCallback!=null) {
          unlockCallback();
        }
      }));
    }
    
    document.body.append(overlay);
  }
  
  void unlock() {
    overlay.remove();
    
    for (StreamSubscription s in _subscriptions) {
      s.cancel();
    }
    _subscriptions.clear();
  }
}

class PopupButtons {
  final Element _root;
  final UiLockOverlay _uiLockOverlay;
  List<StreamSubscription<MouseEvent>> _subscriptions = new List();

  PopupButtons(Element this._root) : _uiLockOverlay=new UiLockOverlay();

  bool get overlayOpen {
    return _uiLockOverlay.locked;
  }
  
  void open(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
    if (_uiLockOverlay.locked) {
      _uiLockOverlay.overlay.children.clear();
    }

    print("Opening menu");
    List<Map<String,String>> btnDetailsList = json.parse(jsonButtons);

    DivElement overlay = _uiLockOverlay.overlay;
    DivElement popup = new DivElement();
    DivElement innerDiv = new DivElement();
    
    popup.id = "w2ui-popup";
    popup.classes.add("w2ui-reset");
    popup.classes.add("w2ui-overlay");
    popup.style.position = "fixed";
    popup.style.display = 'none';
    popup.style.minWidth = 'auto';
    popup.style.minHeight = 'auto';
    popup.style.margin = '0px';

    innerDiv.style.margin = "3px";
    innerDiv.style.marginBottom = "7px";
    
    for (Map<String,String> btnDetails in btnDetailsList) {
      String id = btnDetails["id"];
      String name = btnDetails["name"];
      String msg = btnDetails["msg"];
      String extra = btnDetails["extra"];
      
      ButtonElement btn = new ButtonElement();
      btn.innerHtml = '<span style="margin:0px 3px">${name}</span>';
      btn.title = msg;
      btn.style.height = "2.5em";
      btn.style.marginLeft = "1px";
      btn.style.marginRight = "1px";
      StreamSubscription<MouseEvent> subscr = btn.onMouseDown.listen((MouseEvent e){
        print("btn ${btnDetails['id']} clicked!");
        closeOverlay(true);
        e.stopImmediatePropagation();
        e.preventDefault();
        Selection selection = window.getSelection();
        if (!selection.isCollapsed) {
          selection.collapseToStart();
        }
        String id = btnDetails['id'];
        runAsync((){
          js.scoped((){
            js.context.selectionClickResult(id,extra);
          });
        });
      });
      _subscriptions.add(subscr);
      innerDiv.append(btn);
    }

    popup.append(innerDiv);
    overlay.append(popup);
    
    _uiLockOverlay.lock(unlockOnMouseUp:true, unlockOnMouseDown:false, unlockCallback:(){
      Selection selection = window.getSelection();
      if (!selection.isCollapsed) {
        selection.collapseToStart();
      }
      closeOverlay(false);
    });

    num innerW;
    num innerH;
    num outerW;
    num outerH;
    js.scoped((){
      innerW = js.context.$jquery(popup).innerWidth();
      innerH = js.context.$jquery(popup).innerHeight();
      outerW = js.context.$jquery(popup).outerWidth(true);
      outerH = js.context.$jquery(popup).outerHeight(true);
    });

    num left = clientX-outerW/2.0;
    num pointerLeft = innerW/2.0-5;
    if (left<0) {
      pointerLeft -= -left;
      left = 0;
    }
    if (left+outerW>window.innerWidth) {
      pointerLeft += (left+outerW)-window.innerWidth;
      left = window.innerWidth-outerW;
    }
    num top = clientY-outerH-10;

    popup.style.left = '${left}px';
    popup.style.top = '${top}px';

    StyleElement style = new StyleElement();
    style.type = "text/css";
    style.text = """
      div.w2ui-overlay:before {
        -webkit-transform: rotate(135deg);
        -moz-transform: rotate(135deg);
        -ms-transform: rotate(135deg);
        -o-transform: rotate(135deg);
        top:${innerH-4}px;
        left:${pointerLeft}px;
        margin:0px;
      }
    """;
    popup.append(style);
    
    //cancelListenBodyMouseUp();
    print("menu open");

    js.scoped(() {
      var x = js.context.$jquery('#w2ui-popup');
      x.fadeIn('fast');
      x.data('position','100x100');
    });
  }
  
  void closeOverlay(bool unlock) {
    _uiLockOverlay.overlay.children.clear();
    
    for (StreamSubscription s in _subscriptions) {
      s.cancel();
    }
    _subscriptions.clear();    
  }

}


class InternalOutgoingInterface extends OutgoingInterface {

  final Element _root;
  final PopupButtons _popupButtons;

  InternalOutgoingInterface(Element root) :
    _root=root,
    _popupButtons=new PopupButtons(root)
  {
  }
  
  void documentReady() {
    
  }

  void onSelectionStarted(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
    print("InternalOutgoingInterface received selection start");
    _popupButtons.open(clientX, clientY, pageX, pageY, jsonButtons);
  }

  void onSelectionUpdated(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
    print("InternalOutgoingInterface received selection update");
    _popupButtons.open(clientX, clientY, pageX, pageY, jsonButtons);
  }
  
  void onSelectionFinished() {
    print("InternalOutgoingInterface received selection finished");
    _popupButtons.closeOverlay(true);
  }
  
  void onShowBookmarkingOptions(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
    print("InternalOutgoingInterface received bookmark mode start");
    _popupButtons.open(clientX, clientY, pageX, pageY, jsonButtons);
  }
  
  void onMetaNodeClicked(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
    print("InternalOutgoingInterface received meta click");
    _popupButtons.open(clientX, clientY, pageX, pageY, jsonButtons);
  }
  
  void onNoteEdit(String title, String text, String jsonButtons, String noteId) {
    List<StreamSubscription<MouseEvent>> subscriptions = new List();
    
    js.scoped((){
      js.context.unregisterStreamSubscriptions = new js.Callback.once((){
        for (StreamSubscription<MouseEvent> s in subscriptions) {
          s.cancel();
        }
      });
    });
    
    List<Map<String,String>> btnDetailsList = json.parse(jsonButtons);
    StringBuffer btnHtml = new StringBuffer();
    for (Map<String,String> btnDetails in btnDetailsList) {
      String id = btnDetails['id'];
      String name = btnDetails['name'];
      String msg = btnDetails['msg'];
      String extra = btnDetails['extra'];
      String onClick = "{"
          "window.console.log('btn clicked ${id}');"
          "var textVal = \$jquery('#ebook_system_note_text')[0].value;"
          "\$jquery().w2popup('close');"
          //"unregisterStreamSubscriptions();"
          "var param = {extra:'${extra}', text:textVal};"
          "selectionClickResult('${id}',JSON.stringify(param));"
        "}";
      String style = "height:2em; width:10em";
      String html = '''
        <button id="${id}" title="${msg}" onclick="${onClick}" style="${style}">
          ${name}
        </button>
      ''';
      btnHtml.writeln(html);
    }

    js.scoped((){
      var options = js.map({
        "title" : title,
        "body" : '''
        <div style="width:100%; height:100%">
          <textarea id="ebook_system_note_text" style="width:100%; height:100%">${text}</textarea>
        </div>
      ''',
        "buttons" : btnHtml.toString(),
        "modal" : true,
        "showClose" : false,
        "showMax" : true,
        "onClose" : "unregisterStreamSubscriptions()",
      });
      js.context.$jquery().w2popup(options);
      
      void disableMouseUpDown(Element e) {
        subscriptions.add(e.onMouseDown.listen((MouseEvent e){
          e.stopImmediatePropagation();
          //e.preventDefault();
        }));
        subscriptions.add(e.onMouseUp.listen((MouseEvent e){
          e.stopImmediatePropagation();
          //e.preventDefault();
        }));
      }
      
      Element lock = document.body.query("#w2ui-lock");
      if (lock!=null) {
        disableMouseUpDown(lock);
      } else {
        print("Error: Unable to find lock UI component");
      }
      Element popup = document.body.query("#w2ui-popup");
      if (popup!=null) {
        disableMouseUpDown(popup);
      } else {
        print("Error: Unable to find popup UI component");
      }
    });
  }
  
  void onTextCopy(String text) {
    
  }
  
  void scrollToLocation(double pageY) {
    //TODO: Find how to fix this.
    //window.screenY = pageY.toInt();
  }
  
  void onErrorMsg(String msg, String loc, bool quit) {
    print("Error: $msg");
    print("Loc: $loc");
  }
}

class AndroidOutgoingInterface extends OutgoingInterface {
  
  final Element _root;
  
  AndroidOutgoingInterface(Element this._root) {
    js.scoped((){
      js.context.androidInterface.init();
    });
  }
  
//  void onLoadingError(String error) {
//    print("dart:AndroidOutgoingInterface.onLoadingError");
//    js.scoped((){
//      js.context.androidInterface.onLoadingError(error);
//    });
//  }

  void documentReady() {
    print("dart:AndroidOutgoingInterface.documentReady");
    js.scoped((){
      js.context.androidInterface.documentReady();
    });
  }
  
  void onSelectionStarted(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
    print("dart:AndroidOutgoingInterface.onSelectionStarted");
    js.scoped((){
      js.context.androidInterface.onSelectionStarted(clientX, clientY, pageX, pageY, jsonButtons);
    });
  }
  
  void onSelectionUpdated(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
    print("dart:AndroidOutgoingInterface.onSelectionUpdated");
    js.scoped((){
      js.context.androidInterface.onSelectionUpdated(clientX, clientY, pageX, pageY, jsonButtons);
    });
  }
  
  void onSelectionFinished() {
    print("dart:AndroidOutgoingInterface.onSelectionFinished");
    js.scoped((){
      js.context.androidInterface.onSelectionFinished();
    });    
  }
  
  void onShowBookmarkingOptions(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
    print("dart:AndroidOutgoingInterface.onShowBookmarkingOptions");
    js.scoped((){
      js.context.androidInterface.onShowBookmarkingOptions(clientX, clientY, pageX, pageY, jsonButtons);
    });
  }
  
  void onMetaNodeClicked(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
    print("dart:AndroidOutgoingInterface.onMetaNodeClicked");
    js.scoped((){
      js.context.androidInterface.onMetaNodeClicked(clientX, clientY, pageX, pageY, jsonButtons);
    });
  }
  
  void onNoteEdit(String title, String text, String jsonButtons, String noteId) {
    print("dart:AndroidOutgoingInterface.onNoteEdit");
    js.scoped((){
      js.context.androidInterface.onNoteEdit(title, text, jsonButtons, noteId);
    });
  }
  
  void onTextCopy(String text) {
    print("dart:AndroidOutgoingInterface.onTextCopy");
    js.scoped((){
      js.context.androidInterface.onTextCopy(text);
    });
  }
  
  void scrollToLocation(double pageY) {
    print("dart:AndroidOutgoingInterface.scrollToLocation");
    js.scoped((){
      js.context.androidInterface.scrollToLocation(pageY);
    });
  }

  void onErrorMsg(String msg, String loc, bool quit) {
    print("dart:AndroidOutgoingInterface.onErrorMsg");
    js.scoped((){
      js.context.androidInterface.onErrorMsg(msg, loc, quit);
    });
  }
  
}

