package test;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.PipeAdvertisement;

public class PeerClient {
	 private PeerGroup netpg = null;// PeerGroup
	 private DiscoveryService disco; // 发现服务
	 private PipeService pipeSev; // 管道服务
	 private PipeAdvertisement pipeAdv = null;// 管道广告
	 private OutputPipe outputPipe;// 输入管道
	 public static void main(String[] args) {
	  Logger.getLogger("net.jxta").setLevel(Level.SEVERE);
	  PeerClient peer2 = new PeerClient();
	  peer2.launchJXTA();
	 }
	 private void launchJXTA() {
	  System.out.println("Lauching Peer into JXTA NetWork...");
	  try {
	   NetworkConfigurator config = new NetworkConfigurator();
	   config.setPrincipal("peer2");
	   config.setPassword("888888888");
	   config.save();
	   netpg = new NetPeerGroupFactory().getInterface();
	  } catch (Exception e) {
	   System.out.println("Unable to create PeerGroup - Failure");
	   System.exit(-1);
	  }
	  startClient();
	 }
	 private void startClient() {
	  System.out.println("搜索管道广告....");
	  disco = netpg.getDiscoveryService();//获取NetGroup发现服务
	  pipeSev = netpg.getPipeService();//获取NetGroup管道服务
	  Enumeration en;
	  while (true) {
	   try {
	    en = disco.getLocalAdvertisements(DiscoveryService.ADV, "Name",
	      "Pipe");// 本地发现广告，后面对应了广告中Name标签，值为Pipe的管道广告
	    if ((en != null) && en.hasMoreElements()) {
	     break;
	    }
	    disco.getRemoteAdvertisements(null, DiscoveryService.ADV,
	      "Name", "Pipe", 1, null);// 远程发现广告
	    try {
	     Thread.sleep(2000);
	    } catch (InterruptedException e) {
	     e.printStackTrace();
	    }
	   } catch (IOException e) {
	    e.printStackTrace();
	   }
	   System.out.print(".");
	  }
	  System.out.println("已经找到管道广告.......");
	  pipeAdv = (PipeAdvertisement) en
	  .nextElement();
	  if (null == pipeAdv) {
	   System.out.println("没有找到管道广告");
	  }
	  try {
	   outputPipe = pipeSev.createOutputPipe(pipeAdv, 10000);// 创建输出管道，其实是连接Peer1中的输入管道
	  } catch (IOException e) {
	   e.printStackTrace();
	  }
	  String data = "你是谁";// 我们要发送的信息内容
	  Message msg = new Message();
	  StringMessageElement sme = new StringMessageElement("DataTag", data,
	    null);
	  msg.addMessageElement(null, sme);
	  try {
	   outputPipe.send(msg);// 发送信息
	   System.out.println("信息 \"" + data
	     + "\" 已经发送");
	  } catch (IOException e) {
	   e.printStackTrace();
	   System.out
	     .println("发送信息失败！！");
	  }
	 }
}
