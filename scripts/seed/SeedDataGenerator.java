/**
 * Utility class to generate BCrypt hashes for seed data.
 * Run this class to generate password hashes for the seed SQL files.
 * 
 * Compile: javac SeedDataGenerator.java
 * Run: java -cp ".:spring-security-crypto-6.x.jar:bcprov-jdk15on-1.xx.jar" SeedDataGenerator
 * 
 * Or add Spring Security dependency to classpath and run.
 */

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SeedDataGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Test users passwords
        String[] testPasswords = {
            "admin123",      // admin
            "password123",   // user1
            "password123",   // user2
            "password123",   // user3
            "password123",   // user4
            "password123"    // user5
        };
        
        String[] usernames = {
            "admin",
            "user1",
            "user2", 
            "user3",
            "user4",
            "user5"
        };
        
        System.out.println("BCrypt Hashes for Seed Data:");
        System.out.println("============================");
        
        for (int i = 0; i < testPasswords.length; i++) {
            String hash = encoder.encode(testPasswords[i]);
            System.out.println("Username: " + usernames[i]);
            System.out.println("Password: " + testPasswords[i]);
            System.out.println("BCrypt Hash: " + hash);
            System.out.println();
        }
    }
}
