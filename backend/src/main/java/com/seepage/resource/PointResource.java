package com.seepage.resource;

import com.seepage.dto.DtoAssembler;
import com.seepage.dto.Views;
import com.seepage.model.LeakPoint;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/points")
@Produces(MediaType.APPLICATION_JSON)
public class PointResource {

    @Inject
    DtoAssembler dto;

    /** 点位列表：每个点位下挂各户工单，记录互相独立保留 */
    @GET
    public List<Views.PointView> list() {
        return LeakPoint.<LeakPoint>list("order by id").stream()
                .map(p -> dto.point(p, null))
                .toList();
    }
}
