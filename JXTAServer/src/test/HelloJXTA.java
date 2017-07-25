package test;

import java.io.IOException;
import java.io.StringWriter;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;

public class HelloJXTA {
	public static void main(String[] args) {
    System.out.println("Starting JXTA....");
    HelloJXTA helloJXTA=new HelloJXTA();
    helloJXTA.startJXTA();
	}
	public void startJXTA(){
		PeerGroup pg=null;
		try {
			pg=PeerGroupFactory.newNetPeerGroup();
		} catch (PeerGroupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Hello JXTA");
		/*System.out.println("Group name="+pg.getPeerGroupName());
		System.out.println("Group Id="+pg.getPeerGroupID().toString());
		System.out.println("Peer name="+pg.getPeerName());
		System.out.println("Peer ID="+pg.getPeerID().toString());*/
		//获得通告对象
		try {
	    PeerGroupAdvertisement pgadv = pg.getPeerGroupAdvertisement();
	    //解析获取的advertisement
	    StructuredTextDocument doc=(StructuredTextDocument) pgadv.getDocument(new MimeMediaType("text/plain"));
	    StringWriter out=new StringWriter();
        doc.sendToWriter(out);
        System.out.println(out.toString());
        out.close();
        StructuredTextDocument adoc=(StructuredTextDocument) pgadv.getDocument(new MimeMediaType("text/xml"));
		StringWriter aout=new StringWriter();
		adoc.sendToWriter(aout);
		System.out.println(aout.toString());
		aout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void makeAdvertisement(){
		ModuleSpecAdvertisement msa=(ModuleSpecAdvertisement) AdvertisementFactory.newAdvertisement(ModuleSpecAdvertisement.getAdvertisementType());
	    msa.setName("");
	    msa.setVersion("");
	    msa.setCreator("");
	    msa.setSpecURI("");
	    //msa.setModuleSpecID(IDFactory.newModuleSpecID(mcID));
	}
}
