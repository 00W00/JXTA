package test;

import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeListener;

public interface OutputListener extends OutputPipeListener{
public abstract OutputPipe getOutputPipe();
}
