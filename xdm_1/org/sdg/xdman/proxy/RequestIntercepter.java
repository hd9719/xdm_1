

package org.sdg.xdman.proxy;

public interface RequestIntercepter {
	public void intercept(Object obj, Object o);

	public void intercept(Object obj);
}
