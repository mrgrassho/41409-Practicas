package punto3;

import java.util.ArrayList;

public interface Service {	
	
	public String getName();
	public int getPort();
	public Object execute(ArrayList<Object> list);
}
