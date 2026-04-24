package com.javatraining.lombokstruct;

import com.javatraining.lombokstruct.dto.CreateUserRequest;
import com.javatraining.lombokstruct.dto.UserDto;
import com.javatraining.lombokstruct.entity.Address;
import com.javatraining.lombokstruct.entity.User;
import com.javatraining.lombokstruct.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies all mapping methods in UserMapper.
 *
 * <p>The mapper implementation (UserMapperImpl) is generated at compile time
 * by MapStruct's annotation processor and registered as a Spring @Component.
 * Injecting the interface works because Spring finds the generated implementation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MapStructMappingTest {

    @Autowired UserMapper userMapper;

    // ── requestToUser ─────────────────────────────────────────────────────────

    @Test
    void request_to_user_maps_basic_fields() {
        CreateUserRequest request = CreateUserRequest.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .role("USER")
                .build();

        User user = userMapper.requestToUser(request);

        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getRole()).isEqualTo("USER");
    }

    @Test
    void request_to_user_id_is_null() {
        // @Mapping(target = "id", ignore = true) — id is never set from the request
        User user = userMapper.requestToUser(CreateUserRequest.builder()
                .email("bob@example.com").build());
        assertThat(user.getId()).isNull();
    }

    @Test
    void request_to_user_address_is_null() {
        // @Mapping(target = "address", ignore = true) — address not in request
        User user = userMapper.requestToUser(CreateUserRequest.builder()
                .email("carol@example.com").build());
        assertThat(user.getAddress()).isNull();
    }

    // ── userToDto ─────────────────────────────────────────────────────────────

    @Test
    void user_to_dto_combines_first_and_last_name_into_full_name() {
        User user = User.builder()
                .id(1L).firstName("Alice").lastName("Smith")
                .email("alice@example.com").role("USER").build();

        UserDto dto = userMapper.userToDto(user);

        // expression = "java(user.getFirstName() + \" \" + user.getLastName())"
        assertThat(dto.getFullName()).isEqualTo("Alice Smith");
    }

    @Test
    void user_to_dto_flattens_nested_address_city() {
        User user = User.builder()
                .id(2L).firstName("Bob").lastName("Jones")
                .email("bob@example.com").role("ADMIN")
                .address(Address.builder().street("1 High St").city("London").country("UK").build())
                .build();

        UserDto dto = userMapper.userToDto(user);

        // source = "address.city" → target = "city"
        assertThat(dto.getCity()).isEqualTo("London");
    }

    @Test
    void user_to_dto_city_is_null_when_address_is_null() {
        // MapStruct generates a null-safe path: if address==null → city=null (no NPE)
        User user = User.builder()
                .id(3L).firstName("Carol").lastName("White")
                .email("carol@example.com").build();  // address not set

        UserDto dto = userMapper.userToDto(user);

        assertThat(dto.getCity()).isNull();
    }

    // ── usersToDtos (list mapping) ─────────────────────────────────────────────

    @Test
    void users_to_dtos_maps_entire_list() {
        List<User> users = List.of(
                User.builder().id(1L).firstName("Alice").lastName("A").email("a@test.com").build(),
                User.builder().id(2L).firstName("Bob").lastName("B").email("b@test.com").build()
        );

        List<UserDto> dtos = userMapper.usersToDtos(users);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getFullName()).isEqualTo("Alice A");
        assertThat(dtos.get(1).getFullName()).isEqualTo("Bob B");
    }

    // ── updateFromRequest (partial update / PATCH semantics) ──────────────────

    @Test
    void update_from_request_applies_non_null_fields() {
        User user = User.builder()
                .id(1L).firstName("Alice").lastName("Smith")
                .email("alice@example.com").role("USER").build();

        // Only email is being updated — other fields are null in the request
        CreateUserRequest patch = CreateUserRequest.builder()
                .email("alice.new@example.com")
                .build();  // firstName, lastName, role are null

        userMapper.updateFromRequest(patch, user);

        assertThat(user.getEmail()).isEqualTo("alice.new@example.com");  // updated
    }

    @Test
    void update_from_request_null_values_do_not_overwrite_existing() {
        User user = User.builder()
                .id(1L).firstName("Alice").lastName("Smith")
                .email("alice@example.com").role("ADMIN").build();

        // Only email is set; firstName, lastName, role are null
        CreateUserRequest patch = CreateUserRequest.builder()
                .email("new@example.com")
                .build();

        userMapper.updateFromRequest(patch, user);

        // NullValuePropertyMappingStrategy.IGNORE: null fields are skipped
        assertThat(user.getFirstName()).isEqualTo("Alice");    // unchanged
        assertThat(user.getLastName()).isEqualTo("Smith");     // unchanged
        assertThat(user.getRole()).isEqualTo("ADMIN");         // unchanged
    }
}
