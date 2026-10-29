package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.AdoptionRecord;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AdoptionRecordRepositoryTest {

    @Autowired
    private AdoptionRecordRepository adoptionRecordRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findRecentAdoptionsByAnimalType_shouldFilterByTypeAndDate() {
        Animal dog = entityManager.persist(newAnimal("Rex", AnimalType.DOG));
        Animal cat = entityManager.persist(newAnimal("Mia", AnimalType.CAT));

        entityManager.persist(newRecord(dog, "recent-dog@example.com", LocalDateTime.of(2026, 3, 1, 10, 0)));
        entityManager.persist(newRecord(dog, "old-dog@example.com", LocalDateTime.of(2025, 6, 1, 10, 0)));
        entityManager.persist(newRecord(cat, "recent-cat@example.com", LocalDateTime.of(2026, 3, 1, 10, 0)));
        entityManager.flush();

        List<AdoptionRecord> result = adoptionRecordRepository.findRecentAdoptionsByAnimalType(
                AnimalType.DOG, LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAdopterEmail()).isEqualTo("recent-dog@example.com");
    }

    private Animal newAnimal(String name, AnimalType type) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setType(type);
        animal.setStatus(AnimalStatus.ADOPTED);
        return animal;
    }

    private AdoptionRecord newRecord(Animal animal, String email, LocalDateTime adoptedAt) {
        AdoptionRecord record = new AdoptionRecord();
        record.setAnimal(animal);
        record.setAdopterEmail(email);
        record.setAdoptedAt(adoptedAt);
        return record;
    }
}