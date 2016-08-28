package com.jeppeman.sqliteprocessor;

import javax.lang.model.element.Element;

/**
 * @author jesper
 */
@SuppressWarnings("unused")
public class ProcessingException extends RuntimeException {

    private final Element mElement;

    ProcessingException(final Element element, final String message) {
        super(message);
        mElement = element;
    }

    ProcessingException(final Element element, final Throwable cause) {
        super(cause);
        mElement = element;
    }

    ProcessingException(final Element element, final String message, final Throwable cause) {
        super(message, cause);
        mElement = element;
    }

    Element getElement() {
        return mElement;
    }
}
