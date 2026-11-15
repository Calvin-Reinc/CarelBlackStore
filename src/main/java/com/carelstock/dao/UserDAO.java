package com.carelstock.dao;

import com.carelstock.database.DataAccessException;
import com.carelstock.database.DatabaseConnection;
import com.carelstock.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access for the users table.
 *
 * Contract:
 *   - "expected" outcomes are return values (duplicate username -> false, unknown user -> null)
 *   - real database failures throw DataAccessException (they used to be swallowed and look like
 *     "wrong password" / "username taken")
 *
 * @author squid
 */
public class UserDAO {

    private static final Logger LOG = Logger.getLogger(UserDAO.class.getName());

    /** BCrypt work factor. Each +1 doubles the cost. Hashes already stored at a lower cost still verify. */
    private static final int BCRYPT_COST = 12;

    private static final String INSERT_USER =
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";

    private static final String SELECT_BY_USERNAME =
            "SELECT user_id, username, password_hash, role, created_at FROM users WHERE username = ?";

    /**
     * Creates a user with a BCrypt-hashed password.
     *
     * @return true if created, false if the username is already taken
     * @throws IllegalArgumentException if username, password or role is missing
     * @throws DataAccessException      on any other database failure
     */
    public boolean createUser(String username, String plainPassword, String role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("password is required");
        }
        // No silent default: a caller that forgets the role must not quietly get an ADMIN.
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role is required");
        }

        String passwordHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_USER)) {

            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, role);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                LOG.info("createUser: username already exists");
                return false;
            }
            throw new DataAccessException("Failed to create user", e);
        }
    }

    /**
     * Looks a user up by exact username.
     *
     * @return the user, or null if there is no such user
     * @throws DataAccessException on database failure
     */
    public User getUserByUsername(String username) {
        if (username == null) {
            return null;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_USERNAME)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setRole(rs.getString("role"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                return user;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load user", e);
        }
    }

    /** Verifies a plaintext password against a stored BCrypt hash. Never throws. */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Stored value isn't a valid BCrypt hash (e.g. a plaintext password seeded by mistake).
            // Log it so it gets noticed, but never log the hash itself.
            LOG.log(Level.WARNING, "password_hash in the users table is not a valid BCrypt hash", e);
            return false;
        }
    }

    /** SQLSTATE 23505 = unique_violation (PostgreSQL). Error 1062 = duplicate entry (MySQL). */
    private static boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState()) || e.getErrorCode() == 1062;
    }
}