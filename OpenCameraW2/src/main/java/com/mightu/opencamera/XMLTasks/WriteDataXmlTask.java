package com.mightu.opencamera.XMLTasks;

import android.os.AsyncTask;

import com.mightu.opencamera.XmlRootNode;

import org.simpleframework.xml.core.Persister;

import java.io.File;

/**
 * 异步写xml文件
 */
public class WriteDataXmlTask extends AsyncTask<Void, Void, Void> {
    XmlRootNode _xmlRootNode;
    File _file;
    Persister _persister;

    public WriteDataXmlTask() {
    }

    public WriteDataXmlTask setXmlRootNode(XmlRootNode rootNode) {
        _xmlRootNode = rootNode;
        return this;
    }

    public WriteDataXmlTask setFile(File file) {
        _file = file;
        return this;
    }

    public WriteDataXmlTask setPersister(Persister persister) {
        _persister = persister;
        return this;
    }

    @Override
    protected Void doInBackground(Void... params) {
        System.out.println("doInBackground()");
        if (_xmlRootNode == null || _file == null || _persister == null) {
            System.out.println("_xmlRootNode==null || _file==null || _persister == null");
            return null;
        }

        try {
            //          _persister.write(_captureSessionNode, _file);
            _persister.write(_xmlRootNode, _file);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        System.out.println("onPostExecute");
        super.onPostExecute(result);

        //      _captureSessionNode.clearAllNodes();
        _xmlRootNode.clear();

    }

}//WriteDataXmlTask
