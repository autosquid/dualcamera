package com.mightu.opencamera;

import android.util.Log;

import org.simpleframework.xml.Element;

import java.util.List;

public class CollectionNode{
	private static final String TAG = "CollectionNode";

	@Element(name="pic-count")
	int picCount=0;

	@Element(name="pics")
	PicsNode picsNode=new PicsNode();

//	@ElementList(entry="pic")
//	List<PicNode> picList=new ArrayList<PicNode>();

	@Element(name="sensor-name")
	String sensorName;

	public int getPicCount() {
		return picCount;
	}

	public void setPicCount(int picCount) {
		this.picCount = picCount;
	}

	public PicsNode getPicsNode() {
		return picsNode;
	}

	public void setPicsNode(PicsNode picsNode) {
		this.picsNode = picsNode;
	}

	public String getSensorName() {
		return sensorName;
	}

	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}

	public void addPicNodes(List<String> picNames, List<Double> timeStamps ){
//	public void addPicNodes(String[] picNames, float[] timeStamps) {
		this.setPicCount(picNames.size());

		//------add picsNode
		PicsNode picsNode=new PicsNode();
		for (int i = 0; i < picNames.size(); i++) {
			PicNode picNode=new PicNode();
			picNode.picName=picNames.get(i);
			picNode.timeStamp=timeStamps.get(i);
//			System.out.println("picNode.picName, picNode.timeStamp"+picNode.picName+","+picNode.timeStamp);
			Log.i(TAG, "picNode.picName, picNode.timeStamp"+picNode.picName+","+picNode.timeStamp);

			picsNode.picList.add(picNode);
		}

		this.setPicsNode(picsNode);
	}//addPicNodes
}
