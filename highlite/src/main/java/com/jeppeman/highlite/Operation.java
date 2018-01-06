package com.jeppeman.highlite;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface Operation<TCollection, TBase> {
    Flowable<TCollection> asFlowable(BackpressureStrategy strategy);
    Observable<TCollection> asObservable();
    Single<TBase> asSingle();
    Maybe<TBase> asMaybe();
    Completable asCompletable();
}
