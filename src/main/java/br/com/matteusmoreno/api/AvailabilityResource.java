package br.com.matteusmoreno.api;
import br.com.matteusmoreno.domain.availability.Availability;
import br.com.matteusmoreno.domain.availability.response.AvailabilityDetailsResponse;
import br.com.matteusmoreno.domain.availability.service.AvailabilityService;
import br.com.matteusmoreno.domain.availability.request.CreateAvailabilityRequest;
import br.com.matteusmoreno.domain.availability.request.UpdateAvailabilityRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
@Path("/availabilities")
public class AvailabilityResource {
  private final AvailabilityService availabilityService;
  public AvailabilityResource(AvailabilityService availabilityService) {
    this.availabilityService = availabilityService;
  }
  @POST
  @Path("/create" )
  @RolesAllowed("ARTIST")
  public Response create(@Valid CreateAvailabilityRequest request, @Context UriInfo uriInfo) {
    Availability availability = this.availabilityService.createAvailability(request);
    URI uri = uriInfo.getAbsolutePathBuilder().path(String.valueOf(availability.id)).build();
    return Response.created(uri).entity(new AvailabilityDetailsResponse(availability)).build();
  }
  @GET
  @Path("/{id}")
  public Response getAvailabilityById(@PathParam("id") String id) {
    Availability availability = this.availabilityService.findAvailabilityById(id);
    return Response.ok(new AvailabilityDetailsResponse(availability)).build();
  }
  @GET
  @Path("/get-all-by-artist/{artistId}")
  public Response getAllAvailabilitiesByArtist(@PathParam("artistId") String artistId) {
    List<AvailabilityDetailsResponse> availabilities = this.availabilityService.findAllAvailabilitiesByArtist(
        artistId);
    return Response.ok(availabilities).build();
  }
  @GET
  @Path("/available")
  public Response getAllAvailableAvailabilities() {
    List<AvailabilityDetailsResponse> availabilities = this.availabilityService.findAllAvailableAvailabilities();
    return Response.ok(availabilities).build();
  }
  @GET
  @Path("/available-by-artist/{artistId}")
  public Response getAvailableByArtist(@PathParam("artistId") String artistId) {
    List<AvailabilityDetailsResponse> availabilities = this.availabilityService.findAvailableByArtist(artistId);
    return Response.ok(availabilities).build();
  }
  @PUT
  @Path("/update")
  @RolesAllowed("ARTIST")
  public Response updateAvailability(@Valid UpdateAvailabilityRequest request) {
    Availability availability = this.availabilityService.updateAvailability(request);
    return Response.ok(new AvailabilityDetailsResponse(availability)).build();
  }
  @DELETE
  @Path("/delete/{id}")
  @RolesAllowed("ARTIST")
  public Response deleteAvailabilityById(@PathParam("id") String id) {
    this.availabilityService.deleteAvailabilityById(id);
    return Response.noContent().build();
  }
}
