/**
 *
 */
package com.umranium.ebook.epub;

/**
 * @author Umran
 */
public class EbookException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 8966704303926260066L;

    /**
     *
     */
    public EbookException() {
    }

    /**
     * @param detailMessage
     */
    public EbookException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * @param throwable
     */
    public EbookException(Throwable throwable) {
        super(throwable);
    }

    /**
     * @param detailMessage
     * @param throwable
     */
    public EbookException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

}
