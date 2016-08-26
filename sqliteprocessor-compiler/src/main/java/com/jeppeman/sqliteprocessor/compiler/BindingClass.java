package com.jeppeman.sqliteprocessor.compiler;

import com.squareup.javapoet.ClassName;

/**
 * Created by jesper on 2016-08-25.
 */
public class BindingClass {

    private ClassName mWrapperClass;

    BindingClass(final ClassName wrapperClass) {
        mWrapperClass = wrapperClass;
    }
}
