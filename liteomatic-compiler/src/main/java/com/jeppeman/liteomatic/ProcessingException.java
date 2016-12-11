package com.jeppeman.liteomatic;

import javax.lang.model.element.Element;

/**
 * {@link RuntimeException} that holds a reference to the {@link Element} that caused the exception
 * so that the {@link LiteOmaticProcessor} can handle the error properly.
 *
 * @author jesper
 */
@SuppressWarnings("unused")
final class ProcessingException extends RuntimeException {

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
