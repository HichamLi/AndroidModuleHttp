package com.itql.module.http;

import okhttp3.Call;

public class HttpCall {
    private Call mCall;

    public HttpCall() {
    }

    public HttpCall(Call call) {
        mCall = call;
    }

    public Call getCall() {
        return mCall;
    }

    public void setCall(Call call) {
        mCall = call;
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
