/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.api.application;

import java.util.List;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.samples.petclinic.api.dto.VisitDetails;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.samples.petclinic.visits.grpc.VisitsRequest;
import org.springframework.samples.petclinic.visits.grpc.VisitsResponse;
import org.springframework.samples.petclinic.visits.grpc.VisitsServiceGrpc.VisitsServiceBlockingStub;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author Maciej Szarlinski
 */
@Component
public class VisitsServiceClient {
    @GrpcClient("visits-grpc-server")
    private VisitsServiceBlockingStub stub;

    public Mono<Visits> getVisitsForPets(final List<Integer> petIds) {
        return Mono.just(getVisitsByPetIds(petIds));
    }

    public Visits getVisitsByPetIds(final List<Integer> petIds) {
        VisitsResponse response = stub.visits(VisitsRequest.newBuilder()
            .addAllPetId(petIds)
            .build());

        Visits visits = new Visits();
        response.getEleList().forEach(o -> visits.getItems().add(new VisitDetails(o.getId(),
            o.getPetId(), o.getDate(), o.getDescription())));
        return visits;
    }

    public VisitDetails createVisits(VisitDetails visit) {
        org.springframework.samples.petclinic.visits.grpc.Visits response =
            stub.create(org.springframework.samples.petclinic.visits.grpc.Visits.newBuilder()
                .setPetId(visit.getPetId())
                .setDate(visit.getDate())
                .setDescription(visit.getDescription())
                .build());
        return visit;
    }
}
