package br.com.matteusmoreno.infrastructure.image;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import java.io.InputStream;

public class FileUploadRequest {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream file;
}