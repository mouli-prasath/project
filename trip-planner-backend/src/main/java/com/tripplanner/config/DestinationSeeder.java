package com.tripplanner.config;

import com.tripplanner.entity.Destination;
import com.tripplanner.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a starter catalog of real destinations so /api/explore has content
 * on first run, without requiring a manual data-loading step. Runs once -
 * skips entirely if the table already has rows (e.g. after a restart, or
 * once you replace this with real usage-driven data).
 */
@Component
@RequiredArgsConstructor
public class DestinationSeeder implements CommandLineRunner {

    private final DestinationRepository destinationRepository;

    @Override
    public void run(String... args) {
        if (destinationRepository.count() > 0) {
            return;
        }

        List<Destination> seed = List.of(
                dest("Goa", "India", "Beaches, nightlife, and Portuguese-era forts on India's west coast.", "BEACH", 15.2993, 74.1240, 95),
                dest("Kerala", "India", "Backwaters, tea plantations, and Ayurvedic wellness retreats.", "NATURE", 10.8505, 76.2711, 90),
                dest("Ooty", "India", "Hill station in the Nilgiris known for tea gardens and cool weather.", "MOUNTAIN", 11.4064, 76.6932, 75),
                dest("Manali", "India", "Himalayan adventure hub for trekking, paragliding, and snow.", "ADVENTURE", 32.2432, 77.1892, 85),
                dest("Jaipur", "India", "The Pink City - forts, palaces, and Rajasthani heritage.", "HISTORY", 26.9124, 75.7873, 88),
                dest("Varanasi", "India", "One of the world's oldest living cities, on the banks of the Ganges.", "HISTORY", 25.3176, 82.9739, 70),
                dest("Rishikesh", "India", "Yoga capital of the world, on the Ganges at the Himalayan foothills.", "ADVENTURE", 30.0869, 78.2676, 72),
                dest("Andaman Islands", "India", "Turquoise waters, coral reefs, and secluded beaches.", "BEACH", 11.7401, 92.6586, 80),
                dest("Darjeeling", "India", "Tea estates and Himalayan views in the northeast hills.", "MOUNTAIN", 27.0410, 88.2663, 68),
                dest("Udaipur", "India", "The City of Lakes - palaces, havelis, and romantic architecture.", "HISTORY", 24.5854, 73.7125, 82),
                dest("Bali", "Indonesia", "Volcanic mountains, rice paddies, beaches, and coral reefs.", "BEACH", -8.3405, 115.0920, 92),
                dest("Paris", "France", "Iconic landmarks, world-class museums, and café culture.", "CITY", 48.8566, 2.3522, 96),
                dest("Kyoto", "Japan", "Ancient temples, geisha districts, and traditional gardens.", "HISTORY", 35.0116, 135.7681, 84),
                dest("Cape Town", "South Africa", "Table Mountain, coastline, and wildlife nearby.", "NATURE", -33.9249, 18.4241, 77),
                dest("Swiss Alps", "Switzerland", "Dramatic peaks, ski resorts, and alpine villages.", "MOUNTAIN", 46.5197, 7.9640, 86),
                dest("New York City", "USA", "Skyscrapers, Broadway, and nonstop city energy.", "CITY", 40.7128, -74.0060, 93)
        );

        destinationRepository.saveAll(seed);
    }

    private Destination dest(
            String name, String country, String description, String category,
            double lat, double lng, int popularity
    ) {
        return Destination.builder()
                .name(name).country(country).description(description).category(category)
                .latitude(lat).longitude(lng).popularityScore(popularity)
                .build();
    }
}
