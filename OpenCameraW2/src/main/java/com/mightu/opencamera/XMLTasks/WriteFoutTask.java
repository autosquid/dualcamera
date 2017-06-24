package com.mightu.opencamera.XMLTasks;

import android.os.AsyncTask;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 异步写普通数据文件
 */
class WriteFoutTask extends AsyncTask<Void, Void, Void> {
    FileOutputStream fout;
    byte[] data;

    @Override
    protected Void doInBackground(Void... params) {
        try {
            fout.write(data);
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public FileOutputStream getFout() {
        return fout;
    }

    public WriteFoutTask setFout(FileOutputStream fout) {
        this.fout = fout;
        return this;
    }

    public byte[] getData() {
        return data;
    }

    public WriteFoutTask setData(byte[] data) {
        this.data = data;
        return this;
    }
}//WriteFoutTask
