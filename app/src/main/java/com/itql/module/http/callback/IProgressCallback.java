package com.itql.module.http.callback;

public interface IProgressCallback{
    interface download {
        void onDownload(long total, long current, boolean complete);
    }

    interface upload {
        void onUpload(long total, long current, boolean complete);
    }
}
