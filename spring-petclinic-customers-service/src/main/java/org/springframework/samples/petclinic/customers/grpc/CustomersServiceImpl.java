package org.springframework.samples.petclinic.customers.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.samples.petclinic.customers.model.Pet;
import org.springframework.samples.petclinic.customers.model.PetRepository;
import org.springframework.samples.petclinic.customers.web.ResourceNotFoundException;

@GrpcService
@Slf4j
public class CustomersServiceImpl extends CustomersServiceGrpc.CustomersServiceImplBase {
    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Override
    public void createOwner(OwnerResponse request, StreamObserver<OwnerResponse> responseObserver) {
        Owner owner = new Owner();
        BeanUtils.copyProperties(request, owner);
        ownerRepository.save(owner);

        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }

    @Override
    public void findOwner(OwnerRequest request, StreamObserver<OwnerResponse> responseObserver) {
        ownerRepository.findById(request.getOwnerId())
            .ifPresent(owner -> responseObserver.onNext(OwnerResponse.newBuilder()
                .setId(owner.getId())
                .setAddress(owner.getAddress())
                .setCity(owner.getCity())
                .setFirstName(owner.getFirstName())
                .setLastName(owner.getLastName())
                .setTelephone(owner.getTelephone())
                .addAllPets(processPets(owner))
                .build()));

        responseObserver.onCompleted();
    }

    @Override
    public void findAll(Empty request, StreamObserver<Owners> responseObserver) {
        List<Owner> owners = ownerRepository.findAll();
        List<OwnerResponse> result = new ArrayList<>();
        owners.forEach(o -> {
            result.add(OwnerResponse.newBuilder()
                .setId(o.getId())
                .setAddress(o.getAddress())
                .setCity(o.getCity())
                .setFirstName(o.getFirstName())
                .setLastName(o.getLastName())
                .setTelephone(o.getTelephone())
                .addAllPets(processPets(o))
                .build());
        });
        responseObserver.onNext(Owners.newBuilder().addAllEle(result).build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateOwner(OwnerResponse request, StreamObserver<Empty> responseObserver) {
        final Optional<Owner> owner = ownerRepository.findById(request.getId());

        final Owner ownerModel = owner.orElseThrow(() -> new ResourceNotFoundException("Owner "+ request.getId() +" not found"));
        // This is done by hand for simplicity purpose. In a real life use-case we should consider using MapStruct.
        ownerModel.setFirstName(request.getFirstName());
        ownerModel.setLastName(request.getLastName());
        ownerModel.setCity(request.getCity());
        ownerModel.setAddress(request.getAddress());
        ownerModel.setTelephone(request.getTelephone());
        log.info("Saving owner {}", ownerModel);
        ownerRepository.save(ownerModel);

        responseObserver.onCompleted();
    }

    @Override
    public void getPetTypes(Empty request, StreamObserver<PetTypes> responseObserver) {
        List<org.springframework.samples.petclinic.customers.model.PetType> petTypes = petRepository.findPetTypes();
        List<PetType> petTypeList = new ArrayList<>();
        petTypes.forEach(o -> {
            petTypeList.add(PetType.newBuilder()
                .setId(o.getId())
                .setName(o.getName())
                .build());
        });

        responseObserver.onNext(PetTypes.newBuilder().addAllEle(petTypeList).build());
        responseObserver.onCompleted();
    }

    @Override
    public void createPet(PetResponse request, StreamObserver<PetResponse> responseObserver) {
        final Pet pet = new Pet();
        final Optional<Owner> optionalOwner = ownerRepository.findById(request.getOwner().getId());
        Owner owner = optionalOwner.orElseThrow(() ->
            new ResourceNotFoundException("Owner "+ request.getOwner().getId() +" not found"));
        owner.addPet(pet);

        savePet(pet, request);
        responseObserver.onNext(PetResponse.newBuilder()
            .setId(pet.getId())
            .setBirthDate(DateFormatUtils.format(pet.getBirthDate(), "yyyy-MM-dd"))
            .setName(pet.getName())
            .setType(PetType.newBuilder().setId(pet.getType().getId()).setName(pet.getType().getName()).build())
            .build());
        responseObserver.onCompleted();
    }

    @Override
    public void updatePet(PetResponse request, StreamObserver<Empty> responseObserver) {
        int petId = request.getId();
        Pet pet = findPetById(petId);
        savePet(pet, request);

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void findPet(PetRequest request, StreamObserver<PetResponse> responseObserver) {
        Pet pet = findPetById(request.getPetId());

        responseObserver.onNext(PetResponse.newBuilder()
            .setId(pet.getId())
            .setBirthDate(DateFormatUtils.format(pet.getBirthDate(), "yyyy-MM-dd"))
            .setName(pet.getName())
            .setType(PetType.newBuilder().setId(pet.getType().getId()).setName(pet.getType().getName()).build())
            .setOwner(OwnerResponse.newBuilder()
                .setId(pet.getOwner().getId())
                .setAddress(pet.getOwner().getAddress())
                .setCity(pet.getOwner().getCity())
                .setFirstName(pet.getOwner().getFirstName())
                .setLastName(pet.getOwner().getLastName())
                .setTelephone(pet.getOwner().getTelephone())
                .build())
            .build());
        responseObserver.onCompleted();
    }

    private Pet findPetById(int petId) {
        Optional<Pet> pet = petRepository.findById(petId);
        if (!pet.isPresent()) {
            throw new ResourceNotFoundException("Pet "+petId+" not found");
        }
        return pet.get();
    }

    private Pet savePet(final Pet pet, final PetResponse petRequest) {

        pet.setName(petRequest.getName());
        try {
            pet.setBirthDate(DateUtils.parseDate(petRequest.getBirthDate(), new String[]{"yyyy-MM-dd"}));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        petRepository.findPetTypeById(petRequest.getType().getId())
            .ifPresent(pet::setType);

        log.info("Saving pet {}", pet);
        return petRepository.save(pet);
    }

    private List<PetResponse> processPets(Owner owner) {
        log.info("Pets: " + owner.getPets().size() + "#" + owner.getPets());
        List<PetResponse> petResponses = new ArrayList<>();
        owner.getPets().forEach(p -> {
            petResponses.add(PetResponse.newBuilder()
                .setId(p.getId())
                .setBirthDate(DateFormatUtils.format(p.getBirthDate(), "yyyy-MM-dd"))
                .setName(p.getName())
                .setType(PetType.newBuilder().setId(p.getType().getId()).setName(p.getType().getName()).build())
                .build());
        });
        return petResponses;
    }
}
