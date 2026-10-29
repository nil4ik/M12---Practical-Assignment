package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.audit.AuditLogger;
import lv.bootcamp.shelter.audit.RejectionReason;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.model.Adopter;
import lv.bootcamp.shelter.model.AdoptionResult;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AdopterRepository;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdoptionEligibilityServiceTest {

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdoptionEligibilityService service;

    // ---------- rejections without audit ----------

    @Test
    void evaluate_shouldRejectWhenAdopterNotFound() {
        when(adopterRepository.findById(1L)).thenReturn(Optional.empty());

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.ADOPTER_NOT_FOUND);
        verifyNoInteractions(auditLogger, notificationClient);
    }

    @Test
    void evaluate_shouldRejectWhenAnimalNotFound() {
        when(adopterRepository.findById(1L)).thenReturn(Optional.of(validAdopter()));
        when(animalRepository.findById(2L)).thenReturn(Optional.empty());

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.ANIMAL_NOT_FOUND);
        verifyNoInteractions(auditLogger, notificationClient);
    }

    @Test
    void evaluate_shouldRejectWhenAnimalNotAvailable() {
        Animal animal = availableDog();
        animal.setStatus(AnimalStatus.ADOPTED);
        givenAdopterAndAnimal(validAdopter(), animal);

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.ANIMAL_NOT_AVAILABLE);
        verifyNoInteractions(auditLogger, notificationClient);
    }

    // ---------- rejections with audit ----------

    @Test
    void evaluate_shouldRejectUnderageAdopter() {
        Adopter adopter = validAdopter();
        adopter.setAge(17);
        givenAdopterAndAnimal(adopter, availableDog());

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.UNDERAGE);
        verify(auditLogger).logRejection(1L, 2L, RejectionReason.UNDERAGE);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluate_shouldRejectWhenPetLimitReachedOnNormalProperty() {
        Adopter adopter = validAdopter();
        adopter.setCurrentPetCount(3);
        givenAdopterAndAnimal(adopter, availableDog());

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.PET_LIMIT_REACHED);
        verify(auditLogger).logRejection(1L, 2L, RejectionReason.PET_LIMIT_REACHED);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluate_shouldRejectWhenPetLimitReachedOnLargeProperty() {
        Adopter adopter = validAdopter();
        adopter.setLargeProperty(true);
        adopter.setCurrentPetCount(5);
        givenAdopterAndAnimal(adopter, availableDog());

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.PET_LIMIT_REACHED);
        verify(auditLogger).logRejection(1L, 2L, RejectionReason.PET_LIMIT_REACHED);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluate_shouldRejectBirdWithoutExoticPermit() {
        Animal bird = availableDog();
        bird.setType(AnimalType.BIRD);
        givenAdopterAndAnimal(validAdopter(), bird);

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.EXOTIC_PERMIT_REQUIRED);
        verify(auditLogger).logRejection(1L, 2L, RejectionReason.NO_EXOTIC_PERMIT);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluate_shouldRejectRabbitWithoutExoticPermit() {
        Animal rabbit = availableDog();
        rabbit.setType(AnimalType.RABBIT);
        givenAdopterAndAnimal(validAdopter(), rabbit);

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.EXOTIC_PERMIT_REQUIRED);
        verify(auditLogger).logRejection(1L, 2L, RejectionReason.NO_EXOTIC_PERMIT);
        verifyNoInteractions(notificationClient);
    }

    // ---------- approvals at the boundaries ----------

    @Test
    void evaluate_shouldApproveAdopterAgedExactly18() {
        Adopter adopter = validAdopter();
        adopter.setAge(18);
        givenAdopterAndAnimal(adopter, availableDog());

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
    }

    @Test
    void evaluate_shouldApproveWithTwoPetsOnNormalProperty() {
        Adopter adopter = validAdopter();
        adopter.setCurrentPetCount(2);
        givenAdopterAndAnimal(adopter, availableDog());

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
    }

    @Test
    void evaluate_shouldApproveWithFourPetsOnLargeProperty() {
        Adopter adopter = validAdopter();
        adopter.setLargeProperty(true);
        adopter.setCurrentPetCount(4);
        givenAdopterAndAnimal(adopter, availableDog());

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
    }

    @Test
    void evaluate_shouldApproveBirdWhenAdopterHasExoticPermit() {
        Adopter adopter = validAdopter();
        adopter.setExoticPermit(true);
        Animal bird = availableDog();
        bird.setType(AnimalType.BIRD);
        givenAdopterAndAnimal(adopter, bird);

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
    }

    @Test
    void evaluate_shouldLogApprovalAndNotifyAdopterWithScore() {
        Adopter adopter = validAdopter();
        adopter.setPreviousAdoptions(1);
        givenAdopterAndAnimal(adopter, availableDog());

        AdoptionResult result = service.evaluateAdoption(1L, 2L);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(10);
        verify(auditLogger).logApproval(1L, 2L, 10);
        verify(notificationClient).sendApprovalNotification("anna@example.com", "Rex");
    }

    // ---------- priority score ----------

    @Test
    void score_shouldBeZeroForBaselineAdopter() {
        assertThat(service.calculatePriorityScore(validAdopter(), availableDog())).isEqualTo(0);
    }

    @Test
    void score_shouldAdd10ForOnePreviousAdoption() {
        Adopter adopter = validAdopter();
        adopter.setPreviousAdoptions(1);

        assertThat(service.calculatePriorityScore(adopter, availableDog())).isEqualTo(10);
    }

    @Test
    void score_shouldNotAddBonusForExactlyThreePreviousAdoptions() {
        Adopter adopter = validAdopter();
        adopter.setPreviousAdoptions(3);

        assertThat(service.calculatePriorityScore(adopter, availableDog())).isEqualTo(10);
    }

    @Test
    void score_shouldAdd15ForFourPreviousAdoptions() {
        Adopter adopter = validAdopter();
        adopter.setPreviousAdoptions(4);

        assertThat(service.calculatePriorityScore(adopter, availableDog())).isEqualTo(15);
    }

    @Test
    void score_shouldAdd15ForLargeProperty() {
        Adopter adopter = validAdopter();
        adopter.setLargeProperty(true);

        assertThat(service.calculatePriorityScore(adopter, availableDog())).isEqualTo(15);
    }

    @Test
    void score_shouldNotAddSeniorBonusForAnimalAgedExactly7() {
        Animal animal = availableDog();
        animal.setAge(7);

        assertThat(service.calculatePriorityScore(validAdopter(), animal)).isEqualTo(0);
    }

    @Test
    void score_shouldAdd20ForAnimalAged8() {
        Animal animal = availableDog();
        animal.setAge(8);

        assertThat(service.calculatePriorityScore(validAdopter(), animal)).isEqualTo(20);
    }

    @Test
    void score_shouldIgnoreAnimalWithUnknownAge() {
        Animal animal = availableDog();
        animal.setAge(null);

        assertThat(service.calculatePriorityScore(validAdopter(), animal)).isEqualTo(0);
    }

    @Test
    void score_shouldSubtractTwoPerCurrentPet() {
        Adopter adopter = validAdopter();
        adopter.setPreviousAdoptions(1);
        adopter.setCurrentPetCount(2);

        assertThat(service.calculatePriorityScore(adopter, availableDog())).isEqualTo(6);
    }

    @Test
    void score_shouldNeverBeNegative() {
        Adopter adopter = validAdopter();
        adopter.setCurrentPetCount(3);

        assertThat(service.calculatePriorityScore(adopter, availableDog())).isEqualTo(0);
    }

    // ---------- helpers ----------

    private Adopter validAdopter() {
        Adopter adopter = new Adopter();
        adopter.setId(1L);
        adopter.setName("Anna");
        adopter.setEmail("anna@example.com");
        adopter.setAge(30);
        return adopter;
    }

    private Animal availableDog() {
        Animal animal = new Animal();
        animal.setId(2L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setAge(3);
        return animal;
    }

    private void givenAdopterAndAnimal(Adopter adopter, Animal animal) {
        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(2L)).thenReturn(Optional.of(animal));
    }
}