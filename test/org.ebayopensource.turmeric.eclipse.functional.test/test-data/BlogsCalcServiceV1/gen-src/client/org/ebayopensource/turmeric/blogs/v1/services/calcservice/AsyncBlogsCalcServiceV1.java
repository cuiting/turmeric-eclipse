
package org.ebayopensource.turmeric.blogs.v1.services.calcservice;

import java.util.List;
import java.util.concurrent.Future;
import javax.xml.ws.AsyncHandler;
import org.ebayopensource.turmeric.blogs.v1.services.Inparams;

public interface AsyncBlogsCalcServiceV1
    extends BlogsCalcServiceV1
{


    public Future<?> addAsync(Inparams param0, AsyncHandler<org.ebayopensource.turmeric.blogs.v1.services.Response> handler);

    public javax.xml.ws.Response<org.ebayopensource.turmeric.blogs.v1.services.Response> addAsync(Inparams param0);

    public List<javax.xml.ws.Response<?>> poll(boolean block, boolean partial)
        throws InterruptedException
    ;

}
