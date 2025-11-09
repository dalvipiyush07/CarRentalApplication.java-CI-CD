package com.example.carrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Entity
class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String name;
    
    private boolean available = true;
    
    public Car() {}
    
    public Car(String name) {
        this.name = name;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}

@Entity
class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String customerName;
    
    @NotNull
    private Long carId;
    
    private String carName;
    
    @NotNull
    private LocalDate startDate;
    
    @NotNull
    private LocalDate endDate;
    
    public Booking() {}
    
    public Booking(String customerName, Long carId, String carName, LocalDate startDate, LocalDate endDate) {
        this.customerName = customerName;
        this.carId = carId;
        this.carName = carName;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }
    public String getCarName() { return carName; }
    public void setCarName(String carName) { this.carName = carName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}

@Repository
interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByAvailableTrue();
}

@Repository
interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByOrderByIdDesc();
}

@Controller
class CarRentalController {
    private final CarRepository carRepository;
    private final BookingRepository bookingRepository;
    
    public CarRentalController(CarRepository carRepository, BookingRepository bookingRepository) {
        this.carRepository = carRepository;
        this.bookingRepository = bookingRepository;
    }
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("cars", carRepository.findByAvailableTrue());
        return "home";
    }
    
    @PostMapping("/book")
    public String bookCar(@RequestParam String name, 
                         @RequestParam Long carId,
                         @RequestParam LocalDate startDate,
                         @RequestParam LocalDate endDate,
                         Model model) {
        
        if (startDate.isAfter(endDate)) {
            model.addAttribute("error", "Start date must be before or equal to end date");
            model.addAttribute("cars", carRepository.findByAvailableTrue());
            return "home";
        }
        
        Car car = carRepository.findById(carId).orElse(null);
        if (car != null) {
            car.setAvailable(false);
            carRepository.save(car);
            
            Booking booking = new Booking(name, carId, car.getName(), startDate, endDate);
            bookingRepository.save(booking);
            
            model.addAttribute("message", "Booking successful for " + car.getName());
        } else {
            model.addAttribute("error", "Car not found");
        }
        
        model.addAttribute("cars", carRepository.findByAvailableTrue());
        return "home";
    }
    
    @GetMapping("/admin/bookings")
    public String adminBookings(Model model) {
        model.addAttribute("bookings", bookingRepository.findAllByOrderByIdDesc());
        return "admin";
    }
}

@SpringBootApplication
public class CarRentalApplication {
    
    @Bean
    CommandLineRunner initData(CarRepository carRepository) {
        return args -> {
            if (carRepository.count() == 0) {
                carRepository.save(new Car("Honda City"));
                carRepository.save(new Car("Maruti Swift"));
                carRepository.save(new Car("Mahindra Scorpio"));
            }
        };
    }
    
    public static void main(String[] args) {
        SpringApplication.run(CarRentalApplication.class, args);
    }
}

// Thymeleaf templates as Java strings
@Controller
class TemplateController {
    
    @GetMapping(value = "/home", produces = "text/html")
    public String homeTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Car Rental</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    .container { max-width: 800px; margin: 0 auto; }
                    .section { margin-bottom: 30px; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
                    .error { color: red; margin: 10px 0; }
                    .success { color: green; margin: 10px 0; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
                    input, button { padding: 8px; margin: 5px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Car Rental System</h1>
                    
                    <div class="section">
                        <h2>Available Cars</h2>
                        <table>
                            <thead>
                                <tr><th>ID</th><th>Name</th></tr>
                            </thead>
                            <tbody>
                                <tr th:each="car : ${cars}">
                                    <td th:text="${car.id}"></td>
                                    <td th:text="${car.name}"></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    
                    <div class="section">
                        <h2>Book a Car</h2>
                        <div th:if="${error}" class="error" th:text="${error}"></div>
                        <div th:if="${message}" class="success" th:text="${message}"></div>
                        <form method="post" action="/book">
                            <div>
                                <input type="text" name="name" placeholder="Your Name" required>
                            </div>
                            <div>
                                <input type="number" name="carId" placeholder="Car ID" required>
                            </div>
                            <div>
                                <input type="date" name="startDate" required>
                            </div>
                            <div>
                                <input type="date" name="endDate" required>
                            </div>
                            <button type="submit">Book Car</button>
                        </form>
                    </div>
                    
                    <div class="section">
                        <a href="/admin/bookings">View All Bookings (Admin)</a>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
    
    @GetMapping(value = "/admin", produces = "text/html")
    public String adminTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Admin - Bookings</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                    th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
                    .container { max-width: 1000px; margin: 0 auto; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>All Bookings</h1>
                    <a href="/">← Back to Home</a>
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Customer</th>
                                <th>Car ID</th>
                                <th>Car Name</th>
                                <th>Start Date</th>
                                <th>End Date</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr th:each="booking : ${bookings}">
                                <td th:text="${booking.id}"></td>
                                <td th:text="${booking.customerName}"></td>
                                <td th:text="${booking.carId}"></td>
                                <td th:text="${booking.carName}"></td>
                                <td th:text="${booking.startDate}"></td>
                                <td th:text="${booking.endDate}"></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </body>
            </html>
            """;
    }
}