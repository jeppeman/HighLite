package com.jeppeman.highlite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface SQLiteRelationship {

    Class<?> table();
    String backReference();
    RelationshipType relType() default RelationshipType.OneToMany;
    LoadingType loadingType() default LoadingType.Lazy;

    enum RelationshipType {
        OneToOne,
        OneToMany
    }

    enum LoadingType {
        Lazy,
        Eager
    }
}
