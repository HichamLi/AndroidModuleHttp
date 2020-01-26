package com.itql.module.http;

import okhttp3.Call;

public class HttpCall {
    private Call mCall;
    private Object mTag;

    public HttpCall() {
    }

    public HttpCall(Call call) {
        mCall = call;
    }

    public HttpCall(Call call, Object tag) {
        mCall = call;
        mTag = tag;
    }


    public Call getCall() {
        return mCall;
    }

    public void setCall(Call call) {
        mCall = call;
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public void cancel() {
        try {
            if (mCall == null || mCall.isCanceled()) return;
            mCall.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
