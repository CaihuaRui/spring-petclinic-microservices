package org.springframework.samples.petclinic.visits.grpc;

import io.grpc.stub.StreamObserver;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;

@GrpcService
@Slf4j
public class VisitsServiceImpl extends VisitsServiceGrpc.VisitsServiceImplBase {
    @Autowired
    private VisitRepository visitRepository;

    @Override
    public void visits(
        VisitsRequest request, StreamObserver<VisitsResponse> responseObserver) {
        List<Visit> visits = visitRepository.findByPetIdIn(request.getPetIdList());

        List<Visits> list = new ArrayList<>();
        visits.forEach(v -> {
            log.info(v.getId() + "#" + v.getDate() + "#" + v.getDescription() + "#" + v.getPetId());
            list.add(Visits.newBuilder()
                .setId(v.getId())
                .setDate(DateFormatUtils.format(v.getDate(), "yyyy-MM-dd"))
                .setDescription(v.getDescription())
                .setPetId(v.getPetId())
                .build());
        });
        VisitsResponse visitsResponse = VisitsResponse.newBuilder().addAllEle(list).build();
        responseObserver.onNext(visitsResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void create(Visits request, StreamObserver<Visits> responseObserver) {
        log.info("create: " + request.getId() + "#" + request.getDate() + "#" + request.getDescription()
            + "#" + request.getPetId());

        Visit visit = null;
        try {
            visit = Visit.visit()
                .date(DateUtils.parseDate(request.getDate(), new String[]{"yyyy-MM-dd"}))
                .description(request.getDescription())
                .petId(request.getPetId())
                .build();
        } catch (ParseException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        visitRepository.save(visit);
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }

}
