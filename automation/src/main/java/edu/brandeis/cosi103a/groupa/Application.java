package edu.brandeis.cosi103a.groupa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    /**
     * Starts the Spring Boot server and prints endpoint information.
     * Announces server startup success or failure.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        try {
            SpringApplication.run(Application.class, args);
            System.out.println("=== HTTP Server Started Successfully! ===");
            System.out.println("Server is running at: http://localhost:8000");
            System.out.println("Decision endpoint: POST http://localhost:8000/decide");
            System.out.println("Log event endpoint: POST http://localhost:8000/log-event");
        } catch (Exception e) {
            System.err.println("=== HTTP Server Failed to Start! ===");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }
}