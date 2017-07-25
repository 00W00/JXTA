package test;

import net.jxta.endpoint.Message;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;

public class XChat implements PipeMsgListener{

	@Override
	public void pipeMsgEvent(PipeMsgEvent event) {
		System.out.println("come a mesage");
		Message message=event.getMessage();
		
	}

}
