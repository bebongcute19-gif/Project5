#!/usr/bin/env python3
import bcrypt

passwords = {
    "admin": "admin123",
    "user1": "password123",
    "user2": "password123",
    "user3": "password123",
    "user4": "password123",
    "user5": "password123"
}

print("BCrypt Hashes for Project5 Seed Data")
print("=" * 60)

for user, password in passwords.items():
    # Generate BCrypt hash with strength 10
    salt = bcrypt.gensalt(rounds=10)
    hash_value = bcrypt.hashpw(password.encode('utf-8'), salt)
    hash_str = hash_value.decode('utf-8')
    
    print(f"User: {user:6} | Password: {password:15} | Hash: {hash_str}")
    print()
