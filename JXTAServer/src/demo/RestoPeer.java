package demo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.endpoint.InputStreamMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.id.IDFactory;

import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

public class RestoPeer {
	private PeerGroup netpg = null;// 对等组
	private PeerGroup restoNet = null;

	private String brand = "Chez JXTA";
	private String specials = "large($3.00)";

	private int timeout = 3000;
	private int rtimeout = 8000;

	private DiscoveryService disco = null;
	private PipeService pipes = null;
	private PipeAdvertisement myAdv = null;
	private InputPipe pipeIn = null;

	//static String groupURL = "jxta:uuid-4d6172676572696e204272756e6f202002";

	private void startJxta() {
		try {
			// 加入默认对等组NetPeerGroup
			netpg = new NetPeerGroupFactory().getInterface();
			/**
			 * 此处需要注意的是，由于JXTA技术的不断更新，其类库结构和实
			 * 
			 * 现已发生很大变化，此处加入默认对等组是2.4.1版本中的新方法。
			 * 
			 * JXTA技术手册上的方法已过时。
			 */
		} catch (Exception e) {
			System.out.println("创建netPg失败");
			System.exit(-1);
		}
		try {
			joinRestoNet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!createRestoPipe()) {
			System.out
					.println("Aborting due to failure to create RestoPeer pipe");
			System.exit(1);
		}
		handleFriesRequest();
	}

	private void joinRestoNet() {
		int count = 3;// 试图发现的最高循环次数
		System.out.println("试图发现对等组");
		// 从NetPeerGroup获得发现服务
		DiscoveryService hdisco = netpg.getDiscoveryService();
		Enumeration ae = null;// 记录发现的广告。
		// 循环直到我们发现RestoNet对等组或是直到我们达到了试图预期发现的次数。
		while (count-- > 0) {
			try {
				// 第一次搜索对等体的本地缓存来查找RestoNet对等组通告。
				// 通过NetPeerGroup组提供的发现服务发现"Name"属性为"RestoNet"的对等组
				ae = hdisco.getLocalAdvertisements(DiscoveryService.GROUP,
						"Name", "RestoNet");
				// 如果发现RestoNet对等组通告，该方法完成，退出循环。
				if (ae != null && ae.hasMoreElements())
					break;
				// 如果我们没有在本地找到它，便发送发现远程请求。
				// 参数依次为要查找的对等体ID，为空时不以此为发现条件;发现的通告类型，取值还有PEER,和ADV;
				// 要发现的通告属性名称;属性取值;需获取的最大通告数量;发现监听器
				hdisco.getRemoteAdvertisements(null, DiscoveryService.GROUP,
						"Name", "RestoNet", 1, null);
				// 线程暂停一下等待对等体內该发现请求。
				try {
					Thread.sleep(timeout);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		/**
		 * 以上为循环发现目标组过程,以下为加入过程
		 */
		// 创建一个对等组通告引用
		PeerGroupAdvertisement restoNetAdv = null;
		// 检查我们是否找到RestoNet通告。如果没有找到，表示我们可能是第一个试图加入该组的对等体，
		if (ae == null || !ae.hasMoreElements()) {
			// 如果该组不在，给出提示信息，创建该组
			System.out
					.println("could not find RestoNet peergroup;creating one");
			try {
				// 创建一个新的对等组RestoNet,全能对等组
				// 通过NetPeerGroup获得一个一般对等组的通告。
				ModuleImplAdvertisement implAdv = netpg
						.getAllPurposePeerGroupImplAdvertisement();
				// 通过NetPeerGroup创建一个新的对等组，JXTA会自行发布该对等组通告，
				// 参数依次为对等组ID，通告，组名，描述
				restoNet =netpg;
				// 获得一个对等组通告
				restoNetAdv = netpg.getPeerGroupAdvertisement();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error in creating RestoNet Peergroup");
			}
		} else {
			try {
				// RestoNet通告在缓存内找到意味着我们可以加入这个存在的组
				// 在集合中提取一个对等组通告元素
				restoNetAdv = (PeerGroupAdvertisement) ae.nextElement();
				// 加入该对等组，由于该通告已经发布，JXTA不会再行发布。
				restoNet = netpg.newGroup(restoNetAdv);
				System.out.println("Found and join");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			disco = restoNet.getDiscoveryService();
			pipes = restoNet.getPipeService();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error getting services from RestoNet");
		}
		System.out.println("RestoNet Restaurant(" + brand + ")is on-line");
		return;
	}


	// 读取来自HungryPeer的请求,并向它们发送响应.这个方法说明了如何通过管道传递消息
	private void handleFriesRequest() {
		InputStream ip = null;
		PipeAdvertisement hungryPipe = null;
		StructuredDocument request = null;// Request document
		StructuredDocument bid = null;// Response document
		// Document mime types
		MimeMediaType mimeType = new MimeMediaType("text", "xml");
		Element el = null;
		String name = null;
		String size = null;
		OutputPipe pipeOut = null;
		System.out.println("RestoNet Restaurant(" + brand
				+ ")waiting for HungryPeer requests");
		// Loop waiting for HungryPeer requests
		while (true) {
			Message msg = null;// Incoming pipe message
			try {
				// Block until a message arrives on the RestoPeer pipe
				msg = pipeIn.waitForMessage();
				// If message is null,discard message
				if (msg == null) {
					if (Thread.interrupted()) {
						System.out.println("Abort:RestoPeer interrupted");
						return;
					}
				}
				// we received a message,extract the request
				try {
					// Extract the HungryPipe pipe information to reply to the
					// sender
					ip = msg.getMessageElement("HungryPeerPipe").getStream();
					// Construct the associated pipe advertisement via the
					// AdvertisementFactory
					hungryPipe = (PipeAdvertisement) AdvertisementFactory
							.newAdvertisement(mimeType, ip);
					// Extract the sender name and fries size requested,building
					// a StructuredDocument
					ip = msg.getMessageElement("Request").getStream();
					request = StructuredDocumentFactory.newStructuredDocument(
							mimeType, ip);
					Enumeration ae = request.getChildren();
					// Loop over all the elements of the document
					while (ae.hasMoreElements()) {
						el = (Element) ae.nextElement();
						String attr = (String) el.getKey();
						String value = (String) el.getValue();
						// Extract the HungryPeer requester name
						if (attr.equals("Name")) {
							name = value;
							continue;
						}
						// Extract the fries size requested
						else if (attr.equals("Fries")) {
							size = value;
							continue;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("Received Request from HungryPeer" + name
						+ " for" + size + " fries;");
				try {
					System.out
							.println("Attempting to create Output pipe to HungryPeer"
									+ name);
					pipeOut = pipes.createOutputPipe(hungryPipe, rtimeout);
					// check if we have a pipe
					if (pipeOut == null) {
						System.out.println("could not find HungryPeer pipe");
						continue;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				// We have a pipe connection to the Hungrypeer
				// now create the bid response document
				try {
					// Construct the response document
					bid = StructuredDocumentFactory.newStructuredDocument(
							mimeType, "RestoNet:Bid");
					// Set the bid values(brand ,price,special) in the response
					// document
					el = bid.createElement("Brand", brand);
					bid.appendChild(el);
					el = bid.createElement("Price", friesPrice(size));
					bid.appendChild(el);
					el = bid.createElement("Specials", specials);
					bid.appendChild(el);
					// Create a new pipe message
					Message message = new Message();
					InputStreamMessageElement input = new InputStreamMessageElement(
							"Bid", mimeType, bid.getStream(), null);
					message.addMessageElement(input);
					pipeOut.send(message);
					pipeOut.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("Sent Bid Offer to HungryPeer(" + name
						+ ") Fries price=" + friesPrice(size) + ",special="
						+ specials);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private String friesPrice(String size) {
		if (size.equals("small"))
			return "$1.5";
		if (size.equals("medium"))
			return "$2.5";
		if (size.equals("big"))
			return "$3.0";
		return "error";
	}

	private boolean createRestoPipe() {
		int count = 3;// Discovery retry count
		Enumeration ae = null;// Discovery response enumeration
		try {
			System.out.println("Attempting to Discover published RestoPipe");
			// Check if we have already published ourselves
			while (count-- > 0) {
				// First,check locally if the advertisement is cached
				ae = disco.getLocalAdvertisements(DiscoveryService.ADV, "Name",
						"RestoNet:RestoPipe:" + brand);
				// If we found our pipe advertisement,we are done
				if (ae != null && ae.hasMoreElements())
					break;
				// We did not find the advertisement locally;
				// send a remote request
				disco.getRemoteAdvertisements(null, DiscoveryService.ADV,
						"Name", "RestoNet:RestoPipe:" + brand, 1, null);
				// Sleep to allow time for peers to respond to the discovery
				// request
				try {
					Thread.sleep(timeout);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (ae == null || !ae.hasMoreElements()) {
				System.out
						.println("Could not find the Restaurant Pipe Advertisement");
				// Create a pipe advertisement for our RestoPeer
				myAdv = (PipeAdvertisement) AdvertisementFactory
						.newAdvertisement(PipeAdvertisement
								.getAdvertisementType());
				// Assign a unique Id to the pipe
				myAdv.setPipeID(IDFactory.newPipeID(restoNet.getPeerGroupID()));
				// The symbolic name of the pipe is built from the brand name of
				// RestoPeer;
				// each RestoPeer must therefore have a unique name
				myAdv.setName("RestoNet:RestoPipe:" + brand);
				// Set the type of the pipe to be unidirectional
				myAdv.setType(PipeService.UnicastType);
				// we have the advertisement; publish it into our local cache
				// and to the RestoNet peergroup.We use the default lifetime and
				// expiration
				// time for remote publishing
				disco.publish(myAdv, PeerGroup.DEFAULT_LIFETIME,
						PeerGroup.DEFAULT_EXPIRATION);// create the
														// Advertisement
				// publish an advertisement via propagation to other peers on
				// the networks
				disco.remotePublish(myAdv, PeerGroup.DEFAULT_EXPIRATION);
				System.out.println("Create the Restaurant Pipe Advertisement");
			} else {
				// We found an existing pipe advertisement
				myAdv = (PipeAdvertisement) ae.nextElement();
				System.out.println("Found Restaurant Pipe Advertisement");
			}
			// Create my input pipe to listen for HungryPeer's requests
			pipeIn = pipes.createInputPipe(myAdv);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;

	}

	public static void main(String[] args) {
		RestoPeer my = new RestoPeer();// 此处实例化一个对等体对象。
		my.startJxta();// 此处启动JXTA方法，用来加入组，获得服务等等。
		System.exit(0);
	}
}
