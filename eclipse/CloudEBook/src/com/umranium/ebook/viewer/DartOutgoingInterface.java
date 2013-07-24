package com.umranium.ebook.viewer;

/**
 * Interface corresponding to the Dart OutgoingInterface class,
 * which is responsible for sending outgoing messages from dart,
 * to the android environment.
 *
 * @author umran
 */
public interface DartOutgoingInterface {

    void init();

//	void onLoadingError(String error);

    void documentReady();

    void onSelectionStarted(double clientX, double clientY, double pageX,
                            double pageY, String jsonButtons);

    void onSelectionUpdated(double clientX, double clientY, double pageX,
                            double pageY, String jsonButtons);

    void onSelectionFinished();

    void onShowBookmarkingOptions(double clientX, double clientY, double pageX,
                                  double pageY, String jsonButtons);

    void onMetaNodeClicked(double clientX, double clientY, double pageX,
                           double pageY, String jsonButtons);

    void onNoteEdit(String title, String text, String jsonButtons, String noteId);

    void onTextCopy(String text);

    void scrollToLocation(double pageY);

    void onErrorMsg(String msg, String loc, boolean quit);

}
