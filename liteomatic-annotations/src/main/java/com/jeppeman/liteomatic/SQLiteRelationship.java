package com.jeppeman.liteomatic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface SQLiteRelationship {

    RelationshipType type() default RelationshipType.OneToMany;

    enum RelationshipType {
        OneToOne,
        OneToMany
    }
}
