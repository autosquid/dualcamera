package com.mightu.opencamera;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

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
	
	public void collectionCntPlusOne(){
		setCollectionCnt(getCollectionCnt()+1);
	}
}//CollectionProjXml

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