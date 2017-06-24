package com.mightu.opencamera;

import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.List;

public class CollectionsNode{
	private static final String TAG = "CollectionsNode";

	@ElementList(entry="collection", inline=true)
	public
    List<CollectionNode> collectionList=new ArrayList<CollectionNode>();
}//CollectionsNode
