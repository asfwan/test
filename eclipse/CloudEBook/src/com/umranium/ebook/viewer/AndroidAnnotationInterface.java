package com.umranium.ebook.viewer;

import android.util.Log;

public interface AndroidAnnotationInterface {

    /**
     * @return The side on which the side-panel is placed, 0 for left, 1 for right.
     */
    int annotationCache_getSidePanelSide();

    /**
     * @return Whether or not the cache has any annotations.
     */
    boolean annotationCache_hasAnnotations();

    /**
     * @return Newly created annotation ID.
     */
    String annotationCache_createNewAnnotationId();

    /**
     * @param annId   Annotation ID.
     * @param summary Summary text for this annotation.
     */
    void annotationCache_createNewAnnotation(String annId, String summary);

    /**
     * @return List of annotation IDs, expressed in a JSON string.
     */
    String annotationCache_getAnnotations();

    /**
     * Remove all annotations in the cache.
     */
    void annotationCache_removeAllAnnotations();

    /**
     * @return Annotation ID that has the mark ID given, or NULL if none exists.
     */
    String annotationCache_annotationWithMark(String markId);

    /**
     * Clean the cache, removing any annotations that have no marks.
     */
    void annotationCache_cleanAnnotations();

    /**
     * @param jsonMarkIdList List of mark IDs, expressed in JSON string.
     * @param newMarkId      New mark ID.
     */
    void annotationCache_marksReplaced(String jsonMarkIdList, String newMarkId);

    /**
     * @param jsonMarkIdList List of mark IDs, expressed in JSON string.
     */
    void annotationCache_marksDeleted(String jsonMarkIdList);

    /**
     * @return Newly created mark ID.
     */
    String annotationCache_createNewMarkId();

    /**
     * @return Newly created bookmark ID.
     */
    String annotationCache_createNewBookmarkId();

    /**
     * @param bookmarkId Bookmark ID
     * @param summary    Summary text for this bookmark.
     * @param pointJson  JSON containing the point reference for this bookmark.
     */
    void annotationCache_newBookmark(String bookmarkId, String summary, String pointJson);

    /**
     * @param bookmarkId ID of bookmark to delete.
     */
    void annotationCache_deleteBookmark(String bookmarkId);

    /**
     * @return JSON list of all available bookmark IDs.
     */
    String annotationCache_getBookmarks();

    /**
     * @param bookmarkId ID of bookmark required.
     * @return JSON containing the point reference of this bookmark.
     */
    String annotationCache_getBookmark(String bookmarkId);

    /**
     * @param annId Annotation ID.
     * @return The summary string of this annotation.
     */
    String cachedAnnotation_getSummary(String annId);

    /**
     * @param annId Annotation ID.
     * @return Whether or not the annotation has a note.
     */
    boolean cachedAnnotation_hasNote(String annId);

    /**
     * @param annId Annotation ID.
     * @return The note belonging to the given annotation.
     */
    String cachedAnnotation_getNote(String annId);

    /**
     * @param annId Annotation ID.
     * @param note  The text for the given note.
     */
    void cachedAnnotation_setNote(String annId, String note);

    /**
     * @param annId Annotation ID.
     * @return Whether or not the annotation has any marks.
     */
    boolean cachedAnnotation_hasMarks(String annId);

    /**
     * @param annId  Annotation ID.
     * @param markId Mark ID.
     * @return Whether or not the given mark exists in the given annotation.
     */
    boolean cachedAnnotation_hasMark(String annId, String markId);

    /**
     * @param annId  Annotation ID.
     * @param markId Mark ID.
     * @return The JSON string representation of the mark of the given mark-ID
     * belonging to the given annotation.
     */
    String cachedAnnotation_getMark(String annId, String markId);

    /**
     * @param annId Annotation ID.
     * @return A List of mark IDs, belonging to the given annotation,
     *         expressed as a JSON string.
     */
    String cachedAnnotation_getMarks(String annId);

    /**
     * Called when a new mark has been created.
     *
     * @param annId       Annotation ID.
     * @param markId      Mark ID.
     * @param markJsonStr A JSON string representing the mark newly added.
     */
    void cachedAnnotation_markAdded(String annId, String markId, String markJsonStr);

    /**
     * @param annId  Annotation ID.
     * @param markId Mark ID.
     */
    void cachedAnnotation_removeMark(String annId, String markId);

    /**
     * @param dstAnnId The destination annotation to copy the mark to.
     * @param markId   The mark to be copied.
     * @param srcAnnId The source annotation to copy the mark from.
     */
    void cachedAnnotation_copyMark(String dstAnnId, String markId, String srcAnnId);

    public static class AndroidAnnoationInterfaceExceptionWrapper implements AndroidAnnotationInterface {

        private String tag;
        private AndroidAnnotationInterface annotationInterface;

        public AndroidAnnoationInterfaceExceptionWrapper(String tag, AndroidAnnotationInterface annotationInterface) {
            this.tag = tag;
            this.annotationInterface = annotationInterface;
        }

        /**
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_getSidePanelSide()
         */
        public int annotationCache_getSidePanelSide() {
            String call = String.format("annotationCache_getSidePanelSide()");
            Log.d(tag, call);
            try {
                int res = annotationInterface.annotationCache_getSidePanelSide();
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_hasAnnotations()
         */
        public boolean annotationCache_hasAnnotations() {
            String call = String.format("annotationCache_hasAnnotations()");
            Log.d(tag, call);
            try {
                boolean res = annotationInterface.annotationCache_hasAnnotations();
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_createNewAnnotationId()
         */
        public String annotationCache_createNewAnnotationId() {
            String call = String.format("annotationCache_createNewAnnotationId()");
            Log.d(tag, call);
            try {
                String res = annotationInterface.annotationCache_createNewAnnotationId();
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public void annotationCache_createNewAnnotation(String annId, String summary) {
            String call = String.format("annotationCache_createNewAnnotation(%s, %s)",
                    annId, summary);
            Log.d(tag, call);
            try {
                annotationInterface.annotationCache_createNewAnnotation(annId, summary);
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_getAnnotations()
         */
        public String annotationCache_getAnnotations() {
            String call = String.format("annotationCache_getAnnotations()");
            Log.d(tag, call);
            try {
                String res = annotationInterface.annotationCache_getAnnotations();
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_removeAllAnnotations()
         */
        public void annotationCache_removeAllAnnotations() {
            String call = String.format("annotationCache_removeAllAnnotations()");
            Log.d(tag, call);
            try {
                annotationInterface.annotationCache_removeAllAnnotations();
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param markId
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_annotationWithMark(java.lang.String)
         */
        public String annotationCache_annotationWithMark(String markId) {
            String call = String.format("annotationCache_annotationWithMark(%s)",
                    markId);
            Log.d(tag, call);
            try {
                String res = annotationInterface.annotationCache_annotationWithMark(markId);
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_cleanAnnotations()
         */
        public void annotationCache_cleanAnnotations() {
            String call = String.format("annotationCache_cleanAnnotations()");
            Log.d(tag, call);
            try {
                annotationInterface.annotationCache_cleanAnnotations();
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @param jsonMarkIdList
         * @param newMarkId
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_marksReplaced(java.lang.String, java.lang.String)
         */
        public void annotationCache_marksReplaced(String jsonMarkIdList,
                                                  String newMarkId) {
            String call = String.format("annotationCache_marksReplaced(%s, %s)",
                    jsonMarkIdList, newMarkId);
            Log.d(tag, call);
            try {
                annotationInterface.annotationCache_marksReplaced(jsonMarkIdList,
                        newMarkId);
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @param jsonMarkIdList
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_marksDeleted(java.lang.String)
         */
        public void annotationCache_marksDeleted(String jsonMarkIdList) {
            String call = String.format("annotationCache_marksDeleted(%s)",
                    jsonMarkIdList);
            Log.d(tag, call);
            try {
                annotationInterface.annotationCache_marksDeleted(jsonMarkIdList);
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_createNewMarkId()
         */
        public String annotationCache_createNewMarkId() {
            String call = String.format("annotationCache_createNewMarkId()");
            Log.d(tag, call);
            try {
                String res = annotationInterface.annotationCache_createNewMarkId();
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param annId
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_hasNote(java.lang.String)
         */
        public boolean cachedAnnotation_hasNote(String annId) {
            String call = String.format("cachedAnnotation_hasNote(%s)",
                    annId);
            Log.d(tag, call);
            try {
                boolean res = annotationInterface.cachedAnnotation_hasNote(annId);
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param annId
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_getNote(java.lang.String)
         */
        public String cachedAnnotation_getNote(String annId) {
            String call = String.format("cachedAnnotation_getNote(%s)",
                    annId);
            Log.d(tag, call);
            try {
                String res = annotationInterface.cachedAnnotation_getNote(annId);
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param annId
         * @param note
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_setNote(java.lang.String, java.lang.String)
         */
        public void cachedAnnotation_setNote(String annId, String note) {
            String call = String.format("cachedAnnotation_setNote(%s, %s)",
                    annId, note);
            Log.d(tag, call);
            try {
                annotationInterface.cachedAnnotation_setNote(annId, note);
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @param annId
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_hasMarks(java.lang.String)
         */
        public boolean cachedAnnotation_hasMarks(String annId) {
            String call = String.format("cachedAnnotation_hasMarks(%s)",
                    annId);
            Log.d(tag, call);
            try {
                boolean res = annotationInterface.cachedAnnotation_hasMarks(annId);
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param annId
         * @param markId
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_hasMark(java.lang.String, java.lang.String)
         */
        public boolean cachedAnnotation_hasMark(String annId, String markId) {
            String call = String.format("cachedAnnotation_hasMark(%s, %s)",
                    annId, markId);
            Log.d(tag, call);
            try {
                boolean res = annotationInterface.cachedAnnotation_hasMark(annId, markId);
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param annId
         * @param markId
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_getMark(java.lang.String, java.lang.String)
         */
        public String cachedAnnotation_getMark(String annId, String markId) {
            String call = String.format("cachedAnnotation_getMark(%s, %s)",
                    annId, markId);
            Log.d(tag, call);
            try {
                String res = annotationInterface.cachedAnnotation_getMark(annId, markId);
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param annId
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_getMarks(java.lang.String)
         */
        public String cachedAnnotation_getMarks(String annId) {
            String call = String.format("cachedAnnotation_getMarks(%s)",
                    annId);
            Log.d(tag, call);
            try {
                String res = annotationInterface.cachedAnnotation_getMarks(annId);
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param annId
         * @param markId
         * @param markJsonStr
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_markAdded(java.lang.String, java.lang.String, java.lang.String)
         */
        public void cachedAnnotation_markAdded(String annId, String markId,
                                               String markJsonStr) {
            String call = String.format("cachedAnnotation_markAdded(%s, %s, %s)",
                    annId, markId, markJsonStr);
            Log.d(tag, call);
            try {
                annotationInterface.cachedAnnotation_markAdded(annId, markId,
                        markJsonStr);
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @param annId
         * @param markId
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_removeMark(java.lang.String, java.lang.String)
         */
        public void cachedAnnotation_removeMark(String annId, String markId) {
            String call = String.format("cachedAnnotation_removeMark(%s, %s)",
                    annId, markId);
            Log.d(tag, call);
            try {
                annotationInterface.cachedAnnotation_removeMark(annId, markId);
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @param dstAnnId
         * @param markId
         * @param srcAnnId
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_copyMark(java.lang.String, java.lang.String, java.lang.String)
         */
        public void cachedAnnotation_copyMark(String dstAnnId, String markId,
                                              String srcAnnId) {
            String call = String.format("cachedAnnotation_copyMark(%s, %s, %s)",
                    dstAnnId, markId, srcAnnId);
            Log.d(tag, call);
            try {
                annotationInterface.cachedAnnotation_copyMark(dstAnnId, markId,
                        srcAnnId);
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_createNewBookmarkId()
         */
        public String annotationCache_createNewBookmarkId() {
            String call = String.format("annotationCache_createNewBookmarkId()");
            Log.d(tag, call);
            try {
                String res = annotationInterface.annotationCache_createNewBookmarkId();
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param bookmarkId
         * @param summary
         * @param pointJson
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_newBookmark(java.lang.String, java.lang.String, java.lang.String)
         */
        public void annotationCache_newBookmark(String bookmarkId,
                                                String summary, String pointJson) {
            String call = String.format("annotationCache_newBookmark(%s, %s, %s)",
                    bookmarkId, summary, pointJson);
            Log.d(tag, call);
            try {
                annotationInterface.annotationCache_newBookmark(bookmarkId,
                        summary, pointJson);
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @param bookmarkId
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_deleteBookmark(java.lang.String)
         */
        public void annotationCache_deleteBookmark(String bookmarkId) {
            String call = String.format("annotationCache_deleteBookmark(%s)",
                    bookmarkId);
            Log.d(tag, call);
            try {
                annotationInterface.annotationCache_deleteBookmark(bookmarkId);
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
            }
        }

        /**
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_getBookmarks()
         */
        public String annotationCache_getBookmarks() {
            String call = String.format("annotationCache_getBookmarks()");
            Log.d(tag, call);
            try {
                String res = annotationInterface.annotationCache_getBookmarks();
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param bookmarkId
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#annotationCache_getBookmark(java.lang.String)
         */
        public String annotationCache_getBookmark(String bookmarkId) {
            String call = String.format("annotationCache_getBookmark(%s)", bookmarkId);
            Log.d(tag, call);
            try {
                String res = annotationInterface.annotationCache_getBookmark(bookmarkId);
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * @param annId
         * @return
         * @see com.umranium.ebook.viewer.AndroidAnnotationInterface#cachedAnnotation_getSummary(java.lang.String)
         */
        public String cachedAnnotation_getSummary(String annId) {
            String call = String.format("cachedAnnotation_getSummary(%s)", annId);
            Log.d(tag, call);
            try {
                String res = annotationInterface.cachedAnnotation_getSummary(annId);
                Log.d(tag, "\tresult = " + res);
                return res;
            } catch (Exception e) {
                Log.e(tag, "Error while processing: " + call, e);
                throw new RuntimeException(e);
            }
        }

    }
}
