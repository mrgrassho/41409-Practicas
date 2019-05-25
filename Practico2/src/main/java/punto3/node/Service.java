package punto3.node;

import java.util.ArrayList;
import java.util.Map;

public interface Service {	
	
	public String getName();
	public int getPort();
	public Object execute(Object[] list);
}
