package test;

import java.io.FileInputStream;
import java.io.IOException;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.Message;
import net.jxta.exception.PeerGroupException;
import net.jxta.impl.protocol.PipeAdv;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

public class P2PSocket extends Thread implements OutputListener,
		PipeMsgListener {
	private String inputPipeName = null;// 输入管道的名称
	private String outputPipeName = null;// 输出管道的名称
	private PipeAdvertisement inputPipeAdv;// 输入管道广告
	private PipeAdvertisement outputPipeAdv;// 输出管道广告
	private InputPipe inputPipe;// 输入管道
	private OutputPipe outputPipe;// 输出管道
	private PipeMsgListener pml;// 输入管道的监听器
	private OutputListener opl;// 输出管道的监听器
	private PeerGroup pg = null;// 所属的组
	private PipeService ps = null;// 管道服务
	private DiscoveryService disc = null;// 发现服务

	public P2PSocket() {
		if (pg == null)
			this.newPeerGroup();
		ps = pg.getPipeService();
		disc = pg.getDiscoveryService();
	}

	public P2PSocket(String inputPipeName, String outPipeName) {
		if (pg == null)
			this.newPeerGroup();
		ps = pg.getPipeService();
		disc = pg.getDiscoveryService();
		this.setInputPipeName(inputPipeName);
		this.setOutputPipeName(outPipeName);
	}

	public boolean bind() {
		for (int i = 0; i < 5; i++) {
			this.start();
			try {
				if (pml != null)
					inputPipe = ps.createInputPipe(inputPipeAdv, pml);
				else
					inputPipe = ps.createInputPipe(inputPipeAdv, this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (inputPipe != null)
				return true;
		}
		return false;
	}

	public void run() {
		try {
			disc.remotePublish(inputPipeAdv, 10 * 60 * 1000);
			disc.publish(inputPipeAdv, 10 * 60 * 1000, 10 * 60 * 1000);
			this.sleep(10 * 60 * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean connect() {
		for (int i = 0; i < 10; i++) {
			try {
				System.out.println("创建outputpipe");
				if (opl != null)
					ps.createOutputPipe(outputPipeAdv, opl);
				else
					ps.createInputPipe(outputPipeAdv, this);
			} catch (Exception e) {
				// TODO: handle exception
			}
			if (opl != null && opl.getOutputPipe() != null)
				break;
			try {
				Thread.sleep(5 * 1000);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		if (opl != null && opl.getOutputPipe() != null) {
			this.outputPipe = opl.getOutputPipe();
			return true;
		} else {
			System.out.println("通信连接失败");
			System.exit(-1);
		}
		return false;

	}

	public boolean connect(String outputPipeName) {
		this.setOutputPipeName(outputPipeName);
		if (this.connect())
			return true;
		else
			return false;
	}

	public boolean send(Message mess) {
		if (opl != null)
			opl.getOutputPipe();
		while (outputPipe == null)
			try {
				Thread.sleep(5 * 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		try {
			outputPipe.send(mess);
			return true;
		} catch (Exception e) {
			System.out.println("发送信息失败");
			return false;
		}
	}

	private void newPeerGroup() {
		try {
			pg = PeerGroupFactory.newNetPeerGroup();
		} catch (PeerGroupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void pipeMsgEvent(PipeMsgEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void outputPipeEvent(OutputPipeEvent event) {
		outputPipe = event.getOutputPipe();
	}

	public String getInputPipeName() {
		return inputPipeName;
	}

	public void setInputPipeName(String inputPipeName) {
		this.inputPipeName = inputPipeName;
		inputPipeAdv = createPipeAdvFromFile("adv/" + inputPipeName + ".xml");
	}

	public String getOutputPipeName() {
		return outputPipeName;
	}

	public void setOutputPipeName(String outputPipeName) {
		this.outputPipeName = outputPipeName;
		outputPipeAdv = createPipeAdvFromFile("adv/" + outputPipeName + ".xml");
	}

	public PipeAdvertisement getInputPipeAdv() {
		return inputPipeAdv;
	}

	public void setInputPipeAdv(PipeAdvertisement inputPipeAdv) {
		this.inputPipeAdv = inputPipeAdv;
	}

	public PipeAdvertisement getOutputPipeAdv() {
		return outputPipeAdv;
	}

	public void setOutputPipeAdv(PipeAdvertisement outputPipeAdv) {
		this.outputPipeAdv = outputPipeAdv;
	}

	public InputPipe getInputPipe() {
		return inputPipe;
	}

	public void setInputPipe(InputPipe inputPipe) {
		this.inputPipe = inputPipe;
	}

	public OutputPipe getOutputPipe() {
		return outputPipe;
	}

	public void setOutputPipe(OutputPipe outputPipe) {
		this.outputPipe = outputPipe;
	}

	public PipeMsgListener getPml() {
		return pml;
	}

	public void setPml(PipeMsgListener pml) {
		this.pml = pml;
	}

	public OutputListener getOpl() {
		return opl;
	}

	public void setOpl(OutputListener opl) {
		this.opl = opl;
	}

	public PeerGroup getPg() {
		return pg;
	}

	public void setPg(PeerGroup pg) {
		this.pg = pg;
	}

	public PipeService getPs() {
		return ps;
	}

	public void setPs(PipeService ps) {
		this.ps = ps;
	}

	public DiscoveryService getDisc() {
		return disc;
	}

	public void setDisc(DiscoveryService disc) {
		this.disc = disc;
	}

	private PipeAdvertisement createPipeAdvFromFile(String filename) {
		PipeAdvertisement pipeAdv = null;
		try {
			FileInputStream is = new FileInputStream(filename);
			pipeAdv = (PipeAdvertisement) AdvertisementFactory
					.newAdvertisement(new MimeMediaType("text/xml"), is);
			is.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return pipeAdv;
	}
}
