package io.spring2go.zuul.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import io.spring2go.zuul.common.ZuulException;
import io.spring2go.zuul.context.RequestContext;
import io.spring2go.zuul.core.ZuulRunner;

/**
 * 同步模式启动 zuul
 */
public class SyncZuulServlet extends HttpServlet {
	
	private static final long serialVersionUID = -7314825620092836092L;

	private static Logger LOGGER = LoggerFactory.getLogger(SyncZuulServlet.class);

    /**
     * ZuulRunner 是一个 封装类  （wrapper） 内部封装了FilterProcessor
     */
    private ZuulRunner zuulRunner = new ZuulRunner();

    @Override
    public void service(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) throws javax.servlet.ServletException, java.io.IOException {
        try {


            init((HttpServletRequest) req, (HttpServletResponse) res);

            // marks this request as having passed through the "Zuul engine", as opposed to servlets
            // explicitly bound in web.xml, for which requests will not have the same data attached
            RequestContext.getCurrentContext().setZuulEngineRan();

            /**
             * 对应我们架构图的 运行时模块
             */
            try {
                /**
                 * 运行前置过滤器
                 */
                preRoute();
            } catch (ZuulException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                /**
                 * 运行 过滤器
                 */
                route();
            } catch (ZuulException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                /**
                 * 运行后置过滤器
                 */
                postRoute();
            } catch (ZuulException e) {
                error(e);
                return;
            }

        } catch (Throwable e) {
            error(new ZuulException(e, 500, "UNHANDLED_EXCEPTION_" + e.getClass().getName()));
        } finally {
        	RequestContext.getCurrentContext().unset();
        }
    }

    /**
     * executes "post" ZuulFilters
     *
     * @throws ZuulException
     */
    void postRoute() throws ZuulException {
    	zuulRunner.postRoute();
    }

    /**
     * executes "route" filters
     *
     * @throws ZuulException
     */
    void route() throws ZuulException {
    	zuulRunner.route();
    }

    /**
     * executes "pre" filters
     *
     * @throws ZuulException
     */
    void preRoute() throws ZuulException {
        /**
         * 架构图中的 zuul Filter Runner
         */
    	zuulRunner.preRoute();
    }

    /**
     * initializes request
     *
     * @param servletRequest
     * @param servletResponse
     */
    void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    	zuulRunner.init(servletRequest, servletResponse);
    }

	/**
	 * sets error context info and executes "error" filters
	 *
	 * @param e
	 * @throws ZuulException
	 */
	void error(ZuulException e) {
		try {
			RequestContext.getCurrentContext().setThrowable(e);
			zuulRunner.error();
		} catch (Throwable t) {
			Cat.logError(t);
			LOGGER.error(e.getMessage(), e);
		}finally{
			Cat.logError(e);
		}
	}

}
