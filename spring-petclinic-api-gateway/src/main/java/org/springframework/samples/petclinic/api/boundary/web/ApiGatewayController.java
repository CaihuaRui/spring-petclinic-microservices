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
package org.springframework.samples.petclinic.api.boundary.web;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.application.VisitsServiceClient;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.PetDetails;
import org.springframework.samples.petclinic.api.dto.PetRequest;
import org.springframework.samples.petclinic.api.dto.PetType;
import org.springframework.samples.petclinic.api.dto.VisitDetails;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author Maciej Szarlinski
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gateway")
public class ApiGatewayController {

    private final CustomersServiceClient customersServiceClient;

    private final VisitsServiceClient visitsServiceClient;

    private final ReactiveCircuitBreakerFactory cbFactory;

    @GetMapping(value = "owners/*/pets/{petId}/visits")
    public List<VisitDetails> visits(final @PathVariable int petId) {
        return visitsServiceClient.getVisitsByPetIds(Lists.newArrayList(petId)).getItems();
    }

    @PostMapping(value = "owners/*/pets/{petId}/visits")
    public VisitDetails createVisits(@Valid @RequestBody VisitDetails visit,final @PathVariable int petId) {
        visit.setPetId(petId);
        return visitsServiceClient.createVisits(visit);
    }

    @GetMapping(value = "owners/{ownerId}")
    public Mono<OwnerDetails> getOwnerDetails(final @PathVariable int ownerId) {
        return customersServiceClient.getOwner(ownerId)
            .flatMap(owner ->
                visitsServiceClient.getVisitsForPets(owner.getPetIds())
                    .transform(it -> {
                        ReactiveCircuitBreaker cb = cbFactory.create("getOwnerDetails");
                        return cb.run(it, throwable -> emptyVisitsForPets());
                    })
                    .map(addVisitsToOwner(owner))
            );
    }

    @GetMapping(value = "owners")
    public List<OwnerDetails> getAllOwners() {
        return customersServiceClient.getAllOwners();
    }

    @PostMapping(value = "owners")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerDetails createOwner(@Valid @RequestBody OwnerDetails owner) {
        return customersServiceClient.createOwner(owner);
    }

    @PutMapping(value = "owners/{ownerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOwner(@PathVariable("ownerId") int ownerId, @Valid @RequestBody OwnerDetails ownerRequest) {
        ownerRequest.setId(ownerId);
        customersServiceClient.updateOwner(ownerRequest);
    }

    @GetMapping("/petTypes")
    public List<PetType> getPetTypes() {
        return customersServiceClient.getPetTypes();
    }

    @PostMapping("/owners/{ownerId}/pets")
    @ResponseStatus(HttpStatus.CREATED)
    public PetDetails createPet(
        @RequestBody PetRequest petRequest,
        @PathVariable("ownerId") int ownerId) {
        return customersServiceClient.createPet(petRequest, ownerId);
    }

    @PutMapping("/owners/*/pets/{petId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePet(@RequestBody PetRequest petRequest) {
        customersServiceClient.updatePet(petRequest);
    }

    @GetMapping("owners/*/pets/{petId}")
    public PetDetails findPet(@PathVariable("petId") int petId) {
        return customersServiceClient.findPet(petId);
    }

    private Function<Visits, OwnerDetails> addVisitsToOwner(OwnerDetails owner) {
        return visits -> {
            owner.getPets()
                .forEach(pet -> pet.getVisits()
                    .addAll(visits.getItems().stream()
                        .filter(v -> v.getPetId() == pet.getId())
                        .collect(Collectors.toList()))
                );
            return owner;
        };
    }

    private Mono<Visits> emptyVisitsForPets() {
        return Mono.just(new Visits());
    }
}
