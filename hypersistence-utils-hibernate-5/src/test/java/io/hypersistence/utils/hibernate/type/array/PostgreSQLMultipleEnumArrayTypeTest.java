package io.hypersistence.utils.hibernate.type.array;

import io.hypersistence.utils.hibernate.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.util.transaction.JPATransactionFunction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLMultipleEnumArrayTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                UserAccount.class
        };
    }

    @Before
    public void init() {
        DataSource dataSource = newDataSource();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.executeUpdate("DROP TYPE IF EXISTS user_role;");
                statement.executeUpdate("CREATE TYPE user_role AS ENUM ('ROLE_ADMIN', 'ROLE_USER');");
            } finally {
                if (statement!=null){
                    statement.close();
                }
            }
            try {
                statement = connection.createStatement();
                statement.executeUpdate("DROP TYPE IF EXISTS user_type;");
                statement.executeUpdate("CREATE TYPE user_type AS ENUM ('SUPER_USER', 'REGULAR');");
            } finally {
                if (statement!=null){
                    statement.close();
                }
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        } finally {
            if (connection!=null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    fail(e.getMessage());
                }
            }
        }
        super.init();
    }

    @Test
    public void test() {
        final UserRole[] userRoles = {
                UserRole.ROLE_ADMIN,
                UserRole.ROLE_USER
        };

        final UserType[] userTypes = {
                UserType.SUPER_USER,
                UserType.REGULAR
        };

        doInJPA(new JPATransactionFunction<Void>() {
            @Override
            public Void apply(EntityManager entityManager) {
                UserAccount account = new UserAccount();
                account.setUsername("vladmihalcea.com");
                account.setRoles(userRoles);
                account.setTypes(userTypes);
                entityManager.persist(account);
                return null;
            }
        });

        doInJPA(new JPATransactionFunction<Void>() {
            @Override
            public Void apply(EntityManager entityManager) {
                UserAccount singleResult = entityManager
                .createQuery(
                    "select ua " +
                    "from UserAccountEntity ua " +
                    "where ua.username = :username", UserAccount.class)
                .setParameter("username", "vladmihalcea.com")
                .getSingleResult();

                assertArrayEquals(userRoles, singleResult.getRoles());
                assertArrayEquals(userTypes, singleResult.getTypes());
                return null;
            }
        });
    }

    public enum UserRole {
        ROLE_ADMIN,
        ROLE_USER,
    }

    public enum UserType {
        SUPER_USER,
        REGULAR,
    }

    @TypeDefs({
            @TypeDef(
                    name = "pgsql_array",
                    typeClass = EnumArrayType.class
            )

    })
    @Entity(name = "UserAccountEntity")
    @Table(name = "users")
    public static class UserAccount {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String username;

        @Type(
            type = "pgsql_array",
            parameters = @org.hibernate.annotations.Parameter(
                name = "sql_array_type",
                value = "user_role"
            )
        )
        @Column(
            name = "roles",
            columnDefinition = "user_role[]"
        )
        private UserRole roles[];

        @Type(
            type = "pgsql_array",
            parameters = @org.hibernate.annotations.Parameter(
                name = "sql_array_type",
                value = "user_type"
            )
        )
        @Column(
            name = "types",
            columnDefinition = "user_type[]"
        )
        private UserType types[];

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public UserRole[] getRoles() {
            return roles;
        }

        public void setRoles(UserRole[] roles) {
            this.roles = roles;
        }

        public UserType[] getTypes() {
            return types;
        }

        public void setTypes(UserType[] types) {
            this.types = types;
        }
    }
}
