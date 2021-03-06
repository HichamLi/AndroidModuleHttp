package com.itql.module.http;

import java.util.Map;
import java.util.TreeMap;

public class HttpTask {
    private String mUrl;
    private Map<String, Object> mGetParamMap;
    private Map<String, Object> mPostParamMap;
    private String mRawData;
    private Object mTag;

    public HttpTask(Builder builder) {
        mUrl = builder.mUrl;
        mGetParamMap = builder.mGetParamMap;
        mPostParamMap = builder.mPostParamMap;
        mRawData = builder.mRawData;
        mTag = builder.mTag;
    }

    public String getUrl() {
        return mUrl;
    }

    public Map<String, Object> getGetParamMap() {
        return mGetParamMap;
    }

    public Map<String, Object> getPostParamMap() {
        return mPostParamMap;
    }

    public String getRawData() {
        return mRawData;
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public static class Builder {
        private String mUrl;
        private Map<String, Object> mGetParamMap;
        private Map<String, Object> mPostParamMap;
        private String mRawData;
        private Object mTag;

        public Builder(String url) {
            mUrl = url;
        }

        public Builder setGetParam(Map<String, Object> map) {
            mGetParamMap = map;
            return this;
        }

        public Builder addGetParam(String key, Object value) {
            if (mGetParamMap == null) mGetParamMap = new TreeMap<>();
            mGetParamMap.put(key, value);
            return this;
        }

        public Builder setPostParam(Map<String, Object> map) {
            mPostParamMap = map;
            return this;
        }

        public Builder addPostParam(String key, Object value) {
            if (mPostParamMap == null) mPostParamMap = new TreeMap<>();
            mPostParamMap.put(key, value);
            return this;
        }

        public Builder setRawData(String data) {
            mRawData = data;
            return this;
        }

        public Builder setTag(Object tag) {
            mTag = tag;
            return this;
        }

        public HttpTask build() {
            return new HttpTask(this);
        }
    }
}
