/**
 * 
 * Communication Interfaces:
 * 	ebookSelection : calls to ebook.dart.js (selection.dart)
 * 	TextSelection : calls to android
 * 	
 */

// Namespace
var android = new function android(){
	this.selection = new function selection() {
		this.selectionStartRange = null;
		this.selectionEndRange = null;
		
		/** The last point touched by the user. { 'x': xPoint, 'y': yPoint } */
		this.lastTouchPoint = null;
	};
};
	

/** 
 * Starts the touch and saves the given x and y coordinates as last touch point
 */
android.selection.onTouch = function onTouch(x, y){
	
	android.selection.lastTouchPoint = {'x': x, 'y': y};
	
};


/**
 *	Checks to see if there is a selection.
 *
 *	@return boolean
 */
android.selection.hasSelection = function hasSelection(){
	return window.getSelection().toString().length > 0;
};

/**
* Returns the first available range
*/
android.selection.getSelectionRange = function getSelectionRange() {
	try{
		// if current selection clear it.
	   	var sel = window.getSelection();
	   	
	   	if (!sel) {
	   		window.TextSelection.jsError("window.getSelection() returned null");
	   		return;
	   	}
	   	
	   	
	   	if (sel.rangeCount===0) {
	   		return null;
	   	}
	   	
	   	return sel.getRangeAt(0);
	} catch(err){
		window.TextSelection.jsError("android.selection.getSelectionRange");
		window.TextSelection.jsError(err.stack);
	}	
};


/**
 *	Clears the current selection.
 */
android.selection.clearSelection = function clearSelection(){
	
	try{
		// if current selection clear it.
	   	var sel = window.getSelection();
	   	
	   	if (!sel) {
	   		window.TextSelection.jsError("window.getSelection() returned null");
	   		return;
	   	}
	   	
	   	if (!sel.removeAllRanges) {
	   		window.TextSelection.jsError("window.getSelection() has no removeAllRanges() method.");
	   		return;
	   	}
	   	
   		sel.removeAllRanges();
	} catch(err){
		window.TextSelection.jsError("android.selection.clearSelection");
		window.TextSelection.jsError(err.stack);
	}	
};


/**
 *	Handles the long touch action by selecting the last touched element.
 */
android.selection.longTouch = function longTouch() {
	try{
		var newSelection = true;
		
    	if (android.selection.hasSelection()===true) {
    		newSelection = false;
		}
		
		android.selection.clearSelection();
    	
	   	// if current selection clear it.
	   	var sel = window.getSelection();
	   	
	   	var oneWordCaret = document.caretRangeFromPoint(android.selection.lastTouchPoint.x, android.selection.lastTouchPoint.y);
	   	oneWordCaret.expand("word");
	   	
	   	sel.addRange(oneWordCaret);
	   	
	   	var range = sel.getRangeAt(0);
	   	
	   	//android.selection.saveSelectionStart();
	   	//android.selection.saveSelectionEnd();
	   	
		var startRange = document.createRange();
		startRange.setStart(range.startContainer, range.startOffset);
		android.selection.selectionStartRange = startRange;
		
		var endRange = document.createRange();
		endRange.setStart(range.endContainer, range.endOffset);
		android.selection.selectionEndRange = endRange;
	   	
	   	android.selection.setAppContentWidth();
	   	
	   	if (newSelection===true) {
	   		//	ebookSelection comes from Dart
	   		if (ebookSelection) {
	   			ebookSelection.startSelectionMode();
	   		}
	   	}
	   	
	   	android.selection.updateAppHandles();
	   	
	 } catch(err){
		window.TextSelection.jsError("android.selection.longTouch");
	 	window.TextSelection.jsError(err.stack);
	 }
};

/**
* Ends the selection
*/
android.selection.endSelection = function endSelection() {
	try{
		android.selection.clearSelection();
		// ebookSelection comes from Dart
   		if (ebookSelection) {
   			ebookSelection.endSelectionMode();
   		}
	} catch(err){
		window.TextSelection.jsError("android.selection.endSelection");
		window.TextSelection.jsError(err.stack);
	}	
};

/**
 * Sets the last caret position for the start handle.
 */
android.selection.setStartPos = function setStartPos(x, y, finalPos){
	
	try{
		android.selection.selectionStartRange = document.caretRangeFromPoint(x, y);
		
		android.selection.selectBetweenHandles(finalPos, finalPos);
		
		if (finalPos===true) {
			android.selection.updateAppHandles();
			
			// ebookSelection comes from Dart
	   		if (ebookSelection) {
	   			ebookSelection.updateSelection();
	   		}
		}
	}catch(err){
		window.TextSelection.jsError("android.selection.setStartPos");
		window.TextSelection.jsError(err.stack);
	}

};

/**
 * Sets the last caret position for the end handle.
 */
android.selection.setEndPos = function setEndPos(x, y, finalPos){
	
	try {
		android.selection.selectionEndRange = document.caretRangeFromPoint(x, y);
		
		android.selection.selectBetweenHandles(finalPos, finalPos);
		
		if (finalPos===true) {
			android.selection.updateAppHandles();
			
			//	ebookSelection comes from Dart
	   		if (ebookSelection) {
	   			ebookSelection.updateSelection();
	   		}
		}
	} catch(err) {
		window.TextSelection.jsError("android.selection.setEndPos");
		window.TextSelection.jsError(err.stack);
	}

};

/**
 * Tells the app to update handle after selection changes. 
 */
android.selection.setAppContentWidth = function setAppContentWidth() {
	try{
	   	// Set the content width
	   	window.TextSelection.setContentWidth(document.body.clientWidth);
	} catch(err){
		window.TextSelection.jsError("android.selection.updateAppContentWidth");
		window.TextSelection.jsError(err.stack);
	}
};

/**
 * Tells the app to update handle after selection changes. 
 */
android.selection.updateAppHandles = function updateAppHandles(){

	try{
		var sel = window.getSelection();
		if(!sel){
			return;
		}
		
		var range = sel.getRangeAt(0);
		
	   	var clientRects = range.getClientRects();
	   	if (clientRects===null) {
	   		throw new Error("range.getClientRects() returned null");
	   	} 
	   	var start = clientRects[0];
	   	var end = clientRects[clientRects.length-1];
	   	var scrX = window.scrollX;
	   	var scrY = window.scrollY;
	   	
	   	var handleBounds = "{'left': " + (start.left+scrX) + ", ";
	   	handleBounds += "'top': " + (start.top+scrY) + ", ";
	   	handleBounds += "'right': " + (end.right+scrX) + ", ";
	   	handleBounds += "'bottom': " + (end.bottom+scrY) + "}";
	   	
	   	// Menu bounds
	   	var rect = range.getBoundingClientRect();
	   	if (rect===null) {
	   		throw new Error("range.getBoundingClientRect() returned null");
	   	} 
	   	
	   	var menuBounds = "{'left': " + rect.left + ", ";
	   	menuBounds += "'top': " + rect.top + ", ";
	   	menuBounds += "'right': " + rect.right + ", ";
	   	menuBounds += "'bottom': " + rect.bottom + "}";
	   	
	   	// Rangy
	   	//var rangyRange = android.selection.getRange();
	   	
	   	// Text to send to the selection
	   	//var text = window.getSelection().toString();
	   	
	   	// Tell the interface that the selection changed
	   	//window.TextSelection.selectionChanged(rangyRange, text, handleBounds, menuBounds);
	   	window.TextSelection.updateHandles(handleBounds, menuBounds);
	} catch(err){
		window.TextSelection.jsError("android.selection.updateAppHandles");
		window.TextSelection.jsError(err.stack);
	}
};

/*
android.selection.saveSelectionStart = function saveSelectionStart(){
	try{
		// Save the starting point of the selection
	   	var sel = window.getSelection();
		var range = sel.getRangeAt(0);
		
		var saveRange = document.createRange();
		
		saveRange.setStart(range.startContainer, range.startOffset);
		
		android.selection.selectionStartRange = saveRange;
	}catch(err){
		window.TextSelection.jsError("android.selection.saveSelectionStart");
		window.TextSelection.jsError(err.stack);
	}

};

android.selection.saveSelectionEnd = function saveSelectionEnd(){

	try{

		// Save the end point of the selection
	   	var sel = window.getSelection();
		var range = sel.getRangeAt(0);
		
		var saveRange = document.createRange();
		saveRange.setStart(range.endContainer, range.endOffset);
		
		android.selection.selectionEndRange = saveRange;
	}catch(err){
		window.TextSelection.jsError("android.selection.saveSelectionEnd");
		window.TextSelection.jsError(err.stack);
	}
	
};
*/

/**
 *	Selects all content between the two handles
 */
android.selection.selectBetweenHandles = function selectBetweenHandles(expandToWord, storeHandles){
	
	try{
		var startCaret = android.selection.selectionStartRange;
		var endCaret = android.selection.selectionEndRange;
		
		// If we have two carets, update the selection
		if (startCaret && endCaret) {
		
			// If end caret comes before start caret, need to flip
			if(startCaret.compareBoundaryPoints(Range.START_TO_END, endCaret) > 0) {
				var temp = startCaret;
				startCaret = endCaret;
				endCaret = temp;
			}
			
			var range = document.createRange();
			range.setStart(startCaret.startContainer, startCaret.startOffset);
			range.setEnd(endCaret.startContainer, endCaret.startOffset);
			if (expandToWord===true) {
				range.expand("word");
				
				startCaret = document.createRange();
				startCaret.setStart(range.startContainer, range.startOffset);
				startCaret.setEnd(range.startContainer, range.startOffset);

				endCaret = document.createRange();
				endCaret.setStart(range.endContainer, range.endOffset);
				endCaret.setEnd(range.endContainer, range.endOffset);
			}
			
			if (storeHandles===true) {
				android.selection.selectionStartRange = startCaret;
				android.selection.selectionEndRange = endCaret;
			}
			
			android.selection.clearSelection();
			var selection = window.getSelection();
			selection.addRange(range);
			
			//console.log('selection updated to '+range);
		}
   	} catch(err){
		window.TextSelection.jsError("android.selection.selectBetweenHandles");
   		window.TextSelection.jsError(err.stack);
   	}
};



