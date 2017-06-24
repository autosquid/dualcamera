package com.mightu.opencamera.XMLTasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.mightu.opencamera.CollectionNode;
import com.mightu.opencamera.CollectionProjXml;
import com.mightu.opencamera.MainActivity;

import org.simpleframework.xml.core.Persister;

import java.io.File;

/**
 * Created by uuplusu on 24/06/2017.
 */
public class WriteConfXmlTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "WriteConfXmlTask";
    //	private String a;
    MainActivity _mainActivity = null;
    Persister _persister = null;
    File _confFile = null;
    String _dataXmlFname = null;


    //	public WriteConfXmlTask() {
//	}//WriteConfXmlTask
    public WriteConfXmlTask(Context ctx, Persister p, File confFile, String dataXmlFname) {
        super();
        _mainActivity = (MainActivity) ctx;
        _persister = p;
        _confFile = confFile;
        _dataXmlFname = dataXmlFname;
    }//WriteConfXmlTask

    @Override
    protected Void doInBackground(Void... params) {
        System.out.println("WriteConfXmlTask.doInBackground()");

        CollectionProjXml confXmlNode = new CollectionProjXml();

        try {
            // 如果先前创建过此工程， 读旧的：
            if (_confFile.exists()) {
                try {
                    confXmlNode = _persister.read(CollectionProjXml.class,
                            _confFile);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            String projName = _confFile.getName();
            confXmlNode.setProjName(projName);
            confXmlNode.setProjDescription("deprecated entry");
            confXmlNode.collectionCntPlusOne();
            Log.i(TAG,
                    "projName: " + projName + "," + confXmlNode.getProjName()
                            + "," + confXmlNode.getCollectionCnt() + ","
                            + _mainActivity._picNames.size());
            CollectionNode cNode = new CollectionNode();
            cNode.setSensorName(_dataXmlFname);
            cNode.addPicNodes(_mainActivity._picNames,
                    _mainActivity._picTimestamps);

            confXmlNode.getCollectionsNode().collectionList.add(cNode);

            _persister.write(confXmlNode, _confFile);

            //清空 _picNames, _picTimestamps, 防止影响下次采集：
            _mainActivity._picNames.clear();
            _mainActivity._picTimestamps.clear();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}//WriteConfXmlTask
