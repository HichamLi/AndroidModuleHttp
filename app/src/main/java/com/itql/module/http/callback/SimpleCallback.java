package com.itql.module.http.callback;

import com.itql.module.http.HttpCall;

public class SimpleCallback implements IHttpCallback, IProgressCallback.download, IProgressCallback.upload {
    @Override
    public void onUpload(long total, long current, boolean complete) {

    }

    @Override
    public void onDownload(long total, long current, boolean complete) {

    }

    @Override
    public void onStart(HttpCall call) {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onSuccess(String s) {

    }
}
