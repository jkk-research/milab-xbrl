package hu.sze.uni.xbrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlUtilsDataCollector implements XbrlConsts {

	ContainerType currCt;
	Object currContainer;

	Stack stack;
	String currKey;

	void init(Object root) {
		currContainer = root;
		currCt = (currContainer instanceof Map) ? ContainerType.Map : ContainerType.Arr;
		stack = null;
	}

	void setKey(String key) {
		currKey = key;
	}
	
	public Object getCurrContainer() {
		return currContainer;
	}

	void open(ContainerType ct) {
		Object c = (ct == ContainerType.Map) ? new HashMap() : new ArrayList();

		if ( null == currContainer ) {
			init(c);
		} else {
			putValue(c);

			if ( null != currContainer ) {
				if ( null == stack ) {
					stack = new Stack();
				}
				stack.push(currContainer);
			}

			currContainer = c;
			currCt = ct;
		}
	}

	boolean close() {
		if ( (null == stack) || stack.isEmpty() ) {
			return false;
		}
		currContainer = stack.pop();
		currCt = (currContainer instanceof Map) ? ContainerType.Map : ContainerType.Arr;

		return true;
	}

	void putValue(Object val) {
		if ( currCt == ContainerType.Map ) {
			((Map) currContainer).put(currKey, val);
		} else {
			((ArrayList) currContainer).add(val);
		}
	}
	
	@Override
	public String toString() {
		return XbrlUtils.toString(currContainer);
	}

	public ContainerType getCt() {
		return currCt;
	}
}