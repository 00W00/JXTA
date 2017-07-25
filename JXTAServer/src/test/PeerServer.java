package test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.id.UUID.PipeID;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.PipeAdvertisement;

public class PeerServer {
	private DiscoveryService discoveryService = null;
	 private PipeService pipeService = null;
	 private PeerGroup restoNet = null;
	 private PeerGroupID peerGroupID = null;
	 private InputPipe inputPipe = null;

	 public static void main(String[] args) {
	  Logger.getLogger("net.jxta").setLevel(Level.SEVERE);
	  PeerServer peer1 = new PeerServer();
	  peer1.launchJXTA();
	 }
	 private void launchJXTA() {
	  try {
	   NetworkConfigurator config = new NetworkConfigurator();//设置配置可以跳过第1次运行时出现的配置UI
	   config.setPrincipal("peer1");// Peer名称
	   config.setPassword("888888888");// Peer密码
	   config.save();
	   restoNet = new NetPeerGroupFactory().getInterface();
	   peerGroupID = (PeerGroupID) restoNet.getPeerGroupID();
	  } catch (Exception e) {
	   e.printStackTrace();
	   System.exit(-1);
	  }
	  discoveryService = restoNet.getDiscoveryService();// 取得NetPeerGroup的发现服务
	  pipeService = restoNet.getPipeService();// 取得NetPeerGroup的管道服务
	  startServer();// 开始启动
	 }
	 private void startServer() {
	  publishPipeAdvertisement();// 发布管道广告
	 }
	 //创建及发布管道广告
	 private void publishPipeAdvertisement() {
	  PipeAdvertisement pipeAdv = createPipeAdvertisement();
	  // -----------------以下这段代码只是为了把管道广告的内容打印出来--------------------------
	  StructuredTextDocument doc = (StructuredTextDocument) pipeAdv
	    .getDocument(MimeMediaType.XMLUTF8);
	  StringWriter out = new StringWriter();
	  try {
	   doc.sendToWriter(out);
	   System.out.println(out.toString());
	   out.close();
	  } catch (IOException e) {
	   e.printStackTrace();
	  }
	  // -----------------------------打印结束--------------------------------------------
	  try {
	   discoveryService.publish(pipeAdv);//本地发布管道广告
	   discoveryService.remotePublish(pipeAdv);//远程发布管道广告
	   inputPipe = pipeService.createInputPipe(pipeAdv);// 创建输入管道
	  } catch (IOException e) {
	   e.printStackTrace();
	  }
	  while (true) {
	   System.out.println("等待其它Peer端信息的到达.........");
	   Message msg;
	   try {
	    msg = inputPipe.waitForMessage();// 监听输入管道是否有信息传进来
	   } catch (InterruptedException e) {
	    inputPipe.close();
	    System.out.println("接收其它Peer信息出错！");
	    return;// 如果出现异常则返回
	   }
	   String receiveContent = null;
	   Message.ElementIterator en = msg.getMessageElements();// 取得到信息
	   if (!en.hasNext()) {
	    return;
	   }
	   MessageElement msgElement = msg.getMessageElement(null, "DataTag");
	   if (msgElement.toString() != null) {
	    receiveContent = msgElement.toString();
	   }
	   if (receiveContent != null) {
	    System.out.println("接收信息内容: "
	      + receiveContent);
	   } else {
	    System.out
	      .println("没有内容");
	   }
	  }
	 }
	 // 生成管道广告,在这里我们是直接从代码中生成管道广告，当然我们可以读管道广告文件
	 private PipeAdvertisement createPipeAdvertisement() {
	  PipeAdvertisement pipeAdvertisement = null;
	  pipeAdvertisement = (PipeAdvertisement) AdvertisementFactory
	    .newAdvertisement(PipeAdvertisement.getAdvertisementType());// 创建一个管道广告
	  pipeAdvertisement.setPipeID(createPipeID(peerGroupID));
	  pipeAdvertisement.setName("Pipe");
	  pipeAdvertisement.setDescription("JXTA create first pipe");
	  pipeAdvertisement.setType(PipeService.UnicastType);// 管道类型，管道类型在JXTA
	  return pipeAdvertisement;
	 }
	 // 生成管道ID
	 private PipeID createPipeID(PeerGroupID groupID) {
	  PipeID pipeID = null;
	  pipeID = (PipeID) IDFactory.newPipeID(groupID);
	  return pipeID;
	 }
}
