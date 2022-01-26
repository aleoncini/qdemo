package org.acme;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/info")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    JsonWebToken accessToken;
    
    @GET
    public Info get() {
        Info info = new Info();
        StringBuffer buffer = new StringBuffer();
        buffer.append(accessToken.getClaim("given_name").toString()).append(" ").append(accessToken.getClaim("family_name").toString());
        info.name = buffer.toString();
        info.username = accessToken.getName();
        return info;
    }

}