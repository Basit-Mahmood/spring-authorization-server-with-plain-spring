package pk.training.basit.filter.web;

import org.apache.logging.log4j.ThreadContext;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

//Creating a filter is as simple as implementing the Filter interface
public class PreSecurityLoggingFilter implements Filter {
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		String id = UUID.randomUUID().toString();
		ThreadContext.put("id", id);
		try {
			((HttpServletResponse) response).setHeader("X-Wrox-Request-Id", id);
			chain.doFilter(request, response);
		} finally {
			ThreadContext.remove("id");
			ThreadContext.remove("username");
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	// destroy() method is called when the web application is shut down.
	@Override
	public void destroy() {
	}
}
