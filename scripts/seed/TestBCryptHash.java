import java.util.Scanner;

/**
 * Test BCrypt password hashes for verification
 * Add spring-security-crypto-5.x.jar to classpath
 */
public class TestBCryptHash {
    
    public static void main(String[] args) {
        // Test hashes from seed data
        String adminHash = "$2a$10$L9.LMxzJ.sQl3mM.sKGKs.P7T1q8PWh.4bHKPaXbQZVXVzABKxRju";
        String userHash = "$2a$10$l3Y4Z8K7N2M9B6V5X1C4O.sO8A7Z6Y5X4W3V2U1T0S9R8Q7P6O5N";
        
        String adminPassword = "admin123";
        String userPassword = "password123";
        
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║       BCrypt Hash Verification for Test Accounts      ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");
        
        try {
            // Try to use Spring Security's BCryptPasswordEncoder
            Class<?> bcryptClass = Class.forName("org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder");
            Object encoder = bcryptClass.getDeclaredConstructor().newInstance();
            
            // Get the matches method
            var matchesMethod = bcryptClass.getMethod("matches", CharSequence.class, String.class);
            
            // Test admin hash
            System.out.println("[Testing Admin Account]");
            System.out.println("Password: admin123");
            System.out.println("Hash: " + adminHash.substring(0, 20) + "...");
            boolean adminMatches = (boolean) matchesMethod.invoke(encoder, adminPassword, adminHash);
            System.out.println("Result: " + (adminMatches ? "✓ MATCH" : "✗ NO MATCH"));
            
            // Test user hash
            System.out.println("\n[Testing User Account]");
            System.out.println("Password: password123");
            System.out.println("Hash: " + userHash.substring(0, 20) + "...");
            boolean userMatches = (boolean) matchesMethod.invoke(encoder, userPassword, userHash);
            System.out.println("Result: " + (userMatches ? "✓ MATCH" : "✗ NO MATCH"));
            
            if (!adminMatches || !userMatches) {
                System.out.println("\n⚠️  Hash mismatch detected! Generating correct hashes...\n");
                
                var encodeMethod = bcryptClass.getMethod("encode", CharSequence.class);
                
                String newAdminHash = (String) encodeMethod.invoke(encoder, adminPassword);
                String newUserHash = (String) encodeMethod.invoke(encoder, userPassword);
                
                System.out.println("[Generated Correct Hashes]");
                System.out.println("Admin (admin123):    " + newAdminHash);
                System.out.println("User (password123):  " + newUserHash);
                System.out.println("\nUse these hashes in identity-seed.sql");
            }
            
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Spring Security BCryptPasswordEncoder not found in classpath");
            System.out.println("Please ensure spring-security-crypto is in the classpath");
            
            // Fallback: Generate hashes manually
            System.out.println("\nℹ️  Run this from Identity Service directory:");
            System.out.println("   java -cp target/classes:target/lib/* TestBCryptHash");
            
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
