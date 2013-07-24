package com.umranium.ebook.viewer;

public interface SelectionJavascriptInterface {

    /**
     * Informs the listener that there was a javascript error.
     *
     * @param range
     */
    public abstract void jsError(String error);

    /**
     * Tells the listener to update the locations of the handles.
     * The bounds parameter contains a json string representing the selection bounds in the form
     * { 'left': leftPoint, 'top': topPoint, 'right': rightPoint, 'bottom': bottomPoint }
     *
     * @param handleBounds
     * @param menuBounds
     */
    public abstract void updateHandles(String handleBounds, String menuBounds);

    /**
     * Sends the content width to the listener.
     * Necessary because Android web views don't allow you to get the content width.
     *
     * @param contentWidth
     */
    public abstract void setContentWidth(float contentWidth);

}
