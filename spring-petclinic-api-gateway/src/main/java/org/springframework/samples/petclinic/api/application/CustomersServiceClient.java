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

import com.google.protobuf.Empty;
import java.util.ArrayList;
import java.util.List;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.PetDetails;
import org.springframework.samples.petclinic.api.dto.PetRequest;
import org.springframework.samples.petclinic.api.dto.PetType;
import org.springframework.samples.petclinic.customers.grpc.CustomersServiceGrpc.CustomersServiceBlockingStub;
import org.springframework.samples.petclinic.customers.grpc.OwnerRequest;
import org.springframework.samples.petclinic.customers.grpc.OwnerResponse;
import org.springframework.samples.petclinic.customers.grpc.Owners;
import org.springframework.samples.petclinic.customers.grpc.PetResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author Maciej Szarlinski
 */
@Component
public class CustomersServiceClient {

    @GrpcClient("customers-grpc-server")
    private CustomersServiceBlockingStub stub;

    public Mono<OwnerDetails> getOwner(final int ownerId) {
        OwnerResponse owner = stub.findOwner(OwnerRequest.newBuilder().setOwnerId(ownerId).build());
        return Mono.just(copyOwner(owner));
    }

    public List<OwnerDetails> getAllOwners() {
        List<OwnerDetails> ownerDetails = new ArrayList<>();
        Owners owners = stub.findAll(Empty.newBuilder().build());
        owners.getEleList().forEach(o -> {
            ownerDetails.add(copyOwner(o));
        });
        return ownerDetails;
    }

    public OwnerDetails createOwner(OwnerDetails ownerRequest) {
        stub.createOwner(OwnerResponse.newBuilder()
            .setId(ownerRequest.getId())
            .setFirstName(ownerRequest.getFirstName())
            .setLastName(ownerRequest.getLastName())
            .setCity(ownerRequest.getCity())
            .setAddress(ownerRequest.getAddress())
            .setTelephone(ownerRequest.getTelephone())
            .build());
        return ownerRequest;
    }

    public void updateOwner(OwnerDetails ownerRequest) {
        stub.updateOwner(OwnerResponse.newBuilder()
            .setId(ownerRequest.getId())
            .setFirstName(ownerRequest.getFirstName())
            .setLastName(ownerRequest.getLastName())
            .setCity(ownerRequest.getCity())
            .setAddress(ownerRequest.getAddress())
            .setTelephone(ownerRequest.getTelephone())
            .build());
    }

    public List<PetType> getPetTypes() {
        List<PetType> petTypes = new ArrayList<>();
        stub.getPetTypes(Empty.newBuilder().build()).getEleList().forEach(type -> {
            petTypes.add(copyPetType(type));
        });
        return petTypes;
    }

    public PetDetails createPet(PetRequest petRequest, int ownerId) {
        PetResponse pet = PetResponse.newBuilder()
            .setId(petRequest.getId())
            .setName(petRequest.getName())
            .setType(org.springframework.samples.petclinic.customers.grpc.PetType.newBuilder()
                .setId(petRequest.getTypeId()).build())
            .setBirthDate(DateFormatUtils.format(petRequest.getBirthDate(), "yyyy-MM-dd"))
            .setOwner(OwnerResponse.newBuilder().setId(ownerId).build())
            .build();
        pet = stub.createPet(pet);

        PetDetails petDetails = new PetDetails();
        BeanUtils.copyProperties(pet, petDetails);
        return petDetails;
    }

    public void updatePet(PetRequest petRequest) {
        PetResponse pet = PetResponse.newBuilder()
            .setId(petRequest.getId())
            .setName(petRequest.getName())
            .setType(org.springframework.samples.petclinic.customers.grpc.PetType.newBuilder()
                .setId(petRequest.getTypeId()).build())
            .setBirthDate(DateFormatUtils.format(petRequest.getBirthDate(), "yyyy-MM-dd"))
            .build();
        stub.updatePet(pet);
    }

    public PetDetails findPet(int petId) {
        PetResponse petResponse = stub.findPet(org.springframework.samples.petclinic.customers.grpc.PetRequest.newBuilder()
            .setPetId(petId).build());
        return copyPet(petResponse);
    }

    private OwnerDetails copyOwner(OwnerResponse owner) {
        OwnerDetails ownerDetails = new OwnerDetails();
        BeanUtils.copyProperties(owner, ownerDetails);
        owner.getPetsList().forEach(p -> {
            ownerDetails.getPets().add(copyPet(p));
        });
        return ownerDetails;
    }

    private PetDetails copyPet(PetResponse petResponse) {
        PetDetails petDetails = new PetDetails();
        BeanUtils.copyProperties(petResponse, petDetails);
        petDetails.setType(copyPetType(petResponse.getType()));
        petDetails.setOwner(petResponse.getOwner().getFirstName() + " " + petResponse.getOwner().getLastName());
        return petDetails;
    }

    private PetType copyPetType(org.springframework.samples.petclinic.customers.grpc.PetType type) {
        PetType petType = new PetType();
        BeanUtils.copyProperties(type, petType);
        return petType;
    }
}
