package com.example.exam;

import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class OnlineExamApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineExamApplication.class, args);
	}

	@Bean
	CommandLineRunner createAdminUser(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder) {

		return args -> {


			if (userRepository.findByUsername("admin").isEmpty()) {

				User admin = new User();
				admin.setUsername("admin");

				// ✅ PASSWORD
				admin.setPassword(passwordEncoder.encode("adminpass"));

				admin.setRole("ROLE_ADMIN");
				admin.setFullName("System Admin");

				userRepository.save(admin);

				System.out.println("✅ ADMIN CREATED: admin / adminpass");
			}
		};
	}
}