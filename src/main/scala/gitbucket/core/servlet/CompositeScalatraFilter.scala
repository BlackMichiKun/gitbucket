package gitbucket.core.servlet

import javax.servlet._
import javax.servlet.http.HttpServletRequest

import org.scalatra.ScalatraFilter

import scala.collection.mutable.ListBuffer

class CompositeScalatraFilter extends Filter {

  private val filters = new ListBuffer[(ScalatraFilter, String)]()

  def mount(filter: ScalatraFilter, path: String): Unit = {
    filters += ((filter, path))
  }

  override def init(filterConfig: FilterConfig): Unit = {
    filters.foreach { case (filter, _) =>
      filter.init(filterConfig)
    }
  }

  override def destroy(): Unit = {
    filters.foreach { case (filter, _) =>
      filter.destroy()
    }
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val contextPath = request.getServletContext.getContextPath
    val requestPath = request.asInstanceOf[HttpServletRequest].getRequestURI.substring(contextPath.length)
    val checkPath = if(requestPath.endsWith("/")){
      requestPath
    } else {
      requestPath + "/"
    }

    filters
      .filter { case (_, path) =>
        val start = path.replaceFirst("/\\*$", "/")
        checkPath.startsWith(start)
      }
      .foreach { case (filter, _) =>
        val mockChain = new MockFilterChain()
        filter.doFilter(request, response, mockChain)
        if(mockChain.continue == false){
          return ()
        }
      }

    chain.doFilter(request, response)
  }

}

class MockFilterChain extends FilterChain {
  var continue: Boolean = false

  override def doFilter(request: ServletRequest, response: ServletResponse): Unit = {
    continue = true
  }
}

class FilterChainFilter(chain: FilterChain) extends Filter {
  override def init(filterConfig: FilterConfig): Unit = ()
  override def destroy(): Unit  = ()
  override def doFilter(request: ServletRequest, response: ServletResponse, mockChain: FilterChain) = chain.doFilter(request, response)
}
