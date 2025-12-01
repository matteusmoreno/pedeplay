package br.com.matteusmoreno.api;
import br.com.matteusmoreno.domain.contract.response.ContractDetailsResponse;
import br.com.matteusmoreno.domain.contract.service.ContractService;
import br.com.matteusmoreno.domain.contract.request.CreateContractWithCustomerRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
@Path("/contracts")
public class ContractResource {
  private final ContractService contractService;
  public ContractResource(ContractService contractService) {
    this.contractService = contractService;
  }
  @POST
  @Path("/create")
  public Response createContract(@Valid CreateContractWithCustomerRequest request, @Context UriInfo uriInfo) {
    ContractDetailsResponse contract = this.contractService.createContract(
        request.contract(), 
        request.customer()
    );
    URI uri = uriInfo.getAbsolutePathBuilder().path(contract.id()).build();
    return Response.created(uri).entity(contract).build();
  }
  @GET
  @Path("/{id}")
  public Response getContractById(@PathParam("id") String contractId) {
    ContractDetailsResponse contract = this.contractService.findContractById(contractId);
    return Response.ok(contract).build();
  }
  @GET
  @Path("/find-all-contracts-by-artist-id")
  public Response findAllContractsByArtistId(@QueryParam("artistId") String artistId) {
    List<ContractDetailsResponse> contracts = this.contractService.findAllContractsByAuthenticatedArtist(artistId);
    return Response.ok(contracts).build();
  }
  @GET
  @Path("/by-customer")
  public Response findContractsByCustomer(
      @QueryParam("email") String email, 
      @QueryParam("phoneNumber") String phoneNumber) {
    List<ContractDetailsResponse> contracts = this.contractService.findContractsByCustomer(email, phoneNumber);
    return Response.ok(contracts).build();
  }
  @PATCH
  @Path("/confirm/{contractId}")
  public Response confirmContract(@PathParam("contractId") String contractId) {
    this.contractService.confirmContract(contractId);
    return Response.ok().build();
  }
  @PATCH
  @Path("/reject/{contractId}")
  public Response rejectContract(@PathParam("contractId") String contractId) {
    this.contractService.rejectContract(contractId);
    return Response.ok().build();
  }
  @PATCH
  @Path("/cancel/{contractId}")
  public Response cancelContract(@PathParam("contractId") String contractId) {
    this.contractService.cancelContract(contractId);
    return Response.ok().build();
  }
  @PATCH
  @Path("/complete/{contractId}")
  public Response completeContract(@PathParam("contractId") String contractId) {
    this.contractService.completeContract(contractId);
    return Response.ok().build();
  }
}
