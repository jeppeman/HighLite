package com.jeppeman.sqliteprocessor;

/**
 * Created by jesper on 2016-10-07.
 */

public abstract class Statement {

    public abstract PreparedStatement prepareStatement();

    interface PreparedStatement {

    }
}
