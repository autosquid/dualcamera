package com.zhangxaochen.opencamera;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.zhangxaochen.sensordataxml.NewSessionNode;

import android.R.id;
import android.nfc.Tag;
import android.util.Log;

@Root(name="collection-seq")
public class CollectionProjXml {
	@Element(name="collection-proj-name")
	private String projName;
	
	@Element(name="collection-proj-description")
	private String projDescription;
	
	@Element(name="collection-count")
	private int collectionCnt=0;
	
	@Element(name="collections")
	private CollectionsNode collectionsNode=new CollectionsNode();
	
	//------------getter and setter
	public String getProjName() {
		return projName;
	}

	public void setProjName(String projName) {
		this.projName = projName;
	}

	public String getProjDescription() {
		return projDescription;
	}

	public void setProjDescription(String projDescription) {
		this.projDescription = projDescription;
	}

	public int getCollectionCnt() {
		return collectionCnt;
	}

	public void setCollectionCnt(int collectionCnt) {
		this.collectionCnt = collectionCnt;
	}

	public CollectionsNode getCollectionsNode() {
		return collectionsNode;
	}

	public void setCollectionsNode(CollectionsNode collectionsNode) {
		this.collectionsNode = collectionsNode;
	}
	
	void collectionCntPlusOne(){
		setCollectionCnt(getCollectionCnt()+1);
	}
}//CollectionProjXml

class CollectionsNode{
	private static final String TAG = "CollectionsNode";
	
	@ElementList(entry="collection", inline=true)
	List<CollectionNode> collectionList=new ArrayList<CollectionNode>();
}//CollectionsNode

class CollectionNode{
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

class PicsNode{
	@ElementList(entry="pic", inline=true)
	List<PicNode> picList=new ArrayList<PicNode>();
}

class PicNode{
	@Element(name="name")
	String picName;
	
	@Element(name="timestamp")
	double timeStamp;
}