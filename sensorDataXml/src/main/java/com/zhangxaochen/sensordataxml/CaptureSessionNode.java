package com.zhangxaochen.sensordataxml;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.example.mysensorlistener.MySensorListener;


@Root(name = "CaptureSession")
public class CaptureSessionNode extends XmlRootNode{

	/**
	 * 似乎一个 CaptureSession 只有一个 Nodes 节点？
	 */
	@Element(name = "Nodes")
	private NodesNode nodes = new NodesNode();

	public NodesNode getNodes() {
		return nodes;
	}

	@Deprecated
	public void dataToObj() {
		// ...
	}

	@Override
	public void addNode(MySensorListener.MySensorData sensorData) {
		LinkedList<float[]> abuf = sensorData.getAbuf();
		LinkedList<float[]> gbuf = sensorData.getGbuf();
		LinkedList<float[]> mbuf = sensorData.getMbuf();
		LinkedList<float[]> rbuf = sensorData.getRbuf();
//		LinkedList<Long> tsbuf=sensorData.getTbuf();

		LinkedList<Double> aTsBuf=sensorData.getATsBuf();
		LinkedList<Double> gTsBuf=sensorData.getGTsBuf();
		LinkedList<Double> mTsBuf=sensorData.getMTsBuf();
		LinkedList<Double> rTsBuf=sensorData.getRTsBuf();


		// 加一个 Node 节点：
		NodeNode node = new NodeNode();
		nodes.nodeList.add(node);

		int frames = abuf.size(); // //反正各个 buf.size 一样
		node.setFrames(frames);

		for (int i = 0; i < frames; i++) {
			DataNode dataNode = new DataNode();
			node.datas.add(dataNode);

			//设置时间戳：
//			dataNode.setTs(tsbuf.poll());

			float[] tuple;
			tuple = abuf.poll();
			if (tuple != null) {
				dataNode.setAx(tuple[0]);
				dataNode.setAy(tuple[1]);
				dataNode.setAz(tuple[2]);
			}else{
				System.out.println("abuf!!!!!"+i);
			}

			tuple = gbuf.poll();
			if (tuple != null) {
				dataNode.setGx(tuple[0]);
				dataNode.setGy(tuple[1]);
				dataNode.setGz(tuple[2]);
			}else{
				System.out.println("gbuf!!!!!"+i);
			}

			tuple = mbuf.poll();
			if (tuple != null) {
				dataNode.setMx(tuple[0]);
				dataNode.setMy(tuple[1]);
				dataNode.setMz(tuple[2]);
			}else{
				System.out.println("mbuf!!!!!"+i);
			}

			tuple = rbuf.poll();
			if (tuple != null) {
				float x=tuple[0],
						y=tuple[1],
						z=tuple[2];
				float w=(float) Math.sqrt(1-x*x-y*y-z*z);

				dataNode.setRw(w);
				dataNode.setRx(x);
				dataNode.setRy(y);
				dataNode.setRz(z);
//				if (tuple.length == 4)
//					dataNode.setRw(tuple[3]);
			}else{
				System.out.println("rbuf!!!!!"+i);
			}
		}// for
	}// addNode

	/**
	 * 清掉 CaptureSesson 内容
	 */
	public void clearAllNodes() {
		nodes.nodeList.clear();
	}
}// CaptureSessionNode

class NodesNode {

	/**
	 * 对应 Nodes 节点下的 Node 节点列表， 采样一次得到一个 Node
	 */
	@ElementList(entry = "Node", inline = true)
	List<NodeNode> nodeList = new ArrayList<NodeNode>();

}

class NodeNode {
	/**
	 * 默认值 1, 一个手机当做整体，一般就是 1
	 */
	@Attribute
	private long phyId = 1;

	/**
	 * 每次采样得到的数据帧数
	 */
	@Attribute
	private int frames = 0;

	/**
	 * 对应 xml 文件中的 Data 节点列表
	 */
	@ElementList(entry = "Data", inline = true)
	List<DataNode> datas = new ArrayList<DataNode>();

	public long getPhyId() {
		return phyId;
	}

	public int getFrames() {
		return frames;
	}

	public void setFrames(int frames) {
		this.frames = frames;
	}

	public void setPhyId(long phyId) {
		this.phyId = phyId;
	}

}

/**
 * @author zhangxaochen
 *
 * @hint G 是 gyroscope, A 是 acc， R 暂时按 rotation vector 取值（不知与 type_orientation
 *       怎么区分？）M 是 magnetic field
 *
 *
 */
class DataNode {
	private static int defaultValue=0;

	@Attribute
	private float Gx = defaultValue;
	@Attribute
	private float Gy = defaultValue;
	@Attribute
	private float Gz = defaultValue;

	@Attribute
	private float Mx = defaultValue;
	@Attribute
	private float My = defaultValue;
	@Attribute
	private float Mz = defaultValue;

	@Attribute
	private float Ax = defaultValue;
	@Attribute
	private float Ay = defaultValue;
	@Attribute
	private float Az = defaultValue;

	@Attribute
	private float Rw = defaultValue;
	@Attribute
	private float Rx = defaultValue;
	@Attribute
	private float Ry = defaultValue;
	@Attribute
	private float Rz = defaultValue;

	@Attribute
	private long timestamp=defaultValue;

	public float getGx() {
		return Gx;
	}

	public void setGx(float gx) {
		Gx = gx;
	}

	public float getGy() {
		return Gy;
	}

	public void setGy(float gy) {
		Gy = gy;
	}

	public float getGz() {
		return Gz;
	}

	public void setGz(float gz) {
		Gz = gz;
	}

	public float getMx() {
		return Mx;
	}

	public void setMx(float mx) {
		Mx = mx;
	}

	public float getMy() {
		return My;
	}

	public void setMy(float my) {
		My = my;
	}

	public float getMz() {
		return Mz;
	}

	public void setMz(float mz) {
		Mz = mz;
	}

	public float getAx() {
		return Ax;
	}

	public void setAx(float ax) {
		Ax = ax;
	}

	public float getAy() {
		return Ay;
	}

	public void setAy(float ay) {
		Ay = ay;
	}

	public float getAz() {
		return Az;
	}

	public void setAz(float az) {
		Az = az;
	}

	public float getRw() {
		return Rw;
	}

	public void setRw(float rw) {
		Rw = rw;
	}

	public float getRx() {
		return Rx;
	}

	public void setRx(float rx) {
		Rx = rx;
	}

	public float getRy() {
		return Ry;
	}

	public void setRy(float ry) {
		Ry = ry;
	}

	public float getRz() {
		return Rz;
	}

	public void setRz(float rz) {
		Rz = rz;
	}

	public long getTs() {
		return timestamp;
	}

	public void setTs(long ts) {
		this.timestamp = ts;
	}


}//DataNode