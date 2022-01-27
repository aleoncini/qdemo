package org.acme;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

@Path("/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UsernameResource {

    @GET
    @Produces("application/json")
    public Info getAllHeaders(@Context HttpHeaders headers) {
        Info info = new Info();
        info.username = headers.getRequestHeader("X-User").get(0);
        info.name = "unused";
        return info;
    }
    
}