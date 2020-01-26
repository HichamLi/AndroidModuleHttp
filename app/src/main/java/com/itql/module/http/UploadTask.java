package com.itql.module.http;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UploadTask {
    private String mUrl;
    private Map<String, Object> mParamMap;
    private List<File> mFiles;
    private Object mTag;

    public UploadTask(Builder builder) {
        mUrl = builder.mUrl;
        mParamMap = builder.mParamMap;
        mFiles = builder.mFiles;
        mTag = builder.mTag;
    }

    public String getUrl() {
        return mUrl;
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public Map<String, Object> getParamMap() {
        return mParamMap;
    }

    public List<File> getFiles() {
        return mFiles;
    }

    public static class Builder {
        private String mUrl;
        private Map<String, Object> mParamMap;
        private List<File> mFiles;
        private Object mTag;

        public Builder() {
            mTag= UUID.randomUUID().toString();
        }

        public Builder(String url) {
            mUrl = url;
            mTag= UUID.randomUUID().toString();
        }

        public Builder setUrl(String url) {
            mUrl = url;
            return this;
        }

        public Builder setParam(Map<String, Object> map) {
            mParamMap = map;
            return this;
        }

        public Builder addParam(String key, Object value) {
            if (mParamMap == null) mParamMap = new HashMap<>();
            mParamMap.put(key, value);
            return this;
        }

        public Builder setFile(List<File> list) {
            mFiles = list;
            return this;
        }

        public Builder addFile(File file) {
            if (mFiles == null) mFiles = new ArrayList<>();
            mFiles.add(file);
            return this;
        }

        public Builder setTag(Object tag) {
            mTag = tag;
            return this;
        }

        public UploadTask build() {
            return new UploadTask(this);
        }
    }
}
