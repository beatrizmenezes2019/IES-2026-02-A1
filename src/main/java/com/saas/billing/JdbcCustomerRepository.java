package com.saas.billing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Adaptador JDBC (PostgreSQL) da porta CustomerRepository. */
public class JdbcCustomerRepository implements CustomerRepository {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/saas_db";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin123";

    @Override
    public Optional<Customer> findById(String customerId) {
        String sql = "SELECT email, status FROM customers WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Customer(rs.getString("email"), rs.getString("status")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new InfrastructureException(e.getMessage(), e);
        }
    }

    @Override
    public void saveInvoice(String customerId, Money amount, String status) {
        String sql = "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId);
            stmt.setBigDecimal(2, amount.amount());
            stmt.setString(3, status);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new InfrastructureException(e.getMessage(), e);
        }
    }
}
