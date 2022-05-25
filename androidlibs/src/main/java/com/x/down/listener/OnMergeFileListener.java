package com.x.down.listener;

import java.io.File;
import java.util.List;

public interface OnMergeFileListener {
    void onMerge(File file) throws Exception;

    void onM3u8Merge(File m3u8File, List<File> tsList) throws Exception;

    class IMPL implements OnMergeFileListener {

        @Override
        public void onMerge(File file) throws Exception {

        }

        @Override
        public void onM3u8Merge(File m3u8File, List<File> tsList) throws Exception {

        }
    }
}
