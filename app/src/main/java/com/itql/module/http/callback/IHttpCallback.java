package com.itql.module.http.callback;

import com.itql.module.http.HttpCall;

public interface IHttpCallback {
    void onStart(HttpCall call);

    void onSuccess(String s);

    void onError(Throwable throwable);
}
