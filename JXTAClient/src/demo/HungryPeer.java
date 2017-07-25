package demo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.InputStreamMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;

public class HungryPeer implements DiscoveryListener {
	private PeerGroup netpg = null;
	private PeerGroup restoNet = null;

	private int timeout = 3000;
	private int rtimeout = 30000;

	private DiscoveryService disco;
	private PipeService pipes;
	private PipeAdvertisement myAdv;
	private InputPipe myPipe;
	private MimeMediaType mimeType = new MimeMediaType("text", "xml");

	private Vector restoPeerAdcs = new Vector<>();
	private Vector restoPeerPipes = new Vector<>();

	private String myIdentity = "Bill Joy";
	private String friesRequest = "medium";

	public static void main(String[] args) {
		HungryPeer hungry = new HungryPeer();
		hungry.startJxta();
		System.exit(0);
	}

	private void startJxta() {
		try {
			netpg = new NetPeerGroupFactory().getInterface();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		try {
			if (!joinRestoNet())
				System.out.println("could not find the RestoNet Peergroup");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		if (!setHungryPeerPipe()) {
			System.out
					.println("Aborting due to failure to create our hungryPeer pipe");
			System.exit(1);
		}
		discoverRestoPeers();
		connectToRestoPeers();
		sendFriesAuctionRequest();
		receiveFriesBids();
	}

	private boolean joinRestoNet() {
		int count = 3;
		System.out.println("discover the restoNet peerroup");
		/*
		 * DiscoveryService hdisco = netpg.getDiscoveryService(); Enumeration ae
		 * = null; while (count-- > 0) { try { ae =
		 * hdisco.getLocalAdvertisements(DiscoveryService.GROUP, "Name",
		 * "RestoNet"); if (ae != null && ae.hasMoreElements()) break;
		 * hdisco.getRemoteAdvertisements(null, DiscoveryService.GROUP, "Name",
		 * "RestoNet", 1, null); try { Thread.sleep(timeout); } catch (Exception
		 * e) { e.printStackTrace(); } } catch (Exception e) {
		 * e.printStackTrace(); } }
		 */
		/*
		 * if (ae == null || !ae.hasMoreElements()) return false;
		 * PeerGroupAdvertisement adv = (PeerGroupAdvertisement)
		 * ae.nextElement(); try { restoNet = netpg.newGroup(adv); } catch
		 * (Exception e) { e.printStackTrace(); }
		 */
		try {
			disco = netpg.getDiscoveryService();
			pipes = netpg.getPipeService();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean setHungryPeerPipe() {
		try {
			myAdv = (PipeAdvertisement) AdvertisementFactory
					.newAdvertisement(PipeAdvertisement.getAdvertisementType());
			myAdv.setPipeID(IDFactory.newPipeID(netpg.getPeerGroupID()));
			myAdv.setName("restoNet:HungryPipe:" + myIdentity);
			myAdv.setType(PipeService.UnicastType);
			myPipe = pipes.createInputPipe(myAdv);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private void discoverRestoPeers() {
		int found = 0;
		int count = 10;
		System.out.println("Locating RestoPeers in the RestoNet Peergroup");
		while (count-- > 0) {
			try {
				Enumeration ae = disco.getLocalAdvertisements(
						DiscoveryService.ADV, "Name", "RestoNet:RestoPipe:*");
				if (ae != null && ae.hasMoreElements()) {
					found = 0;
					restoPeerAdcs.removeAllElements();
					while (ae.hasMoreElements()) {
						restoPeerAdcs.addElement(ae.nextElement());
						++found;
					}
				}
				if (found > 1) {
					break;
				}
				disco.getRemoteAdvertisements(null, DiscoveryService.ADV,
						"Name", "RestoNet:RestoPipe:*", 5, null);
				try {
					Thread.sleep(timeout);
				} catch (Exception e) {
					e.printStackTrace();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Found" + found + "RestoPeers(s)");

		}
	}

	private void connectToRestoPeers() {
		for (Enumeration en = restoPeerAdcs.elements(); en.hasMoreElements();) {
			PipeAdvertisement padv = (PipeAdvertisement) en.nextElement();
			try {
				System.out
						.println("Attempting to connect to discovered RestoPeer");
				OutputPipe outputPipe = pipes.createOutputPipe(padv, rtimeout);
				// check if we have a connected pipe
				if (outputPipe == null) {
					// failed ,go to next RestoPeer
					System.out.println("failure to connect to RestoPeer Pipe:"
							+ padv.getName());
					continue;
				}
				// Save the output pipe
				restoPeerPipes.addElement(outputPipe);
				System.out.println("connected pipe to " + padv.getName());

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void sendFriesAuctionRequest() {
		for (Enumeration en = restoPeerPipes.elements(); en.hasMoreElements();) {
			OutputPipe outputPipe = (OutputPipe) en.nextElement();
			try {
				StructuredDocument request = StructuredDocumentFactory
						.newStructuredDocument(mimeType, "RestoNet:Request");
				Element re;
				re = request.createElement("Name", myIdentity);
				request.appendChild(re);
				re = request.createElement("Fries", friesRequest);
				request.appendChild(re);
				// Create message
				Message msg = new Message();
				InputStreamMessageElement input = new InputStreamMessageElement(
						"HungryPeerPipe", mimeType, myAdv.getDocument(mimeType)
								.getStream(), null);
				msg.addMessageElement(input);
				InputStreamMessageElement input1 = new InputStreamMessageElement(
						"Request", mimeType, request.getStream(), null);
				msg.addMessageElement(input1);
				outputPipe.send(msg);
				// outputPipe.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void receiveFriesBids() {
		// Continue until we get all answers
		while (true) {
			Message msg = null;
			String price = null;
			String brand = null;
			String specials = null;
			InputStream ip = null;
			StructuredDocument bid = null;
			try {
				msg = myPipe.waitForMessage();
				if (msg == null) {
					if (Thread.interrupted()) {
						System.out
								.println("Abort Receiving bid loop interrupted");
						myPipe.close();
						return;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// We got a message from a RestoPeer;Extract and display infomation
			// about the bid received
			try {
				// Extract the bid document from the message
				ip = msg.getMessageElement("Bid").getStream();
				bid = StructuredDocumentFactory.newStructuredDocument(mimeType,
						ip);
				// Parse the document to extract bid information
				Enumeration en = bid.getChildren();
				while (en.hasMoreElements()) {
					Element el = (Element) en.nextElement();
					String attr = (String) el.getKey();
					String value = (String) el.getValue();
					if (attr.equals("Price")) {
						price = value;
						continue;
					}
					if (attr.equals("Brand")) {
						brand = value;
						continue;
					}
					if (attr.equals("Specials")) {
						specials = value;
						continue;
					}
				}
				System.out.println("Received Fries bid from RestoPeers("
						+ brand + ") at a Price ($" + price);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void discoveryEvent(DiscoveryEvent event) {
		System.out.println("Processing discovery event");
		DiscoveryResponseMsg msg = event.getResponse();
		Enumeration e = msg.getResponses();
		while (e.hasMoreElements()) {
			try {
				String string = (String) e.nextElement();
				XMLDocument xml = (XMLDocument) StructuredDocumentFactory
						.newStructuredDocument(new MimeMediaType("text/xml"),
								new ByteArrayInputStream(string.getBytes()));
				PipeAdvertisement adv = (PipeAdvertisement) AdvertisementFactory
						.newAdvertisement(xml);
				connectAndSend(adv);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	private void connectAndSend(PipeAdvertisement padv) {
		System.out.println("Attempting to connect to discovered RestoPeer");
		try {
			OutputPipe op = pipes.createOutputPipe(padv, rtimeout);
			if (op == null) {
				System.out.println("Failed to connect to RestoPeer Pipe:"
						+ padv.getName());
				return;
			}
			StructuredDocument request = StructuredDocumentFactory
					.newStructuredDocument(mimeType, "RestoNet:Request");
			Element re;
			re=request.createElement("Name",myIdentity);
			request.appendChild(re);
			re=request.createElement("Fries",friesRequest);
			request.appendChild(re);
			Message msg=new Message();
			InputStreamMessageElement input = new InputStreamMessageElement(
					"HungryPeerPipe", mimeType, myAdv.getDocument(mimeType)
							.getStream(), null);
			msg.addMessageElement(input);
			InputStreamMessageElement input1 = new InputStreamMessageElement(
					"Request", mimeType, request.getStream(), null);
			msg.addMessageElement(input1);
			op.send(msg);
			System.out.println("Sent Fries Auction Request("+friesRequest+") to connected peers");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
